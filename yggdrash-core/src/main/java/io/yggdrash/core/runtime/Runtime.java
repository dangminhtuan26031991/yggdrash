/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.runtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.util.ContractUtils;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.contract.Contract;
import io.yggdrash.core.contract.ExecuteStatus;
import io.yggdrash.core.contract.TransactionReceipt;
import io.yggdrash.core.contract.TransactionReceiptImpl;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.runtime.annotation.Genesis;
import io.yggdrash.core.runtime.annotation.InvokeTransction;
import io.yggdrash.core.store.NonStateStore;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TempStateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Runtime<T> {
    protected static final Logger log = LoggerFactory.getLogger(Runtime.class);

    private StateStore<T> stateStore;
    private NonStateStore nonStateStore;
    private TransactionReceiptStore txReceiptStore;
    private Contract<T> contract;
    private Map<String, Method> invokeMethod;
    private Map<String, Method> queryMethod;
    private Method genesis;
    private Field transactionReceiptField;
    private TempStateStore tmpTxStateStore;
    private TempStateStore tmpBlockStateStore;

    // All block chain has state root
    private byte[] stateRoot;


    // FIX runtime run contract will init
    // TODO Runtime get multi Contract
    public Runtime(Contract<T> contract,
                   StateStore<T> stateStore,
                   NonStateStore nonStateStore,
                   TransactionReceiptStore txReceiptStore) {
        this.stateStore = stateStore;
        this.nonStateStore = nonStateStore;
        this.txReceiptStore = txReceiptStore;
        // init
        queryMethod = new Hashtable<>();
        invokeMethod = new Hashtable<>();

        this.contract = contract;

        // load invoke Method
        invokeMethod = getInvokeMethods();
        queryMethod = getQueryMethods();
        genesis = getGenesisMethod();
        for (Field f : ContractUtils.txReceipt(contract)) {
            transactionReceiptField = f;
            f.setAccessible(true);
        }
        // Block Temp State Store
        tmpBlockStateStore = new TempStateStore(stateStore);
        // Transaction Temp State Store
        tmpTxStateStore = new TempStateStore(tmpBlockStateStore);

        // init state Store
        for (Field f : ContractUtils.stateStore(contract)) {
            try {
                f.setAccessible(true);
                f.set(contract, tmpTxStateStore);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        // init non-state Store
        for (Field f : ContractUtils.nonStateStore(contract)) {
            try {
                f.setAccessible(true);
                f.set(contract, nonStateStore);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }


    public Map<Sha3Hash, Boolean> invokeBlock(BlockHusk block) {
        // Block Data
        // - Hash
        // - BranchId
        // - Index *(Height)
        // Map String

        if (block.getIndex() == 0) {
            // TODO first transaction is genesis
            // TODO genesis method don't call any more
        }
        Map<Sha3Hash, Boolean> result = new HashMap<>();

        for (TransactionHusk tx : block.getBody()) {
            TransactionReceipt txReceipt = new TransactionReceiptImpl(tx);
            // set Block ID
            txReceipt.setBlockId(block.getHash().toString());
            txReceipt.setBlockHeight(block.getIndex());
            txReceipt.setBranchId(block.getBranchId().toString());

            // Transaction invoke here
            // save Tranction Receipt

            TransactionReceipt txResult = invoke(tx, txReceipt);
            if (txResult.isSuccess()) {
                // stateStore revert values
                submitTxState();
            } else {
                // all Change is revert
                tmpTxStateStore.close();
            }
            // print transaction receiptEvent
            if (log.isInfoEnabled()) {
                // transction log print
                log.info("{} Branch {} Block {} Transaction  Status : {} ",
                        txReceipt.getBranchId(),
                        txReceipt.getBlockId(),
                        txReceipt.getTxId(),
                        txReceipt.getStatus()
                );
                for (JsonObject txLog : txReceipt.getTxLog()) {
                    log.info("{} {}", txReceipt.getTxId(), txLog.toString());
                }
            }


            // Save TxReceipt
            txReceiptStore.put(txReceipt);

            result.put(tx.getHash(), txResult.isSuccess());
        }
        submitBlockState();
        // all Transaction run complete

        return result;
    }


    // This invoke is temp run Transaction
    public TransactionReceipt invoke(TransactionHusk tx) {
        TransactionReceipt txReceipt = new TransactionReceiptImpl(tx);

        tmpTxStateStore.close();
        return invoke(tx, txReceipt);
    }


    public TransactionReceipt invoke(TransactionHusk tx, TransactionReceipt txReceipt) {
        // Find invoke method and invoke
        // validation method
        try {

            if (transactionReceiptField != null) {
                // inject Transaction receipt
                transactionReceiptField.set(contract, txReceipt);
            }

            if (tx.getCoreTransaction() != null && tx.getCoreTransaction().getPayloadType() != null) {
                Method method = null;
                switch (tx.getCoreTransaction().getPayloadType()) {
                    case COMMON:
                        method = invokeMethod.get("common");
                        break;
                    case BONDING:
                        method = invokeMethod.get("bonding");
                        break;
                    case DELEGATING:
                        method = invokeMethod.get("delegating");
                        break;
                    case UNSTAKING:
                        method = invokeMethod.get("unstaking");
                        break;
                    case RECOVER:
                        method = invokeMethod.get("recover");
                        break;
                }
                if (method != null) {
                    TransactionReceipt resultReceipt = (TransactionReceipt) method.invoke(contract, tx.getCoreTransaction().getPayload());
                    if (txReceipt.getStatus() != ExecuteStatus.SUCCESS) {
                        txReceipt.setStatus(resultReceipt.getStatus());
                    }
                } else {
                    txReceipt.setStatus(ExecuteStatus.ERROR);
                    JsonObject errorLog = new JsonObject();
                    errorLog.addProperty("error", "method is not exist");
                    txReceipt.addLog(errorLog);
                }
                return txReceipt;
            }
            // transaction is multiple method
            for (JsonElement transactionElement : JsonUtil.parseJsonArray(tx.getBody())) {
                JsonObject txBody = transactionElement.getAsJsonObject();
                String methodName = txBody.get("method").getAsString().toLowerCase();
                Method method = invokeMethod.get(methodName);
                TransactionReceipt resultReceipt = null;
                if (method != null) {
                    if (txBody.has("params")) {
                        JsonObject params = txBody.getAsJsonObject("params");
                        // TODO how to make more simple
                        Optional<Class<?>> m = Arrays.stream(method.getParameterTypes())
                                .filter(p -> p == JsonObject.class).findFirst();
                        if (method.getParameterCount() == 1 && m.isPresent()) {
                            resultReceipt = (TransactionReceipt) method.invoke(contract, params);
                        } else {
                            // TODO fix parameter mapping
                            txReceipt.setStatus(ExecuteStatus.ERROR);
                        }
                    } else {
                        resultReceipt = (TransactionReceipt) method.invoke(contract);
                    }

                } else {
                    txReceipt.setStatus(ExecuteStatus.ERROR);
                    JsonObject errorLog = new JsonObject();
                    errorLog.addProperty("error", "method is not exist");
                    txReceipt.addLog(errorLog);
                    break;
                }
                if (txReceipt.getStatus() != ExecuteStatus.SUCCESS) {
                    txReceipt.setStatus(resultReceipt.getStatus());
                }
                // txReceipt.setTransactionMethod(methodName);
            }

        } catch (Throwable e) {
            txReceipt.setStatus(ExecuteStatus.ERROR);
            JsonObject errorLog = new JsonObject();
            errorLog.addProperty("error", e.getMessage());
            txReceipt.addLog(errorLog);
        }
        return txReceipt;
    }

    public Object query(String method, JsonObject params) throws Exception {
        // Find query method and query
        Method query = queryMethod.get(method.toLowerCase());
        if (query != null) {
            if (params == null) {
                return query.invoke(contract);
            } else {
                return query.invoke(contract, params);
            }

        }
        return null;

    }

    public StateStore<T> getStateStore() {
        return this.stateStore;
    }

    public TransactionReceiptStore getTransactionReceiptStore() {
        return this.txReceiptStore;
    }

    /**
     * Invoke Method filter
     *
     * @return Method map (method nams is lower case)
     */
    private Map<String, Method> getInvokeMethods() {
        return ContractUtils.contractMethods(contract, InvokeTransction.class);
    }

    /**
     * Query Method filter
     *
     * @return Method map (method name is lower case)
     */
    private Map<String, Method> getQueryMethods() {
        return ContractUtils.contractMethods(contract, ContractQuery.class);
    }

    private Method getGenesisMethod() {
        Map<String, Method> genesisMethods = ContractUtils.contractMethods(contract, Genesis.class);
        Map.Entry<String, Method> genesisEntry = genesisMethods.isEmpty()
                ? null : genesisMethods.entrySet().iterator().next();

        if (genesisEntry != null) {
            return genesisEntry.getValue();
        }
        return null;
    }

    private void submitTxState() {
        // TODO calculate transaction state root
        Set<Map.Entry<String, JsonObject>> changeValues = this.tmpTxStateStore.changeValues();
        Iterator<Map.Entry<String, JsonObject>> it = changeValues.iterator();
        while (it.hasNext()) {
            Map.Entry<String, JsonObject> keyValue = it.next();
            tmpBlockStateStore.put(keyValue.getKey(), keyValue.getValue());
        }
        // Submit State and clear all data
        tmpTxStateStore.close();
    }

    private void submitBlockState() {
        // TODO calculate block state root
        Set<Map.Entry<String, JsonObject>> changeValues = this.tmpBlockStateStore.changeValues();
        Iterator<Map.Entry<String, JsonObject>> it = changeValues.iterator();
        while (it.hasNext()) {
            Map.Entry<String, JsonObject> keyValue = it.next();
            stateStore.put(keyValue.getKey(), keyValue.getValue());
        }
        // Submit State and clear all data
        tmpBlockStateStore.close();
    }

}