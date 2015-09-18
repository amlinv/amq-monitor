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

import com.amlinv.activemq.monitor.topo.stats.impl.BrokerDestinationDiscovererManager;
import com.amlinv.activemq.topo.registry.BrokerTopologyRegistryListener;
import com.amlinv.activemq.topo.registry.model.TopologyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created by art on 9/14/15.
 */
public class TopologyDestinationDiscoveryManager {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TopologyStatsCollectionManager.class);

    private Logger log = DEFAULT_LOGGER;

    ///// REQUIRED INJECTIONS
    private MBeanDestinationDiscovererFactory discovererFactory;
    private BrokerDestinationDiscovererManagerFactory managerFactory;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;    // TBD: use java scheduler instead

    ///// INIT STATE
    private MyBrokerTopologyRegistryListener topologyRegistryListener = new MyBrokerTopologyRegistryListener();

    ///// RUNTIME STATE
    private boolean shutdownInd = false;
    private final Map<String, BrokerDestinationDiscovererManager> brokerDiscovererManagerMap = new HashMap<>();

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public MBeanDestinationDiscovererFactory getDiscovererFactory() {
        return discovererFactory;
    }

    public void setDiscovererFactory(MBeanDestinationDiscovererFactory discovererFactory) {
        this.discovererFactory = discovererFactory;
    }

    public BrokerDestinationDiscovererManagerFactory getManagerFactory() {
        return managerFactory;
    }

    public void setManagerFactory(BrokerDestinationDiscovererManagerFactory managerFactory) {
        this.managerFactory = managerFactory;
    }

    public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
        return scheduledThreadPoolExecutor;
    }

    public void setScheduledThreadPoolExecutor(ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
        this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor;
    }

    public MyBrokerTopologyRegistryListener getTopologyRegistryListener() {
        return topologyRegistryListener;
    }

    public void shutdown() {
        //
        // Mark shutdown and grab all the entries out of the poller map.
        //
        Set<Map.Entry<String, BrokerDestinationDiscovererManager>> discoverers;

        synchronized (this.brokerDiscovererManagerMap) {
            this.shutdownInd = true;
            discoverers = this.brokerDiscovererManagerMap.entrySet();

            this.brokerDiscovererManagerMap.clear();
        }

        for (Map.Entry<String, BrokerDestinationDiscovererManager> oneManagerEntry : discoverers) {
            log.debug("shutting down destination discovery on topology: topologyName={}", oneManagerEntry.getKey());
            oneManagerEntry.getValue().shutdown();
        }
    }

    protected void onAddedTopology(String topology, TopologyState topologyState) {
        BrokerDestinationDiscovererManager brokerManager = this.managerFactory.createManager();
        brokerManager.setDestinationDiscovererFactory(this.discovererFactory);
        brokerManager.setScheduledExecutorService(this.scheduledThreadPoolExecutor);
        brokerManager.setQueueRegistry(topologyState.getQueueRegistry());

        boolean startInd;
        synchronized (this.brokerDiscovererManagerMap) {
            if (shutdownInd) {
                return;
            }

            if (! this.brokerDiscovererManagerMap.containsKey(topology)) {
                // Ask the broker registry to send notifications to the new manager
                topologyState.getBrokerRegistry().addListener(brokerManager.getBrokerRegistryListener());

                this.brokerDiscovererManagerMap.put(topology, brokerManager);
                startInd = true;
            } else {
                startInd = false;
            }
        }

        if (startInd) {
            brokerManager.start();
            log.info("started broker discovery manager for topology: topologyName={}", topology);
        } else {
            log.info("ignored duplicate add of topology: topologyName={}", topology);
        }
    }

    protected void onRemoveTopology(String topology, TopologyState topologyState) {
        BrokerDestinationDiscovererManager manager;

        synchronized (this.brokerDiscovererManagerMap) {
            manager = this.brokerDiscovererManagerMap.remove(topology);
        }

        if (manager != null) {
            manager.shutdown();
            log.info("shutdown manager for broker topology: topologyName={}", topology);
        } else {
            log.info("ignoring removal of topology; manager does not exist: topologyName={}", topology);
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
            onRemoveTopology(topology, topologyState);
        }

        @Override
        public void onReplaceEntry(String topology, TopologyState topologyState, TopologyState v1) {
            log.debug("topology updated: topologyName={}", topology);
        }
    }
}
