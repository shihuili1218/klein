package com.ofcoder.klein.storage.file;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.config.StorageProp;
import com.ofcoder.klein.storage.facade.exception.LockException;

public class FileLogManagerTest {
    LogManager join;

    @Before
    public void setUp() {
        join = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin("jvm");
        join.init(new StorageProp());
    }

    @After
    public void shutdown() {
        join.shutdown();
    }

    @Test(expected = LockException.class)
    public void testUpdateInstance_noLock() {
        Instance<String> instance = new Instance<>();
        instance.setProposalNo(1);
        instance.setInstanceId(1);
        instance.setState(Instance.State.PREPARED);
        instance.setGrantedValue(Lists.newArrayList("Zzz"));
        join.updateInstance(instance);
    }

    @Test()
    public void testGetInstance() {
        Instance nil = join.getInstance(1);
        Assert.assertNull(nil);

        Instance<String> instance = new Instance<>();
        instance.setProposalNo(1);
        instance.setInstanceId(1);
        instance.setState(Instance.State.PREPARED);
        instance.setGrantedValue(Lists.newArrayList("Zzz"));

        join.getLock().writeLock().lock();
        join.updateInstance(instance);
        join.getLock().writeLock().unlock();

        Instance actual = join.getInstance(1);


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

        join.getLock().writeLock().lock();
        join.updateInstance(instance1);
        join.updateInstance(instance2);
        join.getLock().writeLock().unlock();

        List instanceNoConfirm = join.getInstanceNoConfirm();
        Assert.assertNotNull(instanceNoConfirm);
        Assert.assertEquals(1, instanceNoConfirm.size());
        Instance<String> actual = (Instance<String>) instanceNoConfirm.get(0);

        Assert.assertEquals(instance1.getInstanceId(), actual.getInstanceId());
        Assert.assertEquals(instance1.getProposalNo(), actual.getProposalNo());
        Assert.assertEquals(instance1.getState(), actual.getState());
        Assert.assertEquals(instance1.getGrantedValue(), actual.getGrantedValue());
    }

    @Test
    public void testLoadMetaData(){

    }
}