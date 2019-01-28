package io.yggdrash.core.types;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class Governance implements Serializable {
    private int maxDelegatableValidatorNums;
    private int validatorNums;
}
