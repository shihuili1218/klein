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
package com.ofcoder.klein.common.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.ofcoder.klein.common.exception.SerializationException;
import com.ofcoder.klein.common.util.StreamUtil;

/**
 * Hessian util.
 *
 * @author far.liu
 */
public class Hessian2Util {

    /**
     * serialize object.
     *
     * @param javaBean object
     * @param <T>      java object type
     * @return serialized data
     */
    public static <T> byte[] serialize(final T javaBean) {
        Hessian2Output ho = null;
        ByteArrayOutputStream baos = null;

        try {
            baos = new ByteArrayOutputStream();
            ho = new Hessian2Output(baos);
            ho.writeObject(javaBean);
            ho.flush();
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new SerializationException(ex.getMessage(), ex);
        } finally {
            if (ho != null) {
                try {
                    ho.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            StreamUtil.close(baos);
        }
    }

    /**
     * deserialize data to java object.
     *
     * @param serializeData serialized data
     * @param <T>           java object type
     * @return java object
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(final byte[] serializeData) {
        Hessian2Input hi = null;
        ByteArrayInputStream bais = null;

        try {
            bais = new ByteArrayInputStream(serializeData);
            hi = new Hessian2Input(bais);
            return (T) hi.readObject();
        } catch (Exception ex) {
            throw new SerializationException(ex.getMessage(), ex);
        } finally {
            if (null != hi) {
                try {
                    hi.close();
                } catch (IOException e) {
                    throw new SerializationException(e.getMessage(), e);
                }
            }
            StreamUtil.close(bais);
        }
    }
}
