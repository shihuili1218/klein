package com.ofcoder.klein.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.ThreadContext;
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
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        CountDownLatch latch = new CountDownLatch(4);
        RepeatedTimer timer = new RepeatedTimer("test-timer", 100) {
            @Override
            protected void onTrigger() {
                Map<String, String> context = ThreadContext.getContext();
                LOG.info("==============run==============, {}", context.get("zzz"));
                latch.countDown();
            }

            @Override
            protected int adjustTimeout(int timeoutMs) {

                ThreadContext.put("zzz", System.currentTimeMillis() + "");
                return 1000;
            }
        };
        timer.start();
        timer.stop();

        LOG.info("==============restart==============");
        timer.restart(true);
        System.out.println(simpleDateFormat.format(new Date()));
        latch.await();
    }


}