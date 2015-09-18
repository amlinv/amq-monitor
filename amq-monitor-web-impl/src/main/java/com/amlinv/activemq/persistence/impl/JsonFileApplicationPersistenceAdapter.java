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

import com.amlinv.activemq.persistence.ApplicationPersistenceAdapter;
import com.amlinv.activemq.persistence.FileStreamFactory;
import com.amlinv.activemq.persistence.IOStreamFactory;
import com.amlinv.activemq.topo.registry.BrokerRegistry;
import com.amlinv.activemq.topo.registry.DestinationRegistry;
import com.amlinv.activemq.topo.registry.model.BrokerInfo;
import com.amlinv.activemq.topo.registry.model.DestinationInfo;
import com.amlinv.activemq.topo.registry.model.DestinationState;
import com.amlinv.activemq.topo.registry.model.LocatedBrokerId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistence adapter that auto-detects the version of the input and processes accordingly.
 *
 * Created by art on 5/16/15.
 */
public class JsonFileApplicationPersistenceAdapter implements ApplicationPersistenceAdapter {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(JsonFileApplicationPersistenceAdapter.class);

    private Logger log = DEFAULT_LOGGER;

    private JsonFileApplicationPersistenceAdapterV1 adapterV1;
    private JsonFileApplicationPersistenceAdapterV2 adapterV2;

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public JsonFileApplicationPersistenceAdapterV1 getAdapterV1() {
        return adapterV1;
    }

    public void setAdapterV1(JsonFileApplicationPersistenceAdapterV1 adapterV1) {
        this.adapterV1 = adapterV1;
    }

    public JsonFileApplicationPersistenceAdapterV2 getAdapterV2() {
        return adapterV2;
    }

    public void setAdapterV2(JsonFileApplicationPersistenceAdapterV2 adapterV2) {
        this.adapterV2 = adapterV2;
    }

    @Override
    public void load() throws IOException {
        try {
            log.info("Attempting to load V2 persistence");

            this.adapterV2.load();
        } catch (FileNotFoundException fnfExc) {
            log.info("Falling back to V1 persistence on file not found: {}", fnfExc.getMessage());
            log.debug("V2 file not found exception detail", fnfExc);

            this.adapterV1.load();
        }
    }

    /**
     * WARNING: saves the file in the older version which does not support topologies.
     *
     * @throws IOException
     */
    @Override
    public void save() throws IOException {
        this.adapterV2.save();
    }

    public void loadOnInit() {
        try {
            this.load();
        } catch (Exception ioExc) {
            log.error("Failed to load persistence file", ioExc);
        }
    }

    public void saveOnDestory() {
        try {
            this.save();
        } catch (Exception ioExc) {
            log.error("Failed to save persistence file", ioExc);
        }
    }
}
