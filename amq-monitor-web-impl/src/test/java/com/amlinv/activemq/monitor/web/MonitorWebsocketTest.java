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

package com.amlinv.activemq.monitor.web;

import com.amlinv.javasched.Scheduler;
import com.amlinv.javasched.Step;
import com.amlinv.javasched.process.StepListSchedulerProcess;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;

import javax.websocket.CloseReason;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by art on 9/2/15.
 */
public class MonitorWebsocketTest {

    private MonitorWebsocket monitorWebsocket;

    private MonitorWebsocketRegistry origRegistry;
    private MonitorWebsocketRegistry mockRegistry;

    private long origSendTimeout;

    private Scheduler origScheduler;
    private Scheduler mockScheduler;

    private Session mockSession;
    private CloseReason mockReason;
    private RemoteEndpoint.Async mockAsyncRemote;
    private RemoteEndpoint.Basic mockBasic;
    private StepListSchedulerProcess mockSendProcess;

    private Logger mockLogger;

    @Before
    public void setupTest() throws Exception {
        this.origRegistry = MonitorWebsocket.getRegistry();
        this.origSendTimeout = MonitorWebsocket.getSendTimeout();
        this.origScheduler = MonitorWebsocket.getScheduler();

        this.mockRegistry = Mockito.mock(MonitorWebsocketRegistry.class);
        this.mockScheduler = Mockito.mock(Scheduler.class);

        this.mockSession = Mockito.mock(Session.class);
        this.mockReason = Mockito.mock(CloseReason.class);
        this.mockAsyncRemote = Mockito.mock(RemoteEndpoint.Async.class);
        this.mockBasic = Mockito.mock(RemoteEndpoint.Basic.class);
        this.mockSendProcess = Mockito.mock(StepListSchedulerProcess.class);

        this.mockLogger = Mockito.mock(Logger.class);

        Mockito.when(this.mockSession.getId()).thenReturn("x-sess-id-x");
        Mockito.when(this.mockSession.getAsyncRemote()).thenReturn(this.mockAsyncRemote);
        Mockito.when(this.mockSession.getBasicRemote()).thenReturn(this.mockBasic);
    }

    @After
    public void cleanupTest() throws Exception {
        MonitorWebsocket.setRegistry(this.origRegistry);
        MonitorWebsocket.setSendTimeout(this.origSendTimeout);
        MonitorWebsocket.setScheduler(this.origScheduler);
    }

    @Test
    public void testGetSetRegistry() throws Exception {
        assertNull(MonitorWebsocket.getRegistry());

        MonitorWebsocket.setRegistry(this.mockRegistry);
        assertSame(this.mockRegistry, MonitorWebsocket.getRegistry());
    }

    @Test
    public void testGetSetSendTimeout() throws Exception {
        assertEquals(MonitorWebsocket.DEFAULT_SEND_TIMEOUT, MonitorWebsocket.getSendTimeout());

        MonitorWebsocket.setSendTimeout(2468L);
        assertEquals(2468L, MonitorWebsocket.getSendTimeout());
    }

    @Test
    public void testGetSetScheduler() throws Exception {
        assertNull(MonitorWebsocket.getScheduler());

        MonitorWebsocket.setScheduler(this.mockScheduler);
        assertSame(this.mockScheduler, MonitorWebsocket.getScheduler());
    }

    @Test
    public void testGetSetLog() throws Exception {
        this.prepareMonitorWebsocket(false);

        assertNotNull(this.monitorWebsocket.getLog());
        assertNotSame(this.mockLogger, this.monitorWebsocket.getLog());

        this.monitorWebsocket.setLog(this.mockLogger);
        assertSame(this.mockLogger, this.monitorWebsocket.getLog());
    }

    @Test
    public void testGetSetSendProcess() throws Exception {
        this.prepareMonitorWebsocket(false);

        assertNotNull(this.monitorWebsocket.getSendProcess());
        assertNotSame(this.mockSendProcess, this.monitorWebsocket.getSendProcess());

        this.monitorWebsocket.setSendProcess(this.mockSendProcess);
        assertSame(this.mockSendProcess, this.monitorWebsocket.getSendProcess());
    }

    @Test
    public void testOnClose() throws Exception {
        this.prepareMonitorWebsocket(true);

        this.monitorWebsocket.onClose(this.mockSession, mockReason);

        Mockito.verify(this.mockRegistry).remove("x-sess-id-x");
        Mockito.verify(this.mockSendProcess).shutdown();
    }

    @Test
    public void testOnError() throws Exception {
        this.prepareMonitorWebsocket(true);

        Exception exc = new Exception("x-exc-x");

        this.monitorWebsocket.onOpen(this.mockSession);
        this.monitorWebsocket.onError(this.mockSession, exc);

        Mockito.verify(this.mockLogger).info("Error on websocket session: sessionId={}", "x-sess-id-x", exc);
        Mockito.verify(this.mockRegistry).remove("x-sess-id-x");
        Mockito.verify(this.mockSession).close();
    }

    @Test
    public void testOnOpen() throws Exception {
        this.prepareMonitorWebsocket(true);

        this.monitorWebsocket.onOpen(this.mockSession);

        Mockito.verify(this.mockRegistry).put("x-sess-id-x", this.monitorWebsocket);
        Mockito.verify(this.mockAsyncRemote).setSendTimeout(MonitorWebsocket.DEFAULT_SEND_TIMEOUT);
    }

    @Test
    public void testOnMessage() throws Exception {
        this.prepareMonitorWebsocket(true);

        this.monitorWebsocket.onMessage("x-client-msg-x", this.mockSession);

        Mockito.verify(this.mockLogger).debug("message from client {}", "x-client-msg-x");
    }

    @Test
    public void testFireMonitorEventNB() throws Exception {
        this.prepareMonitorWebsocket(true);

        this.monitorWebsocket.onOpen(this.mockSession);
        this.monitorWebsocket.fireMonitorEventNB("x-action-x", "x-content-x");

        Step step = this.captureStep();

        step.execute();

        Mockito.verify(this.mockBasic).sendText("{\"action\": \"x-action-x\", \"data\": x-content-x}");
    }

    @Test
    public void testFireMonitorEventNBWithoutSession() throws Exception {
        this.prepareMonitorWebsocket(true);

        this.monitorWebsocket.fireMonitorEventNB("x-action-x", "x-content-x");
        Mockito.verify(this.mockLogger).info("ignoring event; socket session is undefined: sessionId={}", (String)null);
    }

    @Test
    public void testFireMonitorEventNBOnFullBacklog() throws Exception {
        this.prepareMonitorWebsocket(true);

        Mockito.when(this.mockSendProcess.getPendingStepCount()).thenReturn((int) MonitorWebsocket.MAX_MSG_BACKLOG);

        this.monitorWebsocket.onOpen(this.mockSession);
        this.monitorWebsocket.fireMonitorEventNB("x-action-x", "x-content-x");

        Mockito.verify(this.mockLogger).info("websocket backlog is full; aborting connection: sessionId={}",
                "x-sess-id-x");
        Mockito.verify(this.mockSession).close();
    }

    @Test
    public void testIOExceptionOnSafeClose() throws Exception {
        this.prepareMonitorWebsocket(true);

        IOException ioExc = new IOException("x-io-exc-x");
        Mockito.doThrow(ioExc).when(this.mockSession).close();

        this.monitorWebsocket.onOpen(this.mockSession);
        this.monitorWebsocket.onError(this.mockSession, new Exception("x-exc-x"));

        Mockito.verify(this.mockLogger).debug("io exception on safe close of session", ioExc);
    }

    @Test
    public void testNoSessionOnSafeClose() throws Exception {
        this.prepareMonitorWebsocket(true);

        this.monitorWebsocket.onError(this.mockSession, new Exception("x-exc-x"));

        Mockito.verifyZeroInteractions(this.mockRegistry);
    }

    @Test
    public void testIOExceptionOnSendStep() throws Exception {
        this.prepareMonitorWebsocket(true);
        IOException ioExc = new IOException("x-io-exc-x");
        Mockito.doThrow(ioExc).when(this.mockBasic).sendText("{\"action\": \"x-action-x\", \"data\": x-content-x}");

        this.monitorWebsocket.onOpen(this.mockSession);
        this.monitorWebsocket.fireMonitorEventNB("x-action-x", "x-content-x");

        Step step = this.captureStep();

        step.execute();

        Mockito.verify(this.mockLogger).info("IO exception on write to websocket; closing", ioExc);
        Mockito.verify(this.mockSendProcess).shutdown();
        Mockito.verify(this.mockSession).close();
    }

    @Test
    public void testIsBlockingStep() throws Exception {
        this.prepareMonitorWebsocket(true);

        this.monitorWebsocket.onOpen(this.mockSession);
        this.monitorWebsocket.fireMonitorEventNB("x-action-x", "x-content-x");

        Step step = this.captureStep();

        assertTrue(step.isBlocking());
    }

    protected void prepareMonitorWebsocket(boolean injectInstanceMocks) throws Exception {
        MonitorWebsocket.setRegistry(this.mockRegistry);
        MonitorWebsocket.setScheduler(this.mockScheduler);

        this.monitorWebsocket = new MonitorWebsocket();

        if (injectInstanceMocks) {
            this.monitorWebsocket.setLog(this.mockLogger);
            this.monitorWebsocket.setSendProcess(this.mockSendProcess);
        }
    }

    protected Step captureStep() {
        ArgumentCaptor<Step> stepCaptor = ArgumentCaptor.forClass(Step.class);

        Mockito.verify(this.mockSendProcess).addStep(stepCaptor.capture());

        Step step = stepCaptor.getValue();
        assertNotNull(step);

        return step;
    }
}