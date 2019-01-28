package io.yggdrash.core.store;

import io.yggdrash.common.util.SerializeUtil;
import io.yggdrash.core.store.datasource.DbSource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NonStateStore implements Store<Object, Object> {
    private final DbSource<byte[], byte[]> db;

    public NonStateStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }


    @Override
    public void put(Object key, Object value) {
        db.put(SerializeUtil.serialize(key), SerializeUtil.serialize(value));
    }

    @Override
    public <V> V get(Object key) {
        Object o = db.get(SerializeUtil.serialize(key));
        if (o != null) {
            return (V) SerializeUtil.deserializeBytes(db.get(SerializeUtil.serialize(key)));
        }
        return null;
    }

    @Override
    public boolean contains(Object key) {
        return db.get(SerializeUtil.serialize(key)) != null;
    }

    @Override
    public void close() {
        db.close();
    }
}
