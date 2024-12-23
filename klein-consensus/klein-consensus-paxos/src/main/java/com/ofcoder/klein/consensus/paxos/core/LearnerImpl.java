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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.ofcoder.klein.consensus.facade.Command;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.facade.sm.SMApplier;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.consensus.paxos.core.sm.PaxosMemberConfiguration;
import com.ofcoder.klein.consensus.paxos.rpc.vo.ConfirmReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.NodeState;
import com.ofcoder.klein.consensus.paxos.rpc.vo.SnapSyncReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.SnapSyncRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.Sync;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.InvokeCallback;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.Snap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * learner implement.
 *
 * @author 释慧利
 */
public class LearnerImpl implements Learner {
    private static final Logger LOG = LoggerFactory.getLogger(LearnerImpl.class);
    private RpcClient client;
    private final PaxosNode self;
    private final PaxosMemberConfiguration memberConfig;
    private LogManager<Proposal> logManager;
    private final ConcurrentMap<String, SMApplier> sms = new ConcurrentHashMap<>();
    private final DataAligner dataAligner;
    private final ConcurrentMap<Long, List<ProposeDone>> applyCallback = new ConcurrentHashMap<>();
    private ConsensusProp prop;

    public LearnerImpl(final PaxosNode self) {
        this.self = self;
        this.memberConfig = MemberRegistry.getInstance().getMemberConfiguration();
        this.dataAligner = new DataAligner(self);
    }

    @Override
    public void init(final ConsensusProp op) {
        this.prop = op;
        this.logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();
        this.dataAligner.init(op);
    }

    @Override
    public void shutdown() {
        this.dataAligner.close();
        this.sms.values().forEach(SMApplier::close);
    }

    @Override
    public long getLastAppliedInstanceId() {
        return sms.values().stream()
                .mapToLong(SMApplier::getLastAppliedId)
                .max().orElse(-1L);
    }

    @Override
    public long getLastCheckpoint() {
        return sms.values().stream()
                .mapToLong(SMApplier::getLastCheckpoint)
                .max().orElse(-1L);
    }

    private Map<String, Snap> generateSnap() {
        Map<String, SMApplier> sms = new HashMap<>(this.sms);
        ConcurrentMap<String, Snap> result = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(sms.size());

        sms.forEach((group, sm) -> {
            SMApplier.Task e = SMApplier.Task.createTakeSnapTask(new SMApplier.TaskCallback() {
                @Override
                public void onTakeSnap(final Snap snap) {
                    result.put(group, snap);
                    latch.countDown();
                }
            });
            sm.offer(e);
        });

        try {
            if (!latch.await(1L, TimeUnit.SECONDS)) {
                LOG.error("generate snapshot timeout. succ: {}, all: {}", result.keySet(), sms.keySet());
            }
            return result;
        } catch (InterruptedException ex) {
            LOG.error(String.format("generate snapshot occur exception. %s", ex.getMessage()), ex);
            return result;
        }
    }

    @Override
    public Map<String, SMApplier> getSms() {
        return sms;
    }

    @Override
    public void loadSnapSync(final Map<String, Snap> snaps) {
        if (MapUtils.isEmpty(snaps)) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(snaps.size());

        snaps.forEach((group, snap) -> {
            if (sms.containsKey(group)) {
                SMApplier.Task e = SMApplier.Task.createLoadSnapTask(snap, new SMApplier.TaskCallback() {
                    @Override
                    public void onLoadSnap(final long checkpoint) {
                        self.updateCurInstanceId(snap.getCheckpoint());

                        applyCallback.keySet().removeIf(it -> it <= checkpoint);
                        latch.countDown();
//                        replayLog(group, checkpoint);
                    }
                });
                sms.get(group).offer(e);
            } else {
                latch.countDown();
                LOG.warn("load snap failure, group: {}, The state machine is not found."
                        + " It may be that Klein is starting and the state machine has not been loaded. Or, the state machine is unloaded.", group);
            }
        });

        try {
            if (!latch.await(1L, TimeUnit.SECONDS)) {
                LOG.error("load snapshot timeout. snaps: {}, all: {}", snaps.keySet(), sms.keySet());
            }
        } catch (InterruptedException ex) {
            LOG.error(String.format("load snapshot occur exception. %s", ex.getMessage()), ex);
        }
    }

    @Override
    public void replayLog(final String group, final long start) {
        if (!sms.containsKey(group)) {
            LOG.error("{} replay log, but the sm is not exists.", group);
            return;
        }
        SMApplier applier = sms.get(group);

        long curInstanceId = self.getCurInstanceId();
        for (long i = start; i <= curInstanceId; i++) {
            Instance<Proposal> instance = logManager.getInstance(i);
            if (instance == null || instance.getState() != Instance.State.CONFIRMED) {
                break;
            }
            List<Command> replayData = instance.getGrantedValue().stream()
                    .filter(it -> StringUtils.equals(group, it.getGroup()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(replayData)) {
                continue;
            }

            SMApplier.Task e = SMApplier.Task.createReplayTask(i);

            applier.offer(e);
        }
    }

    @Override
    public void loadSM(final String group, final SM sm) {
        if (sms.putIfAbsent(group, new SMApplier(group, sm, prop)) != null) {
            LOG.error("the group[{}] has been loaded with sm.", group);
            return;
        }
        Snap lastSnap = logManager.getLastSnap(group);
        if (lastSnap == null) {
            return;
        }
        loadSnapSync(ImmutableMap.of(group, lastSnap));
    }

    @Override
    public void confirm(final long instanceId, final String checksum, final List<ProposalWithDone> dons) {
        LOG.info("start confirm phase, instanceId: {}", instanceId);
        registerApplyCallback(instanceId, dons.stream().map(ProposalWithDone::getDone).collect(Collectors.toList()));

        // A proposalNo here does not have to use the proposalNo of the accept phase,
        // because the proposal is already in the confirm phase and it will not change.
        // Instead, using self.proposalNo allows you to more quickly advance a proposalNo for another member
        long curProposalNo = self.getCurProposalNo();

        PaxosMemberConfiguration configuration = memberConfig.createRef();

        ConfirmReq req = ConfirmReq.Builder.aConfirmReq()
                .nodeId(self.getSelf().getId())
                .proposalNo(curProposalNo)
                .instanceId(instanceId)
                .checksum(checksum)
                .build();

        // for self
        if (handleConfirmRequest(req)) {
            // apply statemachine
            apply(instanceId);
        } else {
            dons.forEach(it -> it.getDone().applyDone(new HashMap<>()));
        }

        // for other members
        configuration.getMembersWithout(self.getSelf().getId())
                .forEach(it -> client.sendRequestAsync(it, req, new InvokeCallback<Serializable>() {
                    @Override
                    public void error(final Throwable err) {
                        LOG.error("send confirm msg to node-{}, instance[{}], {}", it.getId(), instanceId, err.getMessage());
                        // do nothing
                    }

                    @Override
                    public void complete(final Serializable result) {
                        // do nothing
                    }
                }));
    }

    @Override
    public void alignData(final NodeState state) {
        final Endpoint target = memberConfig.getEndpointById(state.getNodeId());
        if (target == null) {
            return;
        }

        final long targetCheckpoint = state.getLastCheckpoint();
        final long targetApplied = state.getLastAppliedInstanceId();
        long localApplied = RuntimeAccessor.getLearner().getLastAppliedInstanceId();
        long localCheckpoint = RuntimeAccessor.getLearner().getLastCheckpoint();
        if (targetApplied <= localApplied) {
            // same data
            return;
        }

        LOG.info("keepSameData, target[id: {}, cp: {}, maxAppliedInstanceId:{}], local[cp: {}, maxAppliedInstanceId:{}]",
                target.getId(), targetCheckpoint, targetApplied, localCheckpoint, localApplied);
        if (targetCheckpoint > localApplied || targetCheckpoint - localCheckpoint >= 100) {
            this.dataAligner.snap(target, result -> {
                if (result) {
                    alignData(state);
                }
            });
        } else {
            for (long i = localApplied + 1; i <= targetApplied; i++) {
                Instance<Proposal> instance = logManager.getInstance(i);
                if (instance == null || instance.getState() != Instance.State.CONFIRMED) {
                    this.dataAligner.diff(i, target, new ApplyAfterLearnCallback(i));
                } else {
                    apply(i);
                }
            }
        }
    }

    private boolean handleConfirmRequest(final ConfirmReq req) {
        LOG.info("processing the confirm request from node-{}, instance: {}", req.getNodeId(), req.getInstanceId());

        if (req.getInstanceId() <= getLastCheckpoint()) {
            // the instance is compressed.
            LOG.info("the instance[{}] is compressed, checkpoint[{}]", req.getInstanceId(), getLastCheckpoint());
            return false;
        }

        try {
            logManager.getLock(req.getInstanceId()).writeLock().lock();

            Instance<Proposal> localInstance = logManager.getInstance(req.getInstanceId());
            if (localInstance == null || localInstance.getState() == Instance.State.PREPARED) {
                // the accept message is not received, the confirm message is received.
                // however, the instance has reached confirm, indicating that it has reached a consensus.
                LOG.info("confirm message is received, but accept message is not received, instance: {}", req.getInstanceId());
                this.dataAligner.diff(req.getInstanceId(), memberConfig.getEndpointById(req.getNodeId()), new ApplyAfterLearnCallback(req.getInstanceId()));
                return false;
            }

            if (localInstance.getState() == Instance.State.CONFIRMED) {
                // the instance is confirmed.
                LOG.info("the instance[{}] is confirmed", localInstance.getInstanceId());
                return false;
            }

            // check sum
            if (!StringUtils.equals(localInstance.getChecksum(), req.getChecksum())) {
                LOG.info("local.checksum: {}, req.checksum: {}", localInstance.getChecksum(), req.getChecksum());
                this.dataAligner.diff(req.getInstanceId(), memberConfig.getEndpointById(req.getNodeId()), new ApplyAfterLearnCallback(req.getInstanceId()));
                return false;
            }

            localInstance.setState(Instance.State.CONFIRMED);
            localInstance.setProposalNo(req.getProposalNo());
            logManager.updateInstance(localInstance);

            return true;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } finally {
            self.updateCurInstanceId(req.getInstanceId());
            logManager.getLock(req.getInstanceId()).writeLock().unlock();
        }
    }

    @Override
    public void handleConfirmRequest(final ConfirmReq req, final boolean isSelf) {
        if (handleConfirmRequest(req)) {
            // apply statemachine
            apply(req.getInstanceId());
        }
    }

    private void apply(final long instanceId) {
        // _apply: 会执行多次，Applier排队执行的
        // _boost: 会抢占，在accept阶段会有ConcurrentSet
        // _learn: 多次执行，Aligner排队执行的

        final long lastApplyId = getLastAppliedInstanceId();
        final long expectConfirmId = lastApplyId + 1;

        if (instanceId <= lastApplyId) {
            // the instance has been applied.
            return;
        }

        LOG.debug("start apply, instanceId: {}, curAppliedInstanceId: {}, lastCheckpoint: {}", instanceId, getLastAppliedInstanceId(), getLastCheckpoint());
        if (instanceId > expectConfirmId) {
            registerApplyCallback(instanceId - 1, Lists.newArrayList(new ProposeDone() {
                @Override
                public void negotiationDone(final boolean result, final boolean dataChange) {
                }

                @Override
                public void applyDone(final Map<Command, Object> result) {
                    apply(instanceId);
                }
            }));

            // boost.
            for (long i = expectConfirmId; i < instanceId; i++) {
                Instance<Proposal> exceptInstance = logManager.getInstance(i);
                if (exceptInstance != null && exceptInstance.getState() == Instance.State.CONFIRMED) {
                    _apply(i);
                } else {
                    MasterState masterState = RuntimeAccessor.getMaster().getMaster();
                    if (masterState.getElectState().allowBoost()) {
                        LOG.info("try boost instance: {}", instanceId);
                        RuntimeAccessor.getProposer().tryBoost(i, new ProposeDone.FakeProposeDone());
                    } else {
                        final Endpoint master = masterState.getMaster();
                        if (master != null) {
                            this.dataAligner.diff(i, master, new ApplyAfterLearnCallback(i));
                        }
                    }
                }
            }
            return;
        }
        // else: instanceId == exceptConfirmId, do apply.

        _apply(instanceId);
    }

    private void _apply(final long instanceId) {
        List<SMApplier> smAppliers = new ArrayList<>(sms.values());
        List<ProposeDone> remove = applyCallback.remove(instanceId);
        Map<Command, Object> applyResult = new HashMap<>();

        smAppliers.stream().map(smApplier -> {
            final CompletableFuture<Map<Command, Object>> complete = new CompletableFuture<>();
            smApplier.offer(SMApplier.Task.createApplyTask(instanceId, new SMApplier.TaskCallback() {
                @Override
                public void onApply(final Map<Command, Object> result) {
                    complete.complete(result);
                }
            }));
            return complete;
        }).parallel().map(future -> {
            try {
                return future.get(prop.getRoundTimeout(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                LOG.error("apply sm error, instanceId: {}, {}", instanceId, e.getMessage());
                return new HashMap<Command, Object>();
            }
        }).collect(Collectors.toList()).forEach(applyResult::putAll);

        if (remove != null) {
            remove.forEach(it -> it.applyDone(applyResult));
        }
    }

    private void registerApplyCallback(final long instanceId, final List<ProposeDone> callbacks) {
        if (applyCallback.putIfAbsent(instanceId, callbacks) != null) {
            // addAll 存在竞态条件
            applyCallback.get(instanceId).addAll(callbacks);
        }
    }

    @Override
    public LearnRes handleLearnRequest(final LearnReq request) {
        LOG.info("received a learn message from node[{}] about instance[{}]", request.getNodeId(), request.getInstanceId());
        LearnRes.Builder res = LearnRes.Builder.aLearnRes().nodeId(self.getSelf().getId());

        if (request.getInstanceId() <= RuntimeAccessor.getLearner().getLastCheckpoint()) {
            return res.result(Sync.SNAP).build();
        }

        Instance<Proposal> instance = logManager.getInstance(request.getInstanceId());

        if (instance == null || instance.getState() != Instance.State.CONFIRMED) {
            LOG.debug("NO_SUPPORT, learnInstance[{}], cp: {}, cur: {}", request.getInstanceId(),
                    RuntimeAccessor.getLearner().getLastCheckpoint(), self.getCurInstanceId());
            MasterState masterState = RuntimeAccessor.getMaster().getMaster();
            if (masterState.getElectState().allowBoost()) {
                LOG.debug("NO_SUPPORT, but i am master, try boost: {}", request.getInstanceId());

                CountDownLatch latch = new CountDownLatch(1);
                RuntimeAccessor.getProposer().tryBoost(request.getInstanceId(), (result, dataChange) -> latch.countDown());

                try {
                    boolean await = latch.await(this.prop.getRoundTimeout() * this.prop.getRetry(), TimeUnit.MILLISECONDS);
                    // do nothing for await's result
                } catch (InterruptedException e) {
                    LOG.debug(e.getMessage());
                }

                instance = logManager.getInstance(request.getInstanceId());
            }
        }
        if (instance == null || instance.getState() != Instance.State.CONFIRMED) {
            return res.result(Sync.NO_SUPPORT).instance(instance).build();
        } else {
            return res.result(Sync.SINGLE).instance(instance).build();
        }
    }

    @Override
    public SnapSyncRes handleSnapSyncRequest(final SnapSyncReq req) {
        LOG.info("processing the pull snap message from node-{}", req.getNodeId());
        SnapSyncRes res = SnapSyncRes.Builder.aSnapSyncRes()
                .images(new HashMap<>())
                .checkpoint(RuntimeAccessor.getLearner().getLastCheckpoint())
                .build();
        if (getLastAppliedInstanceId() - getLastCheckpoint() >= 100) {
            Map<String, Snap> allSnaps = generateSnap();
            res.getImages().putAll(allSnaps);
        } else {
            for (String group : sms.keySet()) {
                Snap lastSnap = logManager.getLastSnap(group);
                if (lastSnap != null && lastSnap.getCheckpoint() > req.getCheckpoint()) {
                    res.getImages().put(group, lastSnap);
                }
            }
        }
        return res;
    }

    class ApplyAfterLearnCallback implements DataAligner.LearnCallback {
        private final long instanceId;

        ApplyAfterLearnCallback(final long instanceId) {
            this.instanceId = instanceId;
        }

        @Override
        public void learned(final boolean result) {
            if (result) {
                apply(instanceId);
            }
        }
    }
}
