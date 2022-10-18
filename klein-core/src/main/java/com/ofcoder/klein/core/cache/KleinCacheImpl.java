package com.ofcoder.klein.core.cache;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import com.ofcoder.klein.consensus.facade.Consensus;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.core.config.KleinProp;
import com.ofcoder.klein.rpc.facade.serialization.Hessian2Util;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.Storage;

/**
 * @author: 释慧利
 */
public class KleinCacheImpl implements KleinCache {
    protected Consensus consensus;
    protected Storage storage;

    public KleinCacheImpl() {
        KleinProp kleinProp = KleinProp.loadIfPresent();
        this.consensus = ExtensionLoader.getExtensionLoader(Consensus.class).getJoin(kleinProp.getConsensus());
        this.storage = ExtensionLoader.getExtensionLoader(Storage.class).getJoin(kleinProp.getStorage());
    }

    @Override
    public boolean exist(String key) {
        Message message = new Message();
        message.setKey(key);
        message.setOp(Message.EXIST);
        Result result = consensus.propose(ByteBuffer.wrap(Hessian2Util.serialize(message)));
        return Result.SUCCESS.equals(result);
    }

    @Override
    public <D extends Serializable> boolean put(String key, D data) {
        Message message = new Message();
        message.setData(data);
        message.setKey(key);
        message.setOp(Message.PUT);
        Result result = consensus.propose(ByteBuffer.wrap(Hessian2Util.serialize(message)));
        return Result.SUCCESS.equals(result);
    }

    @Override
    public <D extends Serializable> boolean put(String key, D data, Long ttl, TimeUnit unit) {
        Message message = new Message();
        message.setData(data);
        message.setKey(key);
        message.setOp(Message.PUT);
        message.setTtl(unit.toMicros(ttl));
        Result result = consensus.propose(ByteBuffer.wrap(Hessian2Util.serialize(message)));
        return Result.SUCCESS.equals(result);
    }

    @Override
    public <D extends Serializable> boolean putIfPresent(String key, D data) {
        Message message = new Message();
        message.setData(data);
        message.setKey(key);
        message.setOp(Message.PUTIFPRESENT);
        Result result = consensus.propose(ByteBuffer.wrap(Hessian2Util.serialize(message)));
        return Result.SUCCESS.equals(result);
    }

    @Override
    public <D extends Serializable> boolean putIfPresent(String key, D data, Long ttl, TimeUnit unit) {
        Message message = new Message();
        message.setData(data);
        message.setKey(key);
        message.setOp(Message.PUTIFPRESENT);
        message.setTtl(unit.toMicros(ttl));
        Result result = consensus.propose(ByteBuffer.wrap(Hessian2Util.serialize(message)));
        return Result.SUCCESS.equals(result);
    }

    // todo
    @Override
    public <D extends Serializable> D get(String key) {
        Message message = new Message();
        message.setKey(key);
        message.setOp(Message.GET);
        Result result = consensus.propose(ByteBuffer.wrap(Hessian2Util.serialize(message)));
        return null;
    }

    @Override
    public void invalidate(String key) {
        Message message = new Message();
        message.setKey(key);
        message.setOp(Message.INVALIDATE);
        Result result = consensus.propose(ByteBuffer.wrap(Hessian2Util.serialize(message)));
    }

    @Override
    public void invalidateAll() {
        Message message = new Message();
        message.setOp(Message.INVALIDATEALL);
        Result result = consensus.propose(ByteBuffer.wrap(Hessian2Util.serialize(message)));
    }
}
