/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.core.store;

import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.contract.core.store.ReadWriterStore;

public class TransactionHeightStore implements ReadWriterStore<byte[], byte[]> {
    private final DbSource<byte[], byte[]> db;

    TransactionHeightStore(DbSource<byte[], byte[]> db) {
        this.db = db;
    }

    @Override
    public void put(byte[] key, byte[] value) {

    }

    @Override
    public boolean contains(byte[] key) {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public <V> V get(byte[] key) {
        return null;
    }
}
