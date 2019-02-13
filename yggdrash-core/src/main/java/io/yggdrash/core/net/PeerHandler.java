package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;

import java.util.List;

public interface PeerHandler {
    List<Peer> findPeers(Peer requestPeer);

    String ping(String message, Peer peer);

    Peer getPeer();

    void stop();

    List<BlockHusk> syncBlock(BranchId branchId, long offset);

    List<TransactionHusk> syncTransaction(BranchId branchId);

    void broadcastBlock(BlockHusk blockHusk);

    void broadcastTransaction(TransactionHusk txHusk);
}