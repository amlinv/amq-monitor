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

package com.amlinv.activemq.monitor.activemq;

import com.amlinv.activemq.monitor.model.BrokerStatsPackage;
import com.amlinv.jmxutil.polling.JmxAttributePoller;

import java.util.List;

/**
 * Created by art on 9/2/15.
 */
public class BrokerStatsJmxAttributePoller extends JmxAttributePoller {
    private BrokerStatsPackage resultStorage;

    public BrokerStatsJmxAttributePoller(List<Object> polledObjects, BrokerStatsPackage resultStorage) {
        super(polledObjects);

        this.resultStorage = resultStorage;
    }

    public BrokerStatsPackage getResultStorage() {
        return resultStorage;
    }
}
