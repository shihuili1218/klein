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
package com.ofcoder.klein.consensus.facade.sm;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMRegistry.
 *
 * @author 释慧利
 */
public final class SMRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(SMRegistry.class);
    private static final Map<String, SM> SMS = new HashMap<>();

    /**
     * load sm.
     *
     * @param group sm group
     * @param sm    sm
     */
    public static void register(final String group, final SM sm) {
        if (SMS.containsKey(group)) {
            LOG.error("the group[{}] has been loaded with sm.", group);
            return;
        }
        SMS.put(group, sm);
    }

    /**
     * get sm.
     *
     * @return all sm
     */
    public static Map<String, SM> getSms() {
        return SMS;
    }
}
