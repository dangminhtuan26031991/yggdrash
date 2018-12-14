package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;

public interface Contract<T> {
    void init(StateStore<T> store, TransactionReceiptStore txReceiptStore);

    boolean invoke(TransactionHusk tx);

    Object query(JsonObject query) throws Exception;
}
