/*
 * Copyright 2019 Akashic Foundation
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

package io.yggdrash.core.blockchain.pbft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class PbftBlock {
    private static final Logger log = LoggerFactory.getLogger(PbftBlock.class);

    private final Block block;
    private final PbftMessageSet pbftMessageSet;

    public PbftBlock(Block block, PbftMessageSet pbftMessageSet) {
        this.block = block;
        this.pbftMessageSet = pbftMessageSet;
    }

    public PbftBlock(byte[] bytes) {
        this(JsonUtil.parseJsonObject(new String(bytes, StandardCharsets.UTF_8)));
    }

    public PbftBlock(JsonObject jsonObject) {
        this.block = new Block(jsonObject.get("block").getAsJsonObject());

        JsonElement pbftMessageSetJsonElement = jsonObject.get("pbftMessageSet");
        if (pbftMessageSetJsonElement != null) {
            this.pbftMessageSet = new PbftMessageSet(pbftMessageSetJsonElement.getAsJsonObject());
        } else {
            this.pbftMessageSet = null;
        }
    }

    public PbftBlock(PbftProto.PbftBlock protoBlock) {
        this.block = Block.toBlock(protoBlock.getBlock());
        this.pbftMessageSet = new PbftMessageSet(protoBlock.getPbftMessageSet());
    }

    public byte[] toBinary() {
        return this.toJsonObject().toString().getBytes(StandardCharsets.UTF_8);
    }

    public long getIndex() {
        return this.block.getIndex();
    }

    public byte[] getHash() {
        return this.block.getHash();
    }

    public String getHashHex() {
        return Hex.toHexString(getHash());
    }

    public byte[] getPrevBlockHash() {
        return this.block.getHeader().getPrevBlockHash();
    }

    public Block getBlock() {
        return block;
    }

    public PbftMessageSet getPbftMessageSet() {
        return pbftMessageSet;
    }

    public static boolean verify(PbftBlock block) {
        if (block == null || block.getBlock() == null || block.getPbftMessageSet() == null) {
            return false;
        }

        if (block.getIndex() == 0) {
            return block.getBlock().verify();
        } else {
            return block.getBlock().verify()
                    && PbftMessageSet.verify(block.getPbftMessageSet());
        }

    }

    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("block", this.block.toJsonObject());
        if (this.pbftMessageSet != null) {
            jsonObject.add("pbftMessageSet", this.pbftMessageSet.toJsonObject());
        }
        return jsonObject;
    }

    public static PbftProto.PbftBlock toProto(PbftBlock pbftBlock) {
        Proto.Block protoBlock = pbftBlock.getBlock().toProtoBlock();
        PbftProto.PbftMessageSet protoPbftMessageSet =
                PbftMessageSet.toProto(pbftBlock.getPbftMessageSet());

        PbftProto.PbftBlock.Builder protoPbftBlockBuilder = PbftProto.PbftBlock.newBuilder();
        if (protoBlock != null) {
            protoPbftBlockBuilder.setBlock(protoBlock);
        }
        if (protoPbftMessageSet != null) {
            protoPbftBlockBuilder.setPbftMessageSet(protoPbftMessageSet);
        }

        return protoPbftBlockBuilder.build();
    }

    public static PbftProto.PbftBlockList toProtoList(List<PbftBlock> pbftBlockList) {
        if (pbftBlockList == null) {
            return null;
        }

        PbftProto.PbftBlockList.Builder protoPbftBlockListBuilder =
                PbftProto.PbftBlockList.newBuilder();
        for (PbftBlock pbftBlock : pbftBlockList) {
            protoPbftBlockListBuilder.addPbftBlock(PbftBlock.toProto(pbftBlock));
        }

        return protoPbftBlockListBuilder.build();
    }

    public boolean equals(PbftBlock block) {
        if (block == null) {
            return false;
        }
        return Arrays.equals(this.toBinary(), block.toBinary());
    }

    public void clear() {
        this.block.clear();
        this.pbftMessageSet.clear();
    }

    public PbftBlock clone() {
        return new PbftBlock(this.toJsonObject());
    }
}