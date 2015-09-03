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
public class SystemStatsClockTest {

    private SystemStatsClock systemStatsClock;

    @Before
    public void setupTest() throws Exception {
        this.systemStatsClock = new SystemStatsClock();
    }

    /**
     * Validate the system stop watch time.
     *
     * @throws Exception
     */
    @Test
    public void testGetStatsStopWatchTime() throws Exception {
        long now = System.nanoTime();
        long timestamp = now;

        final long value1 = this.systemStatsClock.getStatsStopWatchTime();
        assertTrue(value1 > 0);

        final long value2 = this.systemStatsClock.getStatsStopWatchTime();
        now = System.nanoTime();
        assertTrue(value2 >= value1);
        assertTrue(value2 - value1 <= ((now / 1000000L) - (timestamp / 1000000L)));

        Thread.sleep(500);

        final long value3 = this.systemStatsClock.getStatsStopWatchTime();
        now = System.nanoTime();

        assertTrue(value3 - value2 >= 500);
        assertTrue(value3 - value1 + " <= " + ((now / 1000000L) - (timestamp / 1000000L)),
                value3 - value1 <= ((now / 1000000L) - (timestamp / 1000000L)));
    }
}
