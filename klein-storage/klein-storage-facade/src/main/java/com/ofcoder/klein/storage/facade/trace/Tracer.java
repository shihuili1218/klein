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

package com.ofcoder.klein.storage.facade.trace;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.TraceManager;
import com.ofcoder.klein.storage.facade.config.StorageProp;

public class Tracer {
    private final int blockSize;
    private final String name;
    private final AtomicReference<Content> curTracer;
    private final TraceManager traceManager;

    public Tracer(final String name, final StorageProp op) {
        this.name = name;
        this.blockSize = op.getTraceBlockSize();
        this.curTracer = new AtomicReference<>(new Content(blockSize));
        this.traceManager = ExtensionLoader.getExtensionLoader(TraceManager.class).getJoin();
    }

    /**
     * append content into trace container.
     * @param content content
     */
    public void trace(final String content) {
        if (!curTracer.get().append(content)) {
            renew();
        }
    }

    private void renew() {
        Content preTracer = curTracer.get();
        if (curTracer.compareAndSet(preTracer, new Content(blockSize))) {
            ThreadExecutor.execute(() -> {
                List<String> contents = preTracer.dump();
                traceManager.save(name, contents);
            });
        }
    }

    /**
     * save at shutdown.
     */
    public void shutdown() {
        renew();
    }

    private static class Content {
        private final AtomicInteger index = new AtomicInteger(0);
        private final String[] trace;
        private final int threshold;

        Content(final int size) {
            if (size < 1) {
                throw new IllegalArgumentException("Size must be at least 1");
            }
            trace = new String[size];
            this.threshold = (int) (size * 0.9);
        }

        protected boolean append(final String content) {
            int index = this.index.getAndIncrement();
            trace[index] = content;
            return index < threshold;
        }

        protected List<String> dump() {
            return Arrays.asList(Arrays.copyOfRange(trace, 0, index.get()));
        }
    }

}
