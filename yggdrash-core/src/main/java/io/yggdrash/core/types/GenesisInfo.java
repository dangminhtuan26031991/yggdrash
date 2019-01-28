package io.yggdrash.core.types;

import lombok.Data;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Map;

@Data
public class GenesisInfo implements Serializable {
    private String name;
    private String symbol;
    private String property;
    private String description;
    private String contractId;
    private String timestamp;
    private String owner;
    private String signature;

    private GenesisField genesis;

    @Data
    public static class GenesisField implements Serializable {
        private Map<String, Alloc> alloc;

        public static class Alloc implements Serializable{
            public BigInteger balance;
        }
    }
}
