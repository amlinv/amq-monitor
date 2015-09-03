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

package com.amlinv.activemq.stats;

import com.amlinv.activemq.monitor.model.ActiveMQQueueJmxStats;
import com.amlinv.activemq.topo.registry.DestinationRegistryListener;
import com.amlinv.activemq.topo.registry.model.DestinationState;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by art on 9/1/15.
 */
public class QueueStatisticsRegistryTest {

    private QueueStatisticsRegistry registry;
    private DestinationRegistryListener listener;

    private ActiveMQQueueJmxStats stats;

    @Before
    public void setupTest() throws Exception {
        this.registry = new QueueStatisticsRegistry();
        this.listener = this.registry.getQueueRegistryListener();

        this.stats = new ActiveMQQueueJmxStats("x-broker-name-x", "x-queue-name-x");
        this.stats.setEnqueueCount(22);
        this.stats.setDequeueCount(11);
    }

    @Test
    public void testOnUpdatedStats() throws Exception {
        assertEquals(0, this.registry.getQueueStats().size());

        this.registry.onUpdatedStats(this.stats);
        Map<String, ActiveMQQueueStats> result;
        result = this.registry.getQueueStats();

        assertEquals(1, result.size());
        assertTrue(result.containsKey("x-queue-name-x"));
        assertEquals(22, result.get("x-queue-name-x").getEnqueueCount());
        assertEquals(11, result.get("x-queue-name-x").getDequeueCount());


        this.stats.setEnqueueCount(44);
        this.stats.setDequeueCount(33);
        this.registry.onUpdatedStats(stats);

        result = this.registry.getQueueStats();

        assertEquals(1, result.size());
        assertTrue(result.containsKey("x-queue-name-x"));
        assertEquals(44, result.get("x-queue-name-x").getEnqueueCount());
        assertEquals(33, result.get("x-queue-name-x").getDequeueCount());
    }

    @Test
    public void testRegistryAddQueue() throws Exception {
        this.listener.onPutEntry("x-queue-name-x", new DestinationState("x-queue-name-x"));
    }

    @Test
    public void testRegistryRemoveQueue() throws Exception {
        this.registry.onUpdatedStats(this.stats);
        assertEquals(1, this.registry.getQueueStats().size());

        this.listener.onRemoveEntry("x-queue-name-x", new DestinationState("x-queue-name-x"));
        assertEquals(0, this.registry.getQueueStats().size());
    }

    @Test
    public void testRegistryReplaceQueue() throws Exception {
        this.listener.onReplaceEntry("x-queue-name-x", new DestinationState("x-queue-name-x"),
                new DestinationState("x-queue-name-x"));
    }
}