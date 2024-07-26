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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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
import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.common.util.ChecksumUtil;
import com.ofcoder.klein.common.util.KleinThreadFactory;
import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.facade.Command;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.exception.ConsensusException;
import com.ofcoder.klein.consensus.facade.quorum.SingleQuorum;
import com.ofcoder.klein.consensus.facade.sm.SystemOp;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.consensus.paxos.core.sm.PaxosMemberConfiguration;
import com.ofcoder.klein.consensus.paxos.rpc.vo.AcceptReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.AcceptRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.NodeState;
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
    private RpcClient client;
    private ConsensusProp prop;
    private final PaxosNode self;
    private final PaxosMemberConfiguration memberConfig;
    private long prepareTimeout;
    private long acceptTimeout;
    private RingBuffer<ProposalWithDone> proposeQueue;
    private Disruptor<ProposalWithDone> proposeDisruptor;
    private CountDownLatch shutdownLatch;
    /**
     * The instance of the Prepare phase has been executed.
     */
    private final ConcurrentMap<Long, Instance<Command>> seenInstances = new ConcurrentHashMap<>();
    private final Set<Long> runningInstance = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private LogManager<Proposal> logManager;

    public ProposerImpl(final PaxosNode self) {
        this.self = self;
        this.memberConfig = MemberRegistry.getInstance().getMemberConfiguration();
    }

    @Override
    public void init(final ConsensusProp op) {
        this.prop = op;
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();
        this.prepareTimeout = (long) (op.getRoundTimeout() * 0.4);
        this.acceptTimeout = op.getRoundTimeout() - prepareTimeout;
        this.logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();

        // Disruptor to run propose.
        proposeDisruptor = DisruptorBuilder.<ProposalWithDone>newInstance()
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

        proposeDisruptor.shutdown();
    }

    /**
     * Propose proposal.
     * Put the request on the {@link ProposerImpl#proposeQueue},
     * and process it as {@link ProposeEventHandler}
     *
     * @param data client's data
     * @param done client's callback
     */
    @Override
    public void propose(final Command data, final ProposeDone done, final boolean now) {
        if (this.shutdownLatch != null) {
            throw new ConsensusException("klein is shutting down.");
        }

        if (now) {
            ProposeContext ctxt = new ProposeContext(memberConfig.createRef(), new Holder<Long>() {
                @Override
                protected Long create() {
                    return self.incrementInstanceId();
                }
            }, Lists.newArrayList(new ProposalWithDone(data, done)));

            prepare(ctxt, new PrepareCallback());
        } else {
            final EventTranslator<ProposalWithDone> translator = (event, sequence) -> {
                event.setProposal(data);
                event.setDone(done);
            };
            this.proposeQueue.publishEvent(translator);
        }

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
     *                          if an acceptor returns a confirmed instance, call {@link PhaseCallback.AcceptPhaseCallback#learn(ProposeContext, NodeState)}
     */
    private void accept(final long grantedProposalNo, final ProposeContext ctxt, final PhaseCallback.AcceptPhaseCallback callback) {
        if (!runningInstance.add(ctxt.getInstanceId())) {
            // fixme: 等待其他线程执行完，返回正确的结果
            ctxt.getDataWithCallback().stream().map(ProposalWithDone::getDone).forEach(it -> it.negotiationDone(false, false));
            return;
        }

        try {
            _accept(grantedProposalNo, ctxt, callback);
        } finally {
            runningInstance.remove(ctxt.getInstanceId());
        }
    }

    private void _accept(final long grantedProposalNo, final ProposeContext ctxt, final PhaseCallback.AcceptPhaseCallback callback) {
        LOG.info("start accept phase, proposalNo: {}, instanceId: {}", grantedProposalNo, ctxt.getInstanceId());

        // choose valid proposal, and calculate checksum.
        List<Command> originalProposals = ctxt.getDataWithCallback().stream().map(ProposalWithDone::getProposal).collect(Collectors.toList());
        String originalChecksum = ChecksumUtil.md5(Hessian2Util.serialize(originalProposals));
        ctxt.setDataChange(seenInstances.containsKey(ctxt.getInstanceId())
                && CollectionUtils.isNotEmpty(seenInstances.get(ctxt.getInstanceId()).getGrantedValue())
                && !StringUtils.equals(seenInstances.get(ctxt.getInstanceId()).getChecksum(), originalChecksum));
        ctxt.setConsensusData(ctxt.isDataChange() ? seenInstances.get(ctxt.getInstanceId()).getGrantedValue()
                : originalProposals);
        ctxt.setConsensusChecksum(ctxt.isDataChange() ? seenInstances.get(ctxt.getInstanceId()).getChecksum() : originalChecksum);
        ctxt.setGrantedProposalNo(grantedProposalNo);

        final PaxosMemberConfiguration memberConfiguration = ctxt.getMemberConfiguration().createRef();
        final AcceptReq req = AcceptReq.Builder.anAcceptReq()
                .nodeId(self.getSelf().getId())
                .instanceId(ctxt.getInstanceId())
                .proposalNo(ctxt.getGrantedProposalNo())
                .data(ctxt.getConsensusData())
                .checksum(ctxt.getConsensusChecksum())
                .memberConfigurationVersion(memberConfiguration.getVersion())
                .build();

        // for self
        AcceptRes res = RuntimeAccessor.getAcceptor().handleAcceptRequest(req, true);
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

                        RuntimeAccessor.getSkipPrepare().compareAndSet(PrepareState.PREPARED, PrepareState.NO_PREPARE);
                        ThreadExecutor.execute(() -> prepare(ctxt.createUntappedRef(), new PrepareCallback()));
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
        LOG.info("handling node-{}'s accept response, local.proposalNo: {}, instanceId: {}, remote.proposalNo: {}, result: {}",
                result.getNodeId(), ctxt.getGrantedProposalNo(), ctxt.getInstanceId(), result.getCurProposalNo(), result.getResult());
        self.updateCurProposalNo(result.getCurProposalNo());
        self.updateCurInstanceId(result.getCurInstanceId());

        if (result.getInstanceState() == Instance.State.CONFIRMED
                && ctxt.getAcceptNexted().compareAndSet(false, true)) {
            callback.learn(ctxt, result.getNodeState());
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
                RuntimeAccessor.getSkipPrepare().compareAndSet(PrepareState.PREPARED, PrepareState.NO_PREPARE);
                ThreadExecutor.execute(() -> prepare(ctxt.createUntappedRef(), new PrepareCallback()));
            }
        }
    }

    @Override
    public void tryBoost(final Long instanceId, final ProposeDone done) {
        ProposeContext ctxt = new ProposeContext(memberConfig.createRef(), new Holder<Long>() {
            @Override
            protected Long create() {
                return instanceId;
            }
        }, Lists.newArrayList(new ProposalWithDone(Command.NOOP, done)));
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
        LOG.debug("limit prepare. curProposalNo: {}, skipPrepare: {}", curProposalNo, RuntimeAccessor.getSkipPrepare());

        if (RuntimeAccessor.getSkipPrepare().get() == PrepareState.PREPARED) {
            if (ctxt.getPrepareNexted().compareAndSet(false, true)) {
                callback.granted(curProposalNo, ctxt);
            }
            return;
        }

        synchronized (RuntimeAccessor.getSkipPrepare()) {
            curProposalNo = self.getCurProposalNo();
            if (RuntimeAccessor.getSkipPrepare().get() == PrepareState.PREPARED) {
                if (ctxt.getPrepareNexted().compareAndSet(false, true)) {
                    callback.granted(curProposalNo, ctxt);
                }
                return;
            }
            RuntimeAccessor.getSkipPrepare().set(PrepareState.PREPARING);

            try {
                // do prepare
                CountDownLatch latch = new CountDownLatch(1);
                _prepare(ctxt, new PhaseCallback.PreparePhaseCallback() {
                    @Override
                    public void granted(final long grantedProposalNo, final ProposeContext context) {
                        latch.countDown();
                        RuntimeAccessor.getSkipPrepare().compareAndSet(PrepareState.PREPARING, PrepareState.PREPARED);
                        callback.granted(grantedProposalNo, context);
                    }

                    @Override
                    public void refused(final ProposeContext context) {
                        latch.countDown();
                        RuntimeAccessor.getSkipPrepare().compareAndSet(PrepareState.PREPARING, PrepareState.NO_PREPARE);
                        callback.refused(context);
                    }
                });

                boolean await = latch.await(prepareTimeout * prop.getRetry() + 10, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new ConsensusException(e.getMessage(), e);
            } finally {
                if (RuntimeAccessor.getSkipPrepare().get() == PrepareState.PREPARING) {
                    RuntimeAccessor.getSkipPrepare().set(PrepareState.NO_PREPARE);
                }
            }
        }

    }

    private void _prepare(final ProposeContext ctxt, final PhaseCallback.PreparePhaseCallback callback) {
        // check retry times, refused() is invoked only when the number of retry times reaches the threshold
        if (ctxt.getTimesAndIncrement() >= this.prop.getRetry()) {
            callback.refused(ctxt);
            return;
        }

        seenInstances.clear();

        final long proposalNo = self.generateNextProposalNo();
        final PaxosMemberConfiguration memberConfiguration = ctxt.getMemberConfiguration().createRef();

        LOG.info("start prepare phase, the {} retry, proposalNo: {}", ctxt.getTimes(), proposalNo);

        PrepareReq req = PrepareReq.Builder.aPrepareReq()
                .nodeId(self.getSelf().getId())
                .proposalNo(proposalNo)
                .memberConfigurationVersion(memberConfiguration.getVersion())
                .build();

        // for self
        PrepareRes prepareRes = RuntimeAccessor.getAcceptor().handlePrepareRequest(req, true);
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
                        ThreadExecutor.execute(() -> _prepare(ctxt.createUntappedRef(), callback));
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

        for (Instance<Command> instance : result.getInstances()) {
            if (seenInstances.putIfAbsent(instance.getInstanceId(), instance) != null) {
                synchronized (seenInstances) {
                    Instance<Command> prepared = seenInstances.get(instance.getInstanceId());
                    if (instance.getProposalNo() > prepared.getProposalNo()) {
                        seenInstances.put(instance.getInstanceId(), instance);
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
                ThreadExecutor.execute(() -> _prepare(ctxt.createUntappedRef(), callback));
            }

//            RuntimeAccessor.getLearner().pullSameData(result.getNodeState());
        }
    }

    public class ProposeEventHandler implements EventHandler<ProposalWithDone> {

        private final int batchSize;
        private final List<ProposalWithDone> tasks = new Vector<>();

        public ProposeEventHandler(final int batchSize) {
            this.batchSize = batchSize;
        }

        private void handle() {
            final List<ProposalWithDone> finalEvents = ImmutableList.copyOf(tasks);
            tasks.clear();

            LOG.info("start negotiations, proposal size: {}", finalEvents.size());
            ProposeContext ctxt = new ProposeContext(memberConfig.createRef(), new Holder<Long>() {
                @Override
                protected Long create() {
                    return self.incrementInstanceId();
                }
            }, finalEvents);

            long curProposalNo = self.getCurProposalNo();
            if (RuntimeAccessor.getSkipPrepare().get() != PrepareState.PREPARED) {
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

            if (event.getProposal().getData() instanceof SystemOp ||
                    (RuntimeAccessor.getMaster().getMaster().getElectState().allowPropose()
                            && (this.tasks.size() >= batchSize || endOfBatch))
            ) {
                handle();
            }
        }
    }

    public class PrepareCallback implements PhaseCallback.PreparePhaseCallback {

        @Override
        public void granted(final long grantedProposalNo, final ProposeContext context) {
            LOG.debug("prepare granted. proposalNo: {}", grantedProposalNo);
            ThreadExecutor.execute(() -> accept(grantedProposalNo, context, new AcceptCallback()));
        }

        @Override
        public void refused(final ProposeContext context) {
            LOG.info("prepare refuse.");
            for (ProposalWithDone event : context.getDataWithCallback()) {
                event.getDone().negotiationDone(false, false);
            }
        }
    }

    public class AcceptCallback implements PhaseCallback.AcceptPhaseCallback {

        @Override
        public void granted(final ProposeContext context) {
            LOG.debug("accept granted. proposalNo: {}, instance: {}", context.getGrantedProposalNo(), context.getInstanceId());

            ProposerImpl.this.seenInstances.remove(context.getInstanceId());

            context.getDataWithCallback().forEach(it ->
                    it.getDone().negotiationDone(true, context.isDataChange()));

            ThreadExecutor.execute(() -> {
                // do confirm
                List<ProposalWithDone> dons = new ArrayList<>();
                if (!context.isDataChange()) {
                    dons = context.getDataWithCallback();
                } else {
                    // accelerate convergence
                    context.getDataWithCallback().forEach(it -> it.getDone().applyDone(new HashMap<>()));
                }
                RuntimeAccessor.getLearner().confirm(context.getInstanceId(), context.getConsensusChecksum(), dons);
            });
        }

        @Override
        public void learn(final ProposeContext context, final NodeState target) {
            LOG.debug("accept finds that the instance is confirmed. proposalNo: {}, instance: {}, target: {}", context.getGrantedProposalNo(), context.getInstanceId(), target.getNodeId());
            ProposerImpl.this.seenInstances.remove(context.getInstanceId());

            for (ProposalWithDone event : context.getDataWithCallback()) {
                event.getDone().negotiationDone(false, false);
            }

            ThreadExecutor.execute(() -> {
                // do learn
                RuntimeAccessor.getLearner().alignData(target);
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
