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
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Address;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransactionHusk implements ProtoHusk<Proto.Transaction>, Comparable<TransactionHusk> {

    private static final Logger log = LoggerFactory.getLogger(TransactionHusk.class);

    private Proto.Transaction protoTransaction;
    private transient Transaction coreTransaction;

    public TransactionHusk(Proto.Transaction transaction) {
        this.protoTransaction = transaction;
        try {
            this.coreTransaction = Transaction.toTransaction(transaction);
        } catch (Exception e) {
            throw new InvalidSignatureException(e);
        }
    }

    public TransactionHusk(Transaction transaction) {
        this.coreTransaction = transaction;
        try {
            this.protoTransaction = Transaction.toProtoTransaction(transaction);
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    public TransactionHusk(byte[] data) {
        try {
            this.protoTransaction = Proto.Transaction.parseFrom(data);
            this.coreTransaction = Transaction.toTransaction(protoTransaction);
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    public TransactionHusk(JsonObject jsonObject) {
        this.coreTransaction = new Transaction(jsonObject);
        this.protoTransaction = Transaction.toProtoTransaction(this.coreTransaction);
    }

    Proto.Transaction getProtoTransaction() {
        return protoTransaction;
    }

    public Transaction getCoreTransaction() {
        return coreTransaction;
    }

    public byte[] getSignature() {
        return this.protoTransaction.getSignature().toByteArray();
    }

    public String getBody() {
        return this.protoTransaction.getBody().toStringUtf8();
    }

    // i.e. txHash/0, txHash/1 ...
    String getPropertyByTag(String tag) {
        JsonObject objOfTxBody = new JsonParser().parse(getBody()).getAsJsonArray().get(0).getAsJsonObject();
        return objOfTxBody.get(tag).getAsString();
    }

    List<String> getPropertiesByTag(String tag) {
        List<String> tags = new ArrayList<>();
        new JsonParser()
                .parse(getBody())
                .getAsJsonArray()
                .forEach(txEle -> tags.add(txEle.getAsJsonObject().get(tag).getAsString()));

        return tags;
    }

    public long getLength() {
        return Constants.TX_HEADER_LENGTH + Constants.TX_SIG_LENGTH + coreTransaction.getHeader().getBodyLength();
    }

    void sign(Wallet wallet) {

        try {
            this.coreTransaction =
                    new Transaction(
                            this.coreTransaction.getHeader(),
                            wallet,
                            this.coreTransaction.getBody());
            this.protoTransaction = Transaction.toProtoTransaction(this.coreTransaction);
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    public Sha3Hash getHash() {
        return new Sha3Hash(this.coreTransaction.getHash(), true);
    }

    public byte[] getHashByte() {
        return this.coreTransaction.getHash();
    }

    public Sha3Hash getHashForSigning() {
        return Sha3Hash.createByHashed(this.coreTransaction.getHeader().getHashForSigning());
    }

    @Override
    public byte[] getData() {
        return this.protoTransaction.toByteArray();
    }

    @Override
    public Proto.Transaction getInstance() {
        return this.protoTransaction;
    }

    @Override
    public String toString() {
        return this.coreTransaction.toString();
    }

    public byte[] toBinary() {
        return this.coreTransaction.toBinary();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransactionHusk that = (TransactionHusk) o;
        return Objects.equals(protoTransaction, that.protoTransaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protoTransaction);
    }

    boolean isSigned() {
        return !this.protoTransaction.getSignature().isEmpty();
    }

    public boolean verify() {
        try {
            return this.coreTransaction.verify();
        } catch (Exception e) {
            log.debug(e.getMessage());
            return false;
        }
    }

    /**
     * Get the address.
     *
     * @return address
     */
    public Address getAddress() {
        try {
            return new Address(this.coreTransaction.getAddress());
        } catch (Exception e) {
            log.debug(e.getMessage());
            return null;
        }
    }

    public BranchId getBranchId() {
        byte[] chain = protoTransaction.getHeader().getChain().toByteArray();
        return new BranchId(Sha3Hash.createByHashed(chain));
    }

    public JsonObject toJsonObject() {
        return this.coreTransaction.toJsonObject();
    }

    JsonObject toJsonObjectFromProto() {
        try {
            String print = JsonFormat.printer()
                    .includingDefaultValueFields().print(this.protoTransaction);
            JsonObject asJsonObject = new JsonParser().parse(print).getAsJsonObject();
            asJsonObject.addProperty("txId", getHash().toString());
            return asJsonObject;
        } catch (InvalidProtocolBufferException e) {
            log.warn(e.getMessage());
        }
        return null;
    }

    @Override
    public int compareTo(TransactionHusk o) {
        return Long.compare(
                ByteUtil.byteArrayToLong(
                        protoTransaction.getHeader().getTimestamp().toByteArray()),
                ByteUtil.byteArrayToLong(
                        o.getInstance().getHeader().getTimestamp().toByteArray()));
    }
}
