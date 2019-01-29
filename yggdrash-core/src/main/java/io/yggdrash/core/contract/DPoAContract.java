package io.yggdrash.core.contract;

import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.runtime.annotation.ContractNonStateStore;
import io.yggdrash.core.runtime.annotation.ContractTransactionReceipt;
import io.yggdrash.core.runtime.annotation.Genesis;
import io.yggdrash.core.runtime.annotation.InvokeTransction;
import io.yggdrash.core.store.NonStateStore;
import io.yggdrash.core.types.GenesisInfo;
import io.yggdrash.core.types.ProposeValidatorSet;
import io.yggdrash.core.types.TxPayload;
import io.yggdrash.core.types.Validator;
import io.yggdrash.core.types.ValidatorSet;
import io.yggdrash.core.types.enumeration.PrefixKeyEnum;
import io.yggdrash.core.types.tx.TxValidatorPropose;
import io.yggdrash.core.types.tx.TxValidatorVote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public class DPoAContract {
    @ContractNonStateStore
    NonStateStore nonStateStore;

    @ContractTransactionReceipt
    TransactionReceipt txReceipt;

    @Genesis
    @InvokeTransction // TODO remove InvokeTransaction
    public boolean genesis(String jsonString) {
        ValidatorSet validatorSet = nonStateStore.get(PrefixKeyEnum.VALIDATORS.toValue());
        if (validatorSet != null) {
            return true;
        }

        validatorSet = new ValidatorSet();
        GenesisInfo genesisInfo = JsonUtil.generateJsonToClass(jsonString, GenesisInfo.class);
        for (String validator : genesisInfo.getValidators()) {
            validatorSet.getValidatorMap().put(validator, new Validator(validator));
        }
        nonStateStore.put(PrefixKeyEnum.VALIDATORS.toValue(), validatorSet);
        return true;
    }

    private boolean validateTx(TxPayload txPayload) {
        if (txPayload == null || !txPayload.validate()) {
            txReceipt.setStatus(ExecuteStatus.ERROR);
            return false;
        }
        return true;
    }

    @InvokeTransction
    public TransactionReceipt proposeValidator(String proposeValidator) {
        txReceipt.setStatus(ExecuteStatus.FALSE);

        //Check validation
        TxValidatorPropose txValidatorPropose = JsonUtil.generateJsonToClass(proposeValidator, TxValidatorPropose.class);
        if (!validateTx(txValidatorPropose)) {
            return txReceipt;
        }

        //Is exists proposer
        ValidatorSet validatorSet = nonStateStore.get(PrefixKeyEnum.VALIDATORS.toValue());
        if (validatorSet.getValidatorMap() == null || validatorSet.getValidatorMap().get(txReceipt.getIssuer()) == null) {
            return txReceipt;
        }


        ProposeValidatorSet proposeValidatorSet = nonStateStore.get(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue());
        if (proposeValidatorSet != null && proposeValidatorSet.getValidatorMap() != null) {
            //Is exists validator in propose set
            if (proposeValidatorSet.getValidatorMap().get(txValidatorPropose.getValidatorAddr()) != null) {
                return txReceipt;
            }

            //Is the proposed Validator voting complete
            Iterator<String> iter = proposeValidatorSet.getValidatorMap().keySet().iterator();
            while (iter.hasNext()) {
                if (txReceipt.getIssuer().equals(proposeValidatorSet.getValidatorMap().get(iter.next()).getProposalValidatorAddr())) {
                    return txReceipt;
                }
            }
        }

        //Add propose validator
        if (proposeValidatorSet == null) {
            proposeValidatorSet = new ProposeValidatorSet();
        }
        ProposeValidatorSet.Votable votable = new ProposeValidatorSet.Votable(txReceipt.getIssuer(), validatorSet);
        proposeValidatorSet.getValidatorMap().put(txValidatorPropose.getValidatorAddr(), votable);

        //Save
        nonStateStore.put(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue(), proposeValidatorSet);
        txReceipt.setStatus(ExecuteStatus.SUCCESS);
        return txReceipt;
    }

    @InvokeTransction
    public TransactionReceipt voteValidator(String validatorVote) {
        txReceipt.setStatus(ExecuteStatus.FALSE);

        //Check validation
        TxValidatorVote txValidatorVote = JsonUtil.generateJsonToClass(validatorVote, TxValidatorVote.class);
        if (!validateTx(txValidatorVote)) {
            return txReceipt;
        }

        //Is exists proposed validator
        ProposeValidatorSet proposeValidatorSet = nonStateStore.get(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue());
        if (proposeValidatorSet == null || MapUtils.isEmpty(proposeValidatorSet.getValidatorMap()) || proposeValidatorSet.getValidatorMap().get(txValidatorVote.getValidatorAddr()) == null) {
            return txReceipt;
        }

        //Check available vote
        ProposeValidatorSet.Votable votable = proposeValidatorSet.getValidatorMap().get(txValidatorVote.getValidatorAddr());
        if (votable.getVotedMap().get(txReceipt.getIssuer()) == null || votable.getVotedMap().get(txReceipt.getIssuer()).isVoted()) {
            return txReceipt;
        }

        //Vote
        if (txValidatorVote.isAgree()) {
            votable.setAgreeCnt(votable.getAgreeCnt() + 1);
        } else {
            votable.setDisagreeCnt(votable.getDisagreeCnt() + 1);
        }
        votable.getVotedMap().get(txReceipt.getIssuer()).setAgree(txValidatorVote.isAgree());
        votable.getVotedMap().get(txReceipt.getIssuer()).setVoted(true);

        //Save
        nonStateStore.put(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue(), proposeValidatorSet);
        txReceipt.setStatus(ExecuteStatus.SUCCESS);
        return txReceipt;
    }

    //todo should receive a set of byzantine and a set of validator that participated in the previous block consensus.
    public List<Validator> commit() {
        boolean isUpdateValidator = false;
        boolean isUpdateProposedValidator = false;
        ValidatorSet validatorSet = nonStateStore.get(PrefixKeyEnum.VALIDATORS.toValue());

        ProposeValidatorSet proposeValidatorSet = nonStateStore.get(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue());
        if (proposeValidatorSet != null && proposeValidatorSet.getValidatorMap() != null) {
            for (Iterator<Map.Entry<String, ProposeValidatorSet.Votable>> it = proposeValidatorSet.getValidatorMap().entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, ProposeValidatorSet.Votable> entry = it.next();
                switch (entry.getValue().status()) {
                    case AGREE:
                        if (validatorSet.getValidatorMap().get(entry.getKey()) == null) {
                            isUpdateValidator = true;
                            Validator validator = new Validator(entry.getKey(), entry.getValue());
                            validatorSet.getValidatorMap().put(entry.getKey(), validator);
                        }
                    case DISAGREE:
                        it.remove();
                        isUpdateProposedValidator = true;
                        break;
                    default:
                        break;
                }
            }
        }

        if (isUpdateValidator) {
            nonStateStore.put(PrefixKeyEnum.VALIDATORS.toValue(), validatorSet);
        }
        if (isUpdateProposedValidator) {
            nonStateStore.put(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue(), proposeValidatorSet);
        }

        return validatorSet.order(null);
    }

    //todo need to set governance
    @InvokeTransction
    public TransactionReceipt recoverValidator(String recoverValidator) {
        return null;
    }
}
