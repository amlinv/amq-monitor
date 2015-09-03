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

package com.amlinv.activemq.monitor.model;

import com.amlinv.activemq.stats.ActiveMQQueueStats;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by art on 8/31/15.
 */
public class BrokerStatsPackageTest {

    private BrokerStatsPackage brokerStatsPackage;

    private ActiveMQBrokerStats stats;
    private Map<String, ActiveMQQueueJmxStats> queueStats;

    @Before
    public void setupTest() throws Exception {
        stats = new ActiveMQBrokerStats("x-broker-x");
        queueStats = new HashMap<>();

        this.brokerStatsPackage = new BrokerStatsPackage(stats, queueStats);
    }

    @Test
    public void testGetBrokerStats() throws Exception {
        assertSame(this.stats, this.brokerStatsPackage.getBrokerStats());
    }

    @Test
    public void testGetQueueStats() throws Exception {
        assertSame(this.queueStats, this.brokerStatsPackage.getQueueStats());
    }
}