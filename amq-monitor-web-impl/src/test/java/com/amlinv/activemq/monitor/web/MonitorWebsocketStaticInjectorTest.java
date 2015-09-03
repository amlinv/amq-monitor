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

import com.amlinv.javasched.Scheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * Created by art on 9/2/15.
 */
public class MonitorWebsocketStaticInjectorTest {

    private MonitorWebsocketStaticInjector injector;

    private MonitorWebsocketRegistry origRegistry;
    private MonitorWebsocketRegistry mockRegistry;

    private long origSendTimeout;
    private long testSendTimeout;

    private Scheduler origScheduler;
    private Scheduler mockScheduler;

    @Before
    public void setupTest() throws Exception {
        this.injector = new MonitorWebsocketStaticInjector();

        this.mockRegistry = Mockito.mock(MonitorWebsocketRegistry.class);
        this.mockScheduler = Mockito.mock(Scheduler.class);

        this.origRegistry = MonitorWebsocket.getRegistry();
        this.origSendTimeout = MonitorWebsocket.getSendTimeout();
        this.origScheduler = MonitorWebsocket.getScheduler();
    }

    @After
    public void cleanupTest() throws Exception {
        MonitorWebsocket.setRegistry(this.origRegistry);
        MonitorWebsocket.setSendTimeout(this.origSendTimeout);
        MonitorWebsocket.setScheduler(this.origScheduler);
    }

    @Test
    public void testGetSetRegistry() throws Exception {
        assertSame(MonitorWebsocket.getRegistry(), this.injector.getRegistry());

        this.injector.setRegistry(this.mockRegistry);
        assertSame(this.mockRegistry, this.injector.getRegistry());
        assertSame(this.mockRegistry, MonitorWebsocket.getRegistry());
        assertSame(MonitorWebsocket.getRegistry(), this.injector.getRegistry());
    }

    @Test
    public void testGetSetSendTimeout() throws Exception {
        assertEquals(MonitorWebsocket.getSendTimeout(), this.injector.getSendTimeout());

        this.injector.setSendTimeout(133331L);
        assertEquals(133331L, this.injector.getSendTimeout());
        assertEquals(MonitorWebsocket.getSendTimeout(), this.injector.getSendTimeout());
    }

    @Test
    public void testGetSetScheduler() throws Exception {
        assertSame(MonitorWebsocket.getScheduler(), this.injector.getScheduler());

        this.injector.setScheduler(this.mockScheduler);
        assertSame(this.mockScheduler, this.injector.getScheduler());
        assertSame(MonitorWebsocket.getScheduler(), this.injector.getScheduler());
    }
}