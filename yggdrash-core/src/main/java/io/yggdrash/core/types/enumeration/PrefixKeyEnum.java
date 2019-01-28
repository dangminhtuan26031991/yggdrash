package io.yggdrash.core.types.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PrefixKeyEnum {
    GENESIS("genesis"),
    GOVERNANCE("g-"),
    ACCOUNT("ac-"),
    VALIDATOR("v-");

    private String value;

    PrefixKeyEnum(String value) {
        this.value = value;
    }

    @JsonCreator
    public static PrefixKeyEnum fromValue(String value) {
        switch (value) {
            case "g-":
                return GENESIS;
            case "ac-":
                return ACCOUNT;
            case "v-":
                return VALIDATOR;
        }
        return null;
    }

    @JsonValue
    public String toValue() {
        return this.value;
    }

    public static String getAccountKey(String accountAddr) {
        return String.format("%s%s", PrefixKeyEnum.ACCOUNT.toValue(), accountAddr);
    }
}
