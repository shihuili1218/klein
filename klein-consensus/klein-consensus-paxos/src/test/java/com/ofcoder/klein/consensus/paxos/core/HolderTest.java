package com.ofcoder.klein.consensus.paxos.core;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;

import com.ofcoder.klein.common.Holder;
import junit.framework.TestCase;

public class HolderTest extends TestCase {

    public void testInstanceHolder() {
        AtomicLong i = new AtomicLong(0);
        Holder<Long> instanceHolder = new Holder<Long>() {
            @Override
            protected Long create() {
                System.out.println("generateId");
                return i.incrementAndGet();
            }
        };
        System.out.println("new");
        long actual = 1;
        Assert.assertEquals(instanceHolder.get().longValue(), actual);
        Assert.assertEquals(instanceHolder.get().longValue(), actual);
        Assert.assertEquals(instanceHolder.get().longValue(), actual);
        Assert.assertEquals(instanceHolder.get().longValue(), actual);

    }
}