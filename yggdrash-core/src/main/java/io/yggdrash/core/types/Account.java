package io.yggdrash.core.types;

import io.yggdrash.core.types.enumeration.SerialEnum;
import lombok.Data;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Map;

@Data
public class Account implements Serializable {
    private static final long serialVersionUID = SerialEnum.ACCOUNT.toValue();

    private long txNo;
    private BigInteger balance;
    private BigInteger totalStaking;
    //key : validator addr, value : staking total amount
    private Map<String, BigInteger> staking;
}
