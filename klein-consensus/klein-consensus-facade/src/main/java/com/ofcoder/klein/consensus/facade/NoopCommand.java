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
package com.ofcoder.klein.consensus.facade;

/**
 * noop command.
 * A no-operation command implementation that represents a null operation in the consensus system.
 * This command is typically used as a placeholder in the consensus protocol when there are no actual
 * commands to process, helping maintain the consensus mechanism's continuity.
 */
public enum NoopCommand implements Command {
    NOOP;

    @Override
    public String getGroup() {
        return "NOOP";
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }
}
