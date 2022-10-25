package com.ofcoder.klein.common.disruptor;

import java.io.IOException;
import java.nio.ByteBuffer;

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
                .setThreadFactory(KleinThreadFactory.create("klein-paxos-propose-disruptor-", true)) //
                .setProducerType(ProducerType.MULTI)
                .setWaitStrategy(new BlockingWaitStrategy())
                .build();
        this.applyDisruptor.handleEventsWith(new ProposeEventFactory.ProposeEventHandler());
        this.applyDisruptor.setDefaultExceptionHandler(new DisruptorExceptionHandler<Object>(getClass().getSimpleName()));
        this.applyQueue = this.applyDisruptor.start();
    }

    @Test
    public void test() throws IOException {

        final EventTranslator<ProposeWithDone> translator = new EventTranslator<ProposeWithDone>() {
            @Override
            public void translateTo(ProposeWithDone proposeWithDone, long l) {
                proposeWithDone.data = "";
            }
        };

        this.applyQueue.publishEvent(translator);
        this.applyQueue.publishEvent(translator);

        System.in.read();

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
            public void onEvent(ProposeWithDone proposeWithDone, long l, boolean b) throws Exception {
                System.out.println("zzzzzzzzzzzzzzzzzzzz");

                Thread.sleep(1000);
            }
        }
    }


}