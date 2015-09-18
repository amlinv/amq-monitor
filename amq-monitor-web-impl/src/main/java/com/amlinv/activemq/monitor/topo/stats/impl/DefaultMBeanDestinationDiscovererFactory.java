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

package com.amlinv.activemq.monitor.topo.stats.impl;

import com.amlinv.activemq.monitor.topo.stats.MBeanDestinationDiscovererFactory;
import com.amlinv.activemq.topo.discovery.MBeanDestinationDiscoverer;
import com.amlinv.activemq.topo.jmxutil.polling.InvalidJmxLocationException;
import com.amlinv.activemq.topo.jmxutil.polling.JmxActiveMQUtil2;
import com.amlinv.activemq.topo.registry.DestinationRegistry;

/**
 * Created by art on 9/14/15.
 */
public class DefaultMBeanDestinationDiscovererFactory implements MBeanDestinationDiscovererFactory {
    ///// REQUIRED INJECTIONS
    private JmxActiveMQUtil2 jmxActiveMQUtil;

    public JmxActiveMQUtil2 getJmxActiveMQUtil() {
        return jmxActiveMQUtil;
    }

    public void setJmxActiveMQUtil(JmxActiveMQUtil2 jmxActiveMQUtil) {
        this.jmxActiveMQUtil = jmxActiveMQUtil;
    }

    @Override
    public MBeanDestinationDiscoverer createDiscoverer(String destinationType, String brokerId, String location,
                                                       DestinationRegistry destinationRegistry)
            throws InvalidJmxLocationException {

        // TBD: change brokerId to a locatedBrokerId
        MBeanDestinationDiscoverer result = new MBeanDestinationDiscoverer(destinationType, brokerId);
        result.setmBeanAccessConnectionFactory(jmxActiveMQUtil.getLocationConnectionFactory(location));
        result.setRegistry(destinationRegistry);

        return result;
    }
}
