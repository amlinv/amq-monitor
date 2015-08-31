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

package com.amlinv.activemq.monitor.web;

import com.amlinv.javasched.Scheduler;

/**
 * Spring injection class for getting the websocket registry into the MonitorWebsocket.
 *
 * Created by art on 5/14/15.
 */
public class MonitorWebsocketStaticInjector {
    public MonitorWebsocketRegistry getRegistry() {
        return MonitorWebsocket.getRegistry();
    }

    public void setRegistry(MonitorWebsocketRegistry registry) {
        MonitorWebsocket.setRegistry(registry);
    }

    public long getSendTimeout () {
        return MonitorWebsocket.getSendTimeout();
    }

    public void setSendTimeout (long newTimeout) {
        MonitorWebsocket.setSendTimeout(newTimeout);
    }

    public void setScheduler(Scheduler newScheduler) {
        MonitorWebsocket.setScheduler(newScheduler);
    }
}
