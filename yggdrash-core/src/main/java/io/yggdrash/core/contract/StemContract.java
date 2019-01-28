package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.CurrencyUtil;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.runtime.annotation.ContractNonStateStore;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.runtime.annotation.ContractStateStore;
import io.yggdrash.core.runtime.annotation.ContractTransactionReceipt;
import io.yggdrash.core.runtime.annotation.Genesis;
import io.yggdrash.core.runtime.annotation.InvokeTransction;
import io.yggdrash.core.store.NonStateStore;
import io.yggdrash.core.store.Store;
import io.yggdrash.core.types.Account;
import io.yggdrash.core.types.GenesisInfo;
import io.yggdrash.core.types.Governance;
import io.yggdrash.core.types.TxPayload;
import io.yggdrash.core.types.enumeration.PrefixKeyEnum;
import io.yggdrash.core.types.tx.TxBonding;
import io.yggdrash.core.types.tx.TxCommon;
import io.yggdrash.core.types.tx.TxDelegating;
import io.yggdrash.core.types.tx.TxRecover;
import io.yggdrash.core.types.tx.TxUnStaking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.yggdrash.common.config.Constants.BRANCH_ID;

public class StemContract implements Contract<JsonObject> {

    private static final Logger log = LoggerFactory.getLogger(StemContract.class);

    private final String branchIdListKey = "BRANCH_ID_LIST";

    @ContractStateStore
    Store<String, JsonObject> state;


    @ContractTransactionReceipt
    TransactionReceipt txReceipt;

    @ContractNonStateStore
    NonStateStore nonStateStore;

    @Genesis
    @InvokeTransction // TODO remove InvokeTransaction
    public TransactionReceipt genesis(JsonObject param) {
        Iterator<String> keys = param.keySet().iterator();
        String key = keys.next();
        GenesisInfo genesisInfo = JsonUtil.generateJsonToClass(param.get(key).toString(), GenesisInfo.class);
        applyGenesis(genesisInfo);
        txReceipt = create(param);
        log.info("[StemContract | genesis] SUCCESS! param => " + param);

        return txReceipt;
    }

    /**
     * Returns the id of a registered branch
     *
     * @param params branch   : The branch.json to register on the stem
     */
    @InvokeTransction
    public TransactionReceipt create(JsonObject params) {
        txReceipt.addLog(params);
        for (Map.Entry<String, JsonElement> entry : params.entrySet()) {
            BranchId branchId = BranchId.of(entry.getKey());
            JsonObject json = entry.getValue().getAsJsonObject();

            StemContractStateValue stateValue;
            try {
                stateValue = StemContractStateValue.of(json);
            } catch (Exception e) {
                log.warn("Failed to convert Branch = {}", json);
                continue;
            }
            if (!isBranchExist(branchId.toString()) && isBranchIdValid(branchId, stateValue)) {
                try {
                    stateValue.init();
                    // Branch ID 추가부터
                    addBranchId(branchId);
                    state.put(branchId.toString(), stateValue.getJson());
                    txReceipt.setStatus(ExecuteStatus.SUCCESS);
                } catch (Exception e) {
                    e.printStackTrace();
                    txReceipt.setStatus(ExecuteStatus.FALSE);
                }

                log.info("[StemContract | create] branchId => " + branchId);
                log.info("[StemContract | create] branch => " + json);
            }
        }
        return txReceipt;
    }

    //todo temp code (will change after complete state store)
    private void applyGenesis(GenesisInfo genesisInfo) {
        if (nonStateStore.get(PrefixKeyEnum.GENESIS.toValue()) != null) {
            return;
        }
        if (genesisInfo.getGenesis().getAlloc() == null) {
            genesisInfo.getGenesis().setAlloc(new HashMap<>());

            GenesisInfo.GenesisField.Alloc a1 = new GenesisInfo.GenesisField.Alloc();
            a1.balance = CurrencyUtil.generateCELL(CurrencyUtil.CurrencyType.YEEDType, new BigInteger("10"));

            GenesisInfo.GenesisField.Alloc a2 = new GenesisInfo.GenesisField.Alloc();
            a2.balance = CurrencyUtil.generateCELL(CurrencyUtil.CurrencyType.YEEDType, new BigInteger("5"));

            GenesisInfo.GenesisField.Alloc a3 = new GenesisInfo.GenesisField.Alloc();
            a3.balance = CurrencyUtil.generateCELL(CurrencyUtil.CurrencyType.YEEDType, new BigInteger("1"));

            genesisInfo.getGenesis().getAlloc().put("a2b0f5fce600eb6c595b28d6253bed92be0568ed", a1);
            genesisInfo.getGenesis().getAlloc().put("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94", a2);
            genesisInfo.getGenesis().getAlloc().put("d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95", a3);
        }

        Iterator<String> iter = genesisInfo.getGenesis().getAlloc().keySet().iterator();
        while (iter.hasNext()) {
            String addr = iter.next();
            Account account = new Account();
            account.setBalance(genesisInfo.getGenesis().getAlloc().get(addr).balance);
            nonStateStore.put(PrefixKeyEnum.getAccountKey(addr), account);
        }
        nonStateStore.put(PrefixKeyEnum.GENESIS.toValue(), genesisInfo);

        //Save init governance
        Governance governance = new Governance(30, 4);
        nonStateStore.put(PrefixKeyEnum.GOVERNANCE.toValue(), governance);

    }

    private <T> T convertPayload(Class expectedClass, TxPayload txPayload) {
        if (expectedClass.isAssignableFrom(txPayload.getClass())) {
            return (T) txPayload;
        }
        return null;
    }

    @InvokeTransction
    public TransactionReceipt common(TxPayload txPayload) {
        TxCommon tx = convertPayload(TxCommon.class, txPayload);
        return null;
    }

    @InvokeTransction
    public TransactionReceipt bonding(TxPayload txPayload) {
        Account account = nonStateStore.get(PrefixKeyEnum.getAccountKey(txReceipt.getIssuer()));
        if (account == null) {
            return txReceipt;
        }
        TxBonding tx = convertPayload(TxBonding.class, txPayload);
        System.out.println(txReceipt.getIssuer());
        System.out.println(tx);
        return null;
    }

    @InvokeTransction
    public TransactionReceipt delegating(TxPayload txPayload) {
        TxDelegating tx = convertPayload(TxDelegating.class, txPayload);
        return null;
    }

    @InvokeTransction
    public TransactionReceipt unstaking(TxPayload txPayload) {
        TxUnStaking tx = convertPayload(TxUnStaking.class, txPayload);
        return null;
    }

    @InvokeTransction
    public TransactionReceipt recover(TxPayload txPayload) {
        TxRecover tx = convertPayload(TxRecover.class, txPayload);
        return null;
    }

    /**
     * Returns the id of a updated branch
     *
     * @param params branchId The Id of the branch to update
     *               branch   The branch.json to update on the stem
     */
    @InvokeTransction
    public TransactionReceipt update(JsonObject params) {
        txReceipt.addLog(params);
        for (Map.Entry<String, JsonElement> entry : params.entrySet()) {
            BranchId branchId = BranchId.of(entry.getKey());
            JsonObject json = entry.getValue().getAsJsonObject();

            StemContractStateValue stateValue = getStateValue(branchId.toString());
            if (stateValue != null && isOwnerValid(json.get("owner").getAsString())) {
                updateBranch(stateValue, json);
                state.put(branchId.toString(), stateValue.getJson());
                txReceipt.setStatus(ExecuteStatus.SUCCESS);
                log.info("[StemContract | update] branchId => " + branchId);
                log.info("[StemContract | update] branch => " + stateValue.getJson());
            }
        }
        return txReceipt;
    }

    private void updateBranch(StemContractStateValue stateValue, JsonObject json) {
        if (json.has("tag")) {
            stateValue.setTag(json.get("tag").getAsString());
        }
        if (json.has("description")) {
            stateValue.setDescription(json.get("description").getAsString());
        }
        if (json.has("type")) {
            stateValue.setType(json.get("type").getAsString());
        }
        stateValue.updateContract(json.get("contractId").getAsString());
    }

    /**
     * Returns branch.json as JsonString (query)
     *
     * @param params branchId
     */
    @ContractQuery
    public JsonObject view(JsonObject params) {
        String branchId = params.get(BRANCH_ID).getAsString().toLowerCase();
        if (isBranchExist(branchId)) {
            return getStateValue(branchId).getJson();
        }
        return new JsonObject();
    }

    /**
     * Returns current contract of branch
     *
     * @param params branchId
     */
    @ContractQuery
    public ContractId getcurrentcontract(JsonObject params) {
        String branchId = params.get(BRANCH_ID)
                .getAsString().toLowerCase();
        if (isBranchExist(branchId)) {
            return getStateValue(branchId).getContractId();
        }
        return null;
    }

    /**
     * Returns version history of branch
     *
     * @param params branchId
     */
    @ContractQuery
    public List<ContractId> getcontracthistory(JsonObject params) {
        String branchId = params.get(BRANCH_ID)
                .getAsString().toLowerCase();
        if (isBranchExist(branchId)) {
            return getStateValue(branchId).getContractHistory();
        }
        return Collections.emptyList();
    }

    /**
     * Returns a list contains all branch id
     *
     * @return list of all branch id
     * // TODO REMOVE getAllBranchId
     */
    @ContractQuery
    public Set<String> getallbranchid() {
        JsonObject branchList = state.get(branchIdListKey);
        if (branchList == null) {
            return Collections.emptySet();
        }
        JsonArray branchIds = branchList.getAsJsonArray("branchIds");
        Set<String> branchIdSet = new HashSet<>();
        for (JsonElement branchId : branchIds) {
            branchIdSet.add(branchId.getAsString());
        }
        return branchIdSet;
    }

    private boolean isBranchExist(String branchId) {
        return state.get(branchId) != null;
    }

    // new branchId
    private void addBranchId(BranchId newBranchId) {
        // check branch exist
        if (!isBranchExist(newBranchId.toString())) {
            JsonArray branchIds = new JsonArray();
            for (String branchId : getallbranchid()) {
                branchIds.add(branchId);
            }
            JsonObject obj = new JsonObject();
            branchIds.add(newBranchId.toString());
            obj.add("branchIds", branchIds);
            state.put(branchIdListKey, obj);

        }
    }

    private boolean isOwnerValid(String owner) {
        String sender = this.txReceipt.getIssuer();
        return sender != null && sender.equals(owner);
    }

    private boolean isBranchIdValid(BranchId branchId, Branch branch) {
        return branchId.equals(branch.getBranchId());
    }

    private StemContractStateValue getStateValue(JsonObject param) {
        String branchId = param.get("branchId").getAsString().toLowerCase();
        return getStateValue(branchId);
    }

    private StemContractStateValue getStateValue(String branchId) {
        JsonObject json = state.get(branchId);
        if (json == null) {
            return null;
        } else {
            return new StemContractStateValue(json);
        }
    }


}