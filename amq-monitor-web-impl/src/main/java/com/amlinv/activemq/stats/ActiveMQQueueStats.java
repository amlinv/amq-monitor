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

package com.amlinv.activemq.stats;

import com.amlinv.activemq.monitor.model.ActiveMQQueueJmxStats;

/**
 * Created by art on 5/28/15.
 */
public class ActiveMQQueueStats extends ActiveMQQueueJmxStats {
    private double dequeueRate1Minute = 0.0;
    private double dequeueRate1Hour = 0.0;
    private double dequeueRate1Day = 0.0;

    private double enqueueRate1Minute = 0.0;
    private double enqueueRate1Hour = 0.0;
    private double enqueueRate1Day = 0.0;

    public ActiveMQQueueStats(String brokerName, String queueName) {
        super(brokerName, queueName);
    }

    public double getDequeueRate1Minute() {
        return dequeueRate1Minute;
    }

    public void setDequeueRate1Minute(double dequeueRate1Minute) {
        this.dequeueRate1Minute = dequeueRate1Minute;
    }

    public double getDequeueRate1Hour() {
        return dequeueRate1Hour;
    }

    public void setDequeueRate1Hour(double dequeueRate1Hour) {
        this.dequeueRate1Hour = dequeueRate1Hour;
    }

    public double getDequeueRate1Day() {
        return dequeueRate1Day;
    }

    public void setDequeueRate1Day(double dequeueRate1Day) {
        this.dequeueRate1Day = dequeueRate1Day;
    }

    public double getEnqueueRate1Minute() {
        return enqueueRate1Minute;
    }

    public void setEnqueueRate1Minute(double enqueueRate1Minute) {
        this.enqueueRate1Minute = enqueueRate1Minute;
    }

    public double getEnqueueRate1Hour() {
        return enqueueRate1Hour;
    }

    public void setEnqueueRate1Hour(double enqueueRate1Hour) {
        this.enqueueRate1Hour = enqueueRate1Hour;
    }

    public double getEnqueueRate1Day() {
        return enqueueRate1Day;
    }

    public void setEnqueueRate1Day(double enqueueRate1Day) {
        this.enqueueRate1Day = enqueueRate1Day;
    }
}
