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
package com.ofcoder.klein.common.disruptor;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.ofcoder.klein.common.util.KleinThreadFactory;
import junit.framework.TestCase;

/**
 * @author far.liu
 */
public class DisruptorBuilderTest extends TestCase {
    private Disruptor<ProposeWithDone> applyDisruptor;
    private RingBuffer<ProposeWithDone> applyQueue;

    @Test
    public void test() throws Exception {

        CountDownLatch latch = new CountDownLatch(2);

        this.applyDisruptor = DisruptorBuilder.<ProposeWithDone>newInstance()
                .setRingBufferSize(16384)
                .setEventFactory(ProposeWithDone::new)
                .setThreadFactory(KleinThreadFactory.create("paxos-propose-disruptor-", true)) //
                .setProducerType(ProducerType.MULTI)
                .setWaitStrategy(new BlockingWaitStrategy())
                .build();
        this.applyDisruptor.handleEventsWith(new ProposeEventHandler(latch));
        this.applyDisruptor.setDefaultExceptionHandler(new DisruptorExceptionHandler<Object>(getClass().getSimpleName()));
        this.applyQueue = this.applyDisruptor.start();

        final EventTranslator<ProposeWithDone> translator = (proposeWithDone, l) -> proposeWithDone.data = "";

        this.applyQueue.publishEvent(translator);
        this.applyQueue.publishEvent(translator);

        latch.await();

    }

    public static class ProposeWithDone {
        private String data;
    }

    public static class ProposeEventHandler implements EventHandler<ProposeWithDone> {
        CountDownLatch latch;

        public ProposeEventHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onEvent(ProposeWithDone proposeWithDone, final long l, final boolean b) throws Exception {
            System.out.println("onEvent");

            Thread.sleep(100);
            latch.countDown();
        }
    }

}
