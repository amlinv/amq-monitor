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

package com.amlinv.activemq.monitor.activemq;

import com.amlinv.activemq.monitor.activemq.impl.DefaultBrokerStatsJmxAttributePollerFactory;
import com.amlinv.activemq.stats.StatsClock;
import com.amlinv.activemq.stats.SystemStatsClock;
import com.amlinv.activemq.stats.logging.BrokerStatsLogger;
import com.amlinv.activemq.topo.registry.DestinationRegistry;
import com.amlinv.activemq.topo.registry.DestinationRegistryListener;
import com.amlinv.activemq.topo.registry.model.DestinationState;
import com.amlinv.jmxutil.connection.MBeanAccessConnectionFactory;
import com.amlinv.activemq.monitor.model.ActiveMQBrokerStats;
import com.amlinv.activemq.monitor.model.ActiveMQQueueJmxStats;
import com.amlinv.activemq.monitor.model.BrokerStatsPackage;
import com.amlinv.logging.util.RepeatLogMessageSuppressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 *
 * TBD: use immutable copies of the data
 * Created by art on 4/2/15.
 */
public class ActiveMQBrokerPoller {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(ActiveMQBrokerPoller.class);

    public static final long DEFAULT_MIN_TIME_BETWEEN_STATS_LOG = 60000L;

    private final String brokerName;

    private Timer scheduler = new Timer();

    private DestinationRegistry queueRegistry;
    private DestinationRegistry topicRegistry;

    private MyQueueRegistryListener queueRegistryListener = new MyQueueRegistryListener();
    private BrokerStatsLogger brokerStatsLogger = new BrokerStatsLogger();

    private StatsClock statsClock = new SystemStatsClock();
    private BrokerStatsJmxAttributePollerFactory jmxPollerFactory = new DefaultBrokerStatsJmxAttributePollerFactory();

    private Logger log = DEFAULT_LOGGER;

    private long pollingInterval = 3000;
    private long minTimeBetweenStatsLog = DEFAULT_MIN_TIME_BETWEEN_STATS_LOG;
    private long lastStatsLogUpdateTimestamp = 0;
    private final Object statsLogSync = new Object();

    private final MBeanAccessConnectionFactory mBeanAccessConnectionFactory;
    private final ActiveMQBrokerPollerListener listener;
    private BrokerStatsJmxAttributePoller poller;

    private RepeatLogMessageSuppressor logThrottlePollFailure = new RepeatLogMessageSuppressor();

    private ConcurrencyTestHooks concurrencyTestHooks = new ConcurrencyTestHooks();

    private boolean pollActiveInd = false;
    private boolean started = false;
    private boolean stopped = false;

    /**
     * Create a new poller of statistics from an ActiveMQ broker.
     *
     * @param brokerName
     * @param mBeanAccessConnectionFactory
     * @param listener
     */
    public ActiveMQBrokerPoller(String brokerName, MBeanAccessConnectionFactory mBeanAccessConnectionFactory,
                                ActiveMQBrokerPollerListener listener) {

        this.brokerName = brokerName;
        this.mBeanAccessConnectionFactory = mBeanAccessConnectionFactory;
        this.listener = listener;
    }

    public DestinationRegistry getQueueRegistry() {
        return queueRegistry;
    }

    public void setQueueRegistry(DestinationRegistry queueRegistry) {
        this.queueRegistry = queueRegistry;
    }

    public DestinationRegistry getTopicRegistry() {
        return topicRegistry;
    }

    public void setTopicRegistry(DestinationRegistry topicRegistry) {
        this.topicRegistry = topicRegistry;
    }

    public BrokerStatsLogger getBrokerStatsLogger() {
        return brokerStatsLogger;
    }

    public void setBrokerStatsLogger(BrokerStatsLogger brokerStatsLogger) {
        this.brokerStatsLogger = brokerStatsLogger;
    }

    public Timer getScheduler() {
        return scheduler;
    }

    public void setScheduler(Timer scheduler) {
        this.scheduler = scheduler;
    }

    public StatsClock getStatsClock() {
        return statsClock;
    }

    public void setStatsClock(StatsClock statsClock) {
        this.statsClock = statsClock;
    }

    public BrokerStatsJmxAttributePollerFactory getJmxPollerFactory() {
        return jmxPollerFactory;
    }

    public void setJmxPollerFactory(BrokerStatsJmxAttributePollerFactory jmxPollerFactory) {
        this.jmxPollerFactory = jmxPollerFactory;
    }

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public void setConcurrencyTestHooks(ConcurrencyTestHooks concurrencyTestHooks) {
        this.concurrencyTestHooks = concurrencyTestHooks;
    }

    public void start () {
        synchronized ( this ) {
            if ( this.started || this.stopped ) {
                return;
            }

            this.started = true;

            this.queueRegistry.addListener(this.queueRegistryListener);
            this.poller = this.prepareNewPoller();
        }

        PollerTask pollerTask = new PollerTask();
        this.scheduler.schedule(pollerTask, 0, pollingInterval);
    }

    public void stop () {
        this.scheduler.cancel();

        this.queueRegistry.removeListener(this.queueRegistryListener);

        this.stopped = true;

        synchronized ( this ) {
            this.notifyAll();
        }
    }

    public void waitUntilStopped () throws InterruptedException {
        synchronized ( this ) {
            // Wait until stop is called.
            while ( ! stopped ) {
                this.concurrencyTestHooks.onWaitForShutdown();
                this.wait();
            }

            // Then wait for any active poll to actually complete.
            while ( pollActiveInd ) {
                this.concurrencyTestHooks.onWaitForPollInactive();
                this.wait();
            }
        }
    }

    protected void addMonitoredQueue (String name) {
        BrokerStatsJmxAttributePoller newPoller;

        synchronized ( this ) {
            newPoller = this.prepareNewPoller();
        }

        this.activateNewPoller(newPoller);
    }

    protected void removeMonitoredQueue (String name) {
        BrokerStatsJmxAttributePoller newPoller;

        synchronized ( this ) {
            newPoller = this.prepareNewPoller();
        }

        this.activateNewPoller(newPoller);
    }

    protected BrokerStatsJmxAttributePoller prepareNewPoller() {
        BrokerStatsPackage resultStorage = this.preparePolledResultStorage();
        List<Object> polled = this.preparePolledObjects(resultStorage);

        BrokerStatsJmxAttributePoller newPoller = this.jmxPollerFactory.createPoller(polled, resultStorage);
        newPoller.setmBeanAccessConnectionFactory(this.mBeanAccessConnectionFactory);

        return  newPoller;
    }

    protected BrokerStatsPackage preparePolledResultStorage () {
        Map<String, ActiveMQQueueJmxStats> queueStatsMap = new TreeMap<>();

        for ( String oneQueueName : queueRegistry.keys() ) {
            ActiveMQQueueJmxStats queueStats = new ActiveMQQueueJmxStats(this.brokerName, oneQueueName);

            queueStatsMap.put(oneQueueName, queueStats);
        }

        BrokerStatsPackage result = new BrokerStatsPackage(new ActiveMQBrokerStats(this.brokerName), queueStatsMap);
        // TBD: addCounts Topics to monitor

        return  result;
    }

    protected List<Object> preparePolledObjects (BrokerStatsPackage resultStorage) {
        List<Object> result = new LinkedList<>();

        result.add(resultStorage.getBrokerStats());

        for ( ActiveMQQueueJmxStats oneQueueStats : resultStorage.getQueueStats().values() ) {
            result.add(oneQueueStats);
        }

        // TBD: addCounts Topics to monitor

        return  result;
    }

    protected void activateNewPoller (BrokerStatsJmxAttributePoller newPoller) {
        BrokerStatsJmxAttributePoller oldPoller;
        synchronized (this) {
            oldPoller = this.poller;
            this.poller = newPoller;
        }

        oldPoller.shutdown();
    }

    protected void pollOnce () {
        this.concurrencyTestHooks.onStartPollIndividually();

        BrokerStatsJmxAttributePoller pollerSnapshot = this.poller;

        try {
            pollActiveInd = true;

            this.concurrencyTestHooks.beforePollProcessorStart();

            pollerSnapshot.poll();
        } catch ( IOException ioExc ) {
            this.logThrottlePollFailure.warn(log, "poll of broker {} failed", this.brokerName, ioExc);
        } finally {
            this.concurrencyTestHooks.afterPollProcessorFinish();

            synchronized ( this ) {
                pollActiveInd = false;
                this.notifyAll();
            }
        }

        this.onPollComplete(pollerSnapshot);
    }

    protected void onPollComplete (BrokerStatsJmxAttributePoller poller) {
        BrokerStatsPackage resultStorage = poller.getResultStorage();

        this.listener.onBrokerPollComplete(resultStorage);

        this.logStatsWithRateLimit(resultStorage);
    }

    protected void logStatsWithRateLimit (BrokerStatsPackage resultStorage) {
        long nowMs = this.statsClock.getStatsStopWatchTime();

        synchronized ( this.statsLogSync ) {
            //
            // If enough time has not passed, skip the update.
            //
            if ( ( nowMs - this.lastStatsLogUpdateTimestamp ) < this.minTimeBetweenStatsLog ) {
                log.debug("skipping stats log; now={}; last-update={}; limit={}", nowMs,
                        this.lastStatsLogUpdateTimestamp, this.minTimeBetweenStatsLog);
                return;
            }

            this.lastStatsLogUpdateTimestamp = nowMs;
        }

        //
        // Write the upate now.
        //
        this.brokerStatsLogger.logStats(resultStorage);
    }

    protected class PollerTask extends TimerTask {
        @Override
        public void run() {
            pollOnce();
        }
    }


    /**
     * Listener to the Queue registry to update the poller as destinations are added or removed.
     */
    protected class MyQueueRegistryListener implements DestinationRegistryListener {
        @Override
        public void onPutEntry(String putKey, DestinationState putValue) {
            addMonitoredQueue(putValue.getName());
        }

        @Override
        public void onRemoveEntry(String removeKey, DestinationState removeValue) {
            removeMonitoredQueue(removeValue.getName());
        }

        @Override
        public void onReplaceEntry(String replaceKey, DestinationState oldValue, DestinationState newValue) {
        }
    }

    /**
     * Hooks for internal testing purposes only.
     */
    protected static class ConcurrencyTestHooks {
        public void onWaitForShutdown() {
        }

        public void onWaitForPollInactive() {
        }

        public void onStartPollIndividually() {
        }

        public void beforePollProcessorStart() {
        }

        public void afterPollProcessorFinish() {
        }
    }
}
