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

package com.amlinv.activemq.monitor.topo.stats;

import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPollerListener;
import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPollerListenerFactory;
import com.amlinv.activemq.stats.polling.BrokerPollerManager;
import com.amlinv.activemq.stats.polling.BrokerPollerManagerFactory;
import com.amlinv.activemq.topo.jmxutil.polling.InvalidJmxLocationException;
import com.amlinv.activemq.topo.registry.BrokerRegistryListener;
import com.amlinv.activemq.topo.registry.BrokerTopologyRegistryListener;
import com.amlinv.activemq.topo.registry.DestinationRegistry;
import com.amlinv.activemq.topo.registry.model.BrokerInfo;
import com.amlinv.activemq.topo.registry.model.LocatedBrokerId;
import com.amlinv.activemq.topo.registry.model.TopologyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manager of statistics collection for one or more topologies of brokers.
 * <p/>
 * Created by art on 9/4/15.
 */
// TBD: move to new amq-topo-stats-collection-utils project
public class TopologyStatsCollectionManager {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TopologyStatsCollectionManager.class);

    private Logger log = DEFAULT_LOGGER;

    ///// REQUIRED INJECTIONS
    private BrokerPollerManagerFactory brokerPollerManagerFactory;
    private ActiveMQBrokerPollerListenerFactory listenerFactory;

    ///// INIT STATE
    private MyBrokerTopologyRegistryListener topologyRegistryListener = new MyBrokerTopologyRegistryListener();

    ///// RUNTIME STATE
    private boolean shutdownInd = false;
    private Map<String, BrokerPollerManager> topologyBrokerPollerMap = new HashMap<>();

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public BrokerPollerManagerFactory getBrokerPollerManagerFactory() {
        return brokerPollerManagerFactory;
    }

    public void setBrokerPollerManagerFactory(BrokerPollerManagerFactory brokerPollerManagerFactory) {
        this.brokerPollerManagerFactory = brokerPollerManagerFactory;
    }

    public ActiveMQBrokerPollerListenerFactory getListenerFactory() {
        return listenerFactory;
    }

    public void setListenerFactory(ActiveMQBrokerPollerListenerFactory listenerFactory) {
        this.listenerFactory = listenerFactory;
    }

    public MyBrokerTopologyRegistryListener getTopologyRegistryListener() {
        return topologyRegistryListener;
    }

    public void shutdown() {
        //
        // Mark shutdown and grab all the entries out of the poller map.
        //
        Set<Map.Entry<String, BrokerPollerManager>> pollerManagers;

        synchronized (this.topologyBrokerPollerMap) {
            this.shutdownInd = true;
            pollerManagers = this.topologyBrokerPollerMap.entrySet();

            this.topologyBrokerPollerMap.clear();
        }

        for (Map.Entry<String, BrokerPollerManager> pollerManagerEntry : pollerManagers) {
            log.debug("shutting down polling on topology: topologyName={}", pollerManagerEntry.getKey());
            pollerManagerEntry.getValue().shutdownAllBrokerPollers();
        }
    }


    /////
    ///// TOPOLOGY REGISTRY EVENT HANDLERS
    /////

    protected void onAddedTopology(String topology, TopologyState topologyState) {
        log.debug("adding topology: topologyName={}", topology);

        BrokerPollerManager brokerPollerManager = this.brokerPollerManagerFactory.createBrokerPollerManager();

        //
        // Register for broker events for this topology.
        //
        topologyState.getBrokerRegistry().addListener(new MyBrokerRegistryListener(topology, topologyState));

        boolean addedInd;
        synchronized (this.topologyBrokerPollerMap) {
            if (shutdownInd) {
                log.debug("skipping add due to shutdown: topology={}", topology);
            }

            if (! this.topologyBrokerPollerMap.containsKey(topology)) {
                this.topologyBrokerPollerMap.put(topology, brokerPollerManager);
                addedInd = true;
            } else {
                addedInd = false;
            }
        }

        if (addedInd) {
            Set<Map.Entry<LocatedBrokerId, BrokerInfo>> brokerEntrySet =
                    topologyState.getBrokerRegistry().asMap().entrySet();

            for (Map.Entry<LocatedBrokerId, BrokerInfo> brokerEntry : brokerEntrySet) {
                log.debug("adding broker poller on topology added event: topologyName={}; brokerName={}; location={}",
                        topology, brokerEntry.getKey().getBrokerName(), brokerEntry.getKey().getLocation());

                this.onAddBrokerToTopology(topology,topologyState, brokerEntry.getKey(), brokerEntry.getValue());
            }
        } else {
            log.debug("not adding broker poller for topology; already exists: topologyName={}", topology);
        }
    }

    protected void onRemoveBrokerTopology(String topology, TopologyState topologyState) {
        log.debug("removing topology: topologyName={}", topology);

        BrokerPollerManager brokerPollerManager;

        synchronized (this.topologyBrokerPollerMap) {
            if (shutdownInd) {
                log.debug("skipping removal due to shutddown: topologyName={}", topology);
            }

            brokerPollerManager = this.topologyBrokerPollerMap.get(topology);
        }

        if (brokerPollerManager != null) {
            brokerPollerManager.shutdownAllBrokerPollers();
        } else {
            log.debug("missing broker poller for topology on removal: topologyName={}", topology);
        }
    }


    /////
    ///// BROKER REGISTRY EVENT HANDLERS
    /////

    protected void onAddBrokerToTopology(String topologyName, TopologyState topologyState,
                                         LocatedBrokerId locatedBrokerId, BrokerInfo brokerInfo) {

        log.debug("adding broker to topology: topologyName={}; brokerName={}; location={}", topologyName,
                locatedBrokerId.getBrokerName(), locatedBrokerId.getLocation());

        BrokerPollerManager brokerPollerManager;

        synchronized (this.topologyBrokerPollerMap) {
            if (shutdownInd) {
                log.info("skipping broker addition due to shutdown: topologyName={}; brokerName={}; location={}", topologyName,
                        locatedBrokerId.getBrokerName(), locatedBrokerId.getLocation());
            }

            brokerPollerManager = this.topologyBrokerPollerMap.get(topologyName);
        }

        if (brokerPollerManager != null) {
            ActiveMQBrokerPollerListener listener;
            DestinationRegistry queueRegistry;
            DestinationRegistry topicRegistry;

            listener = this.listenerFactory.createListener(topologyName, locatedBrokerId.getBrokerName(),
                    locatedBrokerId.getLocation());
            queueRegistry = topologyState.getQueueRegistry();
            topicRegistry = topologyState.getTopicRegistry();

            try {
                brokerPollerManager.startBrokerPoller(locatedBrokerId.getBrokerName(),
                        locatedBrokerId.getLocation(), listener, queueRegistry, topicRegistry);

                log.info("started broker poller: topologyName={}; brokerName={}", topologyName,
                        locatedBrokerId.getBrokerName());
            } catch (InvalidJmxLocationException ijlExc) {
                log.warn("unable to poll new broker due to invalid location: topologyName={}; brokerName={}; " +
                                "brokerLocation={}", topologyName, locatedBrokerId.getBrokerName(),
                        locatedBrokerId.getLocation(), ijlExc);
            }
        } else {
            //
            // This seems highly unlikely, but isn't entirely impossible, especially during shutdown of a topology.
            //
            log.info("missing broker poller manager for topology on added broker; possible race condition: " +
                    "topologyName={}; brokerName={}, brokerLocation={}", topologyName,
                    locatedBrokerId.getBrokerName(), locatedBrokerId.getLocation());
        }
    }

    /**
     * Given a broker has been removed from a topology, stop polling of the broker now.
     *
     * @param topologyName name of the topology to which the broker belongs.
     * @param locatedBrokerId ID of the location of the broker which has been removed.
     * @param brokerInfo details of the broker which has been removed.
     */
    protected void onRemoveBrokerFromTopology(String topologyName, TopologyState state, LocatedBrokerId locatedBrokerId,
                                              BrokerInfo brokerInfo) {

        BrokerPollerManager brokerPollerManager;
        synchronized (this.topologyBrokerPollerMap) {
            if (shutdownInd) {
                log.debug("skipping broker removal due to shutdown: topologyName={}; brokerName={}; location={}", topologyName,
                        locatedBrokerId.getBrokerName(), locatedBrokerId.getLocation());
            }

            brokerPollerManager = this.topologyBrokerPollerMap.get(topologyName);
        }

        if (brokerPollerManager != null) {
            log.debug("shutting down polling on broker: topologyName={}; brokerName={}; location={}", topologyName,
                    locatedBrokerId.getBrokerName(), locatedBrokerId.getLocation());

            brokerPollerManager.stopBrokerPoller(locatedBrokerId.getBrokerName(), locatedBrokerId.getLocation());
        } else {
            log.info("missing topology poller manager on attempt to remove broker from toplogy: topologyName={}; " +
                    "brokerName={}; location={}", topologyName, locatedBrokerId.getBrokerName(),
                    locatedBrokerId.getLocation());
        }
    }


    /**
     * Listener to the BrokerTopologyRegistry for additions and removals of topologies.
     */
    protected class MyBrokerTopologyRegistryListener implements BrokerTopologyRegistryListener {
        @Override
        public void onPutEntry(String topology, TopologyState topologyState) {
            log.debug("topology added: topologyName={}", topology);
            onAddedTopology(topology, topologyState);
        }

        @Override
        public void onRemoveEntry(String topology, TopologyState topologyState) {
            log.debug("topology removed: topologyName={}", topology);
            onRemoveBrokerTopology(topology, topologyState);
        }

        @Override
        public void onReplaceEntry(String topology, TopologyState topologyState, TopologyState v1) {
            log.debug("topology updated: topologyName={}", topology);
            onRemoveBrokerTopology(topology, topologyState);
            onAddedTopology(topology, topologyState);
        }
    }

    /**
     * Listener to a single BrokerRegistry, which belongs to a single topology, for additions and removals of brokers.
     */
    protected class MyBrokerRegistryListener implements BrokerRegistryListener {
        private final String topologyName;
        private final TopologyState topologyState;

        public MyBrokerRegistryListener(String topologyName, TopologyState topologyState) {
            this.topologyName = topologyName;
            this.topologyState = topologyState;
        }

        @Override
        public void onPutEntry(LocatedBrokerId locatedBrokerId, BrokerInfo brokerInfo) {
            onAddBrokerToTopology(this.topologyName, this.topologyState, locatedBrokerId, brokerInfo);
        }

        @Override
        public void onRemoveEntry(LocatedBrokerId locatedBrokerId, BrokerInfo brokerInfo) {
            onRemoveBrokerFromTopology(this.topologyName, this.topologyState, locatedBrokerId, brokerInfo);
        }

        @Override
        public void onReplaceEntry(LocatedBrokerId locatedBrokerId, BrokerInfo brokerInfo, BrokerInfo v1) {
        }
    }
}