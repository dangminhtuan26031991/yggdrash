/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.store;

import io.yggdrash.TestUtils;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionStoreTest {

    private TransactionStore ts;
    private TransactionHusk tx;

    @Before
    public void setUp() {
        ts = new TransactionStore(new HashMapDbSource());
        assertThat(ts).isNotNull();
        tx = TestUtils.createTransferTxHusk();
    }

    @Test
    public void shouldBeGetLotOfRecentTxs() {
        int countOfTest = 500;
        for(int i = 0; i < countOfTest; i++) {
            tx = TestUtils.createTransferTxHusk();
            ts.put(tx.getHash(), tx);
        }
        assertThat(ts.getUnconfirmedTxs().size()).isEqualTo(countOfTest);
        batch();
        assertThat(ts.getUnconfirmedTxs().size()).isEqualTo(0);
        assertThat(ts.getCountOfTxs()).isEqualTo(countOfTest);
        Map<Sha3Hash, TransactionHusk> recentTxs = ts.getRecentTxs();
        assertThat(recentTxs.size()).isEqualTo(countOfTest);
    }

    @Test
    public void shouldBeGotRecentTxs() {
        ts.put(tx.getHash(), tx);
        batch();
        Collection<TransactionHusk> unconfirmedTxs = ts.getUnconfirmedTxs();
        assertThat(unconfirmedTxs.size()).isEqualTo(0);
        Map<Sha3Hash, TransactionHusk> txs = ts.getRecentTxs();
        assertThat(txs.size()).isEqualTo(1);
    }

    /* 배치가 돌기 전에는 최근 트랜잭션에 들어가지 않고 언컨펌트랜잭션에서만 조회 가능 */
    @Test
    public void shouldNotGetRecentTxsWhenNotBatched() {
        ts.put(tx.getHash(), tx);
        Map<Sha3Hash, TransactionHusk> txs = ts.getRecentTxs();
        assertThat(txs).isEmpty();
        Collection<TransactionHusk> unconfirmedTxs = ts.getUnconfirmedTxs();
        assertThat(unconfirmedTxs.size()).isEqualTo(1);
    }

    @Test
    public void shouldGetFromDb() {
        Sha3Hash key = tx.getHash();
        ts.put(tx.getHash(), tx);
        batch();
        TransactionHusk transactionHusk = ts.get(key);
        assertThat(transactionHusk).isEqualTo(tx);
    }

    @Test
    public void shouldBeBatched() {
        ts.put(tx.getHash(), tx);
        batch();
        assertThat(ts.getUnconfirmedTxs()).isEmpty();
        assertThat(ts.getCountOfTxs()).isEqualTo(1L);
    }

    @Test
    public void shouldBeGotTxFromCache() {
        Sha3Hash key = tx.getHash();
        ts.put(tx.getHash(), tx);
        TransactionHusk foundTx = ts.get(key);
        assertThat(foundTx).isNotNull();
        assertThat(ts.getUnconfirmedTxs()).isNotEmpty();
    }

    private void batch() {
        Set<Sha3Hash> keys = ts.getUnconfirmedTxs().stream().map(TransactionHusk::getHash)
                .collect(Collectors.toSet());
        ts.batch(keys);
    }
}
