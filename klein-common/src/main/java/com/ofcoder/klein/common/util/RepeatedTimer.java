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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Repeatable timer based on java.util.Timer.
 *
 * @author boyan (boyan@alibaba-inc.com)
 * <p>
 * 2018-Mar-30 3:45:37 PM
 */
public abstract class RepeatedTimer {

    public static final Logger LOG = LoggerFactory.getLogger(RepeatedTimer.class);

    private final Timer timer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile int timeoutMs;
    private final String name;

    public RepeatedTimer(final String name, final int timeoutMs) {
        this(name, timeoutMs, new Timer(name, true));
    }

    public RepeatedTimer(final String name, final int timeoutMs, final Timer timer) {
        super();
        this.name = name;
        this.timeoutMs = timeoutMs;
        this.timer = Requires.requireNonNull(timer, "timer");
    }

    /**
     * Subclasses should implement this method for timer trigger.
     */
    protected abstract void onTrigger();

    /**
     * Adjust timeoutMs before every scheduling.
     *
     * @param timeoutMs timeout millis
     * @return timeout millis
     */
    protected int adjustTimeout(final int timeoutMs) {
        return timeoutMs;
    }

    public void run() {
        try {
            onTrigger();
        } catch (final Throwable t) {
            LOG.error("Run timer failed.", t);
        } finally {
            if (running.get()) {
                schedule();
            }
        }
    }

    /**
     * Start the timer.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            schedule();
        }
    }

    /**
     * Restart the timer.
     * It will be started if it's stopped, and it will be restarted if it's running.
     *
     * @author Qing Wang (kingchin1218@gmail.com)
     * <p>
     * 2020-Mar-26 20:38:37 PM
     */
    public void restart() {
        start();
    }

    private void schedule() {
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    RepeatedTimer.this.run();
                } catch (final Throwable t) {
                    LOG.error("Run timer task failed, taskName={}.", RepeatedTimer.this.name, t);
                }
            }
        };
        this.timer.schedule(timerTask, adjustTimeout(this.timeoutMs));
    }

    /**
     * Stop timer
     */
    public void stop() {
        stop(false);
    }

    public void stop(boolean destroy) {
        running.compareAndSet(true, false);
        if (destroy) {
            this.timer.cancel();
        }
    }
}
