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

package com.amlinv.activemq.persistence;

import com.amlinv.activemq.topo.registry.BrokerRegistry;
import com.amlinv.activemq.topo.registry.DestinationRegistry;

import java.io.IOException;

/**
 * Created by art on 5/16/15.
 */
public interface ApplicationPersistenceAdapter {
    void setQueueRegistry(DestinationRegistry queueRegistry);
    void setTopicRegistry(DestinationRegistry topicRegistry);
    void setBrokerRegistry(BrokerRegistry brokerRegistry);
    void load() throws IOException;
    void save() throws IOException;
}
