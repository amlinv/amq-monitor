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

package com.amlinv.activemq.stats.logging;

import com.amlinv.activemq.monitor.model.ActiveMQBrokerStats;
import com.amlinv.activemq.monitor.model.ActiveMQQueueJmxStats;
import com.amlinv.activemq.monitor.model.BrokerStatsPackage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by art on 9/1/15.
 */
public class BrokerStatsLoggerTest {

    private BrokerStatsLogger brokerStatsLogger;

    private Logger mockLogger;
    private Logger mockStatsLogger;

    private BrokerStatsPackage statsPackage;

    @Before
    public void setupTest() throws Exception {
        this.brokerStatsLogger = new BrokerStatsLogger();

        this.mockLogger = Mockito.mock(Logger.class);
        this.mockStatsLogger = Mockito.mock(Logger.class);
    }

    @Test
    public void testGetSetLog() throws Exception {
        assertNotNull(this.brokerStatsLogger.getLog());
        assertNotSame(this.mockLogger, this.brokerStatsLogger.getLog());

        this.brokerStatsLogger.setLog(this.mockLogger);
        assertSame(this.mockLogger, this.brokerStatsLogger.getLog());
    }

    @Test
    public void testGetStatsLog() throws Exception {
        assertNotNull(this.brokerStatsLogger.getStatsLog());
        assertNotSame(this.mockStatsLogger, this.brokerStatsLogger.getLog());

        this.brokerStatsLogger.setStatsLog(this.mockStatsLogger);
        assertSame(this.mockStatsLogger, this.brokerStatsLogger.getStatsLog());
    }

    @Test
    public void testLogStats() throws Exception {
        this.brokerStatsLogger.setLog(this.mockLogger);
        this.brokerStatsLogger.setStatsLog(this.mockStatsLogger);

        ActiveMQBrokerStats brokerStats = new ActiveMQBrokerStats("x-broker-x");
        brokerStats.setAverageMessageSize(13001);
        brokerStats.setCurrentConnectionsCount(13002);
        brokerStats.setMemoryLimit(13003);
        brokerStats.setMemoryPercentUsage(13004);
        brokerStats.setStorePercentUsage(13005);
        brokerStats.setTotalConsumerCount(13006);
        brokerStats.setTotalDequeueCount(13007);
        brokerStats.setTotalEnqueueCount(13008);
        brokerStats.setTotalMessageCount(13009);
        brokerStats.setUptime("x-uptime-x");
        brokerStats.setUptimeMillis(13010);

        Map<String, ActiveMQQueueJmxStats> queueStats = new HashMap<>();
        queueStats.put("x-queue1-x", makeQueueStats(100, "x-queue1-x"));
        queueStats.put("x-queue2-x", makeQueueStats(200, "x-queue2-x"));
        // Use a null queue name to test the formatting of empty strings
        queueStats.put(null, makeQueueStats(300, null));
        queueStats.put("|", makeQueueStats(400, "|"));

        this.statsPackage = new BrokerStatsPackage(brokerStats, queueStats);
        this.brokerStatsLogger.logStats(this.statsPackage);

        Mockito.verify(this.mockStatsLogger)
                .info("{}", "|broker-stats|x-broker-x|13001|x-uptime-x|" +
                        "13010|13003|13004|13002|13006|13009|13008|13007|13005|");
        Mockito.verify(this.mockStatsLogger)
                .info("{}", "|queue-stats|x-queue1-x|x-broker-name-x|108|103|102|106|107|101|105|104|");
        Mockito.verify(this.mockStatsLogger)
                .info("{}", "|queue-stats|x-queue2-x|x-broker-name-x|208|203|202|206|207|201|205|204|");
        Mockito.verify(this.mockStatsLogger)
                .info("{}", "|queue-stats||x-broker-name-x|308|303|302|306|307|301|305|304|");
        Mockito.verify(this.mockStatsLogger)
                .info("{}", "|queue-stats|%v%|x-broker-name-x|408|403|402|406|407|401|405|404|");
    }

    @Test
    public void testLogNullBrokerStatsAndQueueStats() throws Exception {
        this.statsPackage = new BrokerStatsPackage(null, null);
        this.brokerStatsLogger.logStats(this.statsPackage);
    }

    protected ActiveMQQueueJmxStats makeQueueStats(int base, String queueName) throws Exception {
        ActiveMQQueueJmxStats queueStats = new ActiveMQQueueJmxStats("x-broker-name-x", queueName);
        queueStats.setCursorPercentUsage(base + 1);
        queueStats.setDequeueCount(base + 2);
        queueStats.setEnqueueCount(base + 3);
        queueStats.setInflightCount(base + 4);
        queueStats.setMemoryPercentUsage(base + 5);
        queueStats.setNumConsumers(base + 6);
        queueStats.setNumProducers(base + 7);
        queueStats.setQueueSize(base + 8);

        return queueStats;
    }
}