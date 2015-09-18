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

import com.amlinv.activemq.monitor.TopologyStateFactory;
import com.amlinv.activemq.persistence.ApplicationPersistenceAdapter;
import com.amlinv.activemq.persistence.FileStreamFactory;
import com.amlinv.activemq.persistence.IOStreamFactory;
import com.amlinv.activemq.topo.registry.BrokerTopologyRegistry;
import com.amlinv.activemq.topo.registry.model.BrokerInfo;
import com.amlinv.activemq.topo.registry.model.DestinationInfo;
import com.amlinv.activemq.topo.registry.model.DestinationState;
import com.amlinv.activemq.topo.registry.model.LocatedBrokerId;
import com.amlinv.activemq.topo.registry.model.TopologyInfo;
import com.amlinv.activemq.topo.registry.model.TopologyState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by art on 5/16/15.
 */
public class JsonFileApplicationPersistenceAdapterV2 implements ApplicationPersistenceAdapter {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(JsonFileApplicationPersistenceAdapterV2.class);

    private final File storePath;
    private Logger log = DEFAULT_LOGGER;

    ///// REQUIRED INJECTIONS
    private BrokerTopologyRegistry topologyRegistry;
    private TopologyStateFactory topologyStateFactory;

    /////
    private FileStreamFactory fileStreamFactory = new DefaultFileStreamFactory();
    private IOStreamFactory ioStreamFactory = new DefaultIOStreamFactory();

    public JsonFileApplicationPersistenceAdapterV2(File storePath) {
        this.storePath = storePath;
    }

    public JsonFileApplicationPersistenceAdapterV2(String storePath) {
        this(new File(storePath));
    }

    public FileStreamFactory getFileStreamFactory() {
        return fileStreamFactory;
    }

    public void setFileStreamFactory(FileStreamFactory fileStreamFactory) {
        this.fileStreamFactory = fileStreamFactory;
    }

    public IOStreamFactory getIoStreamFactory() {
        return ioStreamFactory;
    }

    public void setIoStreamFactory(IOStreamFactory ioStreamFactory) {
        this.ioStreamFactory = ioStreamFactory;
    }

    public TopologyStateFactory getTopologyStateFactory() {
        return topologyStateFactory;
    }

    public void setTopologyStateFactory(TopologyStateFactory topologyStateFactory) {
        this.topologyStateFactory = topologyStateFactory;
    }

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public BrokerTopologyRegistry getTopologyRegistry() {
        return topologyRegistry;
    }

    public void setTopologyRegistry(BrokerTopologyRegistry topologyRegistry) {
        this.topologyRegistry = topologyRegistry;
    }

    @Override
    public void load() throws IOException {
        log.info("Loading persistence data from file: file={}", this.storePath);

        Gson gson = new GsonBuilder()
                .create();

        try (InputStream inputStream = this.fileStreamFactory.getInputStream(this.storePath)) {
            try (Reader rdr = this.ioStreamFactory.createInputReader(inputStream)) {
                MyDataModel updated = gson.fromJson(rdr, MyDataModel.class);

                if ( updated != null ) {
                    // Update the existing data with the new so that anyone else sharing those structures will also share
                    //  the updates.
                    this.update(updated);
                } else {
                    log.info("Persistence file empty: file={}", this.storePath);
                }
            }
        }
    }

    @Override
    public void save() throws IOException {
        log.info("Saving V2 persistence data to file: file={}", this.storePath);

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        try (OutputStream outputStream = this.fileStreamFactory.getOutputStream(this.storePath)) {
            try (Writer writer = this.ioStreamFactory.createOutputWriter(outputStream)) {
                MyDataModel outBound = new MyDataModel();

                if (this.topologyRegistry != null) {
                    outBound.topologyRegistry = new HashMap<>();

                    for (Map.Entry<String, TopologyState> topologyEntry : this.topologyRegistry.asMap().entrySet()) {
                        TopologyState topologyState = topologyEntry.getValue();

                        outBound.topologyRegistry.put(topologyEntry.getKey(), this.createMyTopologyInfo(topologyState));
                    }
                }

                gson.toJson(outBound, writer);
            }
        }
    }

    public void loadOnInit() {
        try {
            this.load();
        } catch (Exception ioExc) {
            log.error("Failed to load persistence file: file={}", this.storePath, ioExc);
        }
    }

    public void saveOnDestory() {
        try {
            this.save();
        } catch (Exception ioExc) {
            log.error("Failed to save persistence file: file={}", this.storePath, ioExc);
        }
    }

    protected MyTopologyInfo createMyTopologyInfo (TopologyState topologyState) {
        MyTopologyInfo result = new MyTopologyInfo();

        if (topologyState.getBrokerRegistry() != null) {
            //
            // Convert from registry format to persistence format.
            //
            result.brokerDetails = new LinkedList<>();
            for (Map.Entry<LocatedBrokerId, BrokerInfo> brokerEntry :
                    topologyState.getBrokerRegistry().asMap().entrySet()) {

                result.brokerDetails.add(new MyBrokerDetails(brokerEntry.getKey(), brokerEntry.getValue()));
            }
        }

        if (topologyState.getQueueRegistry() != null) {
            result.queueRegistry = new HashMap<>();

            for (String queueName : topologyState.getQueueRegistry().asMap().keySet()) {
                result.queueRegistry.put(queueName, new DestinationInfo(queueName));
            }
        }

        if (topologyState.getTopicRegistry() != null) {
            result.topicRegistry = new HashMap<>();

            for (String queueName : topologyState.getQueueRegistry().asMap().keySet()) {
                result.topicRegistry.put(queueName, new DestinationInfo(queueName));
            }
        }

        return result;
    }

    protected void update (MyDataModel source) {
        if (source.topologyRegistry != null) {
            for (Map.Entry<String, MyTopologyInfo> topologyEntry : source.topologyRegistry.entrySet()) {
                MyTopologyInfo topologyInfo = topologyEntry.getValue();

                TopologyInfo info = new TopologyInfo(
                        topologyEntry.getKey(), Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);

                TopologyState topologyState = this.topologyStateFactory.createTopologyState(info);

                // Add all of the configured brokers to the registry
                if (topologyInfo.brokerDetails != null) {
                    for (MyBrokerDetails brokerDetails : topologyInfo.brokerDetails) {
                        topologyState.getBrokerRegistry().put(brokerDetails.locatedBrokerId, brokerDetails.brokerInfo);
                    }
                }

                // Add all of the configured queues to the registry
                if (topologyInfo.queueRegistry != null) {
                    for (String queueName : topologyInfo.queueRegistry.keySet()) {
                        topologyState.getQueueRegistry().put(queueName, new DestinationState(queueName));
                    }
                }

                // Add all of the configured topics to the registry
                if (topologyInfo.topicRegistry != null) {
                    for (String queueName : topologyInfo.topicRegistry.keySet()) {
                        topologyState.getTopicRegistry().put(queueName, new DestinationState(queueName));
                    }
                }

                // Now add this topology to the registry
                this.topologyRegistry.put(topologyEntry.getKey(), topologyState);
            }
        }
    }

    protected static class MyDataModel {
        public Map<String, MyTopologyInfo> topologyRegistry;
    }

    protected static class MyTopologyInfo {
        public List<MyBrokerDetails> brokerDetails;
        public Map<String, DestinationInfo> queueRegistry;
        public Map<String, DestinationInfo> topicRegistry;
    }

    protected static class MyBrokerDetails {
        public LocatedBrokerId locatedBrokerId;
        public BrokerInfo brokerInfo;

        public MyBrokerDetails(LocatedBrokerId locatedBrokerId, BrokerInfo brokerInfo) {
            this.locatedBrokerId = locatedBrokerId;
            this.brokerInfo = brokerInfo;
        }
    }
}
