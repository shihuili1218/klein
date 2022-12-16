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
package com.ofcoder.klein.common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * common thread executor.
 *
 * @author 释慧利
 */
public class ThreadExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(ThreadExecutor.class);
    // fixme to user custom
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(cpus(), Math.max(100, cpus() * 5),
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            KleinThreadFactory.create("common-thread", true));

    private static int cpus() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * submit task to thread pool.
     *
     * @param task runnable
     */
    public static void submit(final Runnable task) {
        EXECUTOR.execute(task);
    }

    /**
     * graceful shutdown.
     *
     * @param pool need close pool
     * @return is closed
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
     *
     * @param pool          need close pool
     * @param timeoutMillis close timeout
     * @return is closed
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
