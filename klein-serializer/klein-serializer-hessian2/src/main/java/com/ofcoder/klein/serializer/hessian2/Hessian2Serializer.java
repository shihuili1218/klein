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
package com.ofcoder.klein.serializer.hessian2;

import com.caucho.hessian.io.Hessian2Input;
import com.ofcoder.klein.common.exception.SerializationException;
import com.ofcoder.klein.serializer.Serializer;
import com.ofcoder.klein.spi.Join;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hessian Serializer.
 *
 * @author hang.li
 */
@Join
public class Hessian2Serializer<T extends Serializable> implements Serializer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Hessian2Serializer.class);

    @Override
    public byte[] serialize(final T t) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             KleinHessian2Output ho = new KleinHessian2Output(baos)) {
            ho.writeObject(t);
            ho.flush();
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new SerializationException(ex.getMessage(), ex);
        }
    }

    @Override
    public T deserialize(final byte[] bytes) {
        T obj = null;
        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
            Hessian2Input input = new Hessian2Input(is);
            obj = (T) input.readObject();
            input.close();
        } catch (IOException e) {
            LOGGER.error("Hessian decode error:{}", e.getMessage(), e);
        }
        return obj;
    }
}
