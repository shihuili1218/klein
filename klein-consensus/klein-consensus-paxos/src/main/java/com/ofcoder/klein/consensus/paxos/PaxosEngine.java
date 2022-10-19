package com.ofcoder.klein.consensus.paxos;

import com.ofcoder.klein.consensus.facade.ConsensusEngine;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.spi.Join;

/**
 * @author far.liu
 */
@Join
public class PaxosEngine implements ConsensusEngine {
    @Override
    public void init(ConsensusProp op) {

    }

    @Override
    public void shutdown() {

    }
}
