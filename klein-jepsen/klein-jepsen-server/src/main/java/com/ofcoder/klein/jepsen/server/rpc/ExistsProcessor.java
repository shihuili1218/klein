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
package com.ofcoder.klein.jepsen.server.rpc;

import com.ofcoder.klein.core.cache.KleinCache;
import com.ofcoder.klein.rpc.facade.RpcProcessor;

/**
 * cache exists request processor.
 *
 * @author 释慧利
 */
public class ExistsProcessor implements RpcProcessor<ExistsReq, Boolean> {
    private KleinCache cache;

    public ExistsProcessor(final KleinCache cache) {
        this.cache = cache;
    }

    @Override
    public Boolean handleRequest(final ExistsReq request) {
        return cache.exist(request.getKey());
    }

    @Override
    public String service() {
        return ExistsReq.class.getSimpleName();
    }
}
