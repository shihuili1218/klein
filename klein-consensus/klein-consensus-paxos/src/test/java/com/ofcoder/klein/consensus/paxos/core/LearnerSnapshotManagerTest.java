package com.ofcoder.klein.consensus.paxos.core;

import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LearnerSnapshotManagerTest {

    private Learner learner;
    private ConsensusProp consensusProp;
    private LearnerSnapshotManager learnerSnapshotManager;

    @Before
    public void setUp() {
        learner = mock(Learner.class);
        consensusProp = mock(ConsensusProp.class);
        when(consensusProp.getSnapshotStrategy()).thenReturn("10 5"); // 每10秒5个请求生成一次快照

        LearnerSnapshotManager.initAndStart(consensusProp, learner);
        learnerSnapshotManager = LearnerSnapshotManager.getInstance();
    }

    @Test
    public void testInitAndStart() {
        assertNotNull(learnerSnapshotManager);
    }

    @Test
    public void testAddReqCount() {
        learnerSnapshotManager.addReqCount();
        assertEquals(1, learnerSnapshotManager.getReqCount());
    }

//    @Test
//    public void testGenerateSnap() {
//        Map<String, SMApplier> sms = new HashMap<>();
//        SMApplier smApplier = mock(SMApplier.class);
//        sms.put("testGroup", smApplier);
//
//        when(learner.getSms()).thenReturn(sms);
//
//        Map<String, Snap> snaps = learnerSnapshotManager.generateSnap();
//
//        assertNotNull(snaps);
//        assertTrue(snaps.containsKey("testGroup"));
//    }
//
//    @Test
//    public void testCheckAndSnapshot() throws InterruptedException {
//        Map<String, SMApplier> sms = new HashMap<>();
//        SMApplier smApplier = mock(SMApplier.class);
//        sms.put("testGroup", smApplier);
//
//        when(learner.getSms()).thenReturn(sms);
//
//        learnerSnapshotManager.addReqCount();
//        learnerSnapshotManager.addReqCount();
//        learnerSnapshotManager.addReqCount();
//        learnerSnapshotManager.addReqCount();
//        learnerSnapshotManager.addReqCount();
//
//        TimeUnit.SECONDS.sleep(11); // 等待足够时间以触发快照生成
//
//        Map<String, Snap> snaps = learnerSnapshotManager.generateSnap();
//
//        assertNotNull(snaps);
//        assertTrue(snaps.containsKey("testGroup"));
//    }

    @Test
    public void testShutdown() {
        learnerSnapshotManager.shutdown();
        verify(learner).getSms(); // 验证在关闭时会调用 generateSnap
    }
}
