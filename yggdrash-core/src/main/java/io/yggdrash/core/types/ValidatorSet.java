package io.yggdrash.core.types;

import io.yggdrash.core.types.enumeration.SerialEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ValidatorSet implements Serializable {
    private static final long serialVersionUID = SerialEnum.VALIDATOR_SET.toValue();

    private Map<String, Validator> validatorMap;

    public ValidatorSet() {
        validatorMap = new HashMap<>();
    }

    public List<Validator> order(Comparator comparator) {
        List<Validator> validators = new ArrayList<>();
        if (validatorMap != null && validatorMap.size() > 0) {
            validatorMap.forEach((k, v) -> {
                if (!v.isFreezing()) {
                    validators.add(v);
                }
            });
            Collections.sort(validators, comparator == null ? Comparator.reverseOrder() : comparator);
        }

        return validators;
    }
}
