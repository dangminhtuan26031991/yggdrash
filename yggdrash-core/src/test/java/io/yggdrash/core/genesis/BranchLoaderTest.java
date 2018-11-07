/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.genesis;

import io.yggdrash.common.config.DefaultConfig;
import org.junit.Test;

public class BranchLoaderTest {

    @Test
    public void getBranchInfo() {
        BranchLoader loader = new BranchLoader(new DefaultConfig());
        loader.getGenesisBlockList();
    }
}