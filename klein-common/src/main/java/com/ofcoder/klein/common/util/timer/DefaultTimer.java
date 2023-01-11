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
package com.ofcoder.klein.common.util.timer;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ofcoder.klein.common.util.KleinThreadFactory;
import com.ofcoder.klein.common.util.Requires;
import com.ofcoder.klein.common.util.ThreadExecutor;


/**
 * default timer.
 *
 * @author jiachun.fjc
 */
public class DefaultTimer implements Timer {

    private final ScheduledExecutorService scheduledExecutorService;

    public DefaultTimer(final int workerNum, final String name) {
        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(workerNum, KleinThreadFactory.create(name, true));
    }

    @Override
    public Timeout newTimeout(final TimerTask task, final long delay, final TimeUnit unit) {
        Requires.requireNonNull(task, "task");
        Requires.requireNonNull(unit, "unit");

        final TimeoutTask timeoutTask = new TimeoutTask(task);
        final ScheduledFuture<?> future = this.scheduledExecutorService.schedule(new TimeoutTask(task), delay, unit);
        timeoutTask.setFuture(future);
        return timeoutTask.getTimeout();
    }

    @Override
    public Set<Timeout> stop() {
        ThreadExecutor.shutdownAndAwaitTermination(this.scheduledExecutorService);
        return Collections.emptySet();
    }

    private final class TimeoutTask implements Runnable {

        private final TimerTask task;
        private final Timeout timeout;
        private volatile ScheduledFuture<?> future;

        private TimeoutTask(final TimerTask task) {
            this.task = task;
            this.timeout = new Timeout() {

                @Override
                public Timer timer() {
                    return DefaultTimer.this;
                }

                @Override
                public TimerTask task() {
                    return task;
                }

                @Override
                public boolean isExpired() {
                    // never use
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    final ScheduledFuture<?> f = future;
                    return f != null && f.isCancelled();
                }

                @Override
                public boolean cancel() {
                    final ScheduledFuture<?> f = future;
                    return f != null && f.cancel(false);
                }
            };
        }

        public Timeout getTimeout() {
            return timeout;
        }

        public ScheduledFuture<?> getFuture() {
            return future;
        }

        public void setFuture(final ScheduledFuture<?> future) {
            this.future = future;
        }

        @Override
        public void run() {
            try {
                this.task.run(this.timeout);
            } catch (final Throwable ignored) {
                // never get here
            }
        }
    }
}
