package com.ofcoder.klein.common.util;

import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class TrueTimeTest extends TestCase {
    private static final Logger LOG = LoggerFactory.getLogger(ThreadExecutorTest.class);

    public void testCurrentTimeMillis() throws Exception {
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        long trueTime = TrueTime.currentTimeMillis();
        LOG.info(s.format(trueTime));

        LOG.info(TrueTime.currentTimeMillis() - trueTime + "");

        TrueTime.heartbeat(trueTime + 1000);
        trueTime = TrueTime.currentTimeMillis();
        LOG.info(s.format(trueTime));

        Thread.sleep(6L);

        trueTime = TrueTime.currentTimeMillis();
        LOG.info(s.format(trueTime));

        trueTime = TrueTime.currentTimeMillis();
        LOG.info(s.format(trueTime));

    }
}