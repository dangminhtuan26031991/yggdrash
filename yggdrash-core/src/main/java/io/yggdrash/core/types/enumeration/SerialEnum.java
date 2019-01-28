package io.yggdrash.core.types.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SerialEnum {
    ACCOUNT(1),
    VALIDATOR(2);

    private int value;

    SerialEnum(int value) {
        this.value = value;
    }

    @JsonCreator
    public static SerialEnum fromValue(int value) {
        switch (value) {
            case 1:
                return ACCOUNT;
            case 2:
                return VALIDATOR;
        }
        return null;
    }

    @JsonValue
    public int toValue() {
        return this.value;
    }
}
