/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ofcoder.klein.common.disruptor;

import java.util.concurrent.ThreadFactory;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.ofcoder.klein.common.util.KleinThreadFactory;
import com.ofcoder.klein.common.util.Requires;

/**
 * A builder to build a disruptor instance.
 *
 * @author boyan(boyan @ antfin.com)
 */
public final class DisruptorBuilder<T> {
    private EventFactory<T> eventFactory;
    private Integer ringBufferSize;
    private ThreadFactory threadFactory = KleinThreadFactory.create("disruptor-", true);
    private ProducerType producerType = ProducerType.MULTI;
    private WaitStrategy waitStrategy = new BlockingWaitStrategy();

    private DisruptorBuilder() {
    }

    /**
     * New instance.
     *
     * @param <T> Disruptor element type
     * @return DisruptorBuilder
     */
    public static <T> DisruptorBuilder<T> newInstance() {
        return new DisruptorBuilder<>();
    }

    /**
     * Get EventFactory.
     *
     * @return Disruptor's EventFactory
     */
    public EventFactory<T> getEventFactory() {
        return this.eventFactory;
    }

    /**
     * Set EventFactory.
     *
     * @param eventFactory Disruptor's EventFactory
     * @return DisruptorBuilder
     */
    public DisruptorBuilder<T> setEventFactory(final EventFactory<T> eventFactory) {
        this.eventFactory = eventFactory;
        return this;
    }

    /**
     * Get RingBufferSize.
     *
     * @return RingBuffer's size
     */
    public int getRingBufferSize() {
        return this.ringBufferSize;
    }

    /**
     * Set RingBufferSize.
     *
     * @param ringBufferSize RingBuffer's size
     * @return DisruptorBuilder
     */
    public DisruptorBuilder<T> setRingBufferSize(final int ringBufferSize) {
        this.ringBufferSize = ringBufferSize;
        return this;
    }

    /**
     * Get ThreadFactory.
     *
     * @return Disruptor's threadFactory
     */
    public ThreadFactory getThreadFactory() {
        return this.threadFactory;
    }

    /**
     * Set ThreadFactory.
     *
     * @param threadFactory Disruptor's threadFactory
     * @return DisruptorBuilder
     */
    public DisruptorBuilder<T> setThreadFactory(final ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    /**
     * Get ProducerType.
     *
     * @return ProducerType
     */
    public ProducerType getProducerType() {
        return this.producerType;
    }

    /**
     * Set ProducerType.
     *
     * @param producerType Disruptor's producerType
     * @return DisruptorBuilder
     */
    public DisruptorBuilder<T> setProducerType(final ProducerType producerType) {
        this.producerType = producerType;
        return this;
    }

    /**
     * Get WaitStrategy.
     *
     * @return WaitStrategy
     */
    public WaitStrategy getWaitStrategy() {
        return this.waitStrategy;
    }

    /**
     * Set WaitStrategy.
     *
     * @param waitStrategy Disruptor's WaitStrategy
     * @return DisruptorBuilder
     */
    public DisruptorBuilder<T> setWaitStrategy(final WaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
        return this;
    }

    /**
     * Build a Disruptor.
     *
     * @return Disruptor
     */
    public Disruptor<T> build() {
        Requires.requireNonNull(this.ringBufferSize, " Ring buffer size not set");
        Requires.requireNonNull(this.eventFactory, "Event factory not set");
        return new Disruptor<>(this.eventFactory, this.ringBufferSize, this.threadFactory, this.producerType,
                this.waitStrategy);
    }

}
