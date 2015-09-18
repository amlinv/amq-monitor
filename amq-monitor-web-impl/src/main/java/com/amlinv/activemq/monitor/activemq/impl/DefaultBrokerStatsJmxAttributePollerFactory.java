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

package com.amlinv.activemq.monitor.activemq.impl;

import com.amlinv.activemq.monitor.activemq.BrokerStatsJmxAttributePoller;
import com.amlinv.activemq.monitor.activemq.BrokerStatsJmxAttributePollerFactory;
import com.amlinv.activemq.monitor.model.BrokerStatsPackage;
import com.amlinv.javasched.Scheduler;

import java.util.List;

/**
 * Created by art on 9/2/15.
 */
public class DefaultBrokerStatsJmxAttributePollerFactory implements BrokerStatsJmxAttributePollerFactory {
    @Override
    public BrokerStatsJmxAttributePoller createPoller(List<Object> polledObjects, BrokerStatsPackage resultStorage,
                                                      Scheduler scheduler) {

        return new BrokerStatsJmxAttributePoller(polledObjects, resultStorage, scheduler);
    }
}
