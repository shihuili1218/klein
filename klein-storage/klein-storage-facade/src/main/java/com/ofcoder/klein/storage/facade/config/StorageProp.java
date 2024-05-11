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
package com.ofcoder.klein.storage.facade.config;

import java.io.File;

import com.ofcoder.klein.common.util.SystemPropertyUtil;

/**
 * Storage Prop.
 *
 * @author far.liu
 */
public class StorageProp {
    private String id = SystemPropertyUtil.get("klein.id", "1");
    private String dataPath = SystemPropertyUtil.get("klein.storage.data-path", SystemPropertyUtil.get("user.dir", "") + File.separator + "data") + File.separator + id;
    private int traceBlockSize = SystemPropertyUtil.getInt("klein.storage.trace-block", 1024);

    /**
     * self id.
     *
     * @return node id
     */
    public String getId() {
        return id;
    }

    /**
     * set self id.
     *
     * @param id node id
     */
    public void setId(final String id) {
        this.id = id;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(final String dataPath) {
        this.dataPath = dataPath;
    }

    public int getTraceBlockSize() {
        return traceBlockSize;
    }

    public void setTraceBlockSize(final int traceBlockSize) {
        this.traceBlockSize = traceBlockSize;
    }
}
