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
import com.amlinv.activemq.monitor.model.BrokerStatsPackage;
import com.amlinv.jmxutil.polling.JmxAttributePoller;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by art on 9/2/15.
 */
public class DefaultBrokerStatsJmxAttributePollerFactoryTest {

    private DefaultBrokerStatsJmxAttributePollerFactory factory;

    private List<Object> polledObjects;
    private BrokerStatsPackage statsPackage;

    @Before
    public void setupTest() throws Exception {
        factory = new DefaultBrokerStatsJmxAttributePollerFactory();

        this.statsPackage = Mockito.mock(BrokerStatsPackage.class);

        this.polledObjects = new LinkedList<Object>();
        this.polledObjects.add("x-obj1-x");
        this.polledObjects.add("x-obj2-x");
    }

    @Test
    public void testCreatePoller() throws Exception {
        BrokerStatsJmxAttributePoller poller = this.factory.createPoller(this.polledObjects, this.statsPackage);

        assertNotNull(poller);
        assertEquals(new HashSet<>(this.polledObjects), new HashSet<>(poller.getPolledObjects()));
        assertEquals(this.statsPackage, poller.getResultStorage());
    }
}