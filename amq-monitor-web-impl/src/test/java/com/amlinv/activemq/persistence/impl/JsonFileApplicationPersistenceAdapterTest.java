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

package com.amlinv.activemq.persistence.impl;

import com.amlinv.activemq.persistence.FileStreamFactory;
import com.amlinv.activemq.persistence.IOStreamFactory;
import com.amlinv.activemq.topo.registry.BrokerRegistry;
import com.amlinv.activemq.topo.registry.DestinationRegistry;
import com.amlinv.activemq.topo.registry.model.BrokerInfo;
import com.amlinv.activemq.topo.registry.model.DestinationState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import static org.junit.Assert.*;

/**
 * Created by art on 9/1/15.
 */
public class JsonFileApplicationPersistenceAdapterTest {

    private JsonFileApplicationPersistenceAdapter adapter;

    private FileStreamFactory mockFileStreamFactory;
    private FileInputStream mockFileInputStream;
    private FileOutputStream mockFileOutputStream;
    private IOStreamFactory mockIOStreamFactory;
    private InputStreamReader mockInputStreamReader;
    private OutputStreamWriter mockOutputStreamWriter;

    private Logger mockLogger;

    private BrokerRegistry brokerRegistry;
    private DestinationRegistry queueRegistry;
    private DestinationRegistry topicRegistry;

    private ByteArrayInputStream inputData;
    private ByteArrayOutputStream outputData;
    private File path;


    @Before
    public void setupTest() throws Exception {
        this.adapter = new JsonFileApplicationPersistenceAdapter("x-file-path-x");

        this.path = new File("x-file-path-x");

        this.mockFileStreamFactory = Mockito.mock(FileStreamFactory.class);
        this.mockFileInputStream = Mockito.mock(FileInputStream.class);
        this.mockFileOutputStream = Mockito.mock(FileOutputStream.class);
        this.mockIOStreamFactory = Mockito.mock(IOStreamFactory.class);
        this.mockInputStreamReader = Mockito.mock(InputStreamReader.class);
        this.mockOutputStreamWriter = Mockito.mock(OutputStreamWriter.class);

        this.mockLogger = Mockito.mock(Logger.class);

        this.brokerRegistry = new BrokerRegistry();
        this.queueRegistry = new DestinationRegistry();
        this.topicRegistry = new DestinationRegistry();

        this.outputData = new ByteArrayOutputStream();

        Mockito.when(this.mockFileStreamFactory.getInputStream(this.path))
                .thenReturn(this.mockFileInputStream);
        Mockito.when(this.mockFileStreamFactory.getOutputStream(this.path))
                .thenReturn(this.mockFileOutputStream);

        Mockito.when(this.mockIOStreamFactory.createInputReader(this.mockFileInputStream))
                .thenReturn(this.mockInputStreamReader);
        Mockito.when(this.mockIOStreamFactory.createOutputWriter(this.mockFileOutputStream))
                .thenReturn(this.mockOutputStreamWriter);

        this.prepareMockInputStream();
        this.prepareMockOutputStream();
    }

    @Test
    public void testAlternateConstructor() throws Exception {
        this.adapter = new JsonFileApplicationPersistenceAdapter(new File("x-alt-file-path-x"));
    }

    @Test
    public void testGetSetFileStreamFactory() throws Exception {
        assertNotNull(this.adapter.getFileStreamFactory());
        assertNotSame(this.mockFileStreamFactory, this.adapter.getFileStreamFactory());

        this.adapter.setFileStreamFactory(this.mockFileStreamFactory);
        assertSame(this.mockFileStreamFactory, this.adapter.getFileStreamFactory());
    }

    @Test
    public void testGetSetIOStreamFactory() throws Exception {
        assertNotNull(this.adapter.getIoStreamFactory());
        assertNotSame(this.mockIOStreamFactory, this.adapter.getIoStreamFactory());

        this.adapter.setIoStreamFactory(this.mockIOStreamFactory);
        assertSame(this.mockIOStreamFactory, this.adapter.getIoStreamFactory());
    }

    @Test
    public void testGetSetLog() throws Exception {
        assertNotNull(this.adapter.getLog());
        assertNotSame(this.mockLogger, this.adapter.getLog());

        this.adapter.setLog(this.mockLogger);
        assertSame(this.mockLogger, this.adapter.getLog());
    }

    @Test
    public void testLoad() throws Exception {
        prepareLoad();

        this.adapter.load();

        validateLoad();
    }

    @Test
    public void testLoadEmpty() throws Exception {
        this.inputData = new ByteArrayInputStream(new byte[0]);

        this.brokerRegistry = Mockito.mock(BrokerRegistry.class);
        this.queueRegistry = Mockito.mock(DestinationRegistry.class);
        this.topicRegistry = Mockito.mock(DestinationRegistry.class);

        this.adapter.setFileStreamFactory(this.mockFileStreamFactory);

        this.adapter.setBrokerRegistry(this.brokerRegistry);
        this.adapter.setQueueRegistry(this.queueRegistry);
        this.adapter.setTopicRegistry(this.topicRegistry);

        this.adapter.load();

        Mockito.verifyZeroInteractions(this.brokerRegistry);
        Mockito.verifyZeroInteractions(this.queueRegistry);
        Mockito.verifyZeroInteractions(this.topicRegistry);
    }

    @Test
    public void testExceptionOnLoad() throws Exception {
        prepareLoad();

        FileInputStream mockFileInputStream2 = Mockito.mock(FileInputStream.class);

        IOException ioExc = new IOException("x-io-exc-x");
        Mockito.when(mockFileInputStream2.read()).thenThrow(ioExc);
        Mockito.when(mockFileInputStream2.read(Mockito.any(byte[].class))).thenThrow(ioExc);
        Mockito.when(mockFileInputStream2.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt()))
                .thenThrow(ioExc);

        Mockito.when(this.mockFileStreamFactory.getInputStream(this.path)).thenReturn(mockFileInputStream2);

        try {
            this.adapter.load();
            fail("missing expected exception");
        } catch (Exception actualExc) {
            assertTrue(ioExc.equals(actualExc) || ioExc.equals(actualExc.getCause()));
        }
    }

    @Test
    public void testException2OnLoad() throws Exception {
        prepareLoad();

        IOException ioExc = new IOException("x-io-exc-x");
        Mockito.when(this.mockIOStreamFactory.createInputReader(this.mockFileInputStream)).thenThrow(ioExc);

        this.adapter.setIoStreamFactory(this.mockIOStreamFactory);

        try {
            this.adapter.load();
            fail("missing expected exception");
        } catch (Exception actualExc) {
            assertSame(ioExc, actualExc);
        }
    }

    @Test
    public void testLoadNullContent() throws Exception {
        prepareLoad();
        this.inputData = new ByteArrayInputStream("{}".getBytes());

        this.adapter.load();
    }

    @Test
    public void testSave() throws Exception {
        prepareSave();

        this.adapter.save();

        validateSave();


        String json;

        this.outputData.reset();
        this.brokerRegistry.put("x-broker1-x", new BrokerInfo("x-broker1-x", "x-broker-name-x", "x-broker-url-x"));
        this.queueRegistry.put("x-queue1-x", new DestinationState("x-queue1-x", "x-broker-name-x"));
        this.topicRegistry.put("x-topic1-x", new DestinationState("x-topic1-x", "x-broker-name-x"));

        this.adapter.save();

        json = this.outputData.toString();
        assertTrue(json.matches("(?s).*\"brokerId\"[\\s]*:[\\s]*\"x-broker1-x\".*"));
        assertTrue(json.matches("(?s).*\"brokerName\"[\\s]*:[\\s]*\"x-broker-name-x\".*"));
        assertTrue(json.matches("(?s).*\"brokerUrl\"[\\s]*:[\\s]*\"x-broker-url-x\".*"));
        assertTrue(json.matches("(?s).*\"x-queue1-x\"[\\s]*:[\\s]*\\{[\\s]*\"name\"[\\s]*:[\\s]*\"x-queue1-x\".*"));
        assertTrue(json.matches("(?s).*\"x-topic1-x\"[\\s]*:[\\s]*\\{[\\s]*\"name\"[\\s]*:[\\s]*\"x-topic1-x\".*"));
    }

    @Test
    public void testSaveNullRegistries() throws Exception {
        this.adapter.setFileStreamFactory(this.mockFileStreamFactory);
        prepareSave();

        this.adapter.setBrokerRegistry(null);
        this.adapter.setQueueRegistry(null);
        this.adapter.setTopicRegistry(null);

        this.adapter.save();

        String json = this.outputData.toString();
        assertFalse(json.matches("(?s).*\"brokerId\"[\\s]*:[\\s]*\"x-broker1-x\".*"));
        assertFalse(json.matches("(?s).*\"brokerName\"[\\s]*:[\\s]*\"x-broker-name-x\".*"));
        assertFalse(json.matches("(?s).*\"brokerUrl\"[\\s]*:[\\s]*\"x-broker-url-x\".*"));
        assertFalse(json.matches("(?s).*\"x-queue1-x\"[\\s]*:[\\s]*\\{[\\s]*\"name\"[\\s]*:[\\s]*\"x-queue1-x\".*"));
        assertFalse(json.matches("(?s).*\"x-topic1-x\"[\\s]*:[\\s]*\\{[\\s]*\"name\"[\\s]*:[\\s]*\"x-topic1-x\".*"));
    }

    @Test
    public void testExceptionOnSave() throws Exception {
        prepareLoad();

        FileOutputStream mockFileOutputStream2 = Mockito.mock(FileOutputStream.class);

        IOException ioExc = new IOException("x-io-exc-x");
        Mockito.doThrow(ioExc).when(mockFileOutputStream2).write(Mockito.anyInt());
        Mockito.doThrow(ioExc).when(mockFileOutputStream2).write(Mockito.any(byte[].class));
        Mockito.doThrow(ioExc).when(mockFileOutputStream2)
                .write(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt());

        Mockito.when(this.mockFileStreamFactory.getOutputStream(this.path)).thenReturn(mockFileOutputStream2);

        try {
            this.adapter.save();
            fail("missing expected exception");
        } catch (Exception actualExc) {
            assertTrue(ioExc.equals(actualExc) || ioExc.equals(actualExc.getCause()));
        }
    }

    @Test
    public void testException2OnSave() throws Exception {
        prepareLoad();

        IOException ioExc = new IOException("x-io-exc-x");
        Mockito.when(this.mockIOStreamFactory.createOutputWriter(this.mockFileOutputStream)).thenThrow(ioExc);

        this.adapter.setIoStreamFactory(this.mockIOStreamFactory);

        try {
            this.adapter.save();
            fail("missing expected exception");
        } catch (Exception actualExc) {
            assertSame(ioExc, actualExc);
        }
    }

    protected void validateSave() {
        String json = this.outputData.toString();
        assertTrue(json.matches("(?s).*\"queueRegistry\"[\\s]*:[\\s]*\\{[\\s]*\\}[\\s]*.*"));
        assertTrue(json.matches("(?s).*\"topicRegistry\"[\\s]*:[\\s]*\\{[\\s]*\\}[\\s]*.*"));
        assertTrue(json.matches("(?s).*\"brokerRegistry\"[\\s]*:[\\s]*\\{[\\s]*\\}[\\s]*.*"));
    }

    @Test
    public void testLoadOnInit() throws Exception {
        this.prepareLoad();

        this.adapter.loadOnInit();

        this.validateLoad();
    }

    @Test
    public void testSaveOnDestroy() throws Exception {
        this.prepareSave();

        this.adapter.saveOnDestory();

        this.validateSave();
    }

    @Test
    public void testLoadOnInitException() throws Exception {
        IOException ioExc = new IOException("x-io-exc-x");
        Mockito.when(this.mockFileStreamFactory.getInputStream(this.path)).thenThrow(ioExc);

        this.adapter.setLog(this.mockLogger);
        this.adapter.setFileStreamFactory(this.mockFileStreamFactory);

        this.adapter.loadOnInit();

        Mockito.verify(this.mockLogger).error("Failed to load persistence file: file={}", this.path, ioExc);
    }

    @Test
    public void testSaveOnDestroyException() throws Exception {
        this.prepareSave();
        IOException ioExc = new IOException("x-io-exc-x");
        Mockito.when(this.mockFileStreamFactory.getOutputStream(this.path)).thenThrow(ioExc);

        this.adapter.setLog(this.mockLogger);

        this.adapter.saveOnDestory();

        Mockito.verify(this.mockLogger).error("Failed to save persistence file: file={}", this.path, ioExc);
    }

    protected void prepareLoad() throws Exception {
        this.inputData = new ByteArrayInputStream(this.getTestInput().getBytes());

        this.brokerRegistry = Mockito.mock(BrokerRegistry.class);
        this.queueRegistry = Mockito.mock(DestinationRegistry.class);
        this.topicRegistry = Mockito.mock(DestinationRegistry.class);

        this.adapter.setFileStreamFactory(this.mockFileStreamFactory);

        this.adapter.setBrokerRegistry(this.brokerRegistry);
        this.adapter.setQueueRegistry(this.queueRegistry);
        this.adapter.setTopicRegistry(this.topicRegistry);
    }

    protected void validateLoad() {
        Mockito.verify(this.brokerRegistry).put(Mockito.eq("x-broker1-x"), Mockito.any(BrokerInfo.class));
        Mockito.verify(this.queueRegistry).put(Mockito.eq("x-queue1-x"), Mockito.any(DestinationState.class));
        Mockito.verify(this.topicRegistry).put(Mockito.eq("x-topic1-x"), Mockito.any(DestinationState.class));
    }

    protected void prepareSave() {
        this.adapter.setFileStreamFactory(this.mockFileStreamFactory);

        this.adapter.setBrokerRegistry(this.brokerRegistry);
        this.adapter.setQueueRegistry(this.queueRegistry);
        this.adapter.setTopicRegistry(this.topicRegistry);
    }

    protected void prepareMockInputStream() throws Exception {
        Answer<Integer> readOneCharAnswer = new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                return inputData.read();
            }
        };

        Answer<Integer> readByteArrayAnswer = new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                return inputData.read((byte[]) invocationOnMock.getArguments()[0]);
            }
        };

        Answer<Integer> readByteArrayWithOffsetAndLengthAnswer = new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                return inputData.read((byte[]) args[0], (int) args[1], (int) args[2]);
            }
        };

        Mockito.when(this.mockFileInputStream.read()).thenAnswer(readOneCharAnswer);
        Mockito.when(this.mockFileInputStream.read(Mockito.any(byte[].class))).thenAnswer(readByteArrayAnswer);
        Mockito.when(this.mockFileInputStream.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt()))
                .thenAnswer(readByteArrayWithOffsetAndLengthAnswer);
    }

    protected void prepareMockOutputStream() throws Exception {
        Answer<Void> writeOneCharAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                outputData.write((int) invocationOnMock.getArguments()[0]);
                return null;
            }
        };

        Answer<Void> writeByteArrayAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                outputData.write((byte[]) invocationOnMock.getArguments()[0]);
                return null;
            }
        };

        Answer<Void> writeByteArrayWithOffsetAndLengthAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                outputData.write((byte[]) args[0], (int) args[1], (int) args[2]);
                return null;
            }
        };

        Mockito.doAnswer(writeOneCharAnswer).when(this.mockFileOutputStream).write(Mockito.anyInt());
        Mockito.doAnswer(writeByteArrayAnswer).when(this.mockFileOutputStream).write(Mockito.any(byte[].class));
        Mockito.doAnswer(writeByteArrayWithOffsetAndLengthAnswer).when(this.mockFileOutputStream)
                .write(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt());
    }

    protected String getTestInput() throws Exception {
        return "{\n" +
                "  \"queueRegistry\": {\n" +
                "    \"x-queue1-x\": {\n" +
                "      \"name\": \"x-queue1-x\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"topicRegistry\": {\n" +
                "    \"x-topic1-x\": {\n" +
                "      \"name\": \"x-topic1-x\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"brokerRegistry\": {\n" +
                "    \"x-broker1-x\": {\n" +
                "      \"brokerId\": \"x-broker1-x\",\n" +
                "      \"brokerName\": \"x-broker-name-x\",\n" +
                "      \"brokerUrl\": \"x-broker-url-x\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }
}
