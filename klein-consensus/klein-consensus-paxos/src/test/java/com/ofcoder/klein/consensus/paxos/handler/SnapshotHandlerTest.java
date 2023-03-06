package com.ofcoder.klein.consensus.paxos.handler;

import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.core.impl.MockLearner;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnReq;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 快照触发器
 */
public class SnapshotHandlerTest {
    public SnapshotHandler snapshotHandler;
    public PaxosNode paxosNode;

    @Before
    public void init(){
        paxosNode = new PaxosNode();
        snapshotHandler = new MockLearner(paxosNode);
        snapshotHandler.init(new ConsensusProp());
    }

    @Test
    public void testInitialize() throws InterruptedException {
        assertEquals(snapshotHandler.getRdbConfigList().size(), 3);
        assertNotNull(snapshotHandler.getTimer());
    }
    @Test
    public void testTriggerSnap() throws InterruptedException {
//        "1 10000;5 10;30 1"
        for (int i = 0; i < 10000; i++) {
            snapshotHandler.handleLearnRequest(new LearnReq());
        }
        assertEquals(MockLearner.WRITE_COUNT, 10000);
        assertEquals(MockLearner.GENERATE_SNAP_COUNT, 0);
        assertEquals(snapshotHandler.self.getLastCheckpoint(), 0);

        TimeUnit.SECONDS.sleep(2);
        for (int i = 0; i < 11; i++) {
            snapshotHandler.handleLearnRequest(new LearnReq());
        }
        assertEquals(MockLearner.WRITE_COUNT, 10011);
        assertEquals(MockLearner.GENERATE_SNAP_COUNT, 1);
        assertEquals(snapshotHandler.self.getLastCheckpoint(), 10000);

        TimeUnit.SECONDS.sleep(6);
        assertEquals(MockLearner.WRITE_COUNT, 10011);
        assertEquals(MockLearner.GENERATE_SNAP_COUNT, 2);
        assertEquals(snapshotHandler.self.getLastCheckpoint(), 10011);
    }
}
