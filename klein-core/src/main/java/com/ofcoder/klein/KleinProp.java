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
package com.ofcoder.klein;

import com.ofcoder.klein.common.exception.KleinException;
import com.ofcoder.klein.common.util.SystemPropertyUtil;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.storage.facade.config.StorageProp;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * Klein Prop.
 *
 * @author 释慧利
 */
public class KleinProp {
    private String storage = SystemPropertyUtil.get("klein.storage", "file");
    private String consensus = SystemPropertyUtil.get("klein.consensus", "paxos");
    private String rpc = SystemPropertyUtil.get("klein.rpc", "grpc");
    private ConsensusProp consensusProp = new ConsensusProp();
    private StorageProp storageProp = new StorageProp();
    private RpcProp rpcProp = new RpcProp();

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

    /**
     * load KleinProp from file.
     *
     * @param file file path
     * @return KleinProp
     */
    public static KleinProp loadFromFile(final String file) {
        try (FileInputStream fin = new FileInputStream(file)) {
            Properties sys = System.getProperties();
            sys.load(fin);
            System.setProperties(sys);
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
