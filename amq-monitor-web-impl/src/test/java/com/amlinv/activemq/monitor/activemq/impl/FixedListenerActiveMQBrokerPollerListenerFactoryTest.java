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

import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPoller;
import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPollerListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * Validate operation o the FixedListenerActiveMQBrokerPollerListenerFactory.
 *
 * Created by art on 9/10/15.
 */
public class FixedListenerActiveMQBrokerPollerListenerFactoryTest {

    private FixedListenerActiveMQBrokerPollerListenerFactory factory;

    private ActiveMQBrokerPollerListener mockListener;

    @Before
    public void setupTest() throws Exception {
        this.mockListener = Mockito.mock(ActiveMQBrokerPollerListener.class);

        this.factory = new FixedListenerActiveMQBrokerPollerListenerFactory(this.mockListener);
    }

    @Test
    public void testCreateListener() throws Exception {
        assertSame(this.mockListener, this.factory.createListener("x-topology1-x", "x-broker1-x", "x-location1-x"));
        assertSame(this.mockListener, this.factory.createListener("x-topology2-x", "x-broker2-x", "x-location2-x"));
        assertSame(this.mockListener, this.factory.createListener("x-topology3-x", "x-broker3-x", "x-location3-x"));
    }
}