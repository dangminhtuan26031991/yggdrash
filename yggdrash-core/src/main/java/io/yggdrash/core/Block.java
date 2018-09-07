package io.yggdrash.core;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.Proto;
import io.yggdrash.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;

public class Block implements Cloneable {

    private BlockHeader header;
    private byte[] signature;
    private BlockBody body;

    public Block(BlockHeader header, byte[] signature, BlockBody body) {
        this.header = header;
        this.signature = signature;
        this.body = body;
    }

    public Block(BlockHeader header, Wallet wallet, BlockBody body)
            throws IOException {
        this.header = header;
        this.body = body;
        this.signature = wallet.signHashedData(this.header.getHashForSignning());
    }

    public Block(JsonObject jsonObject) {
        this.header = new BlockHeader(jsonObject.get("header").getAsJsonObject());
        this.signature = Hex.decode(jsonObject.get("signature").getAsString());
        this.body = new BlockBody(jsonObject.getAsJsonArray("body"));
    }

    public BlockHeader getHeader() {
        return header;
    }

    public byte[] getSignature() {
        return signature;
    }

    public BlockBody getBody() {
        return body;
    }

    public byte[] getHash() throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        bao.write(this.header.toBinary());
        bao.write(this.signature);

        return HashUtil.sha3(bao.toByteArray());
    }

    public String getHashHexString() throws IOException {
        return org.spongycastle.util.encoders.Hex.toHexString(this.getHash());
    }

    public byte[] getPubKey() throws IOException, SignatureException {
        BlockSignatureHash blockSigWithHash = new BlockSignatureHash(this.signature, this.header.getHashForSignning());
        return blockSigWithHash.getPubKey();
    }

    public String getPubKeyHexString() throws IOException, SignatureException {
        return Hex.toHexString(this.getPubKey());
    }

    public byte[] getAddress() throws IOException, SignatureException {
        return HashUtil.sha3omit12(this.getPubKey());
    }

    public String getAddressHexString() throws IOException, SignatureException {
        return Hex.toHexString(getAddress());
    }

    public long length() throws IOException {
        return this.header.length() + this.signature.length + this.body.length();
    }

    public JsonObject toJsonObject() {

        JsonObject jsonObject = new JsonObject();

        jsonObject.add("header", this.header.toJsonObject());
        jsonObject.addProperty("signature", Hex.toHexString(this.signature));
        jsonObject.add("body", this.body.toJsonArray());

        return jsonObject;
    }

    public String toString() {
        return this.toJsonObject().toString();
    }

    public String toStringPretty() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this.toJsonObject());
    }

    public byte[] toBinary() throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        bao.write(this.header.toBinary());
        bao.write(this.signature);
        bao.write(this.body.toBinary());

        return bao.toByteArray();
    }

    @Override
    public Block clone() throws CloneNotSupportedException {
        Block block = (Block) super.clone();
        block.header = this.header.clone();
        block.signature = this.signature.clone();
        block.body = this.body.clone();

        return block;
    }

    public Proto.Block toProtoBlock() {
        try {
            return this.toProtoBlock(this);
        } catch (UnsupportedEncodingException e) {
            throw new NotValidateException();
        }
    }

    public static Proto.Block toProtoBlock(Block block) throws UnsupportedEncodingException {
        Proto.Block.Header protoHeader;
        protoHeader = Proto.Block.Header.newBuilder()
            .setChain(ByteString.copyFrom(block.getHeader().getChain()))
            .setVersion(ByteString.copyFrom(block.getHeader().getVersion()))
            .setType(ByteString.copyFrom(block.getHeader().getType()))
            .setPrevBlockHash(ByteString.copyFrom(block.getHeader().getPrevBlockHash()))
            .setIndex(ByteString.copyFrom(ByteUtil.longToBytes(block.getHeader().getIndex())))
            .setTimestamp(
                    ByteString.copyFrom(ByteUtil.longToBytes(block.getHeader().getTimestamp())))
            .setMerkleRoot(ByteString.copyFrom(block.getHeader().getMerkleRoot()))
            .setBodyLength(
                    ByteString.copyFrom(ByteUtil.longToBytes(block.getHeader().getBodyLength())))
            .build();

        Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();
        for (Transaction tx : block.getBody().getBody()) {
            builder.addTransactions(Transaction.toProtoTransaction(tx));
        }

        Proto.Block protoBlock = Proto.Block.newBuilder()
                .setHeader(protoHeader)
                .setSignature(ByteString.copyFrom(block.getSignature()))
                .setBody(builder.build())
                .build();

        return protoBlock;
    }

    public static Block toBlock(Proto.Block protoBlock) {

        BlockHeader blockHeader = new BlockHeader(
                protoBlock.getHeader().getChain().toByteArray(),
                protoBlock.getHeader().getVersion().toByteArray(),
                protoBlock.getHeader().getType().toByteArray(),
                protoBlock.getHeader().getPrevBlockHash().toByteArray(),
                ByteUtil.byteArrayToLong(protoBlock.getHeader().getIndex().toByteArray()),
                ByteUtil.byteArrayToLong(protoBlock.getHeader().getTimestamp().toByteArray()),
                protoBlock.getHeader().getMerkleRoot().toByteArray(),
                ByteUtil.byteArrayToLong(protoBlock.getHeader().getBodyLength().toByteArray())
        );

        BlockSignature blockSignature =  new BlockSignature(protoBlock.getSignature().toByteArray());

        List<Transaction> txList = new ArrayList<>();

        for (Proto.Transaction tx : protoBlock.getBody().getTransactionsList()) {
            txList.add(Transaction.toTransaction(tx));
        }

        BlockBody txBody = new BlockBody(txList);

        return new Block(blockHeader, blockSignature.getSignature(), txBody);

    }

}
