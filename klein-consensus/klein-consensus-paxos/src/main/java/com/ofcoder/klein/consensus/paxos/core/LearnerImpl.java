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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.ofcoder.klein.common.Holder;
import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.facade.Command;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.facade.sm.SMApplier;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.consensus.paxos.core.sm.PaxosMemberConfiguration;
import com.ofcoder.klein.consensus.paxos.rpc.vo.ConfirmReq;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.Snap;

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
    private final ConcurrentMap<Long, List<ProposeDone>> applyCallback = new ConcurrentHashMap<>();
    private ConsensusProp prop;
    private Long lastAppliedId = 0L;
    private final Object appliedIdLock = new Object();

    public LearnerImpl(final PaxosNode self) {
        this.self = self;
        this.memberConfig = MemberRegistry.getInstance().getMemberConfiguration();
    }

    @Override
    public void init(final ConsensusProp op) {
        this.prop = op;
        this.logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();
    }

    @Override
    public void shutdown() {
        generateSnap();
        sms.values().forEach(SMApplier::close);
    }

    @Override
    public Map<String, Snap> generateSnap() {
        ConcurrentMap<String, Snap> result = new ConcurrentHashMap<>();
        // fixme: latch的数量根据sms来定，但是这中间sms可能会rm，那么latch就结束不了了
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
            // ignore offer result.
        });

        try {
            if (latch.await(1L, TimeUnit.SECONDS)) {
                return result;
            } else {
                LOG.error("generate snapshot timeout. succ: {}, all: {}", result.keySet(), sms.keySet());
                return result;
            }
        } catch (InterruptedException ex) {
            LOG.error(String.format("generate snapshot occur exception. %s", ex.getMessage()), ex);
            return result;
        }
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
                        LOG.info("load snap success, group: {}, checkpoint: {}", group, checkpoint);

                        self.updateCurInstanceId(snap.getCheckpoint());
                        updateAppliedId(snap.getCheckpoint());

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

            SMApplier.Task e = SMApplier.Task.createReplayTask(i, replayData);

            applier.offer(e);
        }
    }

    @Override
    public long getLastAppliedInstanceId() {
        return lastAppliedId;
    }

    @Override
    public long getLastCheckpoint() {
        return sms.values().stream()
                .mapToLong(SMApplier::getLastCheckpoint)
                .max().orElse(-1L);
    }

    @Override
    public Set<String> getGroups() {
        return sms.keySet();
    }

    private void updateAppliedId(final long instanceId) {
        if (lastAppliedId < instanceId) {
            synchronized (appliedIdLock) {
                this.lastAppliedId = Math.max(lastAppliedId, instanceId);
            }
        }
    }

    @Override
    public void loadSM(final String group, final SM sm) {
        if (sms.putIfAbsent(group, new SMApplier(group, sm)) != null) {
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
        applyCallback.putIfAbsent(instanceId, dons.stream().map(ProposalWithDone::getDone).collect(Collectors.toList()));

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
                .forEach(it -> client.sendRequestAsync(it, req, new AbstractInvokeCallback<Serializable>() {
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

    private boolean handleConfirmRequest(final ConfirmReq req) {
        LOG.info("processing the confirm message from node-{}, instance: {}", req.getNodeId(), req.getInstanceId());

        if (req.getInstanceId() <= getLastCheckpoint()) {
            // the instance is compressed.
            LOG.info("the instance[{}] is compressed, checkpoint[{}]", req.getInstanceId(), getLastCheckpoint());
            return false;
        }

        try {
            logManager.getLock().writeLock().lock();

            Instance<Proposal> localInstance = logManager.getInstance(req.getInstanceId());
            if (localInstance == null || localInstance.getState() == Instance.State.PREPARED) {
                // the accept message is not received, the confirm message is received.
                // however, the instance has reached confirm, indicating that it has reached a consensus.
                LOG.info("confirm message is received, but accept message is not received, instance: {}", req.getInstanceId());
                RuntimeAccessor.getDataAligner().learn(req.getInstanceId(), memberConfig.getEndpointById(req.getNodeId()), new ApplyAfterLearnCallback(req.getInstanceId()));
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
                RuntimeAccessor.getDataAligner().learn(req.getInstanceId(), memberConfig.getEndpointById(req.getNodeId()), new ApplyAfterLearnCallback(req.getInstanceId()));
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
            logManager.getLock().writeLock().unlock();
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
        // fixme: 存在多个线程同时进来、_apply、_boost、_learn
        // _apply: 会执行多次，Applier排队执行的
        // _boost: 会抢占，
        // _learn: 多次执行，

        final long lastCheckpoint = getLastCheckpoint();
        final long lastApplyId = Math.max(lastAppliedId, lastCheckpoint);
        final long expectConfirmId = lastApplyId + 1;
        LOG.info("start apply, instanceId: {}, curAppliedInstanceId: {}, lastCheckpoint: {}", instanceId, lastAppliedId, lastCheckpoint);

        if (instanceId <= lastApplyId) {
            // the instance has been applied.
            return;
        }

        if (instanceId > expectConfirmId) {
            // boost.
            for (long i = expectConfirmId; i < instanceId; i++) {
                Instance<Proposal> exceptInstance = logManager.getInstance(i);
                if (exceptInstance != null && exceptInstance.getState() == Instance.State.CONFIRMED) {
                    _apply(i);
                } else {
                    if (memberConfig.allowBoost()) {
                        List<Proposal> defaultValue = exceptInstance == null || CollectionUtils.isEmpty(exceptInstance.getGrantedValue())
                                ? Lists.newArrayList(Proposal.NOOP) : exceptInstance.getGrantedValue();
                        _boost(i, defaultValue);
                    } else {
                        final Endpoint master = memberConfig.getMaster();
                        if (master != null) {
                            RuntimeAccessor.getDataAligner().learn(instanceId, master, new ApplyAfterLearnCallback(i));
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
        Instance<Proposal> localInstance = logManager.getInstance(instanceId);
        Map<String, List<Command>> groupProposals = localInstance.getGrantedValue().stream()
                .filter(it -> it != Proposal.NOOP)
                .collect(Collectors.groupingBy(Proposal::getGroup, Collectors.toList()));

        // fixme: 把instanceId传到sms里面去，不用在外面过滤
        groupProposals.forEach((group, proposals) -> {
            if (sms.containsKey(group)) {
                SMApplier.Task task = SMApplier.Task.createApplyTask(instanceId, proposals, new SMApplier.TaskCallback() {

                    @Override
                    public void onApply(final Map<Command, Object> result) {
                        List<ProposeDone> remove = applyCallback.remove(instanceId);
                        if (remove != null) {
                            Map<Proposal, Object> proposalResult = new HashMap<>();
                            result.forEach((k, v) -> proposalResult.put((Proposal) k, v));
                            remove.forEach(it -> it.applyDone(proposalResult));
                        }
                        updateAppliedId(instanceId);
                    }
                });

                sms.get(group).offer(task);
            } else {
                LOG.info("offer apply task, the {} sm is not exists. instanceId: {}", group, instanceId);
            }
        });
    }

    private void _boost(final long instanceId, final List<Proposal> defaultValue) {
        LOG.info("try boost instance: {}", instanceId);
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        RuntimeAccessor.getProposer().tryBoost(
                new Holder<Long>() {
                    @Override
                    protected Long create() {
                        return instanceId;
                    }

                }, defaultValue.stream().map(it -> new ProposalWithDone(it, (result, dataChange) -> future.complete(result)))
                        .collect(Collectors.toList())
        );
        try {
            boolean lr = future.get(prop.getRoundTimeout(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // do nothing
        }
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
