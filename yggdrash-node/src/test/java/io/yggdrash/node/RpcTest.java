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

package io.yggdrash.node;

import io.grpc.ManagedChannel;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.p2p.BlockChainHandler;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.node.service.PeerHandlerProvider;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class RpcTest extends TcpNodeTesting {
    private static final Logger log = LoggerFactory.getLogger(RpcTest.class);
    private static final int NODE_CNT = 2;

    private List<ConsensusBlock> blockList;
    private List<Transaction> txList;
    private BlockChainHandler handler;

    @Override
    public void setUp() {
        super.setUp();
        TestConstants.SlowTest.apply();

        bootstrapNodes(NODE_CNT, true);

        // Peer to send rpc msg
        Peer peer = nodeList.get(1).peerTableGroup.getOwner();
        ManagedChannel channel = createChannel(peer);
        handler = new PeerHandlerProvider.PbftPeerHandler(channel, peer);

        setBlockList();
        setTxList();

        log.debug("{} nodes bootstrapped", NODE_CNT);
        log.debug("BlockList and TxList are set: size of BlockList={}, TxList={}",
                blockList.size(), txList.size());
    }

    private void setBlockList() {
        blockList = new ArrayList<>();
        createBlockList(BlockChainTestUtils.createNextBlock());
    }

    private void createBlockList(ConsensusBlock block) {
        while (blockList.size() < 10) {
            blockList.add(block);
            createBlockList(BlockChainTestUtils.createNextBlock(block));
        }
    }

    private void setSpecificBlockHeightOfBlockChain(BlockChain branch) {
        log.debug("*** Set specific block height of blockChain ***");
        BlockChainTestUtils.setBlockHeightOfBlockChain(branch, 10);
    }

    private void setTxList() {
        txList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            txList.add(BlockChainTestUtils.createTransferTx());
        }
    }

    private void addDummyTx() {
        log.debug("*** Add dummy txs ***");
        BlockChain branch = nodeList.get(1).getDefaultBranch();

        for (Transaction tx : txList) {
            branch.addTransaction(tx);
        }
    }

    @Test
    public void syncTxTest() throws Exception {
        addDummyTx();

        BlockChain branch = nodeList.get(1).getDefaultBranch();

        List<Transaction> foundTxList = (List<Transaction>) handler.syncTx(branch.getBranchId()).get();
        for (Transaction tx : foundTxList) {
            Assert.assertTrue(txList.contains(tx));
        }
        Assert.assertEquals(foundTxList.size(), txList.size());
    }

    @Test
    public void syncBlockTest() throws Exception {
        BlockChain branch = nodeList.get(1).getDefaultBranch();
        setSpecificBlockHeightOfBlockChain(branch);

        Future<List<ConsensusBlock>> futureBlockList = handler.syncBlock(branch.getBranchId(), 5);

        List<ConsensusBlock> blockList = futureBlockList.get();
        for (ConsensusBlock block : blockList) {
            Assert.assertEquals(branch.getBlockChainManager().getBlockByIndex(block.getIndex()), block);
        }
    }

    @Test
    public void broadcastBlockTest() {
        for (ConsensusBlock block : blockList) {
            handler.broadcastBlock(block);
        }
        handler.stop();
    }

    @Test
    public void broadcastTxTest() {
        for (Transaction tx : getTmpTxList()) {
            handler.broadcastTx(tx);
        }
        handler.stop();
    }

    private List<Transaction> getTmpTxList() {
        List<Transaction> txList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            txList.add(BlockChainTestUtils.createTransferTx());
        }
        return txList;
    }
}
