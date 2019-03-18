/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.core.net;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.blockchain.BlockChainSyncManager;
import io.yggdrash.core.blockchain.BranchGroup;

public class BlockChainSyncManagerMock {
    private static final BranchGroup branchGroup = BlockChainTestUtils.createBranchGroup();

    public static final BlockChainSyncManager mock = new BlockChainSyncManager(
            NodeStatusMock.mock, PeerNetworkMock.mock, branchGroup);

    public static BlockChainSyncManager getMockWithBranchGroup(BranchGroup branchGroup) {
        return new BlockChainSyncManager(NodeStatusMock.mock, PeerNetworkMock.mock, branchGroup);
    }

    public static BranchGroup getBranchGroup() {
        return branchGroup;
    }
}
