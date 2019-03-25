package io.yggdrash.validator.store.pbft;

import io.yggdrash.common.config.Constants;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.validator.data.pbft.PbftBlock;
import io.yggdrash.validator.store.BlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public class PbftBlockStore implements BlockStore<byte[], PbftBlock> {
    private static final Logger log = LoggerFactory.getLogger(PbftBlockStore.class);

    private final DbSource<byte[], byte[]> db;
    private long size = 0;

    private final ReentrantLock lock = new ReentrantLock();

    public PbftBlockStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }

    @Override
    public void put(byte[] key, PbftBlock value) {
        if (key == null || value == null) {
            log.debug("Key or value are not vaild.");
            return;
        }

        byte[] valueBin = value.toBinary();
        if (valueBin.length > Constants.MAX_MEMORY) {
            log.error("Block size is not valid.");
            log.error("put "
                    + "(key: " + Hex.toHexString(key) + ")"
                    + "(value length: " + valueBin.length + ")");
            return;
        }

        lock.lock();

        try {
            if (!contains(key)) {
                log.trace("put "
                        + "(key: " + Arrays.toString(key) + ")"
                        + "(value length: " + valueBin.length + ")");
                db.put(key, valueBin);
                size++;
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public PbftBlock get(byte[] key) {
        if (key == null) {
            log.debug("Key is not vaild.");
            return null;
        }

        log.trace("get " + "(" + Hex.toHexString(key) + ")");
        return new PbftBlock(db.get(key));
    }

    @Override
    public boolean contains(byte[] key) {
        if (key == null) {
            return false;
        }

        return db.get(key) != null;
    }

    @Override
    public long size() {
        return this.size;
    }

    @Override
    public void close() {
        this.db.close();
    }
}