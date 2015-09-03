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

package com.amlinv.activemq.monitor.model;

import com.amlinv.jmxutil.MBeanLocationParameterSource;
import com.amlinv.jmxutil.annotation.MBeanAttribute;
import com.amlinv.jmxutil.annotation.MBeanLocation;

/**
 * Created by art on 3/31/15.
 */
@MBeanLocation(onamePattern = "org.apache.activemq:type=Broker,brokerName=${brokerName},destinationType=Queue,destinationName=${queueName}")
public class ActiveMQQueueJmxStats implements MBeanLocationParameterSource {
    private final String brokerName;
    private final String queueName;

    private long queueSize;
    private long enqueueCount;
    private long dequeueCount;
    private long numConsumers;
    private long numProducers;
    private int cursorPercentUsage;
    private int memoryPercentUsage;
    private long inflightCount;

    public ActiveMQQueueJmxStats(String brokerName, String queueName) {
        this.brokerName = brokerName;
        this.queueName = queueName;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public String getQueueName() {
        return queueName;
    }

    public long getQueueSize() {
        return queueSize;
    }

    @MBeanAttribute(name = "QueueSize", type = long.class)
    public void setQueueSize(long queueSize) {
        this.queueSize = queueSize;
    }

    public long getEnqueueCount() {
        return enqueueCount;
    }

    @MBeanAttribute(name = "EnqueueCount", type = long.class)
    public void setEnqueueCount(long enqueueCount) {
        this.enqueueCount = enqueueCount;
    }

    public long getDequeueCount() {
        return dequeueCount;
    }

    @MBeanAttribute(name = "DequeueCount", type = long.class)
    public void setDequeueCount(long dequeueCount) {
        this.dequeueCount = dequeueCount;
    }

    public long getNumConsumers() {
        return numConsumers;
    }

    @MBeanAttribute(name = "ConsumerCount", type = long.class)
    public void setNumConsumers(long numConsumers) {
        this.numConsumers = numConsumers;
    }

    public long getNumProducers() {
        return numProducers;
    }

    @MBeanAttribute(name = "ProducerCount", type = long.class)
    public void setNumProducers(long numProducers) {
        this.numProducers = numProducers;
    }

    public int getCursorPercentUsage() {
        return cursorPercentUsage;
    }

    @MBeanAttribute(name = "CursorPercentUsage", type = int.class)
    public void setCursorPercentUsage(int cursorPercentUsage) {
        this.cursorPercentUsage = cursorPercentUsage;
    }

    public int getMemoryPercentUsage() {
        return memoryPercentUsage;
    }

    @MBeanAttribute(name = "MemoryPercentUsage", type = int.class)
    public void setMemoryPercentUsage(int memoryPercentUsage) {
        this.memoryPercentUsage = memoryPercentUsage;
    }

    public long getInflightCount() {
        return inflightCount;
    }

    @MBeanAttribute(name = "InFlightCount", type = long.class)
    public void setInflightCount(long inflightCount) {
        this.inflightCount = inflightCount;
    }

    @Override
    public String getParameter(String parameterName) {
        if ( parameterName.equals("brokerName") ) {
            return  this.brokerName;
        } else if ( parameterName.equals("queueName") ) {
            return  this.queueName;
        }

        return null;
    }

    /**
     * Return a new queue stats structure with the total of the stats from this structure and the one given.  Returning
     * a new structure keeps all three structures unchanged, in the manner of immutability, to make it easier to have
     * safe usage under concurrency.  Note that non-count values are copied out from this instance; those values from
     * the given other stats are ignored.
     *
     * @param other
     * @param resultBrokerName
     * @return
     */
    public ActiveMQQueueJmxStats addCounts(ActiveMQQueueJmxStats other, String resultBrokerName) {
        ActiveMQQueueJmxStats result = new ActiveMQQueueJmxStats(resultBrokerName, this.queueName);
        result.setCursorPercentUsage(this.getCursorPercentUsage());
        result.setDequeueCount(this.getDequeueCount() + other.getDequeueCount());
        result.setEnqueueCount(this.getEnqueueCount() + other.getEnqueueCount());
        result.setMemoryPercentUsage(this.getMemoryPercentUsage());
        result.setNumConsumers(this.getNumConsumers() + other.getNumConsumers());
        result.setNumProducers(this.getNumProducers() + other.getNumProducers());
        result.setQueueSize(this.getQueueSize() + other.getQueueSize());
        result.setInflightCount(this.getInflightCount() + other.getInflightCount());

        return  result;
    }

    /**
     * Return a duplicate of this queue stats structure.
     *
     * @return new queue stats structure with the same values.
     */
    public ActiveMQQueueJmxStats dup (String brokerName) {
        ActiveMQQueueJmxStats result = new ActiveMQQueueJmxStats(brokerName, this.queueName);
        this.copyOut(result);

        return  result;
    }

    /**
     * Copy out the values to the given destination.
     *
     * @param other target stats object to receive the values from this one.
     */
    public void copyOut (ActiveMQQueueJmxStats other) {
        other.setCursorPercentUsage(this.getCursorPercentUsage());
        other.setDequeueCount(this.getDequeueCount());
        other.setEnqueueCount(this.getEnqueueCount());
        other.setMemoryPercentUsage(this.getMemoryPercentUsage());
        other.setNumConsumers(this.getNumConsumers());
        other.setNumProducers(this.getNumProducers());
        other.setQueueSize(this.getQueueSize());
        other.setInflightCount(this.getInflightCount());
    }

    /**
     * Subtract the given stats from this one and return the difference.  Note that non-count values are copied out
     * from this instance; those values from the given other stats are ignored.
     */
    public ActiveMQQueueJmxStats subtractCounts(ActiveMQQueueJmxStats other) {
        ActiveMQQueueJmxStats result = new ActiveMQQueueJmxStats(this.brokerName, this.queueName);

        result.setCursorPercentUsage(this.getCursorPercentUsage());
        result.setDequeueCount(this.getDequeueCount() - other.getDequeueCount());
        result.setEnqueueCount(this.getEnqueueCount() - other.getEnqueueCount());
        result.setMemoryPercentUsage(this.getMemoryPercentUsage());
        result.setNumConsumers(this.getNumConsumers() - other.getNumConsumers());
        result.setNumProducers(this.getNumProducers() - other.getNumProducers());
        result.setQueueSize(this.getQueueSize() - other.getQueueSize());
        result.setInflightCount(this.getInflightCount() - other.getInflightCount());

        return result;
    }
}
