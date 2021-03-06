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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Collection of statistics for a single Queue.
 *
 * Created by art on 5/28/15.
 */
public class QueueStatisticsCollection {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(QueueStatisticsCollection.class);

    private final String queueName;

    private final Map<String, QueueStatMeasurements> statsByBroker = new HashMap<>();

    private Logger log = DEFAULT_LOGGER;

    private ActiveMQQueueJmxStats aggregatedStats;
    private double aggregateDequeueRateOneMinute = 0.0;
    private double aggregateDequeueRateOneHour = 0.0;
    private double aggregateDequeueRateOneDay = 0.0;
    private double aggregateEnqueueRateOneMinute = 0.0;
    private double aggregateEnqueueRateOneHour = 0.0;
    private double aggregateEnqueueRateOneDay = 0.0;

    private StatsClock statsClock = new SystemStatsClock();

    public QueueStatisticsCollection(String queueName) {
        this.queueName = queueName;
    }

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public String getQueueName() {
        return queueName;
    }

    public StatsClock getStatsClock() {
        return statsClock;
    }

    public void setStatsClock(StatsClock statsClock) {
        this.statsClock = statsClock;
    }

    public void onUpdatedStats (ActiveMQQueueJmxStats updatedStats) {
        this.log.trace("Have updated stats for queue {}; consumer-count={}", this.queueName,
                updatedStats.getNumConsumers());


        String brokerName = updatedStats.getBrokerName();
        synchronized ( this.statsByBroker ) {
            QueueStatMeasurements brokerQueueStats = this.statsByBroker.get(brokerName);
            if ( brokerQueueStats == null ) {
                //
                // First time to see stats for this broker and queue, so don't update message rates.
                //
                brokerQueueStats = new QueueStatMeasurements(updatedStats.dup(brokerName));
                this.statsByBroker.put(brokerName, brokerQueueStats);

                if ( this.aggregatedStats != null ) {
                    this.aggregatedStats = this.aggregatedStats.addCounts(updatedStats, "totals");
                    this.updateNonCountStats(updatedStats);
                } else {
                    this.aggregatedStats = updatedStats.dup("totals");
                }
            } else {
                //
                // Updates to existing stats.  Add in the effect of the new stats: subtractCounts the old stats from the
                //  new and addCounts those back into the aggregated results; addCounts the updated enqueue and dequeue
                //  counts to the rate collector for the queue; and store the results.
                //

                ActiveMQQueueJmxStats diffs = updatedStats.subtractCounts(brokerQueueStats.statsFromBroker);
                this.aggregatedStats = this.aggregatedStats.addCounts(diffs, "totals");

                this.updateNonCountStats(updatedStats);

                brokerQueueStats.statsFromBroker = updatedStats.dup(brokerName);
                this.updateRates(brokerQueueStats, diffs.getDequeueCount(), diffs.getEnqueueCount());
            }
        }
    }

    public ActiveMQQueueStats getQueueTotalStats () {
        ActiveMQQueueStats result = new ActiveMQQueueStats("totals", this.queueName);

        synchronized ( this.statsByBroker ) {
            if ( this.aggregatedStats != null ) {
                aggregatedStats.copyOut(result);
            }

            result.setDequeueRate1Minute(aggregateDequeueRateOneMinute);
            result.setDequeueRate1Hour(aggregateDequeueRateOneHour);
            result.setDequeueRate1Day(aggregateDequeueRateOneDay);

            result.setEnqueueRate1Minute(aggregateEnqueueRateOneMinute);
            result.setEnqueueRate1Hour(aggregateEnqueueRateOneHour);
            result.setEnqueueRate1Day(aggregateEnqueueRateOneDay);
        }

        return result;
    }

    protected void updateNonCountStats(ActiveMQQueueJmxStats updatedStats) {
        int highestCursorPct = 0;
        int highestMemoryPct = 0;

        for ( QueueStatMeasurements measurements : statsByBroker.values() ) {
            if ( measurements.statsFromBroker.getCursorPercentUsage() > highestCursorPct ) {
                highestCursorPct = measurements.statsFromBroker.getCursorPercentUsage();
            }

            if ( measurements.statsFromBroker.getMemoryPercentUsage() > highestMemoryPct ) {
                highestMemoryPct = measurements.statsFromBroker.getMemoryPercentUsage();
            }
        }

        this.aggregatedStats.setCursorPercentUsage(highestCursorPct);
        this.aggregatedStats.setMemoryPercentUsage(highestMemoryPct);
    }

    /**
     * Update message rates given the change in dequeue and enqueue counts for one broker queue.
     *
     * @param rateMeasurements measurements for one broker queue.
     * @param dequeueCountDelta change in the dequeue count since the last measurement for the same broker queue.
     * @param enqueueCountDelta change in the enqueue count since the last measurement for the same broker queue.
     */
    protected void updateRates (QueueStatMeasurements rateMeasurements, long dequeueCountDelta, long enqueueCountDelta) {
        double oldDequeueRateOneMinute = rateMeasurements.messageRates.getOneMinuteAverageDequeueRate();
        double oldDequeueRateOneHour = rateMeasurements.messageRates.getOneHourAverageDequeueRate();
        double oldDequeueRateOneDay = rateMeasurements.messageRates.getOneDayAverageDequeueRate();

        double oldEnqueueRateOneMinute = rateMeasurements.messageRates.getOneMinuteAverageEnqueueRate();
        double oldEnqueueRateOneHour = rateMeasurements.messageRates.getOneHourAverageEnqueueRate();
        double oldEnqueueRateOneDay = rateMeasurements.messageRates.getOneDayAverageEnqueueRate();

        //
        // Protect against negative changes in Enqueue and Dequeue counts - these metrics are designed to only ever
        //  increase, but can reset in cases such as restarting a broker and manually resetting the statistics through
        //  JMX controls.
        //
        if ( dequeueCountDelta < 0 ) {
            this.log.debug("detected negative change in dequeue count; ignoring: queue={}; delta={}", this.queueName,
                    dequeueCountDelta);
            dequeueCountDelta = 0;
        }

        if ( enqueueCountDelta < 0 ) {
            this.log.debug("detected negative change in enqueue count; ignoring: queue={}; delta={}", this.queueName,
                    enqueueCountDelta);
            enqueueCountDelta = 0;
        }


        //
        // Update the rates and add in the changes.
        //
        rateMeasurements.messageRates.onTimestampSample(statsClock.getStatsStopWatchTime(), dequeueCountDelta, enqueueCountDelta);

        aggregateDequeueRateOneMinute -= oldDequeueRateOneMinute;
        aggregateDequeueRateOneMinute += rateMeasurements.messageRates.getOneMinuteAverageDequeueRate();

        aggregateDequeueRateOneHour -= oldDequeueRateOneHour;
        aggregateDequeueRateOneHour += rateMeasurements.messageRates.getOneHourAverageDequeueRate();

        aggregateDequeueRateOneDay -= oldDequeueRateOneDay;
        aggregateDequeueRateOneDay += rateMeasurements.messageRates.getOneDayAverageDequeueRate();


        aggregateEnqueueRateOneMinute -= oldEnqueueRateOneMinute;
        aggregateEnqueueRateOneMinute += rateMeasurements.messageRates.getOneMinuteAverageEnqueueRate();

        aggregateEnqueueRateOneHour -= oldEnqueueRateOneHour;
        aggregateEnqueueRateOneHour += rateMeasurements.messageRates.getOneHourAverageEnqueueRate();

        aggregateEnqueueRateOneDay -= oldEnqueueRateOneDay;
        aggregateEnqueueRateOneDay += rateMeasurements.messageRates.getOneDayAverageEnqueueRate();
    }

    protected class QueueStatMeasurements {
        public ActiveMQQueueJmxStats statsFromBroker;
        public QueueMessageRateCollector messageRates;

        public QueueStatMeasurements(ActiveMQQueueJmxStats statsFromBroker) {
            this.statsFromBroker = statsFromBroker;
            this.messageRates = new QueueMessageRateCollector();
        }
    }
}
