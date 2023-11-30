package com.ofcoder.klein.common.util;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

/**
 * @author far.liu
 */
public class ThreadExecutorTest extends TestCase {
    private static final Logger LOG = LoggerFactory.getLogger(ThreadExecutorTest.class);

    @Test
    public void testPrintException() throws InterruptedException {
        ThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException("klein exception");
            }
        });

        Thread.sleep(500);
    }
}