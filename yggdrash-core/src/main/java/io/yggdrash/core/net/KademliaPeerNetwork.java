/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.p2p.BlockChainHandler;
import io.yggdrash.core.p2p.KademliaOptions;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.core.p2p.PeerTableGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class KademliaPeerNetwork implements PeerNetwork {
    private static final Logger log = LoggerFactory.getLogger(KademliaPeerNetwork.class);

    private final BlockingQueue<Transaction> txQueue = new LinkedBlockingQueue<>();
    private final ExecutorService txExecutor = Executors.newSingleThreadExecutor();

    private final BlockingQueue<ConsensusBlock> blockQueue = new LinkedBlockingQueue<>();
    private final ExecutorService blockExecutor = Executors.newSingleThreadExecutor();

    private final PeerTableGroup peerTableGroup;

    private final PeerDialer peerDialer;

    private final Map<BranchId, List<Peer>> validatorMap = new HashMap<>();

    public KademliaPeerNetwork(PeerTableGroup peerTableGroup, PeerDialer peerDialer) {
        this.peerTableGroup = peerTableGroup;
        this.peerDialer = peerDialer;
    }

    @Override
    public void init() {
        log.info("Init node={}", peerTableGroup.getOwner());
        peerTableGroup.selfRefresh();

        for (BranchId branchId : peerTableGroup.getAllBranchId()) {
            List<Peer> closestPeerList =
                    peerTableGroup.getClosestPeers(branchId, peerTableGroup.getOwner(), KademliaOptions.BROADCAST_SIZE);
            for (Peer peer : closestPeerList) {
                peerDialer.healthCheck(branchId, peerTableGroup.getOwner(), peer);
            }
        }
        txExecutor.execute(new TxWorker());
        blockExecutor.execute(new BlockWorker());
    }

    @Override
    public void destroy() {
        peerDialer.destroyAll();
        txExecutor.shutdown();
        blockExecutor.shutdown();
    }

    @Override
    public List<BlockChainHandler> getHandlerList(BranchId branchId) {
        List<Peer> peerList = peerTableGroup.getBroadcastPeerList(branchId);

        if (validatorMap.containsKey(branchId)) {
            peerList = new ArrayList<>(peerList);
            peerList.addAll(validatorMap.get(branchId));
        }

        return peerDialer.getHandlerList(branchId, peerList);
    }

    @Override
    public void receivedTransaction(Transaction tx) {
        try {
            txQueue.put(tx);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    @Override
    public void chainedBlock(ConsensusBlock block) { //TODO AddBlock BP
        try {
            blockQueue.put(block);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    public void addNetwork(BranchId branchId, String consensus) {
        peerTableGroup.createTable(branchId);
        peerDialer.addConsensus(branchId, consensus);
    }

    public void setValidator(BranchId branchId, List<Peer> validatorList) {
        validatorMap.put(branchId, validatorList);
    }

    private class TxWorker implements Runnable {

        public void run() {
            try {
                while (!txExecutor.isTerminated()) {
                    Transaction tx = txQueue.take();
                    broadcastTx(tx);
                }
            } catch (InterruptedException e) {
                txExecutor.shutdown();
                Thread.currentThread().interrupt();
            }
        }

        private void broadcastTx(Transaction tx) {
            List<BlockChainHandler> getHandlerList = getHandlerList(tx.getBranchId());
            for (BlockChainHandler peerHandler : getHandlerList) {
                try {
                    peerHandler.broadcastTx(tx);
                } catch (Exception e) {
                    log.warn("[KademliaPeerNetwork] broadcast {} -> {}, tx ERR: {}",
                            peerTableGroup.getOwner().getPort(),
                            peerHandler.getPeer().getPort(), e.getMessage());
                    peerDialer.removeHandler(peerHandler);
                }
            }
        }
    }

    private class BlockWorker implements Runnable {

        public void run() {
            try {
                while (!blockExecutor.isTerminated()) {
                    ConsensusBlock block = blockQueue.take();
                    broadcastBlock(block);
                }
            } catch (InterruptedException e) {
                blockExecutor.shutdown();
                Thread.currentThread().interrupt();
            }
        }

        private void broadcastBlock(ConsensusBlock block) {
            List<BlockChainHandler> handlerList = getHandlerList(block.getBranchId());
            for (BlockChainHandler peerHandler : handlerList) {
                try {
                    peerHandler.broadcastBlock(block);
                } catch (Exception e) {
                    log.warn("[KademliaPeerNetwork] broadcast block ERR: {}", e.getMessage());
                    peerDialer.removeHandler(peerHandler);
                }
            }
        }
    }
}
