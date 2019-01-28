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

package io.yggdrash.node.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.types.enumeration.TxPayloadTypeEnum;
import io.yggdrash.core.types.tx.TxBonding;
import io.yggdrash.core.types.tx.TxCommon;
import io.yggdrash.core.types.tx.TxDelegating;
import io.yggdrash.core.types.tx.TxRecover;
import io.yggdrash.core.types.tx.TxUnStaking;
import io.yggdrash.proto.Proto;
import org.spongycastle.util.encoders.Hex;

import java.util.List;
import java.util.Map;

public class TransactionDto {

    public String branchId;
    public String version;
    public String type;
    public long timestamp;
    public String bodyHash;
    public long bodyLength;
    public String signature;
    public TxPayloadTypeEnum payloadType;
    public Map<String, Object> payload;
    public List body;
    public String author;
    public String txId;

    public static TransactionHusk of(TransactionDto dto) {
        Proto.Transaction.Header header = Proto.Transaction.Header.newBuilder()
                .setChain(ByteString.copyFrom(Hex.decode(dto.branchId)))
                .setVersion(ByteString.copyFrom(Hex.decode(dto.version)))
                .setType(ByteString.copyFrom(Hex.decode(dto.type)))
                .setTimestamp(ByteString.copyFrom(ByteUtil.longToBytes(dto.timestamp)))
                .setBodyHash(ByteString.copyFrom(Hex.decode(dto.bodyHash)))
                .setBodyLength(ByteString.copyFrom(ByteUtil.longToBytes(dto.bodyLength)))
                .build();

        Proto.Transaction.Builder txBuilder = Proto.Transaction.newBuilder()
                .setHeader(header)
                .setSignature(ByteString.copyFrom(Hex.decode(dto.signature)))
                .setPayloadType(dto.payloadType.toValue())
                .setBody(ByteString.copyFromUtf8(JsonUtil.convertObjToString(dto.body)));

        String data = JsonUtil.convertObjToString(dto.payload);
        switch (dto.payloadType) {
            case COMMON:
                TxCommon txCommon = JsonUtil.generateJsonToClass(data, TxCommon.class);
                txCommon.mappingClassToProto(txBuilder);
                break;
            case BONDING:
                TxBonding txBonding = JsonUtil.generateJsonToClass(data, TxBonding.class);
                txBonding.mappingClassToProto(txBuilder);
                break;
            case DELEGATING:
                TxDelegating txDelegating = JsonUtil.generateJsonToClass(data, TxDelegating.class);
                txDelegating.mappingClassToProto(txBuilder);
                break;
            case UNSTAKING:
                TxUnStaking txUnStaking = JsonUtil.generateJsonToClass(data, TxUnStaking.class);
                txUnStaking.mappingClassToProto(txBuilder);
                break;
            case RECOVER:
                TxRecover txRecover = JsonUtil.generateJsonToClass(data, TxRecover.class);
                txRecover.mappingClassToProto(txBuilder);
                break;
        }
        return new TransactionHusk(txBuilder.build());
    }

    public static TransactionDto createBy(TransactionHusk tx) {
        TransactionDto transactionDto = new TransactionDto();
        Proto.Transaction.Header header = tx.getInstance().getHeader();

        transactionDto.branchId = Hex.toHexString(header.getChain().toByteArray());
        transactionDto.version = Hex.toHexString(header.getVersion().toByteArray());
        transactionDto.type = Hex.toHexString(header.getType().toByteArray());
        transactionDto.timestamp =
                ByteUtil.byteArrayToLong(header.getTimestamp().toByteArray());
        transactionDto.bodyHash = Hex.toHexString(header.getBodyHash().toByteArray());
        transactionDto.bodyLength =
                ByteUtil.byteArrayToLong(header.getBodyLength().toByteArray());
        transactionDto.signature = Hex.toHexString(tx.getInstance().getSignature().toByteArray());
        try {
            transactionDto.body = new ObjectMapper().readValue(tx.getBody(), List.class);
        } catch (Exception e) {
            transactionDto.body = null;
        }
        transactionDto.author = tx.getAddress().toString();
        transactionDto.txId = tx.getHash().toString();
        return transactionDto;
    }
}
