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

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import static org.junit.Assert.*;

/**
 * Created by art on 9/3/15.
 */
public class DefaultIOStreamFactoryTest {

    private DefaultIOStreamFactory factory;

    @Before
    public void setupTest() throws Exception {
        factory = new DefaultIOStreamFactory();
    }

    @Test
    public void testGetInputReader() throws Exception {
        InputStreamReader rdr = this.factory.createInputReader(new ByteArrayInputStream("xxx".getBytes()));
        assertNotNull(rdr);

        rdr.close();
    }

    @Test
    public void testGetOutputWriter() throws Exception {
        OutputStreamWriter writer = this.factory.createOutputWriter(new ByteArrayOutputStream());
        assertNotNull(writer);

        writer.close();
    }
}