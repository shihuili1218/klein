package com.ofcoder.klein.common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author 释慧利
 */
public class ThreadExecutor {
    // fixme to user custom
    private final static ExecutorService EXECUTOR = new ThreadPoolExecutor(cpus(), Math.max(100, cpus() * 5),
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            KleinThreadFactory.create("common-thread-", true));


    private static int cpus() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static void submit(Runnable task) {
        EXECUTOR.submit(task);
    }


}
