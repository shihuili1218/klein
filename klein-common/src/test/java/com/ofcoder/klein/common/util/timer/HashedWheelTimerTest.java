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


import org.junit.Test;

import java.util.Random;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class HashedWheelTimerTest {

    @Test
    public void testnormalizeTicksPerWheel() {
        for (int i = 0; i < 1000; i++) {
            int num = ThreadLocalRandom.current().nextInt(0, 100);
            assertEquals(normalizeTicksPerWheel0(num), normalizeTicksPerWheel1(num));
        }

        for (int i = 0; i < 1000; i++) {
            int num = ThreadLocalRandom.current().nextInt(100, 1000);
            assertEquals(normalizeTicksPerWheel0(num), normalizeTicksPerWheel1(num));
        }

        for (int i = 0; i < 1000; i++) {
            int num = ThreadLocalRandom.current().nextInt(100000, 999999999);
            assertEquals(normalizeTicksPerWheel0(num), normalizeTicksPerWheel1(num));
        }
    }

    private int normalizeTicksPerWheel0(final int ticksPerWheel) {
        int normalizedTicksPerWheel = 1;
        while (normalizedTicksPerWheel < ticksPerWheel) {
            normalizedTicksPerWheel <<= 1;
        }
        return normalizedTicksPerWheel;
    }

    private int normalizeTicksPerWheel1(final int ticksPerWheel) {
        // Fixed calculation process to avoid multi-cycle inefficiency
        int n = ticksPerWheel - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        // Prevent spillage, 1073741824 = 2^30
        return (n < 0) ? 1 : (n >= 1073741824) ? 1073741824 : n + 1;
    }

    @Test
    public void testScheduleTimeoutShouldNotRunBeforeDelay() throws InterruptedException {
        final Timer timer = new HashedWheelTimer();
        final CountDownLatch barrier = new CountDownLatch(1);
        final Timeout timeout = timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                fail("This should not have run");
                barrier.countDown();
            }
        }, 10, TimeUnit.SECONDS);
        assertFalse(barrier.await(3, TimeUnit.SECONDS));
        assertFalse("timer should not expire", timeout.isExpired());
        timer.stop();
    }

    @Test
    public void testScheduleTimeoutShouldRunAfterDelay() throws InterruptedException {
        final Timer timer = new HashedWheelTimer();
        final CountDownLatch barrier = new CountDownLatch(1);
        final Timeout timeout = timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                barrier.countDown();
            }
        }, 2, TimeUnit.SECONDS);
        assertTrue(barrier.await(3, TimeUnit.SECONDS));
        assertTrue("timer should expire", timeout.isExpired());
        timer.stop();
    }

    @Test(timeout = 3000L)
    public void testStopTimer() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(3);
        final Timer timerProcessed = new HashedWheelTimer();
        for (int i = 0; i < 3; i++) {
            timerProcessed.newTimeout(new TimerTask() {
                @Override
                public void run(final Timeout timeout) throws Exception {
                    latch.countDown();
                }
            }, 1, TimeUnit.MILLISECONDS);
        }

        latch.await();
        assertEquals("Number of unprocessed timeouts should be 0", 0, timerProcessed.stop().size());

        final Timer timerUnprocessed = new HashedWheelTimer();
        for (int i = 0; i < 5; i++) {
            timerUnprocessed.newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                }
            }, 5, TimeUnit.SECONDS);
        }
        Thread.sleep(1000L); // sleep for a second
        assertFalse("Number of unprocessed timeouts should be greater than 0", timerUnprocessed.stop().isEmpty());
    }

    @Test(timeout = 3000L)
    public void testTimerShouldThrowExceptionAfterShutdownForNewTimeouts() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(3);
        final Timer timer = new HashedWheelTimer();
        for (int i = 0; i < 3; i++) {
            timer.newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                    latch.countDown();
                }
            }, 1, TimeUnit.MILLISECONDS);
        }

        latch.await();
        timer.stop();

        try {
            timer.newTimeout(createNoOpTimerTask(), 1, TimeUnit.MILLISECONDS);
            fail("Expected exception didn't occur.");
        } catch (IllegalStateException ignored) {
            // expected
        }
    }

    @Test(timeout = 5000L)
    public void testTimerOverflowWheelLength() throws InterruptedException {
        final HashedWheelTimer timer = new HashedWheelTimer(
                Executors.defaultThreadFactory(), 100, TimeUnit.MILLISECONDS, 32);
        final CountDownLatch latch = new CountDownLatch(3);

        timer.newTimeout(new TimerTask() {
            @Override
            public void run(final Timeout timeout) throws Exception {
                timer.newTimeout(this, 100, TimeUnit.MILLISECONDS);
                latch.countDown();
            }
        }, 100, TimeUnit.MILLISECONDS);

        latch.await();
        assertFalse(timer.stop().isEmpty());
    }

    @Test
    public void testExecutionOnTime() throws InterruptedException {
        int tickDuration = 200;
        int timeout = 125;
        int maxTimeout = 2 * (tickDuration + timeout);
        final HashedWheelTimer timer = new HashedWheelTimer(tickDuration, TimeUnit.MILLISECONDS);
        final BlockingQueue<Long> queue = new LinkedBlockingQueue<Long>();

        int scheduledTasks = 100000;
        for (int i = 0; i < scheduledTasks; i++) {
            final long start = System.nanoTime();
            timer.newTimeout(new TimerTask() {
                @Override
                public void run(final Timeout timeout) throws Exception {
                    queue.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
                }
            }, timeout, TimeUnit.MILLISECONDS);
        }

        for (int i = 0; i < scheduledTasks; i++) {
            long delay = queue.take();
            assertTrue("Timeout + " + scheduledTasks + " delay " + delay + " must be " + timeout + " < " + maxTimeout, delay >= timeout && delay < maxTimeout);
        }

        timer.stop();
    }

    @Test
    public void testExecutionOnTaskExecutor() throws InterruptedException {
        int timeout = 10;

        final CountDownLatch timeoutLatch = new CountDownLatch(1);
        final HashedWheelTimer timer = new HashedWheelTimer(Executors.defaultThreadFactory(), 100,
                TimeUnit.MILLISECONDS, 32);
        timer.newTimeout(timeout1 -> timeoutLatch.countDown(), timeout, TimeUnit.MILLISECONDS);

        timeoutLatch.await();
        timer.stop();
    }

    @Test
    public void testRejectedExecutionExceptionWhenTooManyTimeoutsAreAddedBackToBack() {
        HashedWheelTimer timer = new HashedWheelTimer(Executors.defaultThreadFactory(), 100,
                TimeUnit.MILLISECONDS, 32, 2);
        timer.newTimeout(createNoOpTimerTask(), 5, TimeUnit.SECONDS);
        timer.newTimeout(createNoOpTimerTask(), 5, TimeUnit.SECONDS);
        try {
            timer.newTimeout(createNoOpTimerTask(), 1, TimeUnit.MILLISECONDS);
            fail("Timer allowed adding 3 timeouts when maxPendingTimeouts was 2");
        } catch (RejectedExecutionException e) {
            // Expected
        } finally {
            timer.stop();
        }
    }

    @Test
    public void testNewTimeoutShouldStopThrowingRejectedExecutionExceptionWhenExistingTimeoutIsCancelled()
            throws InterruptedException {
        final int tickDurationMs = 100;
        final HashedWheelTimer timer = new HashedWheelTimer(Executors.defaultThreadFactory(), tickDurationMs,
                TimeUnit.MILLISECONDS, 32);
        timer.newTimeout(createNoOpTimerTask(), 5, TimeUnit.SECONDS);
        Timeout timeoutToCancel = timer.newTimeout(createNoOpTimerTask(), 5, TimeUnit.SECONDS);
        assertTrue(timeoutToCancel.cancel());

        Thread.sleep(tickDurationMs * 5);

        final CountDownLatch secondLatch = new CountDownLatch(1);
        timer.newTimeout(createCountDownLatchTimerTask(secondLatch), 90, TimeUnit.MILLISECONDS);

        secondLatch.await();
        timer.stop();
    }

    @Test(timeout = 3000L)
    public void testNewTimeoutShouldStopThrowingRejectedExecutionExceptionWhenExistingTimeoutIsExecuted()
            throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final HashedWheelTimer timer = new HashedWheelTimer(Executors.defaultThreadFactory(), 25,
                TimeUnit.MILLISECONDS, 4);
        timer.newTimeout(createNoOpTimerTask(), 5, TimeUnit.SECONDS);
        timer.newTimeout(createCountDownLatchTimerTask(latch), 90, TimeUnit.MILLISECONDS);

        latch.await();

        final CountDownLatch secondLatch = new CountDownLatch(1);
        timer.newTimeout(createCountDownLatchTimerTask(secondLatch), 90, TimeUnit.MILLISECONDS);

        secondLatch.await();
        timer.stop();
    }

    @Test()
    public void reportPendingTimeouts() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final HashedWheelTimer timer = new HashedWheelTimer();
        final Timeout t1 = timer.newTimeout(createNoOpTimerTask(), 100, TimeUnit.MINUTES);
        final Timeout t2 = timer.newTimeout(createNoOpTimerTask(), 100, TimeUnit.MINUTES);
        timer.newTimeout(createCountDownLatchTimerTask(latch), 90, TimeUnit.MILLISECONDS);

        assertEquals(3, timer.pendingTimeouts());
        t1.cancel();
        t2.cancel();
        latch.await();

        assertEquals(0, timer.pendingTimeouts());
        timer.stop();
    }

    @Test
    public void testOverflow() throws InterruptedException {
        final HashedWheelTimer timer = new HashedWheelTimer();
        final CountDownLatch latch = new CountDownLatch(1);
        Timeout timeout = timer.newTimeout(timeout1 -> latch.countDown(), Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        assertFalse(latch.await(1, TimeUnit.SECONDS));
        timeout.cancel();
        timer.stop();
    }

    private static TimerTask createNoOpTimerTask() {
        return timeout -> {};
    }

    private static TimerTask createCountDownLatchTimerTask(final CountDownLatch latch) {
        return timeout -> latch.countDown();
    }
}
