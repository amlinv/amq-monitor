/*
 * Copyright 2015 AML Innovation & Consulting LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amlinv.activemq.monitor.web;

import com.amlinv.activemq.monitor.TopologyStateFactory;
import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPollerFactory;
import com.amlinv.activemq.monitor.activemq.impl.DefaultActiveMQBrokerPollerFactory;
import com.amlinv.activemq.topo.discovery.MBeanDestinationDiscovererScheduler;
import com.amlinv.activemq.topo.jmxutil.polling.JmxActiveMQUtil2;
import com.amlinv.activemq.topo.registry.BrokerRegistry;
import com.amlinv.activemq.topo.registry.BrokerTopologyRegistry;
import com.amlinv.activemq.topo.registry.DestinationRegistry;
import com.amlinv.activemq.topo.registry.model.BrokerInfo;
import com.amlinv.activemq.topo.registry.model.LocatedBrokerId;
import com.amlinv.activemq.topo.registry.model.TopologyInfo;
import com.amlinv.activemq.topo.registry.model.TopologyState;
import com.amlinv.javasched.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by art on 3/31/15.
 */
@Path("/monitor")
public class MonitorWebController {
    public static final String DEFAULT_TOPOLOGY_NAME = "unnamed";

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MonitorWebController.class);
    private Logger log = DEFAULT_LOGGER;

    private Map<String, MBeanDestinationDiscovererScheduler> queueDiscoverers;
    private AtomicBoolean started = new AtomicBoolean(false);

    private boolean autoStart = true;
    private boolean autoDiscoverQueues = true;


    ///// REQUIRED INJECTIONS
    private MonitorWebsocketBrokerStatsFeed websocketBrokerStatsFeed;
    private Scheduler scheduler;
    private BrokerTopologyRegistry brokerTopologyRegistry;
    private TopologyStateFactory topologyStateFactory;


    ///// OPTIONAL INJECTIONS
    private ActiveMQBrokerPollerFactory brokerPollerFactory = new DefaultActiveMQBrokerPollerFactory();
    private JmxActiveMQUtil2 jmxActiveMQUtil = new JmxActiveMQUtil2();


    /////
    ///// PUBLIC INTERFACE
    /////
    public MonitorWebController() {
        this.queueDiscoverers = new HashMap<>();
    }

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public MonitorWebsocketBrokerStatsFeed getWebsocketBrokerStatsFeed() {
        return websocketBrokerStatsFeed;
    }

    public void setWebsocketBrokerStatsFeed(MonitorWebsocketBrokerStatsFeed websocketBrokerStatsFeed) {
        this.websocketBrokerStatsFeed = websocketBrokerStatsFeed;
    }

    public BrokerTopologyRegistry getBrokerTopologyRegistry() {
        return brokerTopologyRegistry;
    }

    public void setBrokerTopologyRegistry(BrokerTopologyRegistry brokerTopologyRegistry) {
        this.brokerTopologyRegistry = brokerTopologyRegistry;
    }

    public TopologyStateFactory getTopologyStateFactory() {
        return topologyStateFactory;
    }

    public void setTopologyStateFactory(TopologyStateFactory topologyStateFactory) {
        this.topologyStateFactory = topologyStateFactory;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public boolean isAutoDiscoverQueues() {
        return autoDiscoverQueues;
    }

    public void setAutoDiscoverQueues(boolean autoDiscoverQueues) {
        this.autoDiscoverQueues = autoDiscoverQueues;
    }

    public ActiveMQBrokerPollerFactory getBrokerPollerFactory() {
        return brokerPollerFactory;
    }

    public void setBrokerPollerFactory(ActiveMQBrokerPollerFactory brokerPollerFactory) {
        this.brokerPollerFactory = brokerPollerFactory;
    }

    public JmxActiveMQUtil2 getJmxActiveMQUtil() {
        return jmxActiveMQUtil;
    }

    public void setJmxActiveMQUtil(JmxActiveMQUtil2 jmxActiveMQUtil) {
        this.jmxActiveMQUtil = jmxActiveMQUtil;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void init () {
        log.info("Initializing monitor web controller");
        // TBD: clean this up.  Either re-enable manual start/stop, or elminate this code
//        if ( this.autoStart ) {
//            log.info("Starting monitoring now");
//            this.startMonitoring();
//        }
    }

    // TBD: make sure nothing new gets added during shutdown
    public void shutdown () {
        synchronized ( this.queueDiscoverers ) {
            for (MBeanDestinationDiscovererScheduler oneDiscovererScheduler : this.queueDiscoverers.values()) {
                oneDiscovererScheduler.stop();
            }
        }
    }

    @GET
    @Path("/topologies")
    public List<TopologyInfo> listTopologies() {
        log.debug("listTopologies");

        List<TopologyInfo> result = new LinkedList<>();

        for (TopologyState topologyState : this.brokerTopologyRegistry.values()) {
            result.add(topologyState.getTopologyInfo());
        }

        return result;
    }

    @PUT
    @Path("/topology")
    public String addTopology (@FormParam("topology") String topology) {
        return this.performTopologyAdd(topology);
    }

    @DELETE
    @Path("/topology")
    public String removeTopology(@FormParam("topology") String topology) {
        return this.performTopologyRemoval(topology);
    }

    @GET
    @Path("/brokers")
    public List<BrokerInfo> listMonitoredBrokers(@DefaultValue(DEFAULT_TOPOLOGY_NAME)
                                                 @HeaderParam("topology") String topology) {
        log.debug("listMonitoredBrokers");

        List<BrokerInfo> result = new LinkedList<BrokerInfo>();

        TopologyState topologyState = this.brokerTopologyRegistry.get(topology);

        if (topologyState != null) {
            result.addAll(topologyState.getBrokerRegistry().values());
        }

        return result;
    }

    @PUT
    @Path("/broker")
    @Produces({ "application/json", "application/xml", "text/plain" })
    @Consumes({ "application/json", "application/xml", "application/x-www-form-urlencoded" })
    public String addBroker (@FormParam("brokerName") String brokerName,
                             @FormParam("address") String address,
                             @DefaultValue(DEFAULT_TOPOLOGY_NAME) @FormParam("topology") String topology)
            throws Exception {

        //
        // Automatically create the default topology, if needed.
        //
        this.checkAutoCreateDefaultTopology(topology);

        return this.performBrokerAdd(address, brokerName, topology);
    }

    @DELETE
    @Path("/broker")
    @Produces("text/plain")
    public String removeBrokerForm (@FormParam("brokerName") String brokerName,
                                    @FormParam("address") @QueryParam("address") String address,
                                    @DefaultValue(DEFAULT_TOPOLOGY_NAME) @FormParam("topology") String topology) {
        String result;

        result = performBrokerRemoval(address, brokerName, topology);

        return  result;
    }

    @PUT
    @Path("/queue")
    @Consumes({ "application/json", "application/xml", "application/x-www-form-urlencoded" })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response addQueue (@FormParam("queueName") String queueName,
                              @DefaultValue("*") @FormParam("brokerName") String queryBroker,
                              @DefaultValue("*") @FormParam("address") String address,
                              @DefaultValue(DEFAULT_TOPOLOGY_NAME) @FormParam("topology") String topology
    ) throws Exception {

        Response response = Response.ok("ignored: only auto-discovery of queues supported at this time").build();

        return  response;
    }

    @DELETE
    @Path("/queue")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response removeQueue (@FormParam("queueName") String queueName,
                            @DefaultValue("*") @FormParam("brokerName") String queryBroker,
                            @DefaultValue("*") @FormParam("address") String address,
                            @DefaultValue(DEFAULT_TOPOLOGY_NAME) @FormParam("topology") String topology
    ) throws Exception {

        Response response = Response.ok("ignored: only auto-discovery of queue supported at this time").build();

        return  response;
    }

    /**
     * This method is a no-op as monitoring now always automatically starts.  This may change back in the future.
     *
     * @return simple text indication of the result
     * @throws Exception on failure to start monitoring
     */
    @GET
    @Path("/start")
    @Produces("text/plain")
    public String requestStartMonitoring() throws Exception {
        return  "no-op";
    }

    @GET
    @Path("/queryBrokers")
    @Produces({ "application/json", "application/xml" })
    public String[] queryBrokerNames(@QueryParam("address") String address) throws Exception {
        return  this.jmxActiveMQUtil.queryBrokerNames(address);
    }

    protected void checkAutoCreateDefaultTopology(String topology) {
        //
        // Create the default topology, if it doesn't yet exist.
        //
        if (DEFAULT_TOPOLOGY_NAME.equals(topology)) {
            this.performTopologyAdd(DEFAULT_TOPOLOGY_NAME);
        }
    }

    protected String performTopologyAdd(String topologyName) {
        TopologyInfo info =
                new TopologyInfo(topologyName, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);

        TopologyState topologyState = this.topologyStateFactory.createTopologyState(info);
        topologyState.setBrokerRegistry(new BrokerRegistry());
        topologyState.setQueueRegistry(new DestinationRegistry());
        topologyState.setTopicRegistry(new DestinationRegistry());
        topologyState.setTopologyInfo(info);

        TopologyState existing = this.brokerTopologyRegistry.putIfAbsent(topologyName, topologyState);

        log.debug("add of topology complete: topologyName={}; alreadyExistsInd={}", topologyName, (existing != null));

        if (existing != null)
            return "already exists";
        else
            return "added";
    }

    protected String performTopologyRemoval(String topology) {
        TopologyState topologyState = this.brokerTopologyRegistry.remove(topology);

        log.debug("removal of topology complete: topologyName={}; removedInd={}", topology, (topologyState != null));

        if (topologyState != null) {
            return "removed";
        } else {
            return "not found";
        }
    }

    protected String performBrokerAdd(String address, String brokerName, String topology) throws Exception {
        String result;

        if (brokerName.equals("*")) {
            brokerName = this.lookupBrokerName(address);

            log.debug("broker lookup result: address={}; brokerName={}", address, brokerName);
        }

        TopologyState topologyState = this.brokerTopologyRegistry.get(topology);
        if (topologyState != null) {
            LocatedBrokerId locatedBrokerId = new LocatedBrokerId(address, brokerName);

            BrokerInfo brokerInfo = new BrokerInfo("unknown-broker-id", brokerName, "unknown-broker-url");
            BrokerInfo existing = topologyState.getBrokerRegistry().putIfAbsent(locatedBrokerId, brokerInfo);

            if (existing == null) {
                result = "added";
            } else {
                result = "broker already exists";
            }
        } else {
            result = "topology not found";
        }

        return result;
    }

    protected String performBrokerRemoval(String address, String brokerName, String topology) {
        String result;

        TopologyState topologyState = this.brokerTopologyRegistry.get(topology);
        if (topologyState != null) {
            LocatedBrokerId locatedBrokerId = new LocatedBrokerId(address, brokerName);

            BrokerInfo removedInfo = topologyState.getBrokerRegistry().remove(locatedBrokerId);

            if (removedInfo != null) {
                result = "removed";
            } else {
                result = "broker not found";
            }
        } else {
            result = "topology not found";
        }

        return result;
    }

    protected String lookupBrokerName(String address) throws Exception {
        String[] brokersAtLocation = this.jmxActiveMQUtil.queryBrokerNames(address);
        if ( brokersAtLocation == null ) {
            throw new Exception("unable to locate broker at " + address);
        } else if ( brokersAtLocation.length != 1 ) {
            throw new Exception("number of brokers at " + address + " is not 1: count=" + brokersAtLocation.length);
        }

        return brokersAtLocation[0];
    }
}
