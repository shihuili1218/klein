package com.ofcoder.klein.consensus.paxos;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.ofcoder.klein.consensus.paxos.core.DataAligner;

public class QueueTest {

    PriorityBlockingQueue<Long> learnQueue = new PriorityBlockingQueue<>(1024, Comparator.comparingLong(value -> value));

    @Test
    public void testPeek() throws InterruptedException {
        learnQueue.offer(2L);
        learnQueue.offer(5L);
        learnQueue.offer(4L);
        learnQueue.offer(1L);
        learnQueue.offer(3L);
        System.out.println(learnQueue.size());
        List<Long> longStream = learnQueue.stream().filter(new Predicate<Long>() {
            @Override
            public boolean test(Long aLong) {
                return aLong <= 3;
            }
        }).collect(Collectors.toList());

        System.out.println(longStream);
        System.out.println(learnQueue.size());

    }
}
