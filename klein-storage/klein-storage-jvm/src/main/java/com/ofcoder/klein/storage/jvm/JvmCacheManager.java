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
package com.ofcoder.klein.storage.jvm;/**
 * @author far.liu
 */

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.NotNull;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.spi.Join;
import com.ofcoder.klein.storage.facade.CacheManager;
import com.ofcoder.klein.storage.facade.config.StorageProp;

/**
 * @author 释慧利
 */
@Join
public class JvmCacheManager implements CacheManager {
    public static final String DB_NAME = "jvm-cache";
    private ConcurrentMap<String, MateData> db;

    @Override
    public void init(StorageProp op) {
        DB db = DBMaker.fileDB(DB_NAME).closeOnJvmShutdown().make();
        this.db = db.hashMap(DB_NAME, Serializer.STRING, new Serializer<MateData>() {
            @Override
            public void serialize(@NotNull DataOutput2 out, @NotNull MateData value) throws IOException {
                out.write(Hessian2Util.serialize(value));
            }

            @Override
            public MateData deserialize(@NotNull DataInput2 input, int available) throws IOException {
                return Hessian2Util.deserialize(input.internalByteArray());
            }
        }).make();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public boolean exist(String key) {
        return db.containsKey(key);
    }

    @Override
    public void put(String key, MateData data) {
        db.put(key, data);
    }

    @Override
    public MateData get(String key) {
        if (db.containsKey(key)) {
            return db.get(key);
        }
        return null;
    }

    @Override
    public void remove(String key) {
        db.remove(key);
    }

    @Override
    public void clear() {
        db.clear();
    }

    @Override
    public MateData putIfAbsent(String key, MateData data) {
        return db.putIfAbsent(key, data);
    }

    @Override
    public Object makeImage() {
        return db;
    }

    @Override
    public void loadImage(Object image) {
        if (!(image instanceof ConcurrentMap)) {
            return;
        }
        db.clear();
        db.putAll((Map<String, MateData>) image);
    }

}
