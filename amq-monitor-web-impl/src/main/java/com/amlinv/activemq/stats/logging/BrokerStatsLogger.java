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

package com.amlinv.activemq.stats.logging;

import com.amlinv.activemq.monitor.model.ActiveMQBrokerStats;
import com.amlinv.activemq.monitor.model.ActiveMQQueueJmxStats;
import com.amlinv.activemq.monitor.model.BrokerStatsPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by art on 9/1/15.
 */
public class BrokerStatsLogger {

    public static final String STAT_LOGGER_NAME = "com.amlinv.activemq.monitor.activemq.statsLog";

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(BrokerStatsLogger.class);

    private Logger log = DEFAULT_LOGGER;
    private Logger statsLog = LoggerFactory.getLogger(STAT_LOGGER_NAME);

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public Logger getStatsLog() {
        return statsLog;
    }

    public void setStatsLog(Logger statsLog) {
        this.statsLog = statsLog;
    }

    public void logStats (BrokerStatsPackage resultStorage) {
        ActiveMQBrokerStats brokerStats = resultStorage.getBrokerStats();
        if ( brokerStats != null ) {
            String line = formatBrokerStatsLogLine(brokerStats);

            this.statsLog.info("{}", line.toString());
        }

        if ( resultStorage.getQueueStats() != null ) {
            for (Map.Entry<String, ActiveMQQueueJmxStats> oneEntry : resultStorage.getQueueStats().entrySet()) {
                String line = formatQueueStatsLogLine(oneEntry.getKey(), oneEntry.getValue());

                this.statsLog.info("{}", line.toString());
            }
        }
    }

    /**
     * FIELDS:<br>
     *     <ul>
     *         <li>"broker-stats"</li>
     *         <li>broker-name</li>
     *         <li>average-message-size</li>
     *         <li>uptime-string</li>
     *         <li>uptime-millis</li>
     *         <li>memory-limit</li>
     *         <li>memory-percent-usage</li>
     *         <li>current-connection-count</li>
     *         <li>total-consumer-count</li>
     *         <li>total-message-count</li>
     *         <li>total-enqueue-count</li>
     *         <li>total-dequeue-count</li>
     *         <li>store-percent-usage</li>
     *     </ul>
     * @param brokerStats
     * @return
     */
    protected String formatBrokerStatsLogLine(ActiveMQBrokerStats brokerStats) {
        StringBuilder buffer = new StringBuilder();

        buffer.append("|broker-stats|");
        buffer.append(encodeLogStatString(brokerStats.getBrokerName()));
        buffer.append("|");
        buffer.append(brokerStats.getAverageMessageSize());
        buffer.append("|");
        buffer.append(encodeLogStatString(brokerStats.getUptime()));
        buffer.append("|");
        buffer.append(brokerStats.getUptimeMillis());
        buffer.append("|");
        buffer.append(brokerStats.getMemoryLimit());
        buffer.append("|");
        buffer.append(brokerStats.getMemoryPercentUsage());
        buffer.append("|");
        buffer.append(brokerStats.getCurrentConnectionsCount());
        buffer.append("|");
        buffer.append(brokerStats.getTotalConsumerCount());
        buffer.append("|");
        buffer.append(brokerStats.getTotalMessageCount());
        buffer.append("|");
        buffer.append(brokerStats.getTotalEnqueueCount());
        buffer.append("|");
        buffer.append(brokerStats.getTotalDequeueCount());
        buffer.append("|");
        buffer.append(brokerStats.getStorePercentUsage());
        buffer.append("|");

        return buffer.toString();
    }

    /**
     * FIELDS:<br>
     *     <ul>
     *         <li>"queue-stats"</li>
     *         <li>queue-name</li>
     *         <li>broker-name</li>
     *         <li>queue-size</li>
     *         <li>enqueue-count</li>
     *         <li>dequeue-count</li>
     *         <li>num-consumer</li>
     *         <li>num-producer</li>
     *         <li>cursor-percent-usage</li>
     *         <li>memory-percent-usage</li>
     *         <li>inflight-count</li>
     *     </ul>
     *
     * @param queueName
     * @param stats
     * @return
     */
    protected String formatQueueStatsLogLine(String queueName, ActiveMQQueueJmxStats stats) {
        StringBuilder buffer = new StringBuilder();

        buffer.append("|queue-stats|");
        buffer.append(encodeLogStatString(queueName));
        buffer.append("|");
        buffer.append(encodeLogStatString(stats.getBrokerName()));
        buffer.append("|");
        buffer.append(stats.getQueueSize());
        buffer.append("|");
        buffer.append(stats.getEnqueueCount());
        buffer.append("|");
        buffer.append(stats.getDequeueCount());
        buffer.append("|");
        buffer.append(stats.getNumConsumers());
        buffer.append("|");
        buffer.append(stats.getNumProducers());
        buffer.append("|");
        buffer.append(stats.getCursorPercentUsage());
        buffer.append("|");
        buffer.append(stats.getMemoryPercentUsage());
        buffer.append("|");
        buffer.append(stats.getInflightCount());
        buffer.append("|");

        return buffer.toString();
    }

    protected String encodeLogStatString (String orig) {
        if ( orig == null ) {
            return "";
        }

        return  orig.replaceAll("[|]", "%v%");
    }
}
