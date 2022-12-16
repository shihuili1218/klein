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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.ExceptionHandler;

/**
 * Disruptor exception handler.
 *
 * @author boyan (boyan@alibaba-inc.com)
 */
public final class DisruptorExceptionHandler<T> implements ExceptionHandler<T> {

    private static final Logger LOG = LoggerFactory.getLogger(DisruptorExceptionHandler.class);

    private final String name;
    private final OnEventException<T> onEventException;

    public DisruptorExceptionHandler(final String name) {
        this(name, null);
    }

    public DisruptorExceptionHandler(final String name, final OnEventException<T> onEventException) {
        this.name = name;
        this.onEventException = onEventException;
    }

    @Override
    public void handleOnStartException(final Throwable ex) {
        LOG.error("Fail to start {} disruptor", this.name, ex);
    }

    @Override
    public void handleOnShutdownException(final Throwable ex) {
        LOG.error("Fail to shutdown {} disruptor", this.name, ex);
    }

    @Override
    public void handleEventException(final Throwable ex, final long sequence, final T event) {
        LOG.error("Handle {} disruptor event error, event is {}", this.name, event, ex);
        if (this.onEventException != null) {
            this.onEventException.onException(event, ex);
        }
    }

    public interface OnEventException<T> {

        /**
         * Occur exception.
         *
         * @param event event
         * @param ex    exception
         */
        void onException(T event, Throwable ex);
    }

}
