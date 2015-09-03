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
public class ActiveMQQueueJmxStatsTest {

    private ActiveMQQueueJmxStats queueJmxStats;

    @Before
    public void setupTest() throws Exception {
        this.queueJmxStats = new ActiveMQQueueJmxStats("x-broker-name-x", "x-queue-name-x");
    }

    @Test
    public void testMBeanLocationAnnotation() throws Exception {
        MBeanLocation location = this.queueJmxStats.getClass().getAnnotation(MBeanLocation.class);
        assertNotNull(location);

        assertEquals("org.apache.activemq:type=Broker,brokerName=${brokerName},destinationType=Queue," +
                "destinationName=${queueName}", location.onamePattern());
    }

    @Test
    public void testGetBrokerName() throws Exception {
        assertEquals("x-broker-name-x", this.queueJmxStats.getBrokerName());
    }

    @Test
    public void testGetQueueName() throws Exception {
        assertEquals("x-queue-name-x", this.queueJmxStats.getQueueName());
    }

    @Test
    public void testGetSetQueueSize() throws Exception {
        this.queueJmxStats.setQueueSize(98765L);
        assertEquals(98765L, this.queueJmxStats.getQueueSize());

        this.verifyMBeanAttribute("setQueueSize", "QueueSize", long.class, long.class);
    }

    @Test
    public void testGetSetEnqueueCount() throws Exception {
        this.queueJmxStats.setEnqueueCount(43210L);
        assertEquals(43210L, this.queueJmxStats.getEnqueueCount());

        this.verifyMBeanAttribute("setEnqueueCount", "EnqueueCount", long.class, long.class);
    }

    @Test
    public void testGetSetDequeueCount() throws Exception {
        this.queueJmxStats.setDequeueCount(4444L);
        assertEquals(4444L, this.queueJmxStats.getDequeueCount());

        this.verifyMBeanAttribute("setDequeueCount", "DequeueCount", long.class, long.class);
    }

    @Test
    public void testGetSetNumConsumers() throws Exception {
        this.queueJmxStats.setNumConsumers(55555L);
        assertEquals(55555L, this.queueJmxStats.getNumConsumers());

        this.verifyMBeanAttribute("setNumConsumers", "ConsumerCount", long.class, long.class);
    }

    @Test
    public void testGetNumProducers() throws Exception {
        this.queueJmxStats.setNumProducers(333L);
        assertEquals(333L, this.queueJmxStats.getNumProducers());

        this.verifyMBeanAttribute("setNumProducers", "ProducerCount", long.class, long.class);
    }

    @Test
    public void testGetSetCursorPercentUsage() throws Exception {
        this.queueJmxStats.setCursorPercentUsage(11);
        assertEquals(11, this.queueJmxStats.getCursorPercentUsage());

        this.verifyMBeanAttribute("setCursorPercentUsage", "CursorPercentUsage", int.class, int.class);
    }

    @Test
    public void testGetSetMemoryPercentUsage() throws Exception {
        this.queueJmxStats.setMemoryPercentUsage(22);
        assertEquals(22, this.queueJmxStats.getMemoryPercentUsage());

        this.verifyMBeanAttribute("setMemoryPercentUsage", "MemoryPercentUsage", int.class, int.class);
    }

    @Test
    public void testGetSetInflightCount() throws Exception {
        this.queueJmxStats.setInflightCount(30003L);
        assertEquals(30003L, this.queueJmxStats.getInflightCount());

        this.verifyMBeanAttribute("setInflightCount", "InFlightCount", long.class, long.class);
    }

    @Test
    public void testGetParameter() throws Exception {
        assertEquals("x-broker-name-x", this.queueJmxStats.getParameter("brokerName"));
        assertEquals("x-queue-name-x", this.queueJmxStats.getParameter("queueName"));
        assertNull(this.queueJmxStats.getParameter("no-such-thing"));
    }

    @Test
    public void testAdd() throws Exception {
        ActiveMQQueueJmxStats other = new ActiveMQQueueJmxStats("x-broker-name-x", "x-queue-name-x");

        this.queueJmxStats.setCursorPercentUsage(10);
        this.queueJmxStats.setDequeueCount(20);
        this.queueJmxStats.setEnqueueCount(30);
        this.queueJmxStats.setMemoryPercentUsage(40);
        this.queueJmxStats.setNumConsumers(50);
        this.queueJmxStats.setNumProducers(60);
        this.queueJmxStats.setQueueSize(70);
        this.queueJmxStats.setInflightCount(80);

        other.setCursorPercentUsage(1);
        other.setDequeueCount(2);
        other.setEnqueueCount(3);
        other.setMemoryPercentUsage(4);
        other.setNumConsumers(5);
        other.setNumProducers(6);
        other.setQueueSize(7);
        other.setInflightCount(8);

        ActiveMQQueueJmxStats result = this.queueJmxStats.addCounts(other, "totals");

        assertEquals(10, result.getCursorPercentUsage());
        assertEquals(22, result.getDequeueCount());
        assertEquals(33, result.getEnqueueCount());
        assertEquals(40, result.getMemoryPercentUsage());
        assertEquals(55, result.getNumConsumers());
        assertEquals(66, result.getNumProducers());
        assertEquals(77, result.getQueueSize());
        assertEquals(88, result.getInflightCount());
        assertEquals("totals", result.getBrokerName());
        assertEquals("x-queue-name-x", result.getQueueName());
    }

    @Test
    public void testDup() throws Exception {
        this.queueJmxStats.setCursorPercentUsage(11);
        this.queueJmxStats.setDequeueCount(22);
        this.queueJmxStats.setEnqueueCount(33);
        this.queueJmxStats.setMemoryPercentUsage(44);
        this.queueJmxStats.setNumConsumers(55);
        this.queueJmxStats.setNumProducers(66);
        this.queueJmxStats.setQueueSize(77);
        this.queueJmxStats.setInflightCount(88);

        ActiveMQQueueJmxStats dup = this.queueJmxStats.dup("copy");

        assertEquals(11, dup.getCursorPercentUsage());
        assertEquals(22, dup.getDequeueCount());
        assertEquals(33, dup.getEnqueueCount());
        assertEquals(44, dup.getMemoryPercentUsage());
        assertEquals(55, dup.getNumConsumers());
        assertEquals(66, dup.getNumProducers());
        assertEquals(77, dup.getQueueSize());
        assertEquals(88, dup.getInflightCount());
        assertEquals("copy", dup.getBrokerName());
        assertEquals("x-queue-name-x", dup.getQueueName());
    }

    @Test
    public void testCopyOut() throws Exception {
        this.queueJmxStats.setCursorPercentUsage(11);
        this.queueJmxStats.setDequeueCount(22);
        this.queueJmxStats.setEnqueueCount(33);
        this.queueJmxStats.setMemoryPercentUsage(44);
        this.queueJmxStats.setNumConsumers(55);
        this.queueJmxStats.setNumProducers(66);
        this.queueJmxStats.setQueueSize(77);
        this.queueJmxStats.setInflightCount(88);

        ActiveMQQueueJmxStats other = new ActiveMQQueueJmxStats("xx-broker-name-xx", "xx-queue-name-xx");
        this.queueJmxStats.copyOut(other);

        assertEquals(11, other.getCursorPercentUsage());
        assertEquals(22, other.getDequeueCount());
        assertEquals(33, other.getEnqueueCount());
        assertEquals(44, other.getMemoryPercentUsage());
        assertEquals(55, other.getNumConsumers());
        assertEquals(66, other.getNumProducers());
        assertEquals(77, other.getQueueSize());
        assertEquals(88, other.getInflightCount());
        assertEquals("xx-broker-name-xx", other.getBrokerName());
        assertEquals("xx-queue-name-xx", other.getQueueName());

    }

    @Test
    public void testSubtract() throws Exception {
        ActiveMQQueueJmxStats other = new ActiveMQQueueJmxStats("x-broker-name-x", "x-queue-name-x");

        this.queueJmxStats.setCursorPercentUsage(11);
        this.queueJmxStats.setDequeueCount(22);
        this.queueJmxStats.setEnqueueCount(33);
        this.queueJmxStats.setMemoryPercentUsage(44);
        this.queueJmxStats.setNumConsumers(55);
        this.queueJmxStats.setNumProducers(66);
        this.queueJmxStats.setQueueSize(77);
        this.queueJmxStats.setInflightCount(88);

        other.setCursorPercentUsage(1);
        other.setDequeueCount(2);
        other.setEnqueueCount(3);
        other.setMemoryPercentUsage(4);
        other.setNumConsumers(5);
        other.setNumProducers(6);
        other.setQueueSize(7);
        other.setInflightCount(8);

        ActiveMQQueueJmxStats result = this.queueJmxStats.subtractCounts(other);

        assertEquals(11, result.getCursorPercentUsage());
        assertEquals(20, result.getDequeueCount());
        assertEquals(30, result.getEnqueueCount());
        assertEquals(44, result.getMemoryPercentUsage());
        assertEquals(50, result.getNumConsumers());
        assertEquals(60, result.getNumProducers());
        assertEquals(70, result.getQueueSize());
        assertEquals(80, result.getInflightCount());

        assertEquals("x-broker-name-x", result.getBrokerName());
        assertEquals("x-queue-name-x", result.getQueueName());
    }


                                                 ////             ////
                                                 ////  INTERNALS  ////
                                                 ////             ////

    protected void verifyMBeanAttribute(String methodName, String attributeName, Class attributeType,
                                        Class... methodArguments) throws Exception {

        Method method = this.queueJmxStats.getClass().getMethod(methodName, methodArguments);
        assertNotNull(method);

        MBeanAttribute annotation = method.getAnnotation(MBeanAttribute.class);
        assertNotNull(annotation);

        assertEquals(attributeName, annotation.name());
        assertEquals(attributeType, annotation.type());
    }
}