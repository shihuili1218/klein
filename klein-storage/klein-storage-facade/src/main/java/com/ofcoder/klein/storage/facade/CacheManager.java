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
package com.ofcoder.klein.storage.facade;

import java.io.Serializable;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.storage.facade.config.StorageProp;

/**
 * @author 释慧利
 */
public interface CacheManager extends Lifecycle<StorageProp> {

    boolean exist(String key);

    void put(String key, MateData data);

    MateData get(String key);

    void remove(String key);

    void clear();

    MateData putIfAbsent(String key, MateData data);

    Object makeImage();

    void loadImage(Object image);

    class MateData implements Serializable {
        private long expire = -1;
        private Serializable data;

        public MateData() {
        }

        public MateData(long expire, Serializable data) {
            this.expire = expire;
            this.data = data;
        }

        public long getExpire() {
            return expire;
        }

        public void setExpire(long expire) {
            this.expire = expire;
        }

        public Serializable getData() {
            return data;
        }

        public void setData(Serializable data) {
            this.data = data;
        }
    }
}
