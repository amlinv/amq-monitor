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
public class QueueMessageRateCollectorTest {

    private QueueMessageRateCollector collector;

    @Before
    public void setupTest() throws Exception {
        this.collector = new QueueMessageRateCollector();
    }

    @Test
    public void testOnSample() throws Exception {
        this.collector.onSample(100, 200);

        assertEquals(100.0 / 60.0, this.collector.getOneMinuteAverageDequeueRate(), 0.00001);
        assertEquals(200.0 / 60.0, this.collector.getOneMinuteAverageEnqueueRate(), 0.00001);
    }

    @Test
    public void testOnTimestampSampleOneMinuteAverages() throws Exception {
        int iter;

        iter = 0;
        while ( iter < 60 ) {
            this.collector.onTimestampSample(iter * 1000, 100, 200);

            iter++;

            assertEquals((iter * 100.0) / 60.0, this.collector.getOneMinuteAverageDequeueRate(), 0.00001);
            assertEquals((iter * 200.0) / 60.0, this.collector.getOneMinuteAverageEnqueueRate(), 0.00001);
        }

        //
        // Confirm the rate over time if the rate remains steady.
        //
        while ( iter < 120 ) {
            this.collector.onTimestampSample(iter * 1000, 100, 200);

            iter++;

            assertEquals(100.0, this.collector.getOneMinuteAverageDequeueRate(), 0.00001);
            assertEquals(200.0, this.collector.getOneMinuteAverageEnqueueRate(), 0.00001);
        }

        //
        // And finally, confirm the rate over time if the rate drops instantly to 0.
        //
        while ( iter < 180 ) {
            this.collector.onTimestampSample(iter * 1000, 0, 0);

            iter++;

            int offset = iter - 120;
            assertEquals(100.0 - ( 100.0 * ( offset / 60.0 ) ), this.collector.getOneMinuteAverageDequeueRate(), 0.00001);
            assertEquals(200.0 - ( 200.0 * ( offset / 60.0 ) ), this.collector.getOneMinuteAverageEnqueueRate(), 0.00001);
        }
    }

    @Test
    public void testOnTimestampSampleOneHourAverages() throws Exception {
        int iter;

        iter = 0;
        while ( iter < 240 ) {
            this.collector.onTimestampSample(iter * 15000, 100, 200);

            iter++;

            assertEquals(((iter * 100.0) / 240.0) / 15, this.collector.getOneHourAverageDequeueRate(), 0.00001);
            assertEquals(((iter * 200.0) / 240.0) / 15, this.collector.getOneHourAverageEnqueueRate(), 0.00001);
        }

        //
        // Confirm the rate over time if the rate remains steady.
        //
        while ( iter < 480 ) {
            this.collector.onTimestampSample(iter * 15000, 100, 200);

            iter++;

            assertEquals(100.0 / 15, this.collector.getOneHourAverageDequeueRate(), 0.00001);
            assertEquals(200.0 / 15, this.collector.getOneHourAverageEnqueueRate(), 0.00001);
        }

        //
        // And finally, confirm the rate over time if the rate drops instantly to 0.
        //
        while ( iter < 720 ) {
            this.collector.onTimestampSample(iter * 15000, 0, 0);

            iter++;

            int offset = iter - 480;
            assertEquals((100.0 - ( 100.0 * ( offset / 240.0 ) )) / 15, this.collector.getOneHourAverageDequeueRate(), 0.00001);
            assertEquals((200.0 - ( 200.0 * ( offset / 240.0 ) )) / 15, this.collector.getOneHourAverageEnqueueRate(), 0.00001);
        }
    }

    @Test
    public void testOnTimestampSampleOneDayAverages() throws Exception {
        int iter;

        iter = 0;
        while ( iter < 144 ) {
            this.collector.onTimestampSample(iter * 600000, 100, 200);

            iter++;

            assertEquals(((iter * 100.0) / 144.0) / 600, this.collector.getOneDayAverageDequeueRate(), 0.00001);
            assertEquals(((iter * 200.0) / 144.0) / 600, this.collector.getOneDayAverageEnqueueRate(), 0.00001);
        }

        //
        // Confirm the rate over time if the rate remains steady.
        //
        while ( iter < 288 ) {
            this.collector.onTimestampSample(iter * 600000, 100, 200);

            iter++;

            assertEquals(100.0 / 600, this.collector.getOneDayAverageDequeueRate(), 0.00001);
            assertEquals(200.0 / 600, this.collector.getOneDayAverageEnqueueRate(), 0.00001);
        }

        //
        // And finally, confirm the rate over time if the rate drops instantly to 0.
        //
        while ( iter < 432 ) {
            this.collector.onTimestampSample(iter * 600000, 0, 0);

            iter++;

            int offset = iter - 288;
            assertEquals((100.0 - ( 100.0 * ( offset / 144.0 ) )) / 600, this.collector.getOneDayAverageDequeueRate(), 0.00001);
            assertEquals((200.0 - ( 200.0 * ( offset / 144.0 ) )) / 600, this.collector.getOneDayAverageEnqueueRate(), 0.00001);
        }
    }
}
