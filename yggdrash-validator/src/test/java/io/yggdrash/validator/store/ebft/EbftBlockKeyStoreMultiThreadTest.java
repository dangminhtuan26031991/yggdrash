package io.yggdrash.validator.store.ebft;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import io.yggdrash.StoreTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.util.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.yggdrash.common.config.Constants.EMPTY_BYTE32;
import static org.junit.Assert.assertEquals;

@RunWith(ConcurrentTestRunner.class)
public class EbftBlockKeyStoreMultiThreadTest {
    private static final Logger log = LoggerFactory.getLogger(EbftBlockKeyStoreMultiThreadTest.class);

    private LevelDbDataSource ds;
    private EbftBlockKeyStore blockKeyStore;

    private Wallet wallet0;
    private Wallet wallet1;
    private Wallet wallet2;
    private Wallet wallet3;

    private EbftBlock ebftBlock;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        StoreTestUtils.clearTestDb();

        wallet0 = new Wallet(null, "/tmp/",
                "test0" + TimeUtils.time(), "Password1234!");
        wallet1 = new Wallet(null, "/tmp/",
                "test1" + TimeUtils.time(), "Password1234!");
        wallet2 = new Wallet(null, "/tmp/",
                "test2" + TimeUtils.time(), "Password1234!");
        wallet3 = new Wallet(null, "/tmp/",
                "test3" + TimeUtils.time(), "Password1234!");

        this.ds =
                new LevelDbDataSource(StoreTestUtils.getTestPath(), "ebftBlockKeyStoreTest");
        this.blockKeyStore = new EbftBlockKeyStore(ds);

        this.ebftBlock = makeEbftBlock(0L, Constants.EMPTY_BYTE32);

        this.blockKeyStore.put(this.ebftBlock.getIndex(), this.ebftBlock.getHash());
    }

    private Block makeBlock(long index, byte[] prevHash) {
        return new TestUtils(wallet0).sampleBlock(index, prevHash);
    }

    private List<String> makeConsensusList(Block block) {
        List<String> consensusList = new ArrayList<>();
        consensusList.add(wallet0.signHex(block.getHash(), true));
        consensusList.add(wallet1.signHex(block.getHash(), true));
        consensusList.add(wallet2.signHex(block.getHash(), true));
        consensusList.add(wallet3.signHex(block.getHash(), true));
        return consensusList;
    }

    private EbftBlock makeEbftBlock(long index, byte[] prevHash) {
        Block block = makeBlock(index, prevHash);
        return new EbftBlock(block, makeConsensusList(block));
    }

    @Test
    @ThreadCount(8)
    public void putTestMultiThread() {
        long testNumber = 10000;
        for (long l = 0L; l < testNumber; l++) {
            this.blockKeyStore.put(l, EMPTY_BYTE32);
        }
        log.debug("blockKeyStore size= " + this.blockKeyStore.size());
        assertEquals(testNumber, this.blockKeyStore.size());
    }

    @Test
    @ThreadCount(8)
    public void putMutiThreadMemoryTest() throws InterruptedException {
        TestConstants.PerformanceTest.apply();

        System.gc();
        Thread.sleep(20000);

        this.putTestMultiThread();

        System.gc();
        Thread.sleep(3000000);
    }


    @Test
    @ThreadCount(8)
    public void getMutiThreadMemoryTest() throws InterruptedException {
        TestConstants.PerformanceTest.apply();

        System.gc();
        Thread.sleep(20000);

        this.putTestMultiThread();

        System.gc();
        Thread.sleep(10000);

        for (long l = 0L; l < this.blockKeyStore.size(); l++) {
            log.debug("{} {}", l, Hex.toHexString(this.blockKeyStore.get(l)));
        }

        log.debug("blockKeyStore size= " + this.blockKeyStore.size());

        System.gc();
        Thread.sleep(3000000);
    }

    @Test
    @ThreadCount(8)
    public void containsMutiThreadMemoryTest() throws InterruptedException {
        TestConstants.PerformanceTest.apply();

        System.gc();
        Thread.sleep(20000);

        this.putTestMultiThread();

        System.gc();
        Thread.sleep(10000);

        for (long l = 0L; l < this.blockKeyStore.size(); l++) {
            if (this.blockKeyStore.contains(l)) {
                log.debug("{} {}", l, Hex.toHexString(this.blockKeyStore.get(l)));
            }
        }

        log.debug("blockKeyStore size= " + this.blockKeyStore.size());

        System.gc();
        Thread.sleep(3000000);
    }

    @After
    public void tearDown() {
        StoreTestUtils.clearTestDb();
    }

}
