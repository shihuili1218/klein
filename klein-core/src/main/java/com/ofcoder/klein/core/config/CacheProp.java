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

import com.ofcoder.klein.common.util.SystemPropertyUtil;

import java.io.File;

/**
 * Cache Property.
 *
 * @author 释慧利
 */
public class CacheProp {

    private String id = SystemPropertyUtil.get("klein.id", "1");
    private int memorySize = SystemPropertyUtil.getInt("klein.cache.lru.memory-size", 3);
    private String dataPath = SystemPropertyUtil.get("klein.data-path", "/data") + File.separator + "klein-cache.mdb";

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public int getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(final int memorySize) {
        this.memorySize = memorySize;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(final String dataPath) {
        this.dataPath = dataPath;
    }
}
