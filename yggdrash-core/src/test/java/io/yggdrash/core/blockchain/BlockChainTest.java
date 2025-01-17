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

package io.yggdrash.core.blockchain;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.StoreTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.TestConstants.CiTest;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.NotValidateException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainTest extends CiTest {

    @After
    public void tearDown() {
        StoreTestUtils.clearDefaultConfigDb();
    }

    @Test
    public void shouldBeGetBlockByHash() {
        BlockChain blockChain = generateTestBlockChain(false);
        BlockChainManager blockChainManager = blockChain.getBlockChainManager();
        ConsensusBlock block = blockChainManager.getLastConfirmedBlock(); // goto Genesis
        long nextIndex = blockChainManager.getLastIndex() + 1;
        ConsensusBlock testBlock = getBlockFixture(nextIndex, block.getHash());
        blockChain.addBlock(testBlock, false);

        assertThat(blockChainManager.getBlockByHash(testBlock.getHash())).isEqualTo(testBlock);
    }

    @Test
    public void shouldBeGetBlockByIndex() {
        BlockChain blockChain = generateTestBlockChain();
        BlockChainManager blockChainManager = blockChain.getBlockChainManager();
        ConsensusBlock block = blockChainManager.getLastConfirmedBlock(); // goto Genesis
        long nextIndex = blockChainManager.getLastIndex() + 1;
        ConsensusBlock testBlock = getBlockFixture(nextIndex, block.getHash());
        blockChain.addBlock(testBlock, false);

        assertThat(blockChainManager.getBlockByIndex(nextIndex)).isEqualTo(testBlock);
    }

    @Test
    public void shouldBeExceptedNotValidateException() {
        BlockChain blockChain = generateTestBlockChain(false);
        Sha3Hash prevHash = new Sha3Hash("9358");
        ConsensusBlock block1 = getBlockFixture(1L, prevHash);
        blockChain.addBlock(block1, false);
        Assert.assertEquals(1, blockChain.getBlockChainManager().countOfBlocks());
        ConsensusBlock block2 = getBlockFixture(2L, prevHash);
        blockChain.addBlock(block2, false);
        Assert.assertEquals(1, blockChain.getBlockChainManager().countOfBlocks());
    }

    @Test
    public void shouldBeLoadedStoredBlocks() {
        BlockChain blockChain1 = generateTestBlockChain(true);
        ConsensusBlock genesisBlock = blockChain1.getGenesisBlock();

        ConsensusBlock testBlock = getBlockFixture(1L, genesisBlock.getHash());
        blockChain1.addBlock(testBlock, false);
        blockChain1.close();

        BlockChain blockChain2 = generateTestBlockChain(true);
        BlockChainManager blockChain2Manager = blockChain2.getBlockChainManager();
        ConsensusBlock foundBlock = blockChain2Manager.getBlockByHash(testBlock.getHash());
        blockChain2.close();
        long nextIndex = blockChain2Manager.getLastIndex() + 1;

        assertThat(nextIndex).isEqualTo(2);
        assertThat(testBlock).isEqualTo(foundBlock);
    }

    @Test
    public void shouldBeStoredGenesisTxs() {
        BlockChain blockChain = generateTestBlockChain(true);
        BlockChainManager blockChainManager = blockChain.getBlockChainManager();
        ConsensusBlock genesis = blockChain.getGenesisBlock();
        List<Transaction> txList = genesis.getBody().getTransactionList();

        for (Transaction tx : txList) {
            assertThat(blockChainManager.getTxByHash(tx.getHash())).isNotNull();
        }

        assertThat(blockChainManager.countOfTxs()).isEqualTo(genesis.getBody().getCount());
        blockChain.close();
    }

    @Test
    public void shouldBeGeneratedAfterLoadedStoredBlocks() {
        BlockChain newDbBlockChain = generateTestBlockChain(true);
        ConsensusBlock genesisBlock = newDbBlockChain.getGenesisBlock();

        ConsensusBlock testBlock = getBlockFixture(1L, genesisBlock.getHash());
        newDbBlockChain.addBlock(testBlock, false);
        assertThat(newDbBlockChain.getBlockChainManager().getLastIndex()).isEqualTo(1);
        newDbBlockChain.close();

        BlockChain loadedDbBlockChain = generateTestBlockChain(true);
        assertThat(loadedDbBlockChain.getBlockChainManager().getLastIndex()).isEqualTo(1);
    }

    @Test
    public void shouldBeCallback() {
        BlockChain blockChain = generateTestBlockChain(false);
        BlockChainManager blockChainManager = blockChain.getBlockChainManager();
        blockChain.addListener(new BranchEventListener() {
            @Override
            public void chainedBlock(ConsensusBlock block) {
                assertThat(block).isNotNull();
            }

            @Override
            public void receivedTransaction(Transaction tx) {
                assertThat(tx).isNotNull();
            }
        });
        ConsensusBlock block = blockChainManager.getLastConfirmedBlock(); // goto Genesis
        long nextIndex = blockChainManager.getLastIndex() + 1;

        ConsensusBlock testBlock = getBlockFixture(nextIndex, block.getHash());
        blockChain.addBlock(testBlock, false);
        blockChain.addTransaction(BlockChainTestUtils.createTransferTx());
    }

    private static BlockChain generateTestBlockChain(boolean isProductionMode) {
        return BlockChainTestUtils.createBlockChain(isProductionMode);
    }

    private BlockChain generateTestBlockChain() {
        BlockChain blockChain = generateTestBlockChain(false);
        ConsensusBlock genesisBlock = blockChain.getGenesisBlock();
        ConsensusBlock block1 = getBlockFixture(1L, genesisBlock.getHash());
        blockChain.addBlock(block1, false);
        ConsensusBlock block2 = getBlockFixture(2L, block1.getHash());
        blockChain.addBlock(block2, false);
        return blockChain;
    }

    private static ConsensusBlock getBlockFixture(Long index, Sha3Hash prevHash) {
        return getBlockFixture(index, prevHash.getBytes());
    }

    private static ConsensusBlock getBlockFixture(Long index, byte[] prevHash) {

        try {
            Block tmpBlock = BlockChainTestUtils.genesisBlock().getBlock();
            BlockHeader tmpBlockHeader = tmpBlock.getHeader();
            BlockBody tmpBlockBody = tmpBlock.getBody();

            BlockHeader newBlockHeader = new BlockHeader(
                    tmpBlockHeader.getChain(),
                    tmpBlockHeader.getVersion(),
                    tmpBlockHeader.getType(),
                    prevHash,
                    index,
                    TimeUtils.time(),
                    tmpBlockBody);

            Block block = new BlockImpl(newBlockHeader, TestConstants.wallet(), tmpBlockBody);
            return new PbftBlockMock(block);
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

}
