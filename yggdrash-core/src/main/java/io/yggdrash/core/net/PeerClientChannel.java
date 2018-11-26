package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.proto.NodeInfo;
import io.yggdrash.proto.Pong;
import io.yggdrash.proto.Proto;

import java.util.List;

public interface PeerClientChannel {
    List<NodeInfo> findPeers(BranchId branchId, Peer peer);

    void broadcastConsensus(BranchId branchId, Peer peer);

    Peer getPeer();

    void stop();

    Pong ping(String message);

    List<Proto.Block> syncBlock(BranchId branchId, long offset);

    List<Proto.Transaction> syncTransaction(BranchId branchId);

    void broadcastTransaction(Proto.Transaction[] txs);

    void broadcastBlock(Proto.Block[] blocks);
}
