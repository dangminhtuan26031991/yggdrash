package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.util.TimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BlockTest {

    private static final Logger log = LoggerFactory.getLogger(BlockTest.class);

    private final byte[] chain = new byte[20];
    private final byte[] version = new byte[8];
    private final byte[] type = new byte[8];
    private final byte[] prevBlockHash = new byte[32];

    private BlockBody blockBody1;

    private BlockHeader blockHeader1;
    private BlockHeader blockHeader2;

    private Wallet wallet;

    private BlockSignature blockSig1;
    private BlockSignature blockSig2;

    private Block block1;
    private Block block2;

    @Before
    public void init() throws Exception {

        TransactionBody txBody;
        TransactionHeader txHeader;
        TransactionSignature txSig;
        Transaction tx1;
        Transaction tx2;

        JsonObject jsonParams1 = new JsonObject();
        jsonParams1.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2000");
        jsonParams1.addProperty("amount", "10000000");

        JsonObject jsonObject1 = new JsonObject();
        jsonObject1.addProperty("method", "transfer");
        jsonObject1.add("params", jsonParams1);

        JsonObject jsonParams2 = new JsonObject();
        jsonParams2.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2001");
        jsonParams2.addProperty("amount", "5000000");

        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("method", "transfer");
        jsonObject2.add("params", jsonParams2);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(jsonObject1);
        jsonArray.add(jsonObject2);

        txBody = new TransactionBody(jsonArray);

        long timestamp = TimeUtils.time();
        txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);

        wallet = new Wallet();

        txSig = new TransactionSignature(wallet, txHeader.getHashForSigning());

        tx1 = new Transaction(txHeader, txSig, txBody);

        tx2 = tx1.clone();

        List<Transaction> txs1 = new ArrayList<>();
        txs1.add(tx1);
        txs1.add(tx2);

        log.debug("txs=" + txs1.toString());

        blockBody1 = new BlockBody(txs1);

        timestamp = TimeUtils.time();
        long index = 0;
        blockHeader1 = new BlockHeader(
                chain, version, type, prevBlockHash, index, timestamp,
                blockBody1.getMerkleRoot(), blockBody1.length());

        log.debug(blockHeader1.toString());

        blockHeader2 = blockHeader1.clone();

        blockSig1 = new BlockSignature(wallet, blockHeader1.getHashForSigning());
        blockSig2 = new BlockSignature(wallet, blockHeader2.getHashForSigning());
    }

    @Test
    public void testBlockConstructor() throws Exception {
        Block block0 = new Block(blockHeader1, blockSig1.getSignature(), blockBody1);
        assertTrue(block0.verify());

        log.debug("block0=" + block0.toJsonObject());
        log.debug("block0=" + block0.toString());
        log.debug("block0=" + block0.toStringPretty());

        block1 = new Block(blockHeader1, wallet, blockBody1);
        assertTrue(block1.verify());

        log.debug("block1=" + block1.toJsonObject());
        log.debug("block1=" + block1.toString());
        log.debug("block1=" + block1.toStringPretty());

        block2 = new Block(
                blockHeader1.clone(), block1.getSignature().clone(), blockBody1.clone());
        assertTrue(block2.verify());

        log.debug("block2=" + block2.toJsonObject());
        log.debug("block2=" + block2.toString());
        log.debug("block2=" + block2.toStringPretty());

        assertEquals(block1.toJsonObject(), block2.toJsonObject());

        Block block3 = new Block(block0.toJsonObject());
        assertTrue(block3.verify());

        assertEquals(block0.toJsonObject().toString(), block3.toJsonObject().toString());
    }

    @Test
    public void testBlockClone() throws Exception {
        Block block1 = new Block(blockHeader1, blockSig1.getSignature(), blockBody1);
        log.debug("block1=" + block1.toJsonObject());

        Block block2 = block1.clone();
        log.debug("block2=" + block2.toJsonObject());

        assertEquals(block1.toJsonObject().toString(), block2.toJsonObject().toString());
    }

    @Test
    public void testBlockField() throws Exception {
        Block block1 = new Block(blockHeader1, blockSig1.getSignature(), blockBody1);
        log.debug("block1=" + block1.toJsonObject());

        Block block2 = block1.clone();
        log.debug("block2=" + block2.toJsonObject());

        assertEquals(block1.toJsonObject().toString(), block2.toJsonObject().toString());
        assertArrayEquals(block1.getSignature(), block2.getSignature());
    }

    @Test
    public void testBlockGetHash() throws Exception {
        Block block1 = new Block(blockHeader1, blockSig1.getSignature(), blockBody1);
        log.debug("block1=" + block1.toJsonObject());

        Block block2 = block1.clone();
        log.debug("block2=" + block2.toJsonObject());

        assertEquals(block1.getHashHexString(), block2.getHashHexString());
    }

    @Test
    public void testBlockKey() throws Exception {
        Block block1 = new Block(blockHeader1, blockSig1.getSignature(), blockBody1);
        log.debug("block1 pubKey=" + block1.getPubKeyHexString());

        Block block2 = block1.clone();
        log.debug("block2 pubKey=" + block2.getPubKeyHexString());

        assertEquals(block1.getPubKeyHexString(), block2.getPubKeyHexString());
        assertArrayEquals(block1.getPubKey(), block2.getPubKey());
        assertArrayEquals(block1.getPubKey(), wallet.getPubicKey());

        log.debug("block1 author address=" + block1.getAddressHexString());
        log.debug("block2 author address=" + block2.getAddressHexString());
        log.debug("wallet address=" + wallet.getHexAddress());
        assertEquals(block1.getAddressHexString(), block2.getAddressHexString());
        assertEquals(block1.getAddressHexString(), wallet.getHexAddress());
        assertTrue(block1.verify());
        assertTrue(block2.verify());
    }

}
