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
package com.ofcoder.klein.core.cache;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Cache Snap Model.
 *
 * @author 释慧利
 */
public class CacheSnap implements Serializable {
    private Object cache;
    private Map<String, Set<String>> expiryBuckets;

    public CacheSnap(final Object cache, final Map<String, Set<String>> expiryBuckets) {
        this.cache = cache;
        this.expiryBuckets = expiryBuckets;
    }

    public CacheSnap() {
    }

    public Object getCache() {
        return cache;
    }

    public Map<String, Set<String>> getExpiryBuckets() {
        return expiryBuckets;
    }

}
