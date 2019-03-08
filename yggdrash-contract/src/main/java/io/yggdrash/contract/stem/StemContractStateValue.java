/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.contract.stem;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.branch.Branch;

import java.math.BigDecimal;

/**
 * updatable branch of stem contract
 *
 */
public class StemContractStateValue extends Branch {

    private static BigDecimal fee;
    private static Long blockHeight;

    public StemContractStateValue(JsonObject json) {
        super(json);
    }

    public void init() {
        setFee(BigDecimal.ZERO);
        setBlockHeight(0L);
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
        getJson().addProperty("fee", fee);
    }

    public Long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(Long blockHeight) {
        this.blockHeight = blockHeight;
        getJson().addProperty("blockHeight", blockHeight);
    }

    public void updateValidatorSet(String validator) {
        if (getJson().has("updateValidators")) {
            //TODO if validator array
            JsonArray v = getJson().get("updateValidators").getAsJsonArray();
            v.add(validator);
        } else {
            JsonArray updateValidator = new JsonArray();
            updateValidator.add(validator);
            getJson().add("updateValidators", updateValidator);
        }
    }

    public static StemContractStateValue of(JsonObject json) {
        return new StemContractStateValue(json.deepCopy());
    }

}