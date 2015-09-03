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

import com.amlinv.activemq.monitor.model.ActiveMQQueueJmxStats;
import com.amlinv.activemq.monitor.model.BrokerStatsPackage;
import com.amlinv.activemq.stats.StatsClock;
import com.amlinv.activemq.stats.SystemStatsClock;
import com.amlinv.activemq.stats.logging.BrokerStatsLogger;
import com.amlinv.activemq.topo.registry.DestinationRegistry;
import com.amlinv.activemq.topo.registry.DestinationRegistryListener;
import com.amlinv.activemq.topo.registry.model.DestinationState;
import com.amlinv.jmxutil.connection.MBeanAccessConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Created by art on 9/1/15.
 */
public class ActiveMQBrokerPollerTest {

    private Logger log = LoggerFactory.getLogger(ActiveMQBrokerPollerTest.class);

    private ActiveMQBrokerPoller poller;

    private MBeanAccessConnectionFactory mBeanAccessConnectionFactory;
    private ActiveMQBrokerPollerListener listener;
    private DestinationRegistry mockQueueRegistry;
    private DestinationRegistry mockTopicRegistry;
    private BrokerStatsLogger mockBrokerStatsLogger;
    private Timer mockTimer;
    private StatsClock mockStatsClock;
    private Logger mockLogger;
    private BrokerStatsJmxAttributePollerFactory mockJmxPollerFactory;

    private Deque<BrokerStatsJmxAttributePoller> jmxPollers;
    private ActiveMQBrokerPoller.ConcurrencyTestHooks mockTestHooks;

    @Before
    public void setupTest() throws Exception {
        this.mBeanAccessConnectionFactory = Mockito.mock(MBeanAccessConnectionFactory.class);
        this.listener = Mockito.mock(ActiveMQBrokerPollerListener.class);
        this.mockQueueRegistry = Mockito.mock(DestinationRegistry.class);
        this.mockTopicRegistry = Mockito.mock(DestinationRegistry.class);
        this.mockBrokerStatsLogger = Mockito.mock(BrokerStatsLogger.class);
        this.mockTimer = Mockito.mock(Timer.class);
        this.mockStatsClock = Mockito.mock(StatsClock.class);
        this.mockLogger = Mockito.mock(Logger.class);
        this.mockJmxPollerFactory = Mockito.mock(BrokerStatsJmxAttributePollerFactory.class);
        this.mockTestHooks = Mockito.mock(ActiveMQBrokerPoller.ConcurrencyTestHooks.class);

        this.jmxPollers = new LinkedList<>();

        this.poller = new ActiveMQBrokerPoller("x-broker-x", this.mBeanAccessConnectionFactory, this.listener);
    }

    @Test
    public void testGetSetQueueRegistry() throws Exception {
        assertNull(this.poller.getQueueRegistry());

        this.poller.setQueueRegistry(this.mockQueueRegistry);
        assertSame(this.mockQueueRegistry, this.poller.getQueueRegistry());
    }

    @Test
    public void testGetSetTopicRegistry() throws Exception {
        assertNull(this.poller.getTopicRegistry());

        this.poller.setTopicRegistry(this.mockTopicRegistry);
        assertSame(this.mockTopicRegistry, this.poller.getTopicRegistry());
    }

    @Test
    public void testGetSetBrokerStatsLogger() throws Exception {
        assertNotNull(this.poller.getBrokerStatsLogger());
        assertNotSame(this.mockBrokerStatsLogger, this.poller.getBrokerStatsLogger());

        this.poller.setBrokerStatsLogger(this.mockBrokerStatsLogger);
        assertSame(this.mockBrokerStatsLogger, this.poller.getBrokerStatsLogger());
    }

    @Test
    public void testGetSetScheduler() throws Exception {
        assertNotNull(this.poller.getScheduler());
        assertNotSame(this.mockTimer, this.poller.getScheduler());

        this.poller.setScheduler(this.mockTimer);
        assertSame(this.mockTimer, this.poller.getScheduler());
    }

    @Test
    public void testGetSetStatsClock() throws Exception {
        assertTrue(this.poller.getStatsClock() instanceof SystemStatsClock);

        this.poller.setStatsClock(this.mockStatsClock);
        assertSame(this.mockStatsClock, this.poller.getStatsClock());
    }

    @Test
    public void testGetSetJmxPollerFactory() throws Exception {
        assertNotNull(this.poller.getJmxPollerFactory());
        assertNotSame(this.mockJmxPollerFactory, this.poller.getJmxPollerFactory());

        this.poller.setJmxPollerFactory(this.mockJmxPollerFactory);
        assertSame(this.mockJmxPollerFactory, this.poller.getJmxPollerFactory());
    }

    @Test
    public void testGetSetLog() throws Exception {
        assertNotNull(this.poller.getLog());
        assertNotSame(this.mockLogger, this.poller.getLog());

        this.poller.setLog(this.mockLogger);
        assertSame(this.mockLogger, this.poller.getLog());
    }

    @Test
    public void testStart() throws Exception {
        //
        // SETUP
        //
        this.preparePoller();
        Mockito.when(this.mockStatsClock.getStatsStopWatchTime()).thenReturn(100000L);
        Mockito.when(this.mockQueueRegistry.keys())
                .thenReturn(new HashSet<>(Arrays.asList("x-queue1-x", "x-queue2-x")));


        //
        // EXECUTE
        //
        this.poller.start();


        //
        // VALIDATE
        //
        ArgumentCaptor<TimerTask> captor = ArgumentCaptor.forClass(TimerTask.class);

        Mockito.verify(this.mockTimer).schedule(captor.capture(), Mockito.eq(0L), Mockito.eq(3000L));
        Mockito.verifyZeroInteractions(this.listener);

        TimerTask pollerTask = captor.getValue();


        //
        // EXECUTE the poller timer task now and verify its operation
        //
        pollerTask.run();

        Mockito.verify(this.listener).onBrokerPollComplete(Mockito.any(BrokerStatsPackage.class));
        Mockito.verify(this.mockBrokerStatsLogger).logStats(Mockito.any(BrokerStatsPackage.class));
    }

    @Test
    public void testStop() throws Exception {
        this.preparePoller();

        this.poller.start();

        this.poller.stop();

        Mockito.verify(this.mockQueueRegistry).removeListener(Mockito.any(DestinationRegistryListener.class));
    }

    @Test(timeout = 3000L)
    public void testWaitUntilShutdown() throws Exception {
        //
        // Setup the hook to ensure the wait() call before we call poller.shutdown().
        //
        final CountDownLatch shutdownWaitLatch = new CountDownLatch(1);
        Answer<Void> shutdownAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                log.debug("onWaitForShutdown() called");
                shutdownWaitLatch.countDown();
                return null;
            }
        };
        Mockito.doAnswer(shutdownAnswer).when(this.mockTestHooks).onWaitForShutdown();

        //
        // Setup the hook to wait until a poll is active before we call waitUntilShutdown().
        //
        final CountDownLatch pollActiveLatch = new CountDownLatch(1);
        Answer<Void> waitPollActiveAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                log.debug("beforePollProcessorStart() called");
                pollActiveLatch.countDown();

                return null;
            }
        };
        Mockito.doAnswer(waitPollActiveAnswer).when(this.mockTestHooks).beforePollProcessorStart();


        //
        // Setup the hook to signal when polling is active so we can make sure to call waitUntilShutdown() only while
        //  it is active.
        //
        final CountDownLatch pollFinishLatch = new CountDownLatch(1);
        Answer<Void> pollActiveAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                log.debug("afterPollProcessorFinish() wait started");

                // Don't allow the poll to finish until signaled.
                pollFinishLatch.await();

                log.debug("afterPollProcessorFinish() wait finished");
                return null;
            }
        };
        Mockito.doAnswer(pollActiveAnswer).when(this.mockTestHooks).afterPollProcessorFinish();


        //
        // Setup the hook to ensure the wait() call on active poll before allowing the active poll to complete.
        //
        Answer<Void> waitOnPollAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                log.debug("onWaitForPollInactive() called");

                pollFinishLatch.countDown();

                return null;
            }
        };
        Mockito.doAnswer(waitOnPollAnswer).when(this.mockTestHooks).onWaitForPollInactive();

        //
        // Run waitUntilShutdown() in a separate thread so all the timing controls can be executed in this thread.
        //
        Thread waitUntilShutdownThread = new Thread() {
            @Override
            public void run() {
                try {
                    log.debug("starting waitUntilStopped()");
                    poller.waitUntilStopped();
                    log.debug("finished waitUntilStopped()");
                } catch (InterruptedException intExc) {
                    throw new RuntimeException(intExc);
                }
            }
        };

        //
        // Run a poll() in a separate thread as it needs to be active across the execution of most of the test.
        //
        Thread runPollThread = new Thread() {
            @Override
            public void run() {
                log.debug("starting poll()");
                poller.pollOnce();
                log.debug("finished poll()");
            }
        };

        this.poller.setConcurrencyTestHooks(this.mockTestHooks);
        this.preparePoller();

        this.poller.start();

        // Make sure a poll is active before initiating the shutdown wait
        runPollThread.start();

        pollActiveLatch.await();
        waitUntilShutdownThread.start();

        shutdownWaitLatch.await();
        this.poller.stop();

        // Make sure waitUntilShutdown() finishes.
        waitUntilShutdownThread.join();
    }

    @Test
    public void testStatsLoggingSuppression() throws Exception {
        //
        // SETUP
        //
        this.preparePoller();
        Mockito.when(this.mockStatsClock.getStatsStopWatchTime()).thenReturn(100000L);


        //
        // EXECUTE
        //
        this.poller.start();


        //
        // VALIDATE
        //
        ArgumentCaptor<TimerTask> captor = ArgumentCaptor.forClass(TimerTask.class);
        Mockito.verify(this.mockTimer).schedule(captor.capture(), Mockito.eq(0L), Mockito.eq(3000L));

        TimerTask pollerTask = captor.getValue();


        //
        // EXECUTE the poller timer task now
        //
        pollerTask.run();

        Mockito.verify(this.listener).onBrokerPollComplete(Mockito.any(BrokerStatsPackage.class));
        Mockito.verify(this.mockBrokerStatsLogger).logStats(Mockito.any(BrokerStatsPackage.class));


        //
        // EXECUTE the poller timer task again to confirm logging is suppressed until 60seconds pass
        //
        pollerTask.run();
        Mockito.verifyNoMoreInteractions(this.mockBrokerStatsLogger);

        Mockito.when(this.mockStatsClock.getStatsStopWatchTime()).thenReturn(159999L);
        pollerTask.run();
        Mockito.verifyNoMoreInteractions(this.mockBrokerStatsLogger);

        Mockito.when(this.mockStatsClock.getStatsStopWatchTime()).thenReturn(160000L);
        pollerTask.run();
        Mockito.verify(this.mockBrokerStatsLogger, Mockito.times(2)).logStats(Mockito.any(BrokerStatsPackage.class));
    }

    @Test
    public void testStartTwice() throws Exception {
        this.preparePoller();

        this.poller.start();
        this.poller.start();
    }

    @Test
    public void testStartAfterStop() throws Exception {
        this.preparePoller();

        this.poller.start();
        this.poller.stop();
        this.poller.start();
    }

    @Test
    public void testStartAfterPrematureStop() throws Exception {
        this.preparePoller();

        this.poller.stop();
        this.poller.start();
    }

    @Test
    public void testRegistryAddQueue() throws Exception {
        //
        // INITIAL SETUP
        //
        DestinationRegistryListener listener = this.startPollerAndGetQueueRegistryListener();


        //
        // SETUP INTERACTIONS FOR ADD OF QUEUE VIA REGISTRY
        //

        // Setup a mock jmx poller on creation of a poller with the added queue
        BrokerStatsJmxAttributePoller mockJmxPoller = Mockito.mock(BrokerStatsJmxAttributePoller.class);
        Mockito.when(this.mockJmxPollerFactory.createPoller(this.matchPolledQueues("x-queue-x"),
                Mockito.any(BrokerStatsPackage.class))).thenReturn(mockJmxPoller);


        // Registry now contains the queue "x-queue-x" when asked
        Mockito.when(this.mockQueueRegistry.keys()).thenReturn(new HashSet<String>(Arrays.asList("x-queue-x")));


        //
        // EXECUTE THE LISTENER
        //
        listener.onPutEntry("x-queue-x", new DestinationState("x-queue-x"));

        //
        // Verify the mock JMX poller above was prepared for execution.  This will fail if the interactions above
        //  did not execute properly.
        //
        Mockito.verify(mockJmxPoller).setmBeanAccessConnectionFactory(this.mBeanAccessConnectionFactory);
    }

    @Test
    public void testRegistryRemoveQueue() throws Exception {
        //
        // INITIAL SETUP
        //
        Mockito.when(this.mockQueueRegistry.keys())
                .thenReturn(new HashSet<String>(Arrays.asList("x-queue1-x", "x-queue2-x")));
        DestinationRegistryListener listener = this.startPollerAndGetQueueRegistryListener();


        //
        // SETUP INTERACTIONS FOR ADD OF QUEUE VIA REGISTRY
        //

        // Setup a mock jmx poller on creation of a poller with the added queue
        BrokerStatsJmxAttributePoller mockJmxPoller = Mockito.mock(BrokerStatsJmxAttributePoller.class);
        Mockito.when(this.mockJmxPollerFactory.createPoller(this.matchPolledQueues("x-queue1-x"),
                Mockito.any(BrokerStatsPackage.class))).thenReturn(mockJmxPoller);


        // Registry now only contains the queue "x-queue-x" when asked
        Mockito.when(this.mockQueueRegistry.keys()).thenReturn(new HashSet<String>(Arrays.asList("x-queue1-x")));


        //
        // EXECUTE THE LISTENER
        //
        listener.onRemoveEntry("x-queue1-x", new DestinationState("x-queue1-x"));

        //
        // Verify the mock JMX poller above was prepared for execution.  This will fail if the interactions above
        //  did not execute properly.
        //
        Mockito.verify(mockJmxPoller).setmBeanAccessConnectionFactory(this.mBeanAccessConnectionFactory);
    }

    @Test
    public void testRegistryReplaceQueue() throws Exception {
        DestinationRegistryListener listener = this.startPollerAndGetQueueRegistryListener();

        listener.onReplaceEntry("x-queue-x", new DestinationState("x-queue-x"), new DestinationState("x-queue-x"));
    }

    @Test
    public void testExceptonOnPoll() throws Exception {
        //
        // SETUP
        //
        this.preparePoller();
        Mockito.when(this.mockStatsClock.getStatsStopWatchTime()).thenReturn(100000L);
        Mockito.when(this.mockQueueRegistry.keys())
                .thenReturn(new HashSet<>(Arrays.asList("x-queue1-x", "x-queue2-x")));


        //
        // EXECUTE
        //
        this.poller.start();


        //
        // VALIDATE
        //
        ArgumentCaptor<TimerTask> captor = ArgumentCaptor.forClass(TimerTask.class);

        Mockito.verify(this.mockTimer).schedule(captor.capture(), Mockito.eq(0L), Mockito.eq(3000L));
        Mockito.verifyZeroInteractions(this.listener);

        TimerTask pollerTask = captor.getValue();


        //
        // EXECUTE the poller timer task now and verify its operation
        //
        IOException ioExc = new IOException("x-io-exc-x");
        Mockito.doThrow(ioExc).when(this.jmxPollers.getLast()).poll();

        pollerTask.run();

        Mockito.verify(this.mockLogger).warn("poll of broker {} failed", new Object[]{ "x-broker-x", ioExc });
    }

    /**
     * Call the original test hook implementations for code coverage purposes only.
     *
     * @throws Exception
     */
    @Test
    public void testHookCodeCoverage() throws Exception {
        ActiveMQBrokerPoller.ConcurrencyTestHooks hooks = new ActiveMQBrokerPoller.ConcurrencyTestHooks();

        hooks.onWaitForPollInactive();
        hooks.onWaitForShutdown();
        hooks.onStartPollIndividually();

        hooks.beforePollProcessorStart();
        hooks.afterPollProcessorFinish();
    }


    protected void preparePoller() throws Exception {
        this.poller.setScheduler(this.mockTimer);
        this.poller.setBrokerStatsLogger(this.mockBrokerStatsLogger);
        this.poller.setQueueRegistry(this.mockQueueRegistry);
        this.poller.setTopicRegistry(this.mockTopicRegistry);
        this.poller.setStatsClock(this.mockStatsClock);
        this.poller.setLog(this.mockLogger);
        this.poller.setJmxPollerFactory(this.mockJmxPollerFactory);

        this.preparePollerFactory();
    }

    protected void preparePollerFactory() throws Exception {
        Answer<BrokerStatsJmxAttributePoller> createNewPollerAnswer = new Answer<BrokerStatsJmxAttributePoller>() {
            @Override
            public BrokerStatsJmxAttributePoller answer(InvocationOnMock invocationOnMock) throws Throwable {
                BrokerStatsJmxAttributePoller result = Mockito.mock(BrokerStatsJmxAttributePoller.class);
                jmxPollers.add(result);
                return result;
            }
        };

        Mockito.when(this.mockJmxPollerFactory.createPoller(Mockito.anyList(), Mockito.any(BrokerStatsPackage.class)))
                .thenAnswer(createNewPollerAnswer);
    }

    protected DestinationRegistryListener startPollerAndGetQueueRegistryListener() throws Exception {
        this.preparePoller();

        this.poller.start();

        ArgumentCaptor<DestinationRegistryListener> argumentCaptor =
                ArgumentCaptor.forClass(DestinationRegistryListener.class);
        Mockito.verify(this.mockQueueRegistry).addListener(argumentCaptor.capture());

        return argumentCaptor.getValue();
    }

    protected List<Object> matchPolledQueues(final String... expectedQueues) {
        ArgumentMatcher<List<Object>> matcher = new ArgumentMatcher<List<Object>>() {
            @Override
            public boolean matches(Object o) {
                if (o instanceof List) {
                    List actualList = (List) o;
                    Set<String> expectedSet = new HashSet<>(Arrays.asList(expectedQueues));

                    for (Object actualPolledObj : actualList) {
                        if (actualPolledObj instanceof ActiveMQQueueJmxStats) {
                            ActiveMQQueueJmxStats stats = (ActiveMQQueueJmxStats) actualPolledObj;
                            if ( ! expectedSet.remove(stats.getQueueName()) ) {
                                // The Queue name given doesn't match the set of expected queues
                                log.debug("queue name found but not expected: name={}", stats.getQueueName());
                                return false;
                            }
                        }
                    }

                    if ( expectedSet.size() != 0 ) {
                        // Something expected but missing
                        log.debug("did not find all of the expected queues");
                        return false;
                    }

                    return true;
                }

                // Argument isn't even a list.
                return false;
            }
        };

        return Mockito.argThat(matcher);
    }
}
