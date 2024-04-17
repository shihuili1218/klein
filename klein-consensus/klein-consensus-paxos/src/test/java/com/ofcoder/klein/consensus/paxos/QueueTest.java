package com.ofcoder.klein.consensus.paxos;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

import org.junit.Test;

public class QueueTest {

    PriorityBlockingQueue<Long> learnQueue = new PriorityBlockingQueue<>(1024, Comparator.comparingLong(value -> value));

    @Test
    public void testPeek() throws InterruptedException {
        final Set<Long> runningInstance = Collections.newSetFromMap(new ConcurrentHashMap<>());
        System.out.println(runningInstance.add(1L));
        System.out.println(runningInstance.add(2L));
        System.out.println(runningInstance.add(1L));
        System.out.println(runningInstance.add(3L));
        System.out.println(runningInstance.add(1L));
        System.out.println(runningInstance);
    }
}
