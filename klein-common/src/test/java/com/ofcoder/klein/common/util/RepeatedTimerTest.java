package com.ofcoder.klein.common.util;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.util.timer.RepeatedTimer;
import junit.framework.TestCase;

public class RepeatedTimerTest extends TestCase {
    private static final Logger LOG = LoggerFactory.getLogger(RepeatedTimerTest.class);

    @Test
    public void testTwoTimes() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        RepeatedTimer timer = new RepeatedTimer("test-timer", 100) {
            @Override
            protected void onTrigger() {
                LOG.info("==============run==============");
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                latch.countDown();
            }
        };
        timer.start();

        latch.await();
    }

    @Test
    public void testRestart() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(4);
        RepeatedTimer timer = new RepeatedTimer("test-timer", 100) {
            @Override
            protected void onTrigger() {
                LOG.info("==============run==============");
                latch.countDown();
            }
        };
        timer.start();
        timer.stop();

        timer.restart();
        latch.await();
    }


}