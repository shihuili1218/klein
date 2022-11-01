package com.ofcoder.klein.core.cache;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.exception.KleinException;
import com.ofcoder.klein.consensus.facade.Consensus;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.core.config.KleinProp;
import com.ofcoder.klein.spi.ExtensionLoader;

/**
 * @author 释慧利
 */
public class KleinCacheImpl implements KleinCache {
    private static final Logger LOG = LoggerFactory.getLogger(KleinCacheImpl.class);
    protected Consensus consensus;

    public KleinCacheImpl() {
        KleinProp kleinProp = KleinProp.loadIfPresent();
        this.consensus = ExtensionLoader.getExtensionLoader(Consensus.class).getJoin(kleinProp.getConsensus());
        this.consensus.loadSM(new CacheSM());
    }

    @Override
    public boolean exist(String key) {
        Message message = new Message();
        message.setKey(key);
        message.setOp(Message.EXIST);
        Result<Boolean> result = consensus.propose(message, true);
        if (!Result.State.SUCCESS.equals(result.getState())) {
            throw new KleinException("The consensus negotiation result is UNKNOWN. In this case, the operation may or may not be completed. You need to retry or query to confirm");
        }
        return result.getData();
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
    public <D extends Serializable> D putIfPresent(String key, D data) {
        Message message = new Message();
        message.setData(data);
        message.setKey(key);
        message.setOp(Message.PUTIFPRESENT);
        Result<D> result = consensus.propose(message, true);
        if (!Result.State.SUCCESS.equals(result.getState())) {
            throw new KleinException("The consensus negotiation result is UNKNOWN. In this case, the operation may or may not be completed. You need to retry or query to confirm. key: " + key);
        }
        return result.getData();
    }

    @Override
    public <D extends Serializable> D putIfPresent(String key, D data, Long ttl, TimeUnit unit) {
        Message message = new Message();
        message.setData(data);
        message.setKey(key);
        message.setOp(Message.PUTIFPRESENT);
        message.setExpire(unit.toMicros(ttl));
        Result<D> result = consensus.propose(message, true);
        if (!Result.State.SUCCESS.equals(result.getState())) {
            throw new KleinException("The consensus negotiation result is UNKNOWN. In this case, the operation may or may not be completed. You need to retry or query to confirm");
        }
        return result.getData();
    }

    // todo
    @Override
    public <D extends Serializable> D get(String key) {
        Message message = new Message();
        message.setKey(key);
        message.setOp(Message.GET);
        Result<D> result = consensus.read(message);
        if (!Result.State.SUCCESS.equals(result.getState())) {
            throw new KleinException("The consensus negotiation result is UNKNOWN. In this case, the operation may or may not be completed. You need to retry or query to confirm");
        }
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
