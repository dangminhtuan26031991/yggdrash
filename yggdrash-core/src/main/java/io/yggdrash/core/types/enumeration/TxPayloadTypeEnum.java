package io.yggdrash.core.types.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TxPayloadTypeEnum {
    COMMON(1),
    BONDING(2),
    DELEGATING(3),
    UNSTAKING(4),
    RECOVER(5);

    private int value;

    TxPayloadTypeEnum(int value) {
        this.value = value;
    }

    @JsonCreator
    public static TxPayloadTypeEnum fromValue(int value) {
        switch (value) {
            case 1:
                return COMMON;
            case 2:
                return BONDING;
            case 3:
                return DELEGATING;
            case 4:
                return UNSTAKING;
            case 5:
                return RECOVER;
        }
        return null;
    }

    @JsonValue
    public int toValue() {
        return this.value;
    }
}
