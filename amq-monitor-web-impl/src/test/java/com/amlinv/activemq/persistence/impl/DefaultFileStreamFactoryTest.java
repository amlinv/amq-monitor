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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import static org.junit.Assert.*;

/**
 * Created by art on 9/1/15.
 */
public class DefaultFileStreamFactoryTest {

    private Logger LOG = LoggerFactory.getLogger(DefaultFileStreamFactoryTest.class);

    private DefaultFileStreamFactory factory;

    private File file;

    @Before
    public void setupTest() throws Exception {
        this.factory = new DefaultFileStreamFactory();

        this.file = File.createTempFile("amq-monitor-web-impl-test", ".txt");
        this.LOG.debug("using temporary file {}", this.file);
        this.file.deleteOnExit();
    }

    @After
    public void cleanupTest() throws Exception {
        this.LOG.debug("removing temporary file {}", this.file);
        this.file.delete();
    }

    @Test
    public void testGetInputStream() throws Exception {
        try ( FileInputStream stream = this.factory.getInputStream(this.file) )
        {
            assertNotNull(stream);
        }
    }

    @Test
    public void testGetOutputStream() throws Exception {
        try ( FileOutputStream stream = this.factory.getOutputStream(this.file) )
        {
            assertNotNull(stream);
        }
    }
}