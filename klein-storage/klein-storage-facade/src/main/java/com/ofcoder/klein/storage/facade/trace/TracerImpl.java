package com.ofcoder.klein.storage.facade.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class TracerImpl {
    private final AtomicInteger index = new AtomicInteger(0);
    private final List<String> trace = new ArrayList<>();
    private final int threshold;

    public TracerImpl(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Size must be at least 1");
        }
        this.threshold = (int) (size * 0.75);
    }

    protected boolean trace(final String content) {
        int index = this.index.getAndIncrement();
        trace.add(index, content);
        return trace.size() < threshold;
    }

    protected List<String> dump() {

        return trace;
    }
}

