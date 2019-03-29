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

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants.LIMIT;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.store.OutputStore;
import io.yggdrash.contract.core.store.OutputType;
import io.yggdrash.core.blockchain.osgi.ContractContainer;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.runtime.result.BlockRuntimeResult;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.BranchStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockChain {

    private static final Logger log = LoggerFactory.getLogger(BlockChain.class);

    // <Variable>
    private final Branch branch;
    private final BlockHusk genesisBlock;
    private final List<BranchEventListener> listenerList = new ArrayList<>();

    private final BlockStore blockStore;
    private final TransactionStore transactionStore;
    private final BranchStore branchStore;
    private final StateStore stateStore;
    private final TransactionReceiptStore transactionReceiptStore;

    private BlockHusk prevBlock;

    private final ContractContainer contractContainer;
    private final Map<OutputType, OutputStore> outputStores;

    public BlockChain(Branch branch, BlockHusk genesisBlock, BlockStore blockStore,
                      TransactionStore transactionStore, BranchStore branchStore,
                      StateStore stateStore, TransactionReceiptStore transactionReceiptStore,
                      ContractContainer contractContainer, Map<OutputType, OutputStore> outputStores) {
        this.branch = branch;
        this.genesisBlock = genesisBlock;
        this.blockStore = blockStore;
        this.transactionStore = transactionStore;
        this.branchStore = branchStore;
        this.stateStore = stateStore;
        this.transactionReceiptStore = transactionReceiptStore;
        this.contractContainer = contractContainer;
        this.outputStores = outputStores;


        // Check BlockChain is Ready
        PrepareBlockchain prepareBlockchain = new PrepareBlockchain(contractContainer.getContractPath());
        // check block chain is ready
        if (prepareBlockchain.checkBlockChainIsReady(this)) {
            // install bundles
            // bc.getContractContainer()
            ContractContainer container = getContractContainer();
            for (BranchContract contract : prepareBlockchain.getContractList()) {
                File branchContractFile = prepareBlockchain.loadContractFile(contract.getContractVersion());
                container.installContract(contract.getContractVersion(), branchContractFile, contract.isSystem());
            }

            try {
                container.reloadInject();
            } catch (IllegalAccessException e) {
                log.error(e.getMessage());
                // TODO throw Runtiome Exception
            }
        } else {
            // TODO blockchain ready fails
            log.error("Blockchain is not Ready");
        }

        // getGenesis Block by Store
        Sha3Hash blockHash = branchStore.getGenesisBlockHash();
        if (blockHash == null || !blockStore.contains(blockHash)) {
            log.debug("BlockChain init Genesis");
            initGenesis();
        } else {
            log.debug("BlockChain Load in Storage");
            // Load Block Chain Information
            loadTransaction();

            // load contract
        }
    }

    private void initGenesis() {
        for (TransactionHusk tx : genesisBlock.getBody()) {
            if (!transactionStore.contains(tx.getHash())) {
                transactionStore.put(tx.getHash(), tx);
            }
        }
        addBlock(genesisBlock, false);

        // Add Meta Information
        branchStore.setBranch(branch);
        branchStore.setGenesisBlockHash(genesisBlock.getHash());
        // TODO new Validators
        //branchStore.setValidators(branch.getValidators());
        branchStore.setBranchContracts(branch.getBranchContracts());
    }

    private void loadTransaction() {
        // load recent 1000 block
        // Start Block and End Block
        Long bestBlock = branchStore.getBestBlock();
        Long loadStart = bestBlock > 1000 ? bestBlock - 1000 : 0;
        for (long i = loadStart; i <= bestBlock; i++) {
            // recent block load and update Cache
            BlockHusk block = blockStore.getBlockByIndex(i);
            // TODO node can be shutdown before blockStore.addBlock()
            // addBlock(): branchStore.setBestBlock() -> executeTransactions() -> blockStore.addBlock()
            if (block == null) {
                long prevIdx = i - 1;
                branchStore.setBestBlock(blockStore.getBlockByIndex(prevIdx));
                log.warn("reset branchStore bestBlock: {} -> {}", bestBlock, prevIdx);
                break;
            }
            transactionStore.updateCache(block.getBody());
            // set Last Best Block
            if (i == bestBlock) {
                this.prevBlock = block;
            }
        }
    }

    public void addListener(BranchEventListener listener) {
        listenerList.add(listener);
    }

    public StateStore getStateStore() {
        return stateStore;
    }

    TransactionReceiptStore getTransactionReceiptStore() {
        return transactionReceiptStore;
    }

    TransactionReceipt getTransactionReceipt(String txId) {
        return transactionReceiptStore.get(txId);
    }

    void generateBlock(Wallet wallet) {
        List<TransactionHusk> txs = transactionStore.getUnconfirmedTxsWithLimit(LIMIT.BLOCK_SYNC_SIZE);
        BlockHusk block = new BlockHusk(wallet, txs, getPrevBlock());
        addBlock(block, true);
    }

    Collection<TransactionHusk> getRecentTxs() {
        return transactionStore.getRecentTxs();
    }

    List<TransactionHusk> getUnconfirmedTxs() {
        return new ArrayList<>(transactionStore.getUnconfirmedTxs());
    }

    long countOfTxs() {
        return transactionStore.countOfTxs();
    }

    public Branch getBranch() {
        return branch;
    }

    public BranchId getBranchId() {
        return branch.getBranchId();
    }

    public BlockHusk getGenesisBlock() {
        return this.genesisBlock;
    }

    BlockHusk getPrevBlock() {
        return this.prevBlock;
    }

    public ContractContainer getContractContainer() {
        return contractContainer;
    }

    /**
     * Gets last block index.
     *
     * @return the last block index
     */
    public long getLastIndex() {
        if (isGenesisBlockChain()) {
            return 0;
        }
        return prevBlock.getIndex();
    }

    /**
     * Add block.
     *
     * @param nextBlock the next block
     * @throws NotValidateException the not validate exception
     */
    public BlockHusk addBlock(BlockHusk nextBlock, boolean broadcast) {
        if (blockStore.contains(nextBlock.getHash())) {
            return null;
        }
        if (!isValidNewBlock(prevBlock, nextBlock)) {
            String msg = String.format("Invalid to chain cur=%s, new=%s", prevBlock.getIndex(), nextBlock.getIndex());
            throw new NotValidateException(msg);
        }
        // add best Block
        branchStore.setBestBlock(nextBlock);

        // run Block Transactions
        // TODO run block execute move to other process (or thread)
        // TODO last execute block will invoke
        if (nextBlock.getIndex() > branchStore.getLastExecuteBlockIndex()) {
            //BlockRuntimeResult result = runtime.invokeBlock(nextBlock);
            BlockRuntimeResult result = contractContainer.getContractManager().executeTransactions(nextBlock);
            // Save Result
            contractContainer.getContractManager().commitBlockResult(result);
            //runtime.commitBlockResult(result);
            branchStore.setLastExecuteBlock(nextBlock);

            //Store event
            if (outputStores != null && outputStores.size() > 0) {
                Map<String, JsonObject> transactionMap = new HashMap<>();
                nextBlock.getBody().forEach(tx -> {
                    String txHash = tx.getHash().toString();
                    transactionMap.put(txHash, tx.toJsonObjectFromProto());
                });

                outputStores.forEach((storeType, store) -> {
                    store.put(nextBlock.toJsonObjectByProto());
                    store.put(nextBlock.getHash().toString(), transactionMap);
                });
            }
        }

        // Store Block Index and Block Data
        this.blockStore.addBlock(nextBlock);

        this.prevBlock = nextBlock;
        batchTxs(nextBlock);
        if (!listenerList.isEmpty() && broadcast) {
            listenerList.forEach(listener -> listener.chainedBlock(nextBlock));
        }
        log.debug("Added idx=[{}], tx={}, branch={}, blockHash={}", nextBlock.getIndex(),
                nextBlock.getBodyCount(), getBranchId(), nextBlock.getHash());
        return nextBlock;
    }

    private boolean isValidNewBlock(BlockHusk prevBlock, BlockHusk nextBlock) {
        if (prevBlock == null) {
            return true;
        }

        if (prevBlock.getIndex() + 1 != nextBlock.getIndex()) {
            log.warn("invalid index: prev:{} / new:{}", prevBlock.getIndex(), nextBlock.getIndex());
            return false;
        } else if (!prevBlock.getHash().equals(nextBlock.getPrevHash())) {
            log.warn("invalid previous hash= {} {}", prevBlock.getHash(), nextBlock.getPrevHash());
            return false;
        }

        return true;
    }

    public TransactionHusk addTransaction(TransactionHusk tx) {
        return addTransaction(tx, true);
    }

    public TransactionHusk addTransaction(TransactionHusk tx, boolean broadcast) {
        if (transactionStore.contains(tx.getHash())) {
            return null;
        } else if (!tx.verify()) {
            throw new InvalidSignatureException();
        }

        try {
            transactionStore.put(tx.getHash(), tx);
            if (!listenerList.isEmpty() && broadcast) {
                listenerList.forEach(listener -> listener.receivedTransaction(tx));
            }
            return tx;
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }

    public long transactionCount() {
        return blockStore.getBlockchainTransactionSize();
    }

    /**
     * Is valid chain boolean.
     *
     * @return the boolean
     */
    boolean isValidChain() {
        return isValidChain(this);
    }

    /**
     * Is valid chain boolean.
     *
     * @param blockChain the block chain
     * @return the boolean
     */
    private boolean isValidChain(BlockChain blockChain) {
        if (blockChain.getPrevBlock() != null) {
            BlockHusk block = blockChain.getPrevBlock(); // Get Last Block
            while (block.getIndex() != 0L) {
                block = blockChain.getBlockByHash(block.getPrevHash());
            }
            return block.getIndex() == 0L;
        }
        return true;
    }

    public BlockHusk getBlockByIndex(long idx) {
        return blockStore.getBlockByIndex(idx);
    }

    /**
     * Gets block by hash.
     *
     * @param hash the hash
     * @return the block by hash
     */
    public BlockHusk getBlockByHash(String hash) {
        return getBlockByHash(new Sha3Hash(hash));
    }

    /**
     * Gets block by hash.
     *
     * @param hash the hash
     * @return the block by hash
     */
    public BlockHusk getBlockByHash(Sha3Hash hash) {
        return blockStore.get(hash);
    }

    /**
     * Gets transaction by hash.
     *
     * @param hash the hash
     * @return the transaction by hash
     */
    TransactionHusk getTxByHash(Sha3Hash hash) {
        return transactionStore.get(hash);
    }

    /**
     * Is genesis block chain boolean.
     *
     * @return the boolean
     */
    private boolean isGenesisBlockChain() {
        return (this.prevBlock == null);
    }


    private void batchTxs(BlockHusk block) {
        if (block == null || block.getBody() == null) {
            return;
        }
        Set<Sha3Hash> keys = new HashSet<>();

        for (TransactionHusk tx : block.getBody()) {
            keys.add(tx.getHash());
        }
        transactionStore.batch(keys);
    }

    public void close() {
        this.blockStore.close();
        this.transactionStore.close();
        this.branchStore.close();
        // TODO refactoring
        this.stateStore.close();
        this.transactionReceiptStore.close();

    }

    List<BranchContract> getBranchContracts() {
        if (this.branchStore.getBranchContacts() == null) {
            return this.getBranch().getBranchContracts();
        } else {
            return this.branchStore.getBranchContacts();
        }

    }

}
