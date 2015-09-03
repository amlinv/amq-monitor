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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by art on 9/1/15.
 */
public class ActiveMQQueueStatsTest {

    private ActiveMQQueueStats stats;

    @Before
    public void setupTest() throws Exception {
        this.stats = new ActiveMQQueueStats("x-broker-name-x", "x-queue-name-x");
    }

    @Test
    public void testGetSetDequeueRate1Minute() throws Exception {
        assertEquals(0.0, this.stats.getDequeueRate1Minute(), 0.0000001);

        this.stats.setDequeueRate1Minute(1.234);
        assertEquals(1.234, this.stats.getDequeueRate1Minute(), 0.0000001);
    }

    @Test
    public void testGetSetDequeueRate1Hour() throws Exception {
        assertEquals(0.0, this.stats.getDequeueRate1Hour(), 0.0000001);

        this.stats.setDequeueRate1Hour(100.001);
        assertEquals(100.001, this.stats.getDequeueRate1Hour(), 0.0000001);
    }

    @Test
    public void testGetSetDequeueRate1Day() throws Exception {
        assertEquals(0.0, this.stats.getDequeueRate1Day(), 0.0000001);

        this.stats.setDequeueRate1Day(200.002);
        assertEquals(200.002, this.stats.getDequeueRate1Day(), 0.0000001);
    }

    @Test
    public void testGetSetEnqueueRate1Minute() throws Exception {
        assertEquals(0.0, this.stats.getEnqueueRate1Minute(), 0.0000001);

        this.stats.setEnqueueRate1Minute(1.234);
        assertEquals(1.234, this.stats.getEnqueueRate1Minute(), 0.0000001);
    }

    @Test
    public void testGetSetEnqueueRate1Hour() throws Exception {
        assertEquals(0.0, this.stats.getEnqueueRate1Hour(), 0.0000001);

        this.stats.setEnqueueRate1Hour(100.001);
        assertEquals(100.001, this.stats.getEnqueueRate1Hour(), 0.0000001);
    }

    @Test
    public void testGetSetEnqueueRate1Day() throws Exception {
        assertEquals(0.0, this.stats.getEnqueueRate1Day(), 0.0000001);

        this.stats.setEnqueueRate1Day(200.002);
        assertEquals(200.002, this.stats.getEnqueueRate1Day(), 0.0000001);
    }
}