package com.ofcoder.klein.common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 释慧利
 */
public class ThreadExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(ThreadExecutor.class);
    // fixme to user custom
    private final static ExecutorService EXECUTOR = new ThreadPoolExecutor(cpus(), Math.max(100, cpus() * 5),
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            KleinThreadFactory.create("common-thread", true));


    private static int cpus() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static void submit(Runnable task) {
        EXECUTOR.submit(task);
    }


    /**
     * @see #shutdownAndAwaitTermination(ExecutorService, long)
     */
    public static boolean shutdownAndAwaitTermination(final ExecutorService pool) {
        return shutdownAndAwaitTermination(pool, 1000);
    }

    /**
     * The following method shuts down an {@code ExecutorService} in two
     * phases, first by calling {@code shutdown} to reject incoming tasks,
     * and then calling {@code shutdownNow}, if necessary, to cancel any
     * lingering tasks.
     */
    public static boolean shutdownAndAwaitTermination(final ExecutorService pool, final long timeoutMillis) {
        if (pool == null) {
            return true;
        }
        // disable new tasks from being submitted
        pool.shutdown();
        final TimeUnit unit = TimeUnit.MILLISECONDS;
        final long phaseOne = timeoutMillis / 5;
        try {
            // wait a while for existing tasks to terminate
            if (pool.awaitTermination(phaseOne, unit)) {
                return true;
            }
            pool.shutdownNow();
            // wait a while for tasks to respond to being cancelled
            if (pool.awaitTermination(timeoutMillis - phaseOne, unit)) {
                return true;
            }
            LOG.warn("Fail to shutdown pool: {}.", pool);
        } catch (final InterruptedException e) {
            // (Re-)cancel if current thread also interrupted
            pool.shutdownNow();
            // preserve interrupt status
            Thread.currentThread().interrupt();
        }
        return false;
    }

}
