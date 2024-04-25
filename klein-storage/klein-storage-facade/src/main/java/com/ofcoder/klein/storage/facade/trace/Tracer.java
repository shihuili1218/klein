package com.ofcoder.klein.storage.facade.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.TraceManager;
import com.ofcoder.klein.storage.facade.config.StorageProp;

public class Tracer {
    private final int blockSize;
    private final AtomicReference<Content> curTracer;
    private final TraceManager traceManager;

    public Tracer(final StorageProp op) {
        this.blockSize = op.getTraceBlockSize();
        this.curTracer = new AtomicReference<>(new Content(blockSize));
        this.traceManager = ExtensionLoader.getExtensionLoader(TraceManager.class).getJoin();
    }

    public void trace(String content) {
        if (!curTracer.get().append(content)) {
            renew();
        }
    }

    private void renew() {
        Content preTracer = curTracer.get();
        if (curTracer.compareAndSet(preTracer, new Content(blockSize))) {
            ThreadExecutor.execute(() -> {
                List<String> contents = preTracer.dump();
                traceManager.save(contents);
            });
        }
    }

    public void shutdown() {
        renew();
    }

    private static class Content {
        private final AtomicInteger index = new AtomicInteger(0);
        private final List<String> trace = new ArrayList<>();
        private final int threshold;

        public Content(int size) {
            if (size < 1) {
                throw new IllegalArgumentException("Size must be at least 1");
            }
            this.threshold = (int) (size * 0.75);
        }

        protected boolean append(final String content) {
            int index = this.index.getAndIncrement();
            trace.add(index, content);
            return trace.size() < threshold;
        }

        protected List<String> dump() {
            return trace;
        }
    }

}