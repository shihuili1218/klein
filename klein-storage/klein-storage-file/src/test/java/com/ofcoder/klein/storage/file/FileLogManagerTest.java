package com.ofcoder.klein.storage.file;

import com.google.common.collect.Lists;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.Snap;
import com.ofcoder.klein.storage.facade.config.StorageProp;
import com.ofcoder.klein.storage.facade.exception.LockException;
import java.util.List;
import java.util.Random;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FileLogManagerTest {
    LogManager logManager;

    @Before
    public void setUp() {
        logManager = ExtensionLoader.getExtensionLoader(LogManager.class).register("file", new StorageProp());
    }

    @After
    public void shutdown() {
        logManager.shutdown();
    }

    @Test(expected = LockException.class)
    public void testUpdateInstance_noLock() {
        Instance<String> instance = new Instance<>();
        instance.setProposalNo(1);
        instance.setInstanceId(1);
        instance.setState(Instance.State.PREPARED);
        instance.setGrantedValue(Lists.newArrayList("Zzz"));
        logManager.updateInstance(instance);
    }

    @Test()
    public void testGetInstance() {
        Instance nil = logManager.getInstance(1);
        Assert.assertNull(nil);

        Instance<String> instance = new Instance<>();
        instance.setProposalNo(1);
        instance.setInstanceId(1);
        instance.setState(Instance.State.PREPARED);
        instance.setGrantedValue(Lists.newArrayList("Zzz"));

        logManager.getLock(instance.getInstanceId()).writeLock().lock();
        logManager.updateInstance(instance);
        logManager.getLock(instance.getInstanceId()).writeLock().unlock();

        Instance actual = logManager.getInstance(1);


        Assert.assertNotNull(actual);
        Assert.assertEquals(instance.getInstanceId(), actual.getInstanceId());
        Assert.assertEquals(instance.getProposalNo(), actual.getProposalNo());
        Assert.assertEquals(instance.getState(), actual.getState());
        Assert.assertEquals(instance.getGrantedValue(), actual.getGrantedValue());
    }

    @Test()
    public void testGetInstanceNoConfirm() {

        Instance<String> instance1 = new Instance<>();
        instance1.setProposalNo(1);
        instance1.setInstanceId(1);
        instance1.setState(Instance.State.PREPARED);
        instance1.setGrantedValue(Lists.newArrayList("Zzz"));

        Instance<String> instance2 = new Instance<>();
        instance2.setProposalNo(1);
        instance2.setInstanceId(2);
        instance2.setState(Instance.State.CONFIRMED);
        instance2.setGrantedValue(Lists.newArrayList("Zzz"));

        logManager.getLock(instance1.getInstanceId()).writeLock().lock();
        logManager.updateInstance(instance1);
        logManager.getLock(instance1.getInstanceId()).writeLock().unlock();
        logManager.getLock(instance2.getInstanceId()).writeLock().lock();
        logManager.updateInstance(instance2);
        logManager.getLock(instance2.getInstanceId()).writeLock().unlock();

        List instanceNoConfirm = logManager.getInstanceNoConfirm();
        Assert.assertNotNull(instanceNoConfirm);
        Assert.assertEquals(1, instanceNoConfirm.size());
        Instance<String> actual = (Instance<String>) instanceNoConfirm.get(0);

        Assert.assertEquals(instance1.getInstanceId(), actual.getInstanceId());
        Assert.assertEquals(instance1.getProposalNo(), actual.getProposalNo());
        Assert.assertEquals(instance1.getState(), actual.getState());
        Assert.assertEquals(instance1.getGrantedValue(), actual.getGrantedValue());
    }

    @Test
    public void testSaveSnapAndGetLastSnap() {
        Snap snap = new Snap(new Random().nextLong(), new byte[] {0x1, 0x13, 0x12, 0x3, 0x3});
        Snap snap2 = new Snap(new Random().nextLong(), new byte[] {0x1, 0x13, 0x12, 0x3, 0x2});
        String group = "Test";
        logManager.saveSnap(group, snap);
        Snap lastSnap = logManager.getLastSnap(group);

        Assert.assertEquals(snap, lastSnap);

        logManager.saveSnap(group, snap2);
        lastSnap = logManager.getLastSnap(group);

        Assert.assertEquals(snap2, lastSnap);
        Assert.assertNotEquals(snap, lastSnap);
    }

}