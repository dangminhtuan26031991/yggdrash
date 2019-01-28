package io.yggdrash.core.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.yggdrash.core.types.enumeration.SerialEnum;
import lombok.Data;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Map;

@Data
public class Validator implements Serializable, Comparable<Validator> {
    private static final long serialVersionUID = SerialEnum.VALIDATOR.toValue();

    private String addr;

    //bonding + delegating
    private BigInteger totalStakingBalance;
    private BigInteger totalDelegatingBalance;
    private Map<String, BigInteger> delegating;

    private boolean isFreezing;
    private FreezingType freezingType;
    private long freezingBlockHeight;
    private int disconnectCnt;

    private String name;
    private String desc;
    private String url;
    private String logUrl;
    private String lat;
    private String lon;

    @Override
    public int compareTo(Validator o) {
        int val = totalStakingBalance.compareTo(o.totalStakingBalance);
        if (val == 0) {
            val = addr.compareTo(o.addr);
        }
        return val;
    }

    public enum FreezingType {
        BYZANTINE(1), DISCONNECTED(2);

        private int value;

        FreezingType(int value) {
            this.value = value;
        }

        @JsonCreator
        public static FreezingType fromValue(int value) {
            switch (value) {
                case 1:
                    return BYZANTINE;
                case 2:
                    return DISCONNECTED;
            }
            return null;
        }

        @JsonValue
        public int toValue() {
            return this.value;
        }
    }
}
