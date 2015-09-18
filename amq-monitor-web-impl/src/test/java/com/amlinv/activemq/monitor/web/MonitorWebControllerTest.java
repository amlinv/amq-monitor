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

import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPoller;
import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPollerFactory;
import com.amlinv.activemq.topo.jmxutil.polling.JmxActiveMQUtil2;
import com.amlinv.activemq.topo.registry.BrokerRegistry;
import com.amlinv.activemq.topo.registry.BrokerRegistryListener;
import com.amlinv.activemq.topo.registry.BrokerTopologyRegistry;
import com.amlinv.activemq.topo.registry.DestinationRegistry;
import com.amlinv.activemq.topo.registry.model.BrokerInfo;
import com.amlinv.activemq.topo.registry.model.DestinationState;
import com.amlinv.activemq.topo.registry.model.LocatedBrokerId;
import com.amlinv.activemq.topo.registry.model.TopologyState;
import com.amlinv.javasched.Scheduler;
import com.amlinv.jmxutil.connection.MBeanAccessConnection;
import com.amlinv.jmxutil.connection.MBeanAccessConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by art on 9/2/15.
 */
public class MonitorWebControllerTest {

    private MonitorWebController webController;

    private Logger mockLogger;

    private BrokerTopologyRegistry mockBrokerTopologyRegistry;
    private TopologyState mockTopologyState;
    private BrokerRegistry mockBrokerRegistry;
    private DestinationRegistry mockQueueRegistry;
    private MonitorWebsocketBrokerStatsFeed mockFeed;
    private ActiveMQBrokerPollerFactory mockBrokerPollerFactory;
    private ActiveMQBrokerPoller mockBrokerPoller;
    private MBeanAccessConnectionFactory mockMBeanAccessConnectionFactory;
    private MBeanAccessConnection mockMBeanAccessConnection;
    private JmxActiveMQUtil2 mockJmxActiveMQUtil;
    private Scheduler mockScheduler;

    @Before
    public void setupTest() throws Exception {
        this.webController = new MonitorWebController();

        this.mockLogger = Mockito.mock(Logger.class);

        this.mockBrokerTopologyRegistry = Mockito.mock(BrokerTopologyRegistry.class);
        this.mockTopologyState = Mockito.mock(TopologyState.class);
        this.mockBrokerRegistry = Mockito.mock(BrokerRegistry.class);
        this.mockQueueRegistry = Mockito.mock(DestinationRegistry.class);
        this.mockFeed = Mockito.mock(MonitorWebsocketBrokerStatsFeed.class);
        this.mockBrokerPollerFactory = Mockito.mock(ActiveMQBrokerPollerFactory.class);
        this.mockBrokerPoller = Mockito.mock(ActiveMQBrokerPoller.class);
        this.mockMBeanAccessConnectionFactory = Mockito.mock(MBeanAccessConnectionFactory.class);
        this.mockMBeanAccessConnection = Mockito.mock(MBeanAccessConnection.class);
        this.mockJmxActiveMQUtil = Mockito.mock(JmxActiveMQUtil2.class);
        this.mockScheduler = Mockito.mock(Scheduler.class);

        Mockito.when(this.mockJmxActiveMQUtil.queryQueueNames("x-location1-x", "x-broker1-x", "*"))
                .thenReturn(new String[]{"x-queue-discovered1-x", "x-queue-discovered2-x"});
        Mockito.when(this.mockJmxActiveMQUtil.getLocationConnectionFactory("x-location1-x"))
                .thenReturn(this.mockMBeanAccessConnectionFactory);
        Mockito.when(this.mockMBeanAccessConnectionFactory.createConnection())
                .thenReturn(this.mockMBeanAccessConnection);
        Mockito.when(this.mockBrokerPollerFactory
                .createPoller("x-broker1-x", this.mockMBeanAccessConnectionFactory, this.mockFeed, this.mockScheduler))
                .thenReturn(this.mockBrokerPoller);

        Mockito.when(this.mockTopologyState.getBrokerRegistry()).thenReturn(this.mockBrokerRegistry);
        Mockito.when(this.mockTopologyState.getQueueRegistry()).thenReturn(this.mockQueueRegistry);
    }

    @After
    public void cleanupTest() throws Exception {
    }

    @Test
    public void testGetSetLog() throws Exception {
        assertNotNull(this.webController.getLog());
        assertNotSame(this.mockLogger, this.webController.getLog());

        this.webController.setLog(this.mockLogger);
        assertSame(this.mockLogger, this.webController.getLog());
    }

    @Test
    public void testGetSetWebsocketBrokerStatsFeed() throws Exception {
        assertNull(this.webController.getWebsocketBrokerStatsFeed());

        this.webController.setWebsocketBrokerStatsFeed(this.mockFeed);
        assertSame(this.mockFeed, this.webController.getWebsocketBrokerStatsFeed());
    }

    @Test
    public void testIsSetAutoStart() throws Exception {
        assertTrue(this.webController.isAutoStart());

        this.webController.setAutoStart(false);
        assertFalse(this.webController.isAutoStart());
    }

    @Test
    public void testIsSetAutoDiscoverQueues() throws Exception {
        assertTrue(this.webController.isAutoDiscoverQueues());

        this.webController.setAutoDiscoverQueues(false);
        assertFalse(this.webController.isAutoDiscoverQueues());
    }

    @Test
    public void testGetSetActiveMQBrokerPollerFactory() throws Exception {
        assertNotNull(this.webController.getBrokerPollerFactory());
        assertNotSame(this.mockBrokerPollerFactory, this.webController.getBrokerPollerFactory());

        this.webController.setBrokerPollerFactory(this.mockBrokerPollerFactory);
        assertSame(this.mockBrokerPollerFactory, this.webController.getBrokerPollerFactory());
    }

    @Test
    public void testGetSetJmxActiveMQUtil() throws Exception {
        assertNotNull(this.webController.getJmxActiveMQUtil());
        assertNotSame(this.mockJmxActiveMQUtil, this.webController.getJmxActiveMQUtil());

        this.webController.setJmxActiveMQUtil(this.mockJmxActiveMQUtil);
        assertSame(this.mockJmxActiveMQUtil, this.webController.getJmxActiveMQUtil());
    }

    @Test
    public void testInitWithAutoStart() throws Exception {
        this.prepareWebController();

//        TBD
//        this.webController.addBroker("x-broker1-x", "x-location1-x", "x-topology1-x");
//        this.webController.setAutoStart(true);
//        this.webController.init();
//
//        Mockito.verify(this.mockLogger).info("Initializing monitor web controller");
//        Mockito.verify(this.mockLogger).info("Starting monitoring now");
//        Mockito.verify(this.mockBrokerPoller).start();
    }

    @Test
    public void testInitWithoutAutoStart() throws Exception {
        this.prepareWebController();

        this.webController.setAutoStart(false);
        this.webController.init();

        Mockito.verify(this.mockLogger).info("Initializing monitor web controller");
        Mockito.verifyZeroInteractions(this.mockBrokerPollerFactory);
    }

    @Test
    public void testShutdown() throws Exception {
        this.prepareWebController();

        this.webController.addBroker("x-broker1-x", "x-location1-x", "x-topology1-x");
        this.webController.requestStartMonitoring();

        this.webController.shutdown();

//        TBD
//        Mockito.verify(this.mockBrokerPoller).stop();
    }

    @Test
    public void testListMonitoredBrokers() throws Exception {
        this.prepareWebController();
        List<BrokerInfo> brokers = Arrays.asList(
                new BrokerInfo("x-broker-id-1-x", "x-broker-name-1-x", "x-url1-x"),
                new BrokerInfo("x-broker-id-2-x", "x-broker-name-2-x", "x-url2-x"));

        // TBD
//        List<BrokerInfo> result = this.webController.listMonitoredBrokers();

//        assertEquals(brokers, result);
    }

    @Test
    public void testAddBroker() throws Exception {
        this.prepareWebController();

        this.webController.addBroker("x-broker1-x", "x-location1-x", "x-topology1-x");
        this.webController.requestStartMonitoring();

        // TBD
//        Mockito.verify(this.mockBrokerRegistry).put(Mockito.eq("x-location1-x"),
//                this.matchBrokerInfo("unknown-broker-id", "x-broker1-x", "unknown-broker-url"));
    }

    @Test
    public void testAddBrokerWildcard() throws Exception {
        this.prepareWebController();

        String[] brokers = new String[] { "x-broker1-x" };
        Mockito.when(this.mockJmxActiveMQUtil.queryBrokerNames("x-location1-x")).thenReturn(brokers);

        this.webController.addBroker("*", "x-location1-x", "x-topology1-x");
        this.webController.requestStartMonitoring();

        // TBD
//        Mockito.verify(this.mockBrokerRegistry).put(Mockito.eq("x-location1-x"),
//                this.matchBrokerInfo("unknown-broker-id", "x-broker1-x", "unknown-broker-url"));
    }

    @Test
    public void testAddBrokerWildcardNoMatch() throws Exception {
        this.prepareWebController();

        Mockito.when(this.mockJmxActiveMQUtil.queryBrokerNames("x-location1-x")).thenReturn(null);

        try {
            this.webController.addBroker("*", "x-location1-x", "x-topology1-x");
            fail("missing expected exception");
        } catch (Exception actualExc) {
            assertEquals("unable to locate broker at x-location1-x", actualExc.getMessage());
        }
    }

    @Test
    public void testAddBrokerAlreadyExisting() throws Exception {
        this.prepareWebController();

        Mockito.when(this.mockBrokerTopologyRegistry.get("x-topology1-x")).thenReturn(this.mockTopologyState);

        BrokerInfo brokerInfo = new BrokerInfo("unknown-broker-id", "x-broker1-x", "unknown-broker-url");
        Mockito.when(this.mockBrokerRegistry.putIfAbsent(Mockito.eq(new LocatedBrokerId("x-location1-x", "x-broker1-x")),
                this.matchBrokerInfo("unknown-broker-id", "x-broker1-x", "unknown-broker-url")))
                .thenReturn(brokerInfo);

        this.webController.addBroker("x-broker1-x", "x-location1-x", "x-topology1-x");

        String result;
        result = this.webController.addBroker("x-broker1-x", "x-location1-x", "x-topology1-x");

        assertEquals("broker already exists", result);
    }

    @Test
    public void testAddTwoBrokersOneLocation() throws Exception {
        this.prepareWebController();

        String[] brokers = new String[] { "x-broker1-x", "x-broker2-x" };
        Mockito.when(this.mockJmxActiveMQUtil.queryBrokerNames("x-location1-x")).thenReturn(brokers);

        try {
            this.webController.addBroker("*", "x-location1-x", "x-topology1-x");
            fail("missing expected exception");
        } catch (Exception actualExc) {
            assertEquals("number of brokers at x-location1-x is not 1: count=2", actualExc.getMessage());
        }
    }

    @Test
    public void testAddQueueAllLocations() throws Exception {
        this.prepareWebController();
        Mockito.when(this.mockJmxActiveMQUtil.queryBrokerNames("x-location1-x"))
                .thenReturn(new String[] { "x-broker1-x" });
        Mockito.when(this.mockJmxActiveMQUtil.queryBrokerNames("x-location2-x"))
                .thenReturn(new String[]{"x-broker2-x"});
        Mockito.when(this.mockJmxActiveMQUtil.queryQueueNames("x-location1-x", "x-broker1-x", "*"))
                .thenReturn(new String[] { "x-queue11-x", "x-queue12-x" });
        Mockito.when(this.mockJmxActiveMQUtil.queryQueueNames("x-location2-x", "x-broker2-x", "*"))
                .thenReturn(new String[]{"x-queue21-x", "x-queue22-x"});

        this.webController.addQueue("*", "*", "*", "x-topology1-x");

        // TBD
    }

    @Test
    public void testRemoveBrokerForm() throws Exception {
        this.prepareWebController();
        Mockito.when(this.mockBrokerTopologyRegistry.get("x-topology1-x")).thenReturn(this.mockTopologyState);
        Mockito.when(this.mockBrokerRegistry.remove(new LocatedBrokerId("x-location1-x", "x-broker1-x")))
                .thenReturn(new BrokerInfo("unknown-broker-id", "x-broker1-x", "unknown-broker-url"));

        String result = this.webController.removeBrokerForm("x-broker1-x", "x-location1-x", "x-topology1-x");

        assertEquals("removed", result);
    }

    @Test
    public void testRemoveBrokerFormNonExistentBroker() throws Exception {
        this.prepareWebController();

        // TBD
    }

    @Test
    public void testAddQueue() throws Exception {
        this.prepareWebController();

        this.webController.addQueue("x-queue1-x", "x-broker1-x", "x-location1-x", "x-topology1-x");

        // TBD
    }


    @Test
    public void testRequestStartMonitoring() throws Exception {
        this.prepareWebController();

        String result = this.webController.requestStartMonitoring();
        assertEquals("no-op", result);
    }

    @Test
    public void testQueryBrokerNames() throws Exception {
        this.prepareWebController();

        String[] brokers = new String[] { "x-broker1-x", "x-broker2-x" };
        Mockito.when(this.mockJmxActiveMQUtil.queryBrokerNames("x-location1-x")).thenReturn(brokers);

        String[] result = this.webController.queryBrokerNames("x-location1-x");

        assertSame(brokers, result);
    }

    @Test
    public void testAddBrokerWithAutoQueueDiscoveryOff() throws Exception {
        this.prepareWebController();

        this.webController.setAutoDiscoverQueues(false);
        this.webController.addBroker("x-broker1-x", "x-location1-x", "x-topology1-x");

        // NOTE: there should be some validation here.  However, this code also deserves some refactoring which will
        //       greatly simplify this test.
    }

    protected void prepareWebController() throws Exception {
        this.webController.setLog(this.mockLogger);
        this.webController.setBrokerTopologyRegistry(this.mockBrokerTopologyRegistry);
        this.webController.setWebsocketBrokerStatsFeed(this.mockFeed);
        this.webController.setBrokerPollerFactory(this.mockBrokerPollerFactory);
        this.webController.setJmxActiveMQUtil(this.mockJmxActiveMQUtil);
        this.webController.setScheduler(this.mockScheduler);
    }

    protected BrokerInfo matchBrokerInfo(final String brokerId, final String brokerName, final String brokerUrl) {
        ArgumentMatcher<BrokerInfo> brokerInfoMatcher = new ArgumentMatcher<BrokerInfo>() {
            @Override
            public boolean matches(Object o) {
                if (o instanceof BrokerInfo) {
                    BrokerInfo actual = (BrokerInfo) o;
                    if (brokerId.equals(actual.getBrokerId()) && brokerName.equals(actual.getBrokerName()) &&
                            brokerUrl.equals(actual.getBrokerUrl())) {

                        return true;
                    }
                }
                return false;
            }
        };

        return Mockito.argThat(brokerInfoMatcher);
    }
}