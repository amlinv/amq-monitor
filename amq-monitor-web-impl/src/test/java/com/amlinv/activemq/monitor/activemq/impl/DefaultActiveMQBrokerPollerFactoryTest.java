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
import com.amlinv.jmxutil.connection.MBeanAccessConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * Created by art on 9/2/15.
 */
public class DefaultActiveMQBrokerPollerFactoryTest {

    private DefaultActiveMQBrokerPollerFactory factory;

    private MBeanAccessConnectionFactory mockMBeanAccessConnectionFactory;
    private ActiveMQBrokerPollerListener mockListener;

    @Before
    public void setupTest() throws Exception {
        this.factory = new DefaultActiveMQBrokerPollerFactory();

        this.mockMBeanAccessConnectionFactory = Mockito.mock(MBeanAccessConnectionFactory.class);
        this.mockListener = Mockito.mock(ActiveMQBrokerPollerListener.class);
    }

    @Test
    public void testCreatePoller() throws Exception {
        ActiveMQBrokerPoller result =
                this.factory.createPoller("x-broker-x", this.mockMBeanAccessConnectionFactory, this.mockListener);

        assertNotNull(result);
    }
}