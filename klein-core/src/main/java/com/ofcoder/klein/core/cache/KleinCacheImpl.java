package com.ofcoder.klein.core.cache;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.Consensus;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.core.config.KleinProp;
import com.ofcoder.klein.spi.ExtensionLoader;

/**
 * @author 释慧利
 */
public class KleinCacheImpl implements KleinCache{
    private static final Logger LOG = LoggerFactory.getLogger(KleinCacheImpl.class);
    protected Consensus consensus;
    private static final CacheSM SM = new CacheSM();

    public KleinCacheImpl() {
        KleinProp kleinProp = KleinProp.loadIfPresent();
        this.consensus = ExtensionLoader.getExtensionLoader(Consensus.class).getJoin(kleinProp.getConsensus());
        consensus.loadSM(SM);
    }

    @Override
    public boolean exist(String key) {
        Message message = new Message();
        message.setKey(key);
        message.setOp(Message.EXIST);
        Result result = consensus.propose(message);
        return Result.State.SUCCESS.equals(result.getState());
    }

    @Override
    public <D extends Serializable> boolean put(String key, D data) {
        Message message = new Message();
        message.setData(data);
        message.setKey(key);
        message.setOp(Message.PUT);
        Result result = consensus.propose(message);
        return Result.State.SUCCESS.equals(result.getState());
    }

    @Override
    public <D extends Serializable> boolean put(String key, D data, Long ttl, TimeUnit unit) {
        Message message = new Message();
        message.setData(data);
        message.setKey(key);
        message.setOp(Message.PUT);

        message.setExpire(System.currentTimeMillis() + unit.toMillis(ttl));
        Result result = consensus.propose(message);
        return Result.State.SUCCESS.equals(result.getState());
    }

    @Override
    public <D extends Serializable> boolean putIfPresent(String key, D data) {
        Message message = new Message();
        message.setData(data);
        message.setKey(key);
        message.setOp(Message.PUTIFPRESENT);
        Result result = consensus.propose(message);
        return Result.State.SUCCESS.equals(result.getState());
    }

    @Override
    public <D extends Serializable> boolean putIfPresent(String key, D data, Long ttl, TimeUnit unit) {
        Message message = new Message();
        message.setData(data);
        message.setKey(key);
        message.setOp(Message.PUTIFPRESENT);
        message.setExpire(unit.toMicros(ttl));
        Result result = consensus.propose(message);
        return Result.State.SUCCESS.equals(result.getState());
    }

    // todo
    @Override
    public <D extends Serializable> D get(String key) {
        Message message = new Message();
        message.setKey(key);
        message.setOp(Message.GET);
        Result<D> result = consensus.read(message);
        return result.getData();
    }

    @Override
    public void invalidate(String key) {
        Message message = new Message();
        message.setKey(key);
        message.setOp(Message.INVALIDATE);
        Result result = consensus.propose(message);
    }

    @Override
    public void invalidateAll() {
        Message message = new Message();
        message.setOp(Message.INVALIDATEALL);
        Result result = consensus.propose(message);
    }

}
