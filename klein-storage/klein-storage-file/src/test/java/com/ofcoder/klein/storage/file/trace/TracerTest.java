package com.ofcoder.klein.storage.file.trace;

import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.TraceManager;
import com.ofcoder.klein.storage.facade.config.StorageProp;
import com.ofcoder.klein.storage.facade.trace.Tracer;
import junit.framework.TestCase;

public class TracerTest extends TestCase {

    public void testTrace() throws InterruptedException {
        StorageProp prop = new StorageProp();
        prop.setTraceBlockSize(20);

        ExtensionLoader.getExtensionLoader(TraceManager.class).register("file", prop);
        Tracer tracer = new Tracer(prop);
        for (int i = 0; i < 50; i++) {
            tracer.trace(i + "zzzzzzzz");
        }

        Thread.sleep(500L);
    }
}