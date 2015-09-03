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

import com.amlinv.activemq.monitor.model.ActiveMQBrokerStats;
import com.amlinv.activemq.monitor.model.ActiveMQQueueJmxStats;
import com.amlinv.activemq.monitor.model.BrokerStatsPackage;
import com.amlinv.activemq.stats.QueueStatisticsRegistry;
import com.amlinv.activemq.topo.registry.DestinationRegistryListener;
import com.amlinv.activemq.topo.registry.model.DestinationState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by art on 9/2/15.
 */
public class MonitorWebsocketBrokerStatsFeedTest {

    private Logger log = LoggerFactory.getLogger(MonitorWebsocketBrokerStatsFeedTest.class);

    private MonitorWebsocketBrokerStatsFeed feed;

    private MonitorWebsocketRegistry mockWebsocketRegistry;
    private QueueStatisticsRegistry mockQueueStatisticsRegistry;
    private MonitorWebsocket mockMonitorWebsocket;
    private Logger mockLogger;

    private BrokerStatsPackage brokerStatsPackage;

    private Map<String, ActiveMQQueueJmxStats> testQueueStats;
    private ActiveMQBrokerStats testBrokerStats;

    @Before
    public void setupTest() throws Exception {
        this.feed = new MonitorWebsocketBrokerStatsFeed();

        this.mockWebsocketRegistry = Mockito.mock(MonitorWebsocketRegistry.class);
        this.mockQueueStatisticsRegistry = Mockito.mock(QueueStatisticsRegistry.class);
        this.mockMonitorWebsocket = Mockito.mock(MonitorWebsocket.class);
        this.mockLogger = Mockito.mock(Logger.class);

        this.testBrokerStats = new ActiveMQBrokerStats("x-broker-x");
        this.testQueueStats = new HashMap<>();
        this.brokerStatsPackage = new BrokerStatsPackage(this.testBrokerStats, this.testQueueStats);

        Mockito.when(this.mockWebsocketRegistry.values()).thenReturn(Arrays.asList(this.mockMonitorWebsocket));
    }

    @Test
    public void testGetSetWebsocketRegistry() throws Exception {
        assertNull(this.feed.getWebsocketRegistry());

        this.feed.setWebsocketRegistry(this.mockWebsocketRegistry);
        assertSame(this.mockWebsocketRegistry, this.feed.getWebsocketRegistry());
    }

    @Test
    public void testGetQueueRegistryListener() throws Exception {
        assertNotNull(this.feed.getQueueRegistryListener());
    }

    @Test
    public void testGetSetQueueStatisticsRegistry() throws Exception {
        assertNull(this.feed.getQueueStatisticsRegistry());

        this.feed.setQueueStatisticsRegistry(this.mockQueueStatisticsRegistry);
        assertSame(this.mockQueueStatisticsRegistry, this.feed.getQueueStatisticsRegistry());
    }

    @Test
    public void testGetSetLogger() {
        assertNotNull(this.feed.getLog());
        assertNotSame(this.mockLogger, this.feed.getLog());

        this.feed.setLog(this.mockLogger);
        assertSame(this.mockLogger, this.feed.getLog());
    }

    @Test
    public void testInit() throws Exception {
        this.feed.init();
    }

    @Test
    public void testOnBrokerPollComplete() throws Exception {
        this.setupFeed();

        this.testBrokerStats.setTotalMessageCount(112211L);
        this.testQueueStats.put("x-queue-x", new ActiveMQQueueJmxStats("x-broker-x", "x-queue-x"));

        this.feed.onBrokerPollComplete(this.brokerStatsPackage);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(this.mockMonitorWebsocket)
                .fireMonitorEventNB(Mockito.eq("brokerStats"), jsonCaptor.capture());

        String json = jsonCaptor.getValue();
        log.info(json);
        assertTrue(json.matches("(?s).*\"brokerStats\".*\"brokerName\"[\\s]*:[\\s]*\"x-broker-x\".*"));
        assertTrue(json.matches("(?s).*\"brokerStats\".*\"totalMessageCount\"[\\s]*:[\\s]*112211.*"));
        assertTrue(json.matches("(?s).*\"queueStats\".*\"queueName\"[\\s]*:[\\s]*\"x-queue-x\".*"));
    }

    @Test
    public void testOnBrokerPollCompleteExceptionDuringFireNotification() throws Exception {
        RuntimeException rtExc = new RuntimeException("x-rt-exc-x");
        Mockito.doThrow(rtExc).when(this.mockMonitorWebsocket)
                .fireMonitorEventNB(Mockito.eq("brokerStats"), Mockito.anyString());

        setupFeed();

        this.feed.onBrokerPollComplete(this.brokerStatsPackage);

        Mockito.verify(this.mockLogger).info("error attempting to send event to listener", rtExc);
    }

    @Test
    public void testRegistryListenerPutEntry() throws Exception {
        this.setupFeed();

        DestinationRegistryListener listener = this.feed.getQueueRegistryListener();
        listener.onPutEntry("x-queue-x", new DestinationState("x-queue-x"));

        Mockito.verify(this.mockMonitorWebsocket).fireMonitorEventNB("queueAdded", "\"x-queue-x\"");
    }

    @Test
    public void testRegistryListenerRemoveEntry() throws Exception {
        this.setupFeed();

        DestinationRegistryListener listener = this.feed.getQueueRegistryListener();
        listener.onRemoveEntry("x-queue-x", new DestinationState("x-queue-x"));

        Mockito.verify(this.mockMonitorWebsocket).fireMonitorEventNB("queueRemoved", "\"x-queue-x\"");
    }

    @Test
    public void testRegistryListenerReplaceEntry() throws Exception {
        this.setupFeed();

        DestinationRegistryListener listener = this.feed.getQueueRegistryListener();
        listener.onReplaceEntry("x-queue-x", new DestinationState("x-queue-x"), new DestinationState("x-queue-x"));

        Mockito.verifyZeroInteractions(this.mockMonitorWebsocket);
    }

    protected void setupFeed() {
        this.feed.setQueueStatisticsRegistry(this.mockQueueStatisticsRegistry);
        this.feed.setWebsocketRegistry(this.mockWebsocketRegistry);
        this.feed.setLog(this.mockLogger);
    }
}