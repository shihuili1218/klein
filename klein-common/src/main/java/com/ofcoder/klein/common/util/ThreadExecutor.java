package com.ofcoder.klein.common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: 释慧利
 */
public class ThreadExecutor {
    // fixme to user custom
    private final static ExecutorService EXECUTOR = new ThreadPoolExecutor(10, 30,
            300L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(10000),
            KleinThreadFactory.create("audit-predict", true));


    public static void submit(Runnable task){
        EXECUTOR.submit(task);
    }


}
