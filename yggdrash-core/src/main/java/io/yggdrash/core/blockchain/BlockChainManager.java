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

package io.yggdrash.core.blockchain;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.core.consensus.ConsensusBlock;

import java.util.Collection;
import java.util.List;

public interface BlockChainManager<T> {

    void initGenesis(Block genesisBlock);

    int verify(ConsensusBlock<T> block);

    int verify(Transaction transaction);

    ConsensusBlock<T> addBlock(ConsensusBlock<T> nextBlock);

    void addTransaction(Transaction tx);

    void updateTxCache(Block block);

    void setLastConfirmedBlock(ConsensusBlock<T> block);

    ConsensusBlock<T> getLastConfirmedBlock();

    ConsensusBlock<T> getBlockByHash(Sha3Hash hash);

    ConsensusBlock<T> getBlockByIndex(long index);

    Transaction getTxByHash(Sha3Hash hash);

    Collection<Transaction> getRecentTxs();

    List<Transaction> getUnconfirmedTxs();

    List<Transaction> getUnconfirmedTxsWithLimit(long limit);

    TransactionReceipt getTransactionReceipt(String txId);

    Sha3Hash getLastHash();

    long getLastIndex();

    long countOfTxs();

    long countOfBlocks();

    boolean containsBlockHash(Sha3Hash blockHash);

    boolean containsTxHash(Sha3Hash txHash);

    boolean contains(Block block);

    boolean contains(Transaction transaction);

    void close();
}
