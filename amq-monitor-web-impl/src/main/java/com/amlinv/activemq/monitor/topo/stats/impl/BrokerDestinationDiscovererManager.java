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

package com.amlinv.activemq.monitor.topo.stats.impl;

import com.amlinv.activemq.monitor.topo.stats.MBeanDestinationDiscovererFactory;
import com.amlinv.activemq.topo.discovery.MBeanDestinationDiscoverer;
import com.amlinv.activemq.topo.discovery.MBeanDestinationDiscovererScheduler;
import com.amlinv.activemq.topo.jmxutil.polling.InvalidJmxLocationException;
import com.amlinv.activemq.topo.registry.BrokerRegistryListener;
import com.amlinv.activemq.topo.registry.DestinationRegistry;
import com.amlinv.activemq.topo.registry.model.BrokerInfo;
import com.amlinv.activemq.topo.registry.model.LocatedBrokerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by art on 9/14/15.
 */
public class BrokerDestinationDiscovererManager {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(BrokerDestinationDiscovererManager.class);

    private Logger log = DEFAULT_LOGGER;

    ///// REQUIRED INJECTIONS
    // TBD: replace with java scheduler (how to schedule at fixed rates though?)
    private ScheduledExecutorService scheduledExecutorService;
    private MBeanDestinationDiscovererFactory destinationDiscovererFactory;
    private DestinationRegistry queueRegistry;

    ///// INIT STATE
    private MyBrokerRegistryListener brokerRegistryListener = new MyBrokerRegistryListener();

    ///// RUNTIME STATE
    private final Map<LocatedBrokerId, MBeanDestinationDiscovererScheduler> discovererSchedulerMap = new HashMap<>();

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public MBeanDestinationDiscovererFactory getDestinationDiscovererFactory() {
        return destinationDiscovererFactory;
    }

    public void setDestinationDiscovererFactory(MBeanDestinationDiscovererFactory destinationDiscovererFactory) {
        this.destinationDiscovererFactory = destinationDiscovererFactory;
    }

    public DestinationRegistry getQueueRegistry() {
        return queueRegistry;
    }

    public void setQueueRegistry(DestinationRegistry queueRegistry) {
        this.queueRegistry = queueRegistry;
    }

    public MyBrokerRegistryListener getBrokerRegistryListener() {
        return brokerRegistryListener;
    }

    public void start() {
        // TBD: do we need to start all the existing brokers in the registry?  Is it possible to miss the addition
        // TBD: of a broker?  Also, how do we make sure not to process the removal of a broker out-of-order with
        // TBD: its addition?
    }

    public void shutdown() {
        this.scheduledExecutorService.shutdown();
    }

    protected void onAddBroker(LocatedBrokerId locatedBrokerId, BrokerInfo brokerInfo) {
        try {
            MBeanDestinationDiscoverer discoverer =
                    this.destinationDiscovererFactory.createDiscoverer(
                            "Queue", locatedBrokerId.getBrokerName(), locatedBrokerId.getLocation(),
                            this.queueRegistry);

            MBeanDestinationDiscovererScheduler scheduler = new MBeanDestinationDiscovererScheduler();
            scheduler.setDiscoverer(discoverer);
            scheduler.setExecutor(this.scheduledExecutorService);

            boolean startInd;

            synchronized (this.discovererSchedulerMap) {
                if (!this.discovererSchedulerMap.containsKey(locatedBrokerId)) {
                    this.discovererSchedulerMap.put(locatedBrokerId, scheduler);
                    startInd = true;
                } else {
                    startInd = false;
                }
            }

            if (startInd) {
                scheduler.start();
                log.info("started destination discovery on broker: brokerName={}; brokerLocation={}",
                        locatedBrokerId.getBrokerName(), locatedBrokerId.getLocation());
            } else {
                log.info("ignoring duplicate request to start destination discovery on broker: brokerName={}; " +
                        "brokerLocation={}", locatedBrokerId.getBrokerName(), locatedBrokerId.getLocation());
            }
        } catch (InvalidJmxLocationException ijlExc) {
            log.warn("failed to add discovery of queues for broker: brokerName={}; location={}",
                    locatedBrokerId.getBrokerName(), locatedBrokerId.getLocation(), ijlExc);
        }
    }

    protected void onRemoveBroker(LocatedBrokerId locatedBrokerId, BrokerInfo brokerInfo) {
        MBeanDestinationDiscovererScheduler scheduler;
        synchronized (this.discovererSchedulerMap) {
            scheduler = this.discovererSchedulerMap.remove(locatedBrokerId);
        }

        if (scheduler != null) {
            scheduler.stop();
            log.info("stopped destination discoverer on broker: brokerName={}; brokerLocation={}",
                    locatedBrokerId.getBrokerName(), locatedBrokerId.getLocation());
        } else {
            log.info("ignoring removal of broker as discoverer is not active: brokerName={}; brokerLocation={}",
                    locatedBrokerId.getBrokerName(), locatedBrokerId.getLocation());
        }
    }

    private class MyBrokerRegistryListener implements BrokerRegistryListener {
        @Override
        public void onPutEntry(LocatedBrokerId locatedBrokerId, BrokerInfo brokerInfo) {
            onAddBroker(locatedBrokerId, brokerInfo);
        }

        @Override
        public void onRemoveEntry(LocatedBrokerId locatedBrokerId, BrokerInfo brokerInfo) {
            onRemoveBroker(locatedBrokerId, brokerInfo);
        }

        @Override
        public void onReplaceEntry(LocatedBrokerId replaceKey, BrokerInfo oldValue, BrokerInfo newValue) {
        }
    }
}
