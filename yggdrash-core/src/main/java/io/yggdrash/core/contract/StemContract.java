package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.config.Constants.KEY;
import io.yggdrash.common.contract.Contract;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.Genesis;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static io.yggdrash.common.config.Constants.BRANCH_ID;
import static io.yggdrash.common.config.Constants.TX_ID;

public class StemContract implements Contract {
    private static final Logger log = LoggerFactory.getLogger(StemContract.class);
    private static final String BRANCH_ID_LIST = "BRANCH_ID_LIST";

    @ContractStateStore
    ReadWriterStore<String, JsonObject> state;


    @ContractTransactionReceipt
    TransactionReceipt txReceipt;

    @Genesis
    @InvokeTransaction // TODO remove InvokeTransaction
    public TransactionReceipt init(JsonObject param) {
        create(param);
        log.info("[StemContract | genesis] SUCCESS! param => {}", param);
        return txReceipt;
    }

    /**
     * Returns the id of a registered branch
     *
     * @param params branch   : The branch.json to register on the stem
     */
    @InvokeTransaction
    public TransactionReceipt create(JsonObject params) {
        StemContractStateValue stateValue;
        try {
            stateValue = StemContractStateValue.of(params);
            BranchId branchId = stateValue.getBranchId();
            if (!isBranchExist(branchId.toString())
                    && isBranchIdValid(branchId, stateValue)
                    && certificateAuthority(params)) {
                stateValue.init();
                setStateValue(stateValue, params);
                putState(stateValue, branchId);
            }
        } catch (Exception e) {
            log.warn("Failed to convert Branch = {}", params);
        }
        return txReceipt;
    }

    /**
     * Returns the id of a updated branch
     *
     * @param params branchId The Id of the branch to update
     *               branch   The branch.json to update on the stem
     */
    @InvokeTransaction
    public TransactionReceipt update(JsonObject params) {
        StemContractStateValue stateValue;
        try {
            String preBranchId = params.get(BRANCH_ID).getAsString();
            JsonObject preBranchJson = state.get(preBranchId);
            stateValue = StemContractStateValue.of(preBranchJson);
            BranchId branchId = stateValue.getBranchId();

            if (certificateAuthority(preBranchJson) && isBranchExist(branchId.toString())) {
                updateValue(stateValue, params);
                putState(stateValue, branchId);
            }
        } catch (Exception e) {
            log.warn("Failed to convert Branch = {}", params);
        }

        return txReceipt;
    }

    private void putState(StemContractStateValue stateValue, BranchId branchId) {
        try {
            addBranchId(branchId);
            state.put(branchId.toString(), stateValue.getJson());
            addTxId(branchId);

            txReceipt.setStatus(ExecuteStatus.SUCCESS);
            log.info("[StemContract] branchId => {}", branchId);
            log.info("[StemContract] branch => {}", stateValue.getJson());
        } catch (Exception e) {
            txReceipt.setStatus(ExecuteStatus.FALSE);
        }
    }

    /**
     * fee = fee - transaction size fee
     * tx size fee = txSize / 1mbyte
     * 1mbyte to 1yeed
     *
     * @param stateValue params
     */
    private void setStateValue(StemContractStateValue stateValue, JsonObject params) {
        if (params.has("fee") && txReceipt.getTxSize() != null) {
            BigDecimal fee = params.get("fee").getAsBigDecimal();
            BigDecimal txSize = BigDecimal.valueOf(txReceipt.getTxSize());
            BigDecimal txFee = txSize.divide(BigDecimal.valueOf(1000000));
            BigDecimal resultFee = fee.subtract(txFee);
            BigDecimal remainFee = feeState(stateValue);

            stateValue.setFee(remainFee.longValue() > 0
                    ? resultFee.add(remainFee) : resultFee);
            stateValue.setBlockHeight(txReceipt.getBlockHeight());
        }
    }

    private void updateValue(StemContractStateValue stateValue, JsonObject params) {
        setStateValue(stateValue, params);
        if (params.has(KEY.VALIDATOR)) {
            stateValue.updateValidatorSet(params.get(KEY.VALIDATOR).getAsString());
        }
    }

    /**
     * Returns boolean
     *
     * */
    public void messageCall(BranchId branchId) {
        // TODO message call to contract
        // TODO isEnoughFee
    }

    /**
     * Returns a list contains all branch id
     *
     * @return list of all branch id
     */
    @ContractQuery
    public Set<String> getBranchIdList() {
        JsonObject branchList = state.get(BRANCH_ID_LIST);
        if (branchList == null) {
            return Collections.emptySet();
        }
        JsonArray branchIds = branchList.getAsJsonArray("branchIds");
        Set<String> branchIdSet = new HashSet<>();
        for (JsonElement branchId : branchIds) {
            StemContractStateValue stateValue =
                    getBranchStateValue(branchId.getAsString());
            if (isEnoughFee(stateValue)) {
                branchIdSet.add(branchId.getAsString());
            }
        }
        return branchIdSet;
    }

    /**
     * @param params branch id
     *
     * @return branch json object
     */
    @ContractQuery
    public JsonObject getBranch(JsonObject params) {
        String branchId = params.get(BRANCH_ID).getAsString();
        StemContractStateValue stateValue = getBranchStateValue(branchId);

        if (stateValue != null && isBranchExist(branchId) && isEnoughFee(stateValue)) {
            stateValue.setFee(feeState(stateValue));
            return stateValue.getJson();
        }
        // TODO fee not enough mesaage
        return new JsonObject();
    }

    /**
     * @param params transaction id
     *
     * @return branch id
     */
    @ContractQuery
    public String getBranchIdByTxId(JsonObject params) {
        String txId = params.get(TX_ID).getAsString();
        JsonObject branchIdJson = state.get(txId);
        if (branchIdJson != null && branchIdJson.has(BRANCH_ID)) {
            String branchId = branchIdJson.get(BRANCH_ID).getAsString();
            StemContractStateValue stateValue = getBranchStateValue(branchId);
            if (isBranchExist(branchId) && isEnoughFee(stateValue)) {
                return branchIdJson.get(BRANCH_ID).getAsString();
            }
        }
        return "";
    }

    /**
     * @param params branch id
     *
     * @return contract json object
     */
    @ContractQuery
    public Set<JsonElement> getContract(JsonObject params) {
        String branchId = params.get(BRANCH_ID).getAsString();
        Set<JsonElement> contractSet = new HashSet<>();
        StemContractStateValue stateValue = getBranchStateValue(branchId);

        if (stateValue != null && isBranchExist(branchId) && isEnoughFee(stateValue)) {
            JsonArray contracts = stateValue.getJson().getAsJsonArray("contracts");
            for (JsonElement c : contracts) {
                contractSet.add(c);
            }
        }
        return contractSet;
    }

    /**
     * @param params branch id
     *
     * @return validator set
     */
    @ContractQuery
    public Set<String> getValidator(JsonObject params) {
        String branchId = params.get(BRANCH_ID).getAsString();
        Set<String> validatorSet = new HashSet<>();
        StemContractStateValue stateValue = getBranchStateValue(branchId);

        if (stateValue != null && isBranchExist(branchId) && isEnoughFee(stateValue)) {
            JsonArray validators = stateValue.getJson().getAsJsonArray(KEY.VALIDATOR);
            for (JsonElement v : validators) {
                validatorSet.add(v.getAsString());
            }
        }
        return validatorSet;
    }


    /**
     * @param params branch id
     *
     * @return branch id set
     */
    @ContractQuery
    public Set<String> getBranchIdByValidator(JsonObject params) {
        String validator = params.get(KEY.VALIDATOR).getAsString();
        Set<String> branchIdSet = new HashSet<>();

        getBranchIdList().forEach(id -> {
            StemContractStateValue stateValue = getBranchStateValue(id);
            if (isEnoughFee(stateValue)) {
                getBranchStateValue(id).getValidators().forEach(v -> {
                    if (validator.equals(v)) {
                        branchIdSet.add(id);
                    }
                });
            }
        });
        return branchIdSet;
    }

    /**
     * @param params branch id
     *
     * @return fee state
     */
    public BigDecimal feeState(JsonObject params) {
        String branchId = params.get(BRANCH_ID).getAsString();
        BigDecimal result = BigDecimal.ZERO;
        StemContractStateValue stateValue = getBranchStateValue(branchId);
        if (stateValue == null) {
            return result;
        }

        if (isBranchExist(branchId)) {
            Long currentHeight = txReceipt.getBlockHeight();
            Long createPointHeight = stateValue.getBlockHeight();
            long height = currentHeight - createPointHeight;

            //1block to 1yeed
            BigDecimal currentFee = stateValue.getFee();
            result = currentFee.subtract(BigDecimal.valueOf(height));
        }
        return result.longValue() > 0 ? result : BigDecimal.ZERO;
    }

    private BigDecimal feeState(StemContractStateValue stateValue) {
        BigDecimal currentFee = stateValue.getFee();
        if (currentFee.longValue() > 0) {
            Long currentHeight = txReceipt.getBlockHeight();
            Long createPointHeight = stateValue.getBlockHeight();
            long overTimeHeight = currentHeight - createPointHeight;
            return currentFee.subtract(BigDecimal.valueOf(overTimeHeight));
        }
        return BigDecimal.ZERO;
    }

    private Boolean isEnoughFee(StemContractStateValue stateValue) {
        return feeState(stateValue).longValue() > 0;
    }

    private boolean isBranchExist(String branchId) {
        return state.contains(branchId);
    }

    private void addBranchId(BranchId newBranchId) {
        if (!isBranchExist(newBranchId.toString())) {
            JsonArray branchIds = new JsonArray();
            for (String branchId : getBranchIdList()) {
                branchIds.add(branchId);
            }
            JsonObject obj = new JsonObject();
            branchIds.add(newBranchId.toString());
            obj.add("branchIds", branchIds);
            state.put(BRANCH_ID_LIST, obj);

        }
    }

    private void addTxId(BranchId branchId) {
        if (isBranchExist(branchId.toString())
                && txReceipt.getTxId() != null) {
            JsonObject bid = new JsonObject();
            bid.addProperty(BRANCH_ID, branchId.toString());
            state.put(txReceipt.getTxId(), bid);
        }
    }

    private Boolean certificateAuthority(JsonObject params) {
        String sender = this.txReceipt.getIssuer();
        JsonArray validators = params.get(KEY.VALIDATOR).getAsJsonArray();
        for (JsonElement v : validators) {
            if (params.has("updateValidators")) {
                JsonArray uvs = params.get("updateValidators").getAsJsonArray();
                for (JsonElement uv : uvs) {
                    if (sender != null && sender.equals(uv.getAsString())) {
                        return true;
                    }
                }
            }
            if (sender != null && sender.equals(v.getAsString())) {
                return true;
            }
        }

        return false;
    }

    private boolean isBranchIdValid(BranchId branchId, Branch branch) {
        return branchId.equals(branch.getBranchId());
    }

    private StemContractStateValue getBranchStateValue(String branchId) {
        JsonObject json = state.get(branchId);
        if (json == null) {
            return null;
        } else {
            return new StemContractStateValue(json);
        }
    }
}