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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by art on 9/1/15.
 */
public class DefaultFileStreamFactory implements FileStreamFactory {
    @Override
    public FileInputStream getInputStream(File file) throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public FileOutputStream getOutputStream(File file) throws IOException {
        return new FileOutputStream(file);
    }
}
