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
package com.ofcoder.klein.consensus.paxos.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.ofcoder.klein.common.Holder;
import com.ofcoder.klein.common.disruptor.DisruptorBuilder;
import com.ofcoder.klein.common.disruptor.DisruptorExceptionHandler;
import com.ofcoder.klein.common.exception.ShutdownException;
import com.ofcoder.klein.common.util.KleinThreadFactory;
import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.facade.SingleQuorum;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.exception.ConsensusException;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.core.sm.PaxosMemberConfiguration;
import com.ofcoder.klein.consensus.paxos.rpc.vo.AcceptReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.AcceptRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareRes;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;

/**
 * Proposer implement.
 *
 * @author far.liu
 */
public class ProposerImpl implements Proposer {
    private static final Logger LOG = LoggerFactory.getLogger(ProposerImpl.class);
    private static final int RUNNING_BUFFER_SIZE = 16384;
    private final AtomicReference<PrepareState> skipPrepare = new AtomicReference<>(PrepareState.NO_PREPARE);
    private RpcClient client;
    private ConsensusProp prop;
    private final PaxosNode self;
    private final PaxosMemberConfiguration memberConfig;
    private long prepareTimeout;
    private long acceptTimeout;
    private RingBuffer<ProposalWithDone> proposeQueue;
    private CountDownLatch shutdownLatch;
    /**
     * The instance of the Prepare phase has been executed.
     */
    private final ConcurrentMap<Long, Instance<Proposal>> preparedInstanceMap = new ConcurrentHashMap<>();
    private LogManager<Proposal> logManager;
    private boolean allowPropose = false;

    public ProposerImpl(final PaxosNode self) {
        this.self = self;
        this.memberConfig = self.getMemberConfig();
    }

    @Override
    public void init(final ConsensusProp op) {
        this.prop = op;
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();
        this.prepareTimeout = (long) (op.getRoundTimeout() * 0.4);
        this.acceptTimeout = op.getRoundTimeout() - prepareTimeout;
        this.logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();

        // Disruptor to run propose.
        Disruptor<ProposalWithDone> proposeDisruptor = DisruptorBuilder.<ProposalWithDone>newInstance()
                .setRingBufferSize(RUNNING_BUFFER_SIZE)
                .setEventFactory(ProposalWithDone::new)
                .setThreadFactory(KleinThreadFactory.create("paxos-propose-disruptor-", true))
                .setProducerType(ProducerType.MULTI)
                .setWaitStrategy(new BlockingWaitStrategy())
                .build();
        ProposeEventHandler eventHandler = new ProposeEventHandler(this.prop.getBatchSize());
        proposeDisruptor.handleEventsWith(eventHandler);
        proposeDisruptor.setDefaultExceptionHandler(new DisruptorExceptionHandler<Object>(getClass().getSimpleName()));
        this.proposeQueue = proposeDisruptor.start();
        RoleAccessor.getMaster().addHealthyListener(healthy -> {
            allowPropose = Master.ElectState.allowPropose(healthy);
            if (allowPropose) {
                eventHandler.triggerHandle();
            }
        });
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

    /**
     * Propose proposal.
     * Put the request on the {@link ProposerImpl#proposeQueue},
     * and process it as {@link ProposeEventHandler}
     *
     * @param data client's data
     * @param done client's callbck
     */
    @Override
    public void propose(final Proposal data, final ProposeDone done) {
        if (this.shutdownLatch != null) {
            throw new ConsensusException("klein is shutting down.");
        }

        final EventTranslator<ProposalWithDone> translator = (event, sequence) -> {
            event.setProposal(data);
            event.setDone(done);
        };
        this.proposeQueue.publishEvent(translator);
    }

    /**
     * Send accept message to all Acceptor.
     *
     * @param grantedProposalNo This is a proposalNo that has executed the prepare phase;
     *                          You cannot use {@code self.curProposalNo} directly in the accept phase.
     *                          Because:
     *                          * T1: PREPARED
     *                          * T2: PREPARED → NO_PREPARE
     *                          * T2: increment self.curProposalNo enter pre(proposalNo = 2)
     *                          * T2: processing...
     *                          * T1: enter accept phase, acc(proposalNo = self.curProposalNo = 2)
     *                          * This is incorrect because 2 has not been PREPARED yet and it is not known whether other members are granted other grantedValue
     * @param ctxt              Negotiation Context
     * @param callback          Callback of accept phase,
     *                          if the majority approved accept, call {@link PhaseCallback.AcceptPhaseCallback#granted(ProposeContext)}
     *                          if an acceptor returns a confirmed instance, call {@link PhaseCallback.AcceptPhaseCallback#learn(ProposeContext, Endpoint)}
     */
    private void accept(final long grantedProposalNo, final ProposeContext ctxt, final PhaseCallback.AcceptPhaseCallback callback) {
        LOG.info("start accept phase, proposalNo: {}, instanceId: {}", grantedProposalNo, ctxt.getInstanceId());

        ctxt.setGrantedProposalNo(grantedProposalNo);
        ctxt.setConsensusData(preparedInstanceMap.containsKey(ctxt.getInstanceId()) && CollectionUtils.isNotEmpty(preparedInstanceMap.get(ctxt.getInstanceId()).getGrantedValue())
                ? preparedInstanceMap.get(ctxt.getInstanceId()).getGrantedValue()
                : ctxt.getDataWithCallback().stream().map(ProposalWithDone::getProposal).collect(Collectors.toList()));

        // todo get member configuration from ProposeContext
        final PaxosMemberConfiguration memberConfiguration = memberConfig.createRef();

        final AcceptReq req = AcceptReq.Builder.anAcceptReq()
                .nodeId(self.getSelf().getId())
                .instanceId(ctxt.getInstanceId())
                .proposalNo(ctxt.getGrantedProposalNo())
                .data(ctxt.getConsensusData())
                .memberConfigurationVersion(memberConfiguration.getVersion())
                .build();

        // for self
        AcceptRes res = RoleAccessor.getAcceptor().handleAcceptRequest(req);
        handleAcceptResponse(ctxt, callback, res, self.getSelf());

        // for other members
        memberConfiguration.getMembersWithout(self.getSelf().getId()).forEach(it -> {
            client.sendRequestAsync(it, req, new AbstractInvokeCallback<AcceptRes>() {
                @Override
                public void error(final Throwable err) {
                    LOG.error("send accept msg to node-{}, proposalNo: {}, instanceId: {}, occur exception, {}", it.getId(), grantedProposalNo, ctxt.getInstanceId(), err.getMessage());

                    ctxt.getAcceptQuorum().refuse(it);
                    if (ctxt.getAcceptQuorum().isGranted() == SingleQuorum.GrantResult.REFUSE
                            && ctxt.getAcceptNexted().compareAndSet(false, true)) {

                        skipPrepare.compareAndSet(PrepareState.PREPARED, PrepareState.NO_PREPARE);
                        ThreadExecutor.submit(() -> prepare(ctxt, new PrepareCallback()));
                    }
                }

                @Override
                public void complete(final AcceptRes result) {
                    handleAcceptResponse(ctxt, callback, result, it);
                }
            }, acceptTimeout);
        });

    }

    private void handleAcceptResponse(final ProposeContext ctxt, final PhaseCallback.AcceptPhaseCallback callback,
                                      final AcceptRes result, final Endpoint it) {
        LOG.info("handling node-{}'s accept response, local.proposalNo: {}, instanceId: {}, remote.instanceState: {}, result: {}",
                result.getNodeId(), ctxt.getGrantedProposalNo(), ctxt.getInstanceId(), result.getInstanceState(), result.getResult());
        self.updateCurProposalNo(result.getCurProposalNo());
        self.updateCurInstanceId(result.getCurInstanceId());

        if (result.getInstanceState() == Instance.State.CONFIRMED
                && ctxt.getAcceptNexted().compareAndSet(false, true)) {
            callback.learn(ctxt, it);
            return;
        }

        if (result.getResult()) {
            ctxt.getAcceptQuorum().grant(it);
            if (ctxt.getAcceptQuorum().isGranted() == SingleQuorum.GrantResult.PASS
                    && ctxt.getAcceptNexted().compareAndSet(false, true)) {
                // do learn phase and return client.
                callback.granted(ctxt);
            }
        } else {
            ctxt.getAcceptQuorum().refuse(it);

            // do prepare phase
            if (ctxt.getAcceptQuorum().isGranted() == SingleQuorum.GrantResult.REFUSE
                    && ctxt.getAcceptNexted().compareAndSet(false, true)) {
                skipPrepare.compareAndSet(PrepareState.PREPARED, PrepareState.NO_PREPARE);
                ThreadExecutor.submit(() -> prepare(ctxt, new PrepareCallback()));
            }
        }
    }

    @Override
    public void tryBoost(final Holder<Long> instanceHolder, final List<Proposal> proposal, final ProposeDone done) {
        List<ProposalWithDone> proposalWithDones = proposal.stream().map(it -> {
            ProposalWithDone event = new ProposalWithDone();
            event.setProposal(it);
            event.setDone(done);
            return event;
        }).collect(Collectors.toList());

        ProposeContext ctxt = new ProposeContext(memberConfig.createRef(), instanceHolder, proposalWithDones);
        prepare(ctxt, new PrepareCallback());
    }

    /**
     * Send Prepare message to all Acceptor.
     * Only one thread is executing this method at the same time.
     *
     * @param ctxt     Negotiation Context
     * @param callback Callback of prepare phase,
     *                 if the majority approved prepare, call {@link PhaseCallback.PreparePhaseCallback#granted(long, ProposeContext)}
     *                 if the majority refused prepare after several retries, call {@link PhaseCallback.PreparePhaseCallback#refused(ProposeContext)}
     */
    private void prepare(final ProposeContext ctxt, final PhaseCallback.PreparePhaseCallback callback) {

        // limit the prepare phase to only one thread.
        long curProposalNo = self.getCurProposalNo();
        if (skipPrepare.get() == PrepareState.PREPARED
                && ctxt.getPrepareNexted().compareAndSet(false, true)) {
            callback.granted(curProposalNo, ctxt);
            return;
        }

        LOG.debug("limit prepare. curProposalNo: {}, skipPrepare: {}", curProposalNo, skipPrepare);
        if (!skipPrepare.compareAndSet(PrepareState.NO_PREPARE, PrepareState.PREPARING)) {
            synchronized (skipPrepare) {
                try {
                    skipPrepare.wait(prepareTimeout);
                } catch (InterruptedException e) {
                    throw new ConsensusException(e.getMessage(), e);
                }
            }
            curProposalNo = self.getCurProposalNo();
            if (skipPrepare.get() == PrepareState.PREPARED
                    && ctxt.getPrepareNexted().compareAndSet(false, true)) {
                callback.granted(curProposalNo, ctxt);
                return;
            } else {
                prepare(ctxt, callback);
                return;
            }
        }

        // do prepare
        forcePrepare(ctxt, callback);
    }

    private void forcePrepare(final ProposeContext context, final PhaseCallback.PreparePhaseCallback callback) {
        // check retry times, refused() is invoked only when the number of retry times reaches the threshold
        if (context.getTimesAndIncrement() >= this.prop.getRetry()) {
            callback.refused(context);
            return;
        }

        preparedInstanceMap.clear();

        final ProposeContext ctxt = context.createUntappedRef();
        final long proposalNo = self.generateNextProposalNo();
        final PaxosMemberConfiguration memberConfiguration = memberConfig.createRef();

        LOG.info("start prepare phase, the {} retry, proposalNo: {}", context.getTimes(), proposalNo);

        PrepareReq req = PrepareReq.Builder.aPrepareReq()
                .nodeId(self.getSelf().getId())
                .proposalNo(proposalNo)
                .memberConfigurationVersion(memberConfiguration.getVersion())
                .build();

        // for self
        PrepareRes prepareRes = RoleAccessor.getAcceptor().handlePrepareRequest(req, true);
        handlePrepareResponse(proposalNo, ctxt, callback, prepareRes, self.getSelf());

        // for other members
        memberConfiguration.getMembersWithout(self.getSelf().getId()).forEach(it -> {
            client.sendRequestAsync(it, req, new AbstractInvokeCallback<PrepareRes>() {
                @Override
                public void error(final Throwable err) {
                    LOG.error("send prepare msg to node-{}, proposalNo: {}, occur exception, {}", it.getId(), proposalNo, err.getMessage());
                    ctxt.getPrepareQuorum().refuse(it);
                    if (ctxt.getPrepareQuorum().isGranted() == SingleQuorum.GrantResult.REFUSE
                            && ctxt.getPrepareNexted().compareAndSet(false, true)) {
                        ThreadExecutor.submit(() -> forcePrepare(ctxt, callback));
                    }
                }

                @Override
                public void complete(final PrepareRes result) {
                    handlePrepareResponse(proposalNo, ctxt, callback, result, it);
                }
            }, prepareTimeout);
        });

    }

    private void handlePrepareResponse(final long proposalNo, final ProposeContext ctxt, final PhaseCallback.PreparePhaseCallback callback,
                                       final PrepareRes result, final Endpoint it) {
        LOG.info("handling node-{}'s prepare response, proposalNo: {}, result: {}", result.getNodeId(), result.getCurProposalNo(), result.getResult());
        self.updateCurProposalNo(result.getCurProposalNo());
        self.updateCurInstanceId(result.getCurInstanceId());

        for (Instance<Proposal> instance : result.getInstances()) {
            if (preparedInstanceMap.putIfAbsent(instance.getInstanceId(), instance) != null) {
                synchronized (preparedInstanceMap) {
                    Instance<Proposal> prepared = preparedInstanceMap.get(instance.getInstanceId());
                    if (instance.getProposalNo() > prepared.getProposalNo()) {
                        preparedInstanceMap.put(instance.getInstanceId(), instance);
                    }
                }
            }
        }

        if (result.getResult()) {
            boolean grant = ctxt.getPrepareQuorum().grant(it);
            LOG.debug("handling node-{}'s prepare response, grant: {}, {}", result.getNodeId(), grant, ctxt.getPrepareQuorum().isGranted());
            if (ctxt.getPrepareQuorum().isGranted() == SingleQuorum.GrantResult.PASS
                    && ctxt.getPrepareNexted().compareAndSet(false, true)) {
                // do accept phase.
                callback.granted(proposalNo, ctxt);
            }
        } else {
            boolean refuse = ctxt.getPrepareQuorum().refuse(it);
            LOG.debug("handling node-{}'s prepare response, refuse: {}, {}", result.getNodeId(), refuse, ctxt.getPrepareQuorum().isGranted());

            // do prepare phase
            if (ctxt.getPrepareQuorum().isGranted() == SingleQuorum.GrantResult.REFUSE
                    && ctxt.getPrepareNexted().compareAndSet(false, true)) {
                ThreadExecutor.submit(() -> forcePrepare(ctxt, callback));
            }

            RoleAccessor.getLearner().pullSameData(result.getNodeState());
        }
    }

    public class ProposeEventHandler implements EventHandler<ProposalWithDone> {

        private final int batchSize;
        private final List<ProposalWithDone> tasks = new Vector<>();

        public ProposeEventHandler(final int batchSize) {
            this.batchSize = batchSize;
        }

        private void triggerHandle() {
            if (tasks.isEmpty()) {
                return;
            }
            propose(Proposal.NOOP, new ProposeDone.DefaultProposeDone());
        }

        private void handle() {
            List<ProposalWithDone> temp = new ArrayList<>(tasks);
            tasks.clear();

            LOG.info("start negotiations, proposal size: {}", temp.size());
            final List<ProposalWithDone> finalEvents = ImmutableList.copyOf(temp);
            ProposeContext ctxt = new ProposeContext(memberConfig.createRef(), new Holder<Long>() {
                @Override
                protected Long create() {
                    return self.incrementInstanceId();
                }
            }, finalEvents);

            long curProposalNo = self.getCurProposalNo();
            if (ProposerImpl.this.skipPrepare.get() != PrepareState.PREPARED) {
                prepare(ctxt, new PrepareCallback());
            } else {
                accept(curProposalNo, ctxt, new AcceptCallback());
            }
        }

        @Override
        public void onEvent(final ProposalWithDone event, final long sequence, final boolean endOfBatch) {
            if (event.getShutdownLatch() != null) {
                if (!this.tasks.isEmpty()) {
                    handle();
                }
                event.getShutdownLatch().countDown();
                return;
            }
            this.tasks.add(event);

            if (allowPropose && (this.tasks.size() >= batchSize || endOfBatch)) {
                handle();
            }
        }
    }

    public class PrepareCallback implements PhaseCallback.PreparePhaseCallback {

        @Override
        public void granted(final long grantedProposalNo, final ProposeContext context) {
            LOG.debug("prepare granted. proposalNo: {}", grantedProposalNo);
            synchronized (skipPrepare) {
                skipPrepare.compareAndSet(PrepareState.PREPARING, PrepareState.PREPARED);
                skipPrepare.notifyAll();
            }
            ThreadExecutor.submit(() -> accept(grantedProposalNo, context, new AcceptCallback()));
        }

        @Override
        public void refused(final ProposeContext context) {
            LOG.info("prepare refuse.");
            synchronized (skipPrepare) {
                skipPrepare.compareAndSet(PrepareState.PREPARING, PrepareState.NO_PREPARE);
                skipPrepare.notifyAll();
            }

            ThreadExecutor.submit(() -> {
                for (ProposalWithDone event : context.getDataWithCallback()) {
                    event.getDone().negotiationDone(false, null);
                }
            });
        }
    }

    public class AcceptCallback implements PhaseCallback.AcceptPhaseCallback {

        @Override
        public void granted(final ProposeContext context) {
            LOG.debug("accept granted. proposalNo: {}, instance: {}", context.getGrantedProposalNo(), context.getInstanceId());

            ProposerImpl.this.preparedInstanceMap.remove(context.getInstanceId());

            ThreadExecutor.submit(() -> {
                for (ProposalWithDone event : context.getDataWithCallback()) {
                    event.getDone().negotiationDone(true, context.getConsensusData());
                }
            });

            ThreadExecutor.submit(() -> {
                // do confirm
                List<ProposeDone> dons = context.getDataWithCallback().stream().map(ProposalWithDone::getDone).collect(Collectors.toList());
                RoleAccessor.getLearner().confirm(context.getInstanceId(), dons);
            });

        }

        @Override
        public void learn(final ProposeContext context, final Endpoint it) {
            LOG.debug("accept finds that the instance is confirmed. proposalNo: {}, instance: {}, target: {}", context.getGrantedProposalNo(), context.getInstanceId(), it.getId());
            ProposerImpl.this.preparedInstanceMap.remove(context.getInstanceId());

            ThreadExecutor.submit(() -> {
                // do learn
                RoleAccessor.getLearner().learn(context.getInstanceId(), it);
            });

            ThreadExecutor.submit(() -> {
                for (ProposalWithDone event : context.getDataWithCallback()) {
                    event.getDone().negotiationDone(false, null);
                }
            });

        }
    }


    /**
     * Prepare State.
     *
     * @author 释慧利
     */
    public enum PrepareState {
        NO_PREPARE,
        PREPARING,
        PREPARED;
    }
}
