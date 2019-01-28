package io.yggdrash.core.types.tx;

import io.yggdrash.core.types.TxPayload;
import io.yggdrash.proto.Proto;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Data
public class TxRecover implements Serializable, TxPayload {
    private String input;

    @Override
    public void mappingClassToProto(Proto.Transaction.Builder builder) {
        Proto.TxRecover.Builder txBuilder = Proto.TxRecover.newBuilder();
        if (!StringUtils.isEmpty(input)) {
            txBuilder.setInput(input);
        }
        builder.setRecover(txBuilder.build());
    }

    @Override
    public void mappingProtoToClass(Proto.Transaction txProto) {
        Proto.TxRecover tx = txProto.getRecover();
        input = tx.getInput();
    }

    @Override
    public boolean validate() {
        return true;
    }
}
