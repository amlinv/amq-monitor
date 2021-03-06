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

import com.amlinv.activemq.persistence.IOStreamFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by art on 9/3/15.
 */
public class DefaultIOStreamFactory implements IOStreamFactory {
    @Override
    public InputStreamReader createInputReader(InputStream inputStream) throws IOException {
        return new InputStreamReader(inputStream);
    }

    @Override
    public OutputStreamWriter createOutputWriter(OutputStream outputStream) throws IOException {
        return new OutputStreamWriter(outputStream);
    }
}