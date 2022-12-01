package com.ofcoder.klein.consensus.paxos.core;

import com.ofcoder.klein.consensus.paxos.PaxosNode;

import java.io.Serializable;

/**
 * 描述: 写请求的转发
 * @author petter
 */
public interface ProposerRedirect {

    <E extends Serializable> void call(final String group, final E data, final PaxosNode node, final ProposeDone done);
}
