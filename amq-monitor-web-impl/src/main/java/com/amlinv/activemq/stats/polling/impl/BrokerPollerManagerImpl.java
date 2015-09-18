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

package com.amlinv.activemq.stats.polling.impl;

import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPoller;
import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPollerFactory;
import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPollerListener;
import com.amlinv.activemq.monitor.activemq.impl.DefaultActiveMQBrokerPollerFactory;
import com.amlinv.activemq.stats.polling.BrokerPollerManager;
import com.amlinv.activemq.topo.jmxutil.polling.InvalidJmxLocationException;
import com.amlinv.activemq.topo.jmxutil.polling.JmxActiveMQUtil2;
import com.amlinv.activemq.topo.registry.DestinationRegistry;
import com.amlinv.activemq.topo.registry.model.LocatedBrokerId;
import com.amlinv.javasched.Scheduler;
import com.amlinv.jmxutil.connection.MBeanAccessConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manager of broker pollers which creates pollers, schedules polling, and tracks pollers for individual brokers.
 *
 * <p/>
 * Created by art on 9/8/15.
 */
// TBD: move to new amq-topo-stats-collection-utils project
public class BrokerPollerManagerImpl implements BrokerPollerManager {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(BrokerPollerManagerImpl.class);

    private Logger log = DEFAULT_LOGGER;

    ///// REQUIRED VIA INJECTION:
    private Scheduler scheduler;
    private JmxActiveMQUtil2 jmxActiveMQUtil;

    ///// OPTIONALLY INJECTED:
    private ActiveMQBrokerPollerFactory brokerPollerFactory = new DefaultActiveMQBrokerPollerFactory();

    ///// RUNTIME STATE
    private boolean shutdownInd = false;
    private Map<LocatedBrokerId, ActiveMQBrokerPoller> brokerPollerMap = new HashMap<>();

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public JmxActiveMQUtil2 getJmxActiveMQUtil() {
        return jmxActiveMQUtil;
    }

    public void setJmxActiveMQUtil(JmxActiveMQUtil2 jmxActiveMQUtil) {
        this.jmxActiveMQUtil = jmxActiveMQUtil;
    }

    public ActiveMQBrokerPollerFactory getBrokerPollerFactory() {
        return brokerPollerFactory;
    }

    public void setBrokerPollerFactory(ActiveMQBrokerPollerFactory brokerPollerFactory) {
        this.brokerPollerFactory = brokerPollerFactory;
    }

    @Override
    public void startBrokerPoller(String brokerName, String location, ActiveMQBrokerPollerListener listener,
                                  DestinationRegistry queueRegistry, DestinationRegistry topicRegistry)
            throws InvalidJmxLocationException {

        LocatedBrokerId locatedBrokerId = new LocatedBrokerId(location, brokerName);

        //
        // Create a poller first.
        //
        ActiveMQBrokerPoller newPoller =
                this.createPollerForBroker(brokerName, location, listener, queueRegistry, topicRegistry);

        //
        // Next do an atomic check-and-update to ensure only one poller exists for a given broker.
        //
        boolean newInd;
        synchronized (this.brokerPollerMap) {
            if (this.shutdownInd) {
                log.debug("aborting start of broker poller; already shutdown");
                return;
            }

            if (!this.brokerPollerMap.containsKey(locatedBrokerId)) {
                this.brokerPollerMap.put(locatedBrokerId, newPoller);
                newInd = true;
            } else {
                // Abort - poller already registered
                newInd = false;
            }
        }

        //
        // Start the poller now, if it's new.
        //
        if (newInd) {
            this.log.debug("starting poller for broker: name={}; location={}", brokerName, location);

            newPoller.start();
        } else {
            this.log.debug("not starting poller for broker - already active: name={}; location={}", brokerName,
                    location);
        }
    }

    @Override
    public void stopBrokerPoller(String brokerName, String location) {
        LocatedBrokerId locatedBrokerId = new LocatedBrokerId(location, brokerName);
        ActiveMQBrokerPoller poller;

        synchronized (this.brokerPollerMap) {
            poller = this.brokerPollerMap.remove(locatedBrokerId);
        }

        if (poller != null) {
            this.log.debug("stopping poller for broker: name={}; location={}", brokerName, location);
            poller.stop();
        } else {
            this.log.debug("request to stop polling on inactive broker: name={}; location={}", brokerName, location);
        }
    }

    @Override
    public void shutdownAllBrokerPollers() {
        Set<Map.Entry<LocatedBrokerId, ActiveMQBrokerPoller>> pollerEntries;

        synchronized (this.brokerPollerMap) {
            this.shutdownInd = true;

            //
            // Grab all of the pollers and clear the map.
            //
            pollerEntries = this.brokerPollerMap.entrySet();
            this.brokerPollerMap.clear();
        }

        for (Map.Entry<LocatedBrokerId, ActiveMQBrokerPoller> pollerEntry : pollerEntries) {
            log.debug("stopping poller for broker at shutdown: brokerName={}; brokerLocation={}",
                    pollerEntry.getKey().getBrokerName(), pollerEntry.getKey().getLocation());

            // Stop the poller now.
            pollerEntry.getValue().stop();
        }
    }

    /**
     * Prepare polling for the named broker at the given polling address.
     *
     * @param brokerName
     * @param address
     * @return
     * @throws Exception
     */
    protected ActiveMQBrokerPoller createPollerForBroker(String brokerName, String address,
                                                         ActiveMQBrokerPollerListener listener,
                                                         DestinationRegistry queueRegistry,
                                                         DestinationRegistry topicRegistry)
            throws InvalidJmxLocationException {

        MBeanAccessConnectionFactory mBeanAccessConnectionFactory =
                this.jmxActiveMQUtil.getLocationConnectionFactory(address);

        ActiveMQBrokerPoller brokerPoller =
                this.brokerPollerFactory.createPoller(brokerName, mBeanAccessConnectionFactory, listener,
                        this.scheduler);

        brokerPoller.setQueueRegistry(queueRegistry);
        brokerPoller.setTopicRegistry(topicRegistry);

        return brokerPoller;
    }
}
