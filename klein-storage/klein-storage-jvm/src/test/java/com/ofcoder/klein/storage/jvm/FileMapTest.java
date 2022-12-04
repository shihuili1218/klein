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
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.storage.facade.CacheManager;

import junit.framework.TestCase;

/**
 * @author 释慧利
 */
public class FileMapTest extends TestCase {

    public void testFileMap() {

        DB db = DBMaker.fileDB("jvm").closeOnJvmShutdown().make();

        ConcurrentMap<String, CacheManager.MateData> map =db.hashMap("jvm", Serializer.STRING, new Serializer<CacheManager.MateData>() {
            @Override
            public void serialize(@NotNull DataOutput2 out, @NotNull CacheManager.MateData value) throws IOException {
                out.write(Hessian2Util.serialize(value));
            }

            @Override
            public CacheManager.MateData deserialize(@NotNull DataInput2 input, int available) throws IOException {
                return Hessian2Util.deserialize(input.internalByteArray());
            }
        }).make();
        CacheManager.MateData zzz = new CacheManager.MateData(-1, "zzz");
        map.put("hello", zzz);
        CacheManager.MateData hello = map.get("hello");
        Assert.assertNotNull(hello);
        Assert.assertEquals(hello.getExpire(), zzz.getExpire());
        Assert.assertEquals(hello.getData(), zzz.getData());
    }
}
