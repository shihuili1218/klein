package com.ofcoder.klein.consensus.paxos;

import com.ofcoder.klein.consensus.facade.ConsensusBootstrap;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.spi.Join;

/**
 * @author far.liu
 */
@Join
public class PaxosBootstrap implements ConsensusBootstrap {
    @Override
    public void startup(ConsensusProp prop) {

    }
}
