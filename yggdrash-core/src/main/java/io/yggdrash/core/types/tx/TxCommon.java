package io.yggdrash.core.types.tx;

import io.yggdrash.core.types.TxPayload;
import io.yggdrash.proto.Proto;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigInteger;

@Data
public class TxCommon implements Serializable, TxPayload {
    private String to;
    private BigInteger amount;
    private String input;

    @Override
    public void mappingClassToProto(Proto.Transaction.Builder builder) {
        Proto.TxCommon.Builder txBuilder = Proto.TxCommon.newBuilder();
        if (!StringUtils.isEmpty(to)) {
            txBuilder.setTo(to);
        }
        if (amount != null) {
            txBuilder.setAmount(amount.toString());
        }
        if (!StringUtils.isEmpty(input)) {
            txBuilder.setInput(input);
        }
        builder.setCommon(txBuilder.build());
    }

    @Override
    public void mappingProtoToClass(Proto.Transaction txProto) {
        Proto.TxCommon tx = txProto.getCommon();
        to = tx.getTo();
        if (!StringUtils.isEmpty(tx.getAmount())) {
            amount = new BigInteger(tx.getAmount());
        }
        input = tx.getInput();
    }

    @Override
    public boolean validate() {
        if (StringUtils.isEmpty(to)) {
            return false;
        }
        if (amount == null || "0".equals(amount.toString())) {
            return false;
        }
        return true;
    }
}
