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
import org.mockito.Mockito;
import org.slf4j.Logger;

import static org.junit.Assert.*;

/**
 * Created by art on 8/31/15.
 */
public class QueueStatisticsCollectionTest {

    private QueueStatisticsCollection collection;

    private ActiveMQQueueStats mockQueueStatsPolled001;
    private ActiveMQQueueStats mockQueueStatsPolled002;
    private ActiveMQQueueStats mockQueueStatsTotal001;
    private ActiveMQQueueStats mockQueueStatsTotal002;
    private ActiveMQQueueStats mockQueueStatsDup;
    private ActiveMQQueueStats mockQueueStatsSubtracted;
    private StatsClock mockStatsClock;
    private Logger mockLogger;

    @Before
    public void setupTest() throws Exception {
        this.collection = new QueueStatisticsCollection("x-queue-name-x");

        this.mockQueueStatsPolled001 = Mockito.mock(ActiveMQQueueStats.class);
        this.mockQueueStatsPolled002 = Mockito.mock(ActiveMQQueueStats.class);
        this.mockQueueStatsTotal001 = Mockito.mock(ActiveMQQueueStats.class);
        this.mockQueueStatsTotal002 = Mockito.mock(ActiveMQQueueStats.class);
        this.mockQueueStatsDup = Mockito.mock(ActiveMQQueueStats.class);
        this.mockQueueStatsSubtracted = Mockito.mock(ActiveMQQueueStats.class);
        this.mockStatsClock = Mockito.mock(StatsClock.class);
        this.mockLogger = Mockito.mock(Logger.class);

        Mockito.when(this.mockQueueStatsPolled001.getBrokerName()).thenReturn("x-broker-name-001-x");
        Mockito.when(this.mockQueueStatsPolled002.getBrokerName()).thenReturn("x-broker-name-002-x");
        Mockito.when(this.mockQueueStatsPolled001.dup("x-broker-name-001-x")).thenReturn(this.mockQueueStatsDup);
        Mockito.when(this.mockQueueStatsPolled001.dup("totals")).thenReturn(this.mockQueueStatsTotal001);

        Mockito.when(this.mockQueueStatsPolled001.getCursorPercentUsage()).thenReturn(37);
        Mockito.when(this.mockQueueStatsPolled001.getMemoryPercentUsage()).thenReturn(47);
    }

    @Test
    public void testGetSetLog() throws Exception {
        assertNotNull(this.collection.getLog());
        assertNotSame(this.mockLogger, this.collection.getLog());

        this.collection.setLog(this.mockLogger);
        assertSame(this.mockLogger, this.collection.getLog());
    }

    @Test
    public void testGetQueueName() throws Exception {
        assertEquals("x-queue-name-x", this.collection.getQueueName());
    }

    @Test
    public void testGetSetStatsClock() throws Exception {
        assertTrue(this.collection.getStatsClock() instanceof SystemStatsClock);

        this.collection.setStatsClock(this.mockStatsClock);
        assertSame(this.mockStatsClock, this.collection.getStatsClock());
    }

    @Test
    public void testOnUpdatedStats() throws Exception {
        this.collection.onUpdatedStats(this.mockQueueStatsPolled001);

        ActiveMQQueueStats result = this.collection.getQueueTotalStats();

        Mockito.verify(this.mockQueueStatsTotal001).copyOut(result);
    }

    @Test
    public void testGetStatsBeforeUpdate() throws Exception {
        ActiveMQQueueStats result = this.collection.getQueueTotalStats();

        Mockito.verify(this.mockQueueStatsTotal001, Mockito.times(0)).copyOut(result);
    }

    @Test
    public void testOnUpdatedStatsTwoBrokers() throws Exception {
        Mockito.when(this.mockQueueStatsTotal001.addCounts(this.mockQueueStatsPolled002, "totals"))
                .thenReturn(this.mockQueueStatsTotal002);

        this.collection.onUpdatedStats(this.mockQueueStatsPolled001);
        this.collection.onUpdatedStats(this.mockQueueStatsPolled002);

        ActiveMQQueueStats result = this.collection.getQueueTotalStats();

        Mockito.verify(this.mockQueueStatsTotal001).addCounts(this.mockQueueStatsPolled002, "totals");
        Mockito.verify(this.mockQueueStatsTotal002).copyOut(result);
    }

    @Test
    public void testUpdateSameBrokerTwice() throws Exception {
        Mockito.when(this.mockQueueStatsPolled001.subtractCounts(this.mockQueueStatsDup))
                .thenReturn(this.mockQueueStatsSubtracted);
        Mockito.when(this.mockQueueStatsTotal001.addCounts(this.mockQueueStatsSubtracted, "totals"))
                .thenReturn(mockQueueStatsTotal002);

        this.collection.onUpdatedStats(this.mockQueueStatsPolled001);
        this.collection.onUpdatedStats(this.mockQueueStatsPolled001);

        ActiveMQQueueStats result = this.collection.getQueueTotalStats();

        Mockito.verify(this.mockQueueStatsTotal002).copyOut(result);
    }

    /**
     * Verify enqueue and dequeue rates.
     *
     * @throws Exception
     */
    @Test
    public void testQueueEnqueueDequeueRate() throws Exception {
        StatsClock testClock = new StatsClock() {
            private long cur = 0;

            @Override
            public long getStatsStopWatchTime() {
                long result = cur;
                cur += 1000;

                return result;
            }
        };

        ActiveMQQueueStats stats = new ActiveMQQueueStats("x-broker-name-x", "x-queue-name-x");
        stats.setEnqueueCount(0);
        stats.setDequeueCount(0);

        this.collection.setStatsClock(testClock);

        this.collection.onUpdatedStats(stats);
        assertEquals(100.0 * ( 0.0 / 60.0 ), this.collection.getQueueTotalStats().getEnqueueRate1Minute(), 0.0000001);

        int iter = 1;
        while ( iter < 60 ) {
            stats.setEnqueueCount(iter * 100);
            stats.setDequeueCount(iter * 200);
            this.collection.onUpdatedStats(stats);

            assertEquals(100.0 * (iter / 60.0), this.collection.getQueueTotalStats().getEnqueueRate1Minute(), 0.0000001);
            assertEquals(200.0 * (iter / 60.0), this.collection.getQueueTotalStats().getDequeueRate1Minute(), 0.0000001);

            iter++;
        }

        //
        // Validate handling of an idle queue.
        //

        while ( iter > 0 ) {
            iter--;

            this.collection.onUpdatedStats(stats);

            assertEquals(100.0 * (iter / 60.0), this.collection.getQueueTotalStats().getEnqueueRate1Minute(), 0.0000001);
            assertEquals(200.0 * (iter / 60.0), this.collection.getQueueTotalStats().getDequeueRate1Minute(), 0.0000001);
        }
    }

    /**
     * Verify enqueue and dequeue rates across statistics resets.
     *
     * @throws Exception
     */
    @Test
    public void testQueueEnqueueDequeueRatesOnStatsReset() throws Exception {
        StatsClock testClock = new StatsClock() {
            private long cur = 0;

            @Override
            public long getStatsStopWatchTime() {
                long result = cur;
                cur += 1000;

                return result;
            }
        };

        ActiveMQQueueStats stats = new ActiveMQQueueStats("x-broker-name-x", "x-queue-name-x");
        stats.setEnqueueCount(0);
        stats.setDequeueCount(0);

        this.collection.setStatsClock(testClock);

        this.collection.onUpdatedStats(stats);
        assertEquals(100.0 * ( 0.0 / 60.0 ), this.collection.getQueueTotalStats().getEnqueueRate1Minute(), 0.0000001);
        assertEquals(200.0 * ( 0.0 / 60.0 ), this.collection.getQueueTotalStats().getDequeueRate1Minute(), 0.0000001);

        stats.setEnqueueCount(100);
        stats.setDequeueCount(200);
        this.collection.onUpdatedStats(stats);
        assertEquals(100.0 * ( 1.0 / 60.0 ), this.collection.getQueueTotalStats().getEnqueueRate1Minute(), 0.0000001);
        assertEquals(200.0 * ( 1.0 / 60.0 ), this.collection.getQueueTotalStats().getDequeueRate1Minute(), 0.0000001);

        //
        // Reset back to 0
        //
        stats.setEnqueueCount(0);
        stats.setDequeueCount(0);
        this.collection.onUpdatedStats(stats);
        assertEquals(100.0 * ( 1.0 / 60.0 ), this.collection.getQueueTotalStats().getEnqueueRate1Minute(), 0.0000001);
        assertEquals(200.0 * ( 1.0 / 60.0 ), this.collection.getQueueTotalStats().getDequeueRate1Minute(), 0.0000001);

        // Put in another 100 and make sure the results are correct
        stats.setEnqueueCount(100);
        stats.setDequeueCount(200);
        this.collection.onUpdatedStats(stats);
        assertEquals(100.0 * ( 2.0 / 60.0 ), this.collection.getQueueTotalStats().getEnqueueRate1Minute(), 0.0000001);
        assertEquals(200.0 * ( 2.0 / 60.0 ), this.collection.getQueueTotalStats().getDequeueRate1Minute(), 0.0000001);

        //
        // Reset back -1 only
        //
        stats.setEnqueueCount(99);
        stats.setDequeueCount(199);
        this.collection.onUpdatedStats(stats);
        assertEquals(100.0 * ( 2.0 / 60.0 ), this.collection.getQueueTotalStats().getEnqueueRate1Minute(), 0.0000001);
        assertEquals(200.0 * ( 2.0 / 60.0 ), this.collection.getQueueTotalStats().getDequeueRate1Minute(), 0.0000001);

        // Put in another 100 and make sure the results are correct
        stats.setEnqueueCount(199);
        stats.setDequeueCount(399);
        this.collection.onUpdatedStats(stats);
        assertEquals(100.0 * ( 3.0 / 60.0 ), this.collection.getQueueTotalStats().getEnqueueRate1Minute(), 0.0000001);
        assertEquals(200.0 * ( 3.0 / 60.0 ), this.collection.getQueueTotalStats().getDequeueRate1Minute(), 0.0000001);
    }
}
