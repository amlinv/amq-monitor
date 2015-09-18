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

package com.amlinv.activemq.monitor.topo.stats;

import com.amlinv.activemq.topo.discovery.MBeanDestinationDiscoverer;
import com.amlinv.activemq.topo.jmxutil.polling.InvalidJmxLocationException;
import com.amlinv.activemq.topo.registry.DestinationRegistry;

/**
 * Created by art on 9/14/15.
 */
public interface MBeanDestinationDiscovererFactory {
    MBeanDestinationDiscoverer createDiscoverer(String destinationType, String brokerName, String location,
                                                DestinationRegistry destinationRegistry) throws InvalidJmxLocationException;
}