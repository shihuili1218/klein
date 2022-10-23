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
package com.ofcoder.klein.consensus.paxos.role;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.common.disruptor.AbstractBatchEventHandler;
import com.ofcoder.klein.common.disruptor.DisruptorBuilder;
import com.ofcoder.klein.common.disruptor.DisruptorEvent;
import com.ofcoder.klein.common.disruptor.DisruptorExceptionHandler;
import com.ofcoder.klein.common.exception.ShutdownException;
import com.ofcoder.klein.common.util.KleinThreadFactory;
import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.facade.MemberManager;
import com.ofcoder.klein.consensus.facade.Quorum;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.exception.ConsensusException;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.PaxosQuorum;
import com.ofcoder.klein.consensus.paxos.rpc.vo.AcceptReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareRes;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.InvokeParam;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import com.ofcoder.klein.rpc.facade.RpcProcessor;
import com.ofcoder.klein.rpc.facade.serialization.Hessian2Util;
import com.ofcoder.klein.storage.facade.Instance;

/**
 * @author far.liu
 */
public class Proposer implements Lifecycle<ConsensusProp> {
    private static final Logger LOG = LoggerFactory.getLogger(Proposer.class);
    private static final int RUNNING_BUFFER_SIZE = 16384;
    private AtomicBoolean skipPrepare = new AtomicBoolean(false);
    private RpcClient client;
    private ConsensusProp prop;
    private final PaxosNode self;
    private long prepareTimeout;
    private long acceptTimeout;
    /**
     * Disruptor to run propose.
     */
    private Disruptor<ProposeWithDone> proposeDisruptor;
    private RingBuffer<ProposeWithDone> proposeQueue;
    private CountDownLatch shutdownLatch;

    public Proposer(PaxosNode self) {
        this.self = self;
    }

    @Override
    public void init(ConsensusProp op) {
        this.prop = op;
        this.client = RpcEngine.getClient();
        this.prepareTimeout = (long) (op.getRoundTimeout() * 0.4);
        this.acceptTimeout = op.getRoundTimeout() - prepareTimeout;

        this.proposeDisruptor = DisruptorBuilder.<ProposeWithDone>newInstance()
                .setRingBufferSize(RUNNING_BUFFER_SIZE)
                .setEventFactory(ProposeWithDone::new)
                .setThreadFactory(KleinThreadFactory.create("klein-paxos-propose-disruptor-", true)) //
                .setProducerType(ProducerType.MULTI)
                .setWaitStrategy(new BlockingWaitStrategy())
                .build();
        this.proposeDisruptor.handleEventsWith(new ProposeEventHandler(this.prop.getBatchSize()));
        this.proposeDisruptor.setDefaultExceptionHandler(new DisruptorExceptionHandler<Object>(getClass().getSimpleName()));
        this.proposeQueue = this.proposeDisruptor.start();
    }

    @Override
    public void shutdown() {
        if (this.proposeQueue != null) {
            this.shutdownLatch = new CountDownLatch(1);
            this.proposeQueue.publishEvent((event, sequence) -> event.setShutdownLatch(this.shutdownLatch));
            try {
                shutdownLatch.await();
            } catch (InterruptedException e) {
                throw new ShutdownException(e.getMessage(), e);
            }
        }
    }

    public void propose(final ByteBuffer data, final ProposeDone done) {
        if (this.shutdownLatch != null) {
            throw new ConsensusException("klein is shutting down.");
        }

        final EventTranslator<ProposeWithDone> translator = (event, sequence) -> {
            event.done = done;
            event.data = data;
        };
        this.proposeQueue.publishEvent(translator);
    }

    public void accept(long instanceId, long proposalNo, List<ByteBuffer> datas, PhaseCallback callback) {
        final PaxosQuorum quorum = PaxosQuorum.createInstance();
        final AtomicBoolean nexted = new AtomicBoolean(false);

        final AcceptReq req = AcceptReq.Builder.anAcceptReq()
                .nodeId(self.getSelf().getId())
                .instanceId(instanceId)
                .proposalNo(proposalNo)
                .datas(datas)
                .build();

        MemberManager.getAllMembers().forEach(it -> {
            InvokeParam param = InvokeParam.Builder.anInvokeParam()
                    .service(AcceptReq.class.getSimpleName())
                    .method(RpcProcessor.KLEIN)
                    .data(ByteBuffer.wrap(Hessian2Util.serialize(req))).build();
            client.sendRequestAsync(it, param, new AbstractInvokeCallback<PrepareRes>() {
                @Override
                public void error(Throwable err) {
                    LOG.error(err.getMessage(), err);

                }

                @Override
                public void complete(PrepareRes result) {

                }
            }, acceptTimeout);
        });

    }

    public void prepare(final long instanceId, int times, final PhaseCallback callback) {
        if (times++ >= this.prop.getRetry()) {
            callback.refused(instanceId);
            return;
        }
        final int finalTimes = times;
        long proposalNo = self.incrementProposalNo();

        PaxosQuorum quorum = PaxosQuorum.createInstance();
        AtomicBoolean nexted = new AtomicBoolean(false);

        PrepareReq req = PrepareReq.Builder.aPrepareReq()
                .instanceId(instanceId)
                .nodeId(self.getSelf().getId())
                .proposalNo(proposalNo)
                .build();

        // fixme exclude self
        MemberManager.getAllMembers().forEach(it -> {
            InvokeParam param = InvokeParam.Builder.anInvokeParam()
                    .service(PrepareReq.class.getSimpleName())
                    .method(RpcProcessor.KLEIN)
                    .data(ByteBuffer.wrap(Hessian2Util.serialize(req))).build();
            client.sendRequestAsync(it, param, new AbstractInvokeCallback<PrepareRes>() {
                @Override
                public void error(Throwable err) {
                    LOG.error(err.getMessage(), err);
                    quorum.refuse(it);
                    if (quorum.isGranted() == Quorum.GrantResult.REFUSE
                            && nexted.compareAndSet(false, true)) {
                        prepare(instanceId, finalTimes, callback);
                    }
                }

                @Override
                public void complete(PrepareRes result) {
                    handlePrepareRequest(instanceId, finalTimes, callback, result, quorum, it, nexted);
                }
            }, prepareTimeout);
        });
    }

    private void handlePrepareRequest(final long instanceId, int times, final PhaseCallback callback, final PrepareRes result
            , final PaxosQuorum quorum, final Endpoint it, final AtomicBoolean nexted) {
        LOG.info("handling node-{}'s prepare response", result.getNodeId());

        if (result.getGrantValue() != null) {
            quorum.setTempValue(result.getProposalNo(), result.getGrantValue());
        }

        if (result.getResult()) {
            quorum.grant(it);
            if (quorum.isGranted() == Quorum.GrantResult.PASS
                    && nexted.compareAndSet(false, true)) {
                // do accept phase.
                callback.granted(instanceId, result.getProposalNo(), quorum.getTempValue());
            }
        } else {
            quorum.refuse(it);
            final long selfProposalNo = self.getCurProposalNo();
            long diff = result.getProposalNo() - selfProposalNo;
            if (diff > 0) {
                self.addProposalNo(diff);
            }

            if (result.getState() == Instance.State.CONFIRMED
                    && nexted.compareAndSet(false, true)) {
                callback.confirmed(instanceId, result.getProposalNo(), quorum.getTempValue());
            } else {
                // do prepare phase
                if (quorum.isGranted() == Quorum.GrantResult.REFUSE
                        && nexted.compareAndSet(false, true)) {
                    prepare(instanceId, times, callback);
                }
            }
        }
    }


    public static class ProposeWithDone extends DisruptorEvent {
        private ByteBuffer data;
        private ProposeDone done;
    }

    public class ProposeEventHandler extends AbstractBatchEventHandler<ProposeWithDone> {

        public ProposeEventHandler(int batchSize) {
            super(batchSize);
        }

        @Override
        protected void handle(List<ProposeWithDone> events) {
            LOG.info("start negotiations, proposal size: {}", events.size());
            if (!Proposer.this.skipPrepare.get()) {
                prepare(self.incrementInstanceId(), 0, new PrepareCallback(events));
            }
        }

        public class PrepareCallback implements PhaseCallback {
            private List<ProposeWithDone> events;

            public PrepareCallback(List<ProposeWithDone> events) {
                this.events = events;
            }

            @Override
            public void granted(long instanceId, long proposalNo, List<ByteBuffer> datas) {
                List<ByteBuffer> v = datas != null ? datas
                        : events.stream().map(it -> it.data).collect(Collectors.toList());
                accept(instanceId, proposalNo, v, new AcceptCallback(events));
            }

            @Override
            public void confirmed(long instanceId, long proposalNo, List<ByteBuffer> datas) {
                // todo learn
                for (ProposeWithDone event : events) {
                    event.done.done(Result.FAILURE);
                }
            }

            @Override
            public void refused(long instanceId) {
                for (ProposeWithDone event : events) {
                    event.done.done(Result.UNKNOWN);
                }
            }
        }

        public class AcceptCallback implements PhaseCallback {
            List<ProposeWithDone> events;

            public AcceptCallback(List<ProposeWithDone> events) {
                this.events = events;
            }

            @Override
            public void granted(long instanceId, long proposalNo, List<ByteBuffer> datas) {
                for (ProposeWithDone event : events) {
                    event.done.done(Result.SUCCESS);
                }
                // todo confirm for async
            }

            @Override
            public void confirmed(long instanceId, long proposalNo, List<ByteBuffer> datas) {
                // todo learn
                for (ProposeWithDone event : events) {
                    event.done.done(Result.FAILURE);
                }
            }

            @Override
            public void refused(long instanceId) {
                for (ProposeWithDone event : events) {
                    event.done.done(Result.UNKNOWN);
                }
            }
        }

    }


}
