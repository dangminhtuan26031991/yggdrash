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

package io.yggdrash.gateway.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Timestamps;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.proto.Proto;
import org.spongycastle.util.encoders.Hex;

import java.util.List;

public class TransactionDto {

    public String branchId;
    public String version;
    public String type;
    public long timestamp;
    public String bodyHash;
    public long bodyLength;
    public String signature;
    public List body;
    public String author;
    public String txId;

    public static TransactionHusk of(TransactionDto dto) {
        Proto.Transaction.Header header = Proto.Transaction.Header.newBuilder()
                .setChain(ByteString.copyFrom(Hex.decode(dto.branchId)))
                .setVersion(ByteString.copyFrom(Hex.decode(dto.version)))
                .setType(ByteString.copyFrom(Hex.decode(dto.type)))
                .setTimestamp(Timestamps.fromMillis(dto.timestamp))
                .setBodyHash(ByteString.copyFrom(Hex.decode(dto.bodyHash)))
                .setBodyLength(dto.bodyLength)
                .build();

        Proto.Transaction tx = Proto.Transaction.newBuilder()
                .setHeader(header)
                .setSignature(ByteString.copyFrom(Hex.decode(dto.signature)))
                .setBody(ByteString.copyFromUtf8(JsonUtil.convertObjToString(dto.body)))
                .build();
        return new TransactionHusk(tx);
    }

    public static TransactionDto createBy(TransactionHusk tx) {
        TransactionDto transactionDto = new TransactionDto();
        Proto.Transaction.Header header = tx.getInstance().getHeader();

        transactionDto.branchId = Hex.toHexString(header.getChain().toByteArray());
        transactionDto.version = Hex.toHexString(header.getVersion().toByteArray());
        transactionDto.type = Hex.toHexString(header.getType().toByteArray());
        transactionDto.timestamp = Timestamps.toMillis(header.getTimestamp());
        transactionDto.bodyHash = Hex.toHexString(header.getBodyHash().toByteArray());
        transactionDto.bodyLength = header.getBodyLength();
        transactionDto.signature = Hex.toHexString(tx.getInstance().getSignature().toByteArray());
        try {
            transactionDto.body = new ObjectMapper().readValue(tx.getBody(), List.class);
        } catch (Exception e) {
            transactionDto.body = null;
        }
        transactionDto.author = tx.getAddress() == null ? null : tx.getAddress().toString();
        transactionDto.txId = tx.getHash().toString();
        return transactionDto;
    }
}
