package io.yggdrash.core.types.tx;

import io.yggdrash.core.types.TxPayload;
import io.yggdrash.core.types.enumeration.SerialEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TxValidatorVote implements Serializable, TxPayload {
    private static final long serialVersionUID = SerialEnum.TX_VALIDATOR_VOTE.toValue();

    private String validatorAddr;
    private boolean isAgree;

    @Override
    public boolean validate() {
        if (StringUtils.isEmpty(validatorAddr)) {
            return false;
        }
        return true;
    }
}
