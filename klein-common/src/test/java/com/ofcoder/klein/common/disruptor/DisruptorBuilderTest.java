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
    /**
     * Disruptor to run propose.
     */
    private Disruptor<ProposeWithDone> applyDisruptor;
    private RingBuffer<ProposeWithDone> applyQueue;

    @Override
    protected void setUp() throws Exception {

        this.applyDisruptor = DisruptorBuilder.<ProposeWithDone>newInstance()
                .setRingBufferSize(16384)
                .setEventFactory(new ProposeEventFactory())
                .setThreadFactory(KleinThreadFactory.create("paxos-propose-disruptor-", true)) //
                .setProducerType(ProducerType.MULTI)
                .setWaitStrategy(new BlockingWaitStrategy())
                .build();
        this.applyDisruptor.handleEventsWith(new ProposeEventFactory.ProposeEventHandler());
        this.applyDisruptor.setDefaultExceptionHandler(new DisruptorExceptionHandler<Object>(getClass().getSimpleName()));
        this.applyQueue = this.applyDisruptor.start();
    }

    @Test
    public void test() throws Exception {

        final EventTranslator<ProposeWithDone> translator = new EventTranslator<ProposeWithDone>() {
            @Override
            public void translateTo(final ProposeWithDone proposeWithDone, final long l) {
                proposeWithDone.data = "";
            }
        };

        this.applyQueue.publishEvent(translator);
        this.applyQueue.publishEvent(translator);

        Thread.sleep(2200);

    }

    public static class ProposeWithDone {
        private String data;
    }

    private static class ProposeEventFactory implements EventFactory<ProposeWithDone> {

        @Override
        public ProposeWithDone newInstance() {
            return new ProposeWithDone();
        }

        public static class ProposeEventHandler implements EventHandler<ProposeWithDone> {

            @Override
            public void onEvent(ProposeWithDone proposeWithDone, final long l, final boolean b) throws Exception {
                System.out.println("onEvent");

                Thread.sleep(1000);
            }
        }
    }

}
