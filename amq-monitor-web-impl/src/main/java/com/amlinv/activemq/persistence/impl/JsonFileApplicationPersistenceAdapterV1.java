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
import java.util.Map;

/**
 * Version 1 of the persistence adapter.
 *
 * Created by art on 5/16/15.
 */
public class JsonFileApplicationPersistenceAdapterV1 implements ApplicationPersistenceAdapter {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(JsonFileApplicationPersistenceAdapterV1.class);

    public static final String DEFAULT_TOPOLOGY_NAME = "unnamed";

    private final File storePath;
    private Logger log = DEFAULT_LOGGER;

    ///// REQUIRED INJECTIONS
    private BrokerTopologyRegistry topologyRegistry;
    private TopologyStateFactory topologyStateFactory;

    ///// OPTIONAL INJECTIONS:
    private String topologyName = DEFAULT_TOPOLOGY_NAME;

    private FileStreamFactory fileStreamFactory = new DefaultFileStreamFactory();
    private IOStreamFactory ioStreamFactory = new DefaultIOStreamFactory();

    public JsonFileApplicationPersistenceAdapterV1(File storePath) {
        this.storePath = storePath;
    }

    public JsonFileApplicationPersistenceAdapterV1(String storePath) {
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

    public TopologyStateFactory getTopologyStateFactory() {
        return topologyStateFactory;
    }

    public void setTopologyStateFactory(TopologyStateFactory topologyStateFactory) {
        this.topologyStateFactory = topologyStateFactory;
    }

    public String getTopologyName() {
        return topologyName;
    }

    public void setTopologyName(String topologyName) {
        this.topologyName = topologyName;
    }

    @Override
    public void load() throws IOException {
        log.info("Loading V1 persistence data from file: file={}", this.storePath);

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

    /**
     * WARNING: saves the file in the older version which does not support topologies.
     *
     * @throws IOException
     */
    @Override
    public void save() throws IOException {
        log.info("Saving V1 persistence data to file: file={}", this.storePath);
        log.warn("V1 persistence does not support topologies");

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        try (OutputStream outputStream = this.fileStreamFactory.getOutputStream(this.storePath)) {
            try (Writer writer = this.ioStreamFactory.createOutputWriter(outputStream)) {
                MyDataModel outBound = new MyDataModel();

                if (this.topologyRegistry != null) {
                    TopologyState topologyState = this.topologyRegistry.get(this.topologyName);

                    if (topologyState != null) {
                        if (topologyState.getBrokerRegistry() != null) {
                            outBound.brokerRegistry = new HashMap<>();

                            for (Map.Entry<LocatedBrokerId, BrokerInfo> entry :
                                    topologyState.getBrokerRegistry().asMap().entrySet()) {

                                outBound.brokerRegistry.put(entry.getKey().getLocation(), entry.getValue());
                            }
                        }

                        if (topologyState.getQueueRegistry() != null) {
                            outBound.queueRegistry = topologyState.getQueueRegistry().asMap();
                        }

                        if (topologyState.getTopicRegistry() != null) {
                            outBound.topicRegistry = topologyState.getTopicRegistry().asMap();
                        }
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

    protected void update (MyDataModel source) {
        // Make sure the target topology exists; don't sweat the extra work if so.
        TopologyInfo newInfo =
                new TopologyInfo(topologyName, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        TopologyState newTopologyState = this.topologyStateFactory.createTopologyState(newInfo);
        this.topologyRegistry.putIfAbsent(topologyName, newTopologyState);

        // Now grab the "real one" regardless.
        TopologyState topologyState = this.topologyRegistry.get(this.topologyName);

        if (source.queueRegistry != null) {
            for (String queueName : source.queueRegistry.keySet()) {
                topologyState.getQueueRegistry().put(queueName,
                        new DestinationState(source.queueRegistry.get(queueName)));
            }
        }

        if (source.topicRegistry != null) {
            for (String topicName : source.topicRegistry.keySet()) {
                topologyState.getTopicRegistry().put(topicName,
                        new DestinationState(source.topicRegistry.get(topicName)));
            }
        }

        if ( source.brokerRegistry != null ) {
            for (Map.Entry<String, BrokerInfo> entry : source.brokerRegistry.entrySet()) {
                topologyState.getBrokerRegistry()
                        .put(new LocatedBrokerId(entry.getKey(), entry.getValue().getBrokerName()), entry.getValue());
            }
        }
    }

    protected static class MyDataModel {
        public Map<String, ? extends DestinationInfo> queueRegistry;
        public Map<String, ? extends DestinationInfo> topicRegistry;
        public Map<String, BrokerInfo> brokerRegistry;
    }
}
