package io.yggdrash.core.types.tx;

import io.yggdrash.core.types.TxPayload;
import io.yggdrash.proto.Proto;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigInteger;

@Data
public class TxDelegating implements Serializable, TxPayload {
    private String to;
    private BigInteger amount;


    @Override
    public void mappingClassToProto(Proto.Transaction.Builder builder) {
        Proto.TxDelegating.Builder txBuilder = Proto.TxDelegating.newBuilder();
        if (!StringUtils.isEmpty(to)) {
            txBuilder.setTo(to);
        }
        if (amount != null) {
            txBuilder.setAmount(amount.toString());
        }
        builder.setDelegating(txBuilder.build());
    }

    @Override
    public void mappingProtoToClass(Proto.Transaction txProto) {
        Proto.TxDelegating tx = txProto.getDelegating();
        to = tx.getTo();
        if (!StringUtils.isEmpty(tx.getAmount())) {
            amount = new BigInteger(tx.getAmount());
        }
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
