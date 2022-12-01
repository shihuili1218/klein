package com.ofcoder.klein.consensus.paxos.core;

import com.google.common.collect.Lists;
import com.ofcoder.klein.common.exception.RedirectException;
import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectRes;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.spi.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

/**
 * @author petter
 */
public class ProxyProposer implements ProposerRedirect{

    private static final Logger LOG = LoggerFactory.getLogger(ProxyProposer.class);
    private RpcClient client;

    public ProxyProposer() {
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();
    }

    @Override
    public <E extends Serializable> void call(String group, E data, PaxosNode node, ProposeDone done) {
        Endpoint master = node.getMemberConfiguration().getMaster();
        RedirectReq req = RedirectReq.Builder.anRedirectReq()
                .nodeId(node.getSelf().getId())
                .instanceId(node.incrementInstanceId())
                .proposalNo(node.getCurProposalNo())
                .data(Lists.newArrayList(new Proposal(group, data)))
                .memberConfigurationVersion(node.getMemberConfiguration().getVersion())
                .build();
        client.sendRequestAsync(master, req, new AbstractInvokeCallback<RedirectRes>() {
            @Override
            public void complete(RedirectRes result) {
                LOG.info("redirect msg to master success, instance[{}]",node.getCurAppliedInstanceId());
            }

            @Override
            public void error(Throwable err) {
                LOG.error("redirect msg to master node-{}, instance[{}], {}", master.getId(), node.getCurAppliedInstanceId(), err.getMessage());
                throw new RedirectException(err.getMessage(), err);
            }

        }, 1000);
    }
}
