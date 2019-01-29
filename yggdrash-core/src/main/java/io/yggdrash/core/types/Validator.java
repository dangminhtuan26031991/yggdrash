package io.yggdrash.core.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.yggdrash.core.types.enumeration.SerialEnum;
import lombok.Data;

import java.io.Serializable;

@Data
public class Validator implements Serializable, Comparable<Validator> {
    private static final long serialVersionUID = SerialEnum.VALIDATOR.toValue();

    private String addr;

    private ProposeValidatorSet.Votable votedHistory;

    private boolean isFreezing;
    private FreezingType freezingType;
    private long freezingBlockHeight;
    private int disconnectCnt;

    public Validator() {

    }

    public Validator(String addr) {
        this.addr = addr;
    }

    public Validator(String addr, ProposeValidatorSet.Votable votedHistory) {
        this.addr = addr;
        this.votedHistory = votedHistory;
    }

    @Override
    public int compareTo(Validator o) {
        return addr.compareTo(o.addr);
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
                default:
                    return null;
            }
        }

        @JsonValue
        public int toValue() {
            return this.value;
        }
    }
}
