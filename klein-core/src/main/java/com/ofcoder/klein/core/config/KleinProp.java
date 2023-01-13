/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ofcoder.klein.core.config;

import java.io.FileInputStream;
import java.util.Properties;

import com.ofcoder.klein.common.exception.KleinException;
import com.ofcoder.klein.common.util.SystemPropertyUtil;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.storage.facade.config.StorageProp;

/**
 * Klein Prop.
 *
 * @author 释慧利
 */
public class KleinProp {
    private String id = SystemPropertyUtil.get("klein.id", "1");
    private int port = SystemPropertyUtil.getInt("klein.port", 1218);
    private String ip = SystemPropertyUtil.get("klein.ip", "127.0.0.1");
    private String storage = SystemPropertyUtil.get("klein.storage", "jvm");
    private String consensus = SystemPropertyUtil.get("klein.consensus", "paxos");
    private String rpc = SystemPropertyUtil.get("klein.rpc", "grpc");
    private ConsensusProp consensusProp = new ConsensusProp();
    private StorageProp storageProp = new StorageProp();
    private RpcProp rpcProp = new RpcProp();
    private CacheProp cacheProp = new CacheProp();

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(final String ip) {
        this.ip = ip;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(final String storage) {
        this.storage = storage;
    }

    public String getConsensus() {
        return consensus;
    }

    public void setConsensus(final String consensus) {
        this.consensus = consensus;
    }

    public ConsensusProp getConsensusProp() {
        return consensusProp;
    }

    public void setConsensusProp(final ConsensusProp consensusProp) {
        this.consensusProp = consensusProp;
    }

    public String getRpc() {
        return rpc;
    }

    public void setRpc(final String rpc) {
        this.rpc = rpc;
    }

    public StorageProp getStorageProp() {
        return storageProp;
    }

    public void setStorageProp(final StorageProp storageProp) {
        this.storageProp = storageProp;
    }

    public RpcProp getRpcProp() {
        return rpcProp;
    }

    public void setRpcProp(final RpcProp rpcProp) {
        this.rpcProp = rpcProp;
    }

    public CacheProp getCacheProp() {
        return cacheProp;
    }

    public void setCacheProp(final CacheProp cacheProp) {
        this.cacheProp = cacheProp;
    }

    /**
     * load KleinProp from file.
     *
     * @param file file path
     * @return KleinProp
     */
    public static KleinProp loadFromFile(final String file) {
        try (FileInputStream fin = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fin);
            System.setProperties(props);
        } catch (Exception e) {
            throw new KleinException(e.getMessage(), e);
        }
        return loadIfPresent();
    }

    /**
     * loadIfPresent.
     *
     * @return KleinProp
     */
    public static KleinProp loadIfPresent() {
        return KleinPropHolder.INSTANCE;
    }

    private static class KleinPropHolder {
        private static final KleinProp INSTANCE = new KleinProp();
    }
}
