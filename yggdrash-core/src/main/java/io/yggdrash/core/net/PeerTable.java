/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.net;

import java.util.List;
import java.util.Map;

public interface PeerTable {

    void loadSeedNodes(List<String> seedPeerList);

    void addPeer(Peer peer);

    void copyLiveNode(long minTableTime);

    List<Peer> getClosestPeers(Peer peer, int limit); // getNeighbor

    List<String> getPeerUriList();

    List<Peer> getLatestPeers(long reqTime);

    int getBucketsCount();

    Map<Integer, List<Peer>> getBucketIdAndPeerList(); //for debugging {ynode://pubkey@ip:port}

    List<String> getAllPeerAddressList(); //for debugging {ip:port}

    PeerBucket getBucketByIndex(int index);

    PeerBucket getBucketByPeer(Peer peer);

    Peer pickReplacement(Peer peer);

    void dropPeer(Peer peer);

    // returns the last node in a random, non-empty bucket
    Peer peerToRevalidate();
}
