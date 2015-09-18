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

import com.amlinv.activemq.stats.polling.BrokerPollerManager;
import com.amlinv.activemq.stats.polling.BrokerPollerManagerFactory;
import com.amlinv.activemq.topo.jmxutil.polling.JmxActiveMQUtil2;
import com.amlinv.javasched.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the BrokerPollerManagerFactory that creates BrokerPollerManagerImpl objects and configures them
 * with the necessary Scheduler and JmxActiveMQUtil2 instances.
 *
 * Created by art on 9/10/15.
 */
public class BrokerPollerManagerFactoryImpl implements BrokerPollerManagerFactory {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(BrokerPollerManagerFactoryImpl.class);

    private Logger log = DEFAULT_LOGGER;

    ///// REQUIRED INJECTIONS
    private Scheduler scheduler;
    private JmxActiveMQUtil2 jmxActiveMQUtil;

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

    @Override
    public BrokerPollerManager createBrokerPollerManager() {
        BrokerPollerManagerImpl result = new BrokerPollerManagerImpl();
        result.setScheduler(this.scheduler);
        result.setJmxActiveMQUtil(this.jmxActiveMQUtil);

        return result;
    }
}
