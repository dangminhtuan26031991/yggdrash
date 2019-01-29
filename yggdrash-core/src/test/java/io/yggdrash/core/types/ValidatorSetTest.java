package io.yggdrash.core.types;

import org.junit.Test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ValidatorSetTest {
    @Test
    public void orderByAscending() {
        Map<String, Validator> validatorMap = new HashMap<>();
        validatorMap.put("a", new Validator("a", null));
        validatorMap.put("b", new Validator("b", null));
        validatorMap.put("c", new Validator("c", null));

        ValidatorSet validatorSet = new ValidatorSet();
        validatorSet.setValidatorMap(validatorMap);

        List<Validator> validators = validatorSet.order(Comparator.naturalOrder());
        assertEquals("a", validators.get(0).getAddr());
        assertEquals("b", validators.get(1).getAddr());
        assertEquals("c", validators.get(2).getAddr());
    }

    @Test
    public void orderByDescending() {
        Map<String, Validator> validatorMap = new HashMap<>();
        validatorMap.put("a", new Validator("a", null));
        validatorMap.put("b", new Validator("b", null));
        validatorMap.put("c", new Validator("c", null));

        ValidatorSet validatorSet = new ValidatorSet();
        validatorSet.setValidatorMap(validatorMap);

        List<Validator> validators = validatorSet.order(Comparator.reverseOrder());
        assertEquals("c", validators.get(0).getAddr());
        assertEquals("b", validators.get(1).getAddr());
        assertEquals("a", validators.get(2).getAddr());
    }
}
