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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.common.util.StreamUtil;
import com.ofcoder.klein.storage.facade.config.StorageProp;
import com.ofcoder.klein.storage.facade.exception.StorageException;

/**
 * @author: 释慧利
 */
public class FileSMManager implements SMManager {

    private static final String PATH = Thread.currentThread().getContextClassLoader().getResource("").getPath() + "data";
    private static final String LAST = PATH + File.separator + "last";
    public static final String LAST_SEPARATOR = ":";

    @Override
    public void init(StorageProp op) {
        File file = new File(PATH);
        if (!file.exists()) {
            boolean mkdir = file.mkdir();
        }
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void saveSnap(Snap snap) {
        File file = new File(PATH + File.separator + snap.getCheckpoint());
        if (file.exists()) {
            return;
        }
        FileOutputStream snapOut = null;
        FileOutputStream lastOut = null;
        try {
            lastOut = new FileOutputStream(LAST);
            snapOut = new FileOutputStream(file);
            IOUtils.write(Hessian2Util.serialize(snap), snapOut);
            IOUtils.write(Hessian2Util.serialize(snap.getCheckpoint() + LAST_SEPARATOR + file.getPath()), lastOut);
        } catch (IOException e) {
            throw new StorageException("save snap, " + e.getMessage(), e);
        } finally {
            StreamUtil.close(snapOut);
            StreamUtil.close(lastOut);
        }
    }

    @Override
    public Snap getLastSnap() {
        File file = new File(LAST);
        if (file.exists()) {
            return null;
        }

        FileInputStream lastIn = null;
        FileInputStream snapIn = null;
        try {
            lastIn = new FileInputStream(file);
            String deserialize = Hessian2Util.deserialize(IOUtils.toByteArray(lastIn));
            String[] split = StringUtils.split(deserialize, LAST_SEPARATOR);
            if (split.length != 2) {
                throw new StorageException("last file content is incorrect, content: " + deserialize);
            }
            snapIn = new FileInputStream(split[1]);
            return Hessian2Util.deserialize(IOUtils.toByteArray(snapIn));
        } catch (IOException e) {
            throw new StorageException("get last snap, " + e.getMessage(), e);
        } finally {
            StreamUtil.close(lastIn);
            StreamUtil.close(snapIn);
        }
    }
}
