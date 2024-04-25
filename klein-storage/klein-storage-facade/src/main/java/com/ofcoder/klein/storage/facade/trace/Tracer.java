package com.ofcoder.klein.storage.facade.trace;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.TraceManager;
import com.ofcoder.klein.storage.facade.config.StorageProp;

public class Tracer {
    private final int blockSize;
    private final AtomicReference<TracerImpl> curTracer;
    private final TraceManager traceManager;

    public Tracer(final StorageProp op) {
        this.blockSize = op.getTraceBlockSize();
        this.curTracer = new AtomicReference<>(new TracerImpl(blockSize));
        this.traceManager = ExtensionLoader.getExtensionLoader(TraceManager.class).getJoin();
    }

    public void trace(String content) {
        if (!curTracer.get().trace(content)) {
            renew();
        }
    }

    private void renew() {
        TracerImpl preTracer = curTracer.get();
        if (curTracer.compareAndSet(preTracer, new TracerImpl(blockSize))) {
            ThreadExecutor.execute(() -> {
                List<String> contents = preTracer.dump();
                traceManager.save(contents);
            });
        }
    }

    public void shutdown() {
        renew();
    }

}