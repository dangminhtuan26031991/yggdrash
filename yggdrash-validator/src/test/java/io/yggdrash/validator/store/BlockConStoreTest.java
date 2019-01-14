package io.yggdrash.validator.store;

import io.yggdrash.StoreTestUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.BlockCon;
import io.yggdrash.validator.util.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BlockConStoreTest {

    private Wallet wallet;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        wallet = new Wallet();
    }

    @Test
    public void BlockConStoreTest() {
        LevelDbDataSource ds =
                new LevelDbDataSource(StoreTestUtils.getTestPath(), "block-con-store-test");
        BlockConStore blockConStore = new BlockConStore(ds);
        Block block = new TestUtils(wallet).sampleBlock();
        List<String> consensusList = new ArrayList<>();
        consensusList.add(Hex.toHexString(wallet.sign(block.getHash())));

        BlockCon blockCon = new BlockCon(block.getHeader().getIndex(),
                block.getHeader().getPrevBlockHash(), block, consensusList);

        blockConStore.put(blockCon.getHash(), blockCon);
        BlockCon foundBlockCon = blockConStore.get(blockCon.getHash());

        StoreTestUtils.clearTestDb();

        assert(blockCon.equals(foundBlockCon));
    }

}
