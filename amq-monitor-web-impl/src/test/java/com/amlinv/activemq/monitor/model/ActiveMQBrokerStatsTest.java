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

import com.amlinv.jmxutil.annotation.MBeanAttribute;
import com.amlinv.jmxutil.annotation.MBeanLocation;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Created by art on 8/31/15.
 */
public class ActiveMQBrokerStatsTest {

    private ActiveMQBrokerStats brokerStats;

    @Before
    public void setupTest() throws Exception {
        this.brokerStats = new ActiveMQBrokerStats("x-broker-name-x");
    }

    @Test
    public void testMBeanLocationAnnotation() throws Exception {
        MBeanLocation location = this.brokerStats.getClass().getAnnotation(MBeanLocation.class);
        assertNotNull(location);

        assertEquals("org.apache.activemq:type=Broker,brokerName=${brokerName}", location.onamePattern());
    }

    @Test
    public void testGetBrokerName() throws Exception {
        assertEquals("x-broker-name-x", this.brokerStats.getBrokerName());
    }

    @Test
    public void testGetSetAverageMessageSize() throws Exception {
        this.brokerStats.setAverageMessageSize(17002L);
        assertEquals(17002L, this.brokerStats.getAverageMessageSize());
    }

    @Test
    public void testGetSetUptime() throws Exception {
        this.brokerStats.setUptime("x-uptime-x");
        assertEquals("x-uptime-x", this.brokerStats.getUptime());

        this.verifyMBeanAttribute("setUptime", "Uptime", String.class, String.class);
    }

    @Test
    public void testGetUptimeMillis() throws Exception {
        this.brokerStats.setUptimeMillis(110011L);
        assertEquals(110011L, this.brokerStats.getUptimeMillis());

        this.verifyMBeanAttribute("setUptimeMillis", "UptimeMillis", long.class, long.class);
    }

    @Test
    public void testGetSetMemoryLimit() throws Exception {
        this.brokerStats.setMemoryLimit(4321L);
        assertEquals(4321L, this.brokerStats.getMemoryLimit());

        this.verifyMBeanAttribute("setMemoryLimit", "MemoryLimit", long.class, long.class);
    }

    @Test
    public void testGetMemoryPercentUsage() throws Exception {
        this.brokerStats.setMemoryPercentUsage(99);
        assertEquals(99, this.brokerStats.getMemoryPercentUsage());

        this.verifyMBeanAttribute("setMemoryPercentUsage", "MemoryPercentUsage", int.class, int.class);
    }

    @Test
    public void testGetSetCurrentConnectionsCount() throws Exception {
        this.brokerStats.setCurrentConnectionsCount(1234L);
        assertEquals(1234L, this.brokerStats.getCurrentConnectionsCount());

        this.verifyMBeanAttribute("setCurrentConnectionsCount", "CurrentConnectionsCount", long.class, long.class);
    }

    @Test
    public void testGetSetStorePercentUsage() throws Exception {
        this.brokerStats.setStorePercentUsage(99);
        assertEquals(99, this.brokerStats.getStorePercentUsage());

        this.verifyMBeanAttribute("setStorePercentUsage", "StorePercentUsage", int.class, int.class);
    }

    @Test
    public void testGetSetTotalConsumerCount() throws Exception {
        this.brokerStats.setTotalConsumerCount(888L);
        assertEquals(888L, this.brokerStats.getTotalConsumerCount());

        this.verifyMBeanAttribute("setTotalConsumerCount", "TotalConsumerCount", long.class, long.class);
    }

    @Test
    public void testGetSetTotalMessageCount() throws Exception {
        this.brokerStats.setTotalMessageCount(3777L);
        assertEquals(3777L, this.brokerStats.getTotalMessageCount());

        this.verifyMBeanAttribute("setTotalMessageCount", "TotalMessageCount", long.class, long.class);
    }

    @Test
    public void testGetTotalEnqueueCount() throws Exception {
        this.brokerStats.setTotalEnqueueCount(2204L);
        assertEquals(2204L, this.brokerStats.getTotalEnqueueCount());

        this.verifyMBeanAttribute("setTotalEnqueueCount", "TotalEnqueueCount", long.class, long.class);
    }

    @Test
    public void testGetSetTotalDequeueCount() throws Exception {
        this.brokerStats.setTotalDequeueCount(11133L);
        assertEquals(11133L, this.brokerStats.getTotalDequeueCount());

        this.verifyMBeanAttribute("setTotalDequeueCount", "TotalDequeueCount", long.class, long.class);
    }

    @Test
    public void testGetParameter() throws Exception {
        assertEquals("x-broker-name-x", this.brokerStats.getParameter("brokerName"));
        assertNull(this.brokerStats.getParameter("no-such-thing"));
    }


                                                 ////             ////
                                                 ////  INTERNALS  ////
                                                 ////             ////

    protected void verifyMBeanAttribute(String methodName, String attributeName, Class attributeType,
                                        Class... methodArguments) throws Exception {

        Method method = this.brokerStats.getClass().getMethod(methodName, methodArguments);
        assertNotNull(method);

        MBeanAttribute annotation = method.getAnnotation(MBeanAttribute.class);
        assertNotNull(annotation);

        assertEquals(attributeName, annotation.name());
        assertEquals(attributeType, annotation.type());
    }
}
