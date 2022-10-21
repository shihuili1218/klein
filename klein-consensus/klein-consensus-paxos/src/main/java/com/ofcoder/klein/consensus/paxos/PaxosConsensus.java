package com.ofcoder.klein.consensus.paxos;

import java.nio.ByteBuffer;

import com.ofcoder.klein.consensus.facade.Consensus;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.SM;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.spi.Join;

/**
 * @author far.liu
 */
@Join
public class PaxosConsensus implements Consensus {
    @Override
    public Result propose(ByteBuffer data) {
        return null;
    }

    @Override
    public Result read(ByteBuffer data) {
        return null;
    }

    @Override
    public void loadSM(SM sm) {

    }

    @Override
    public void init(ConsensusProp op) {

    }

    @Override
    public void shutdown() {

    }
}
