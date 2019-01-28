package io.yggdrash.core.types;

import io.yggdrash.core.types.tx.TxBonding;
import io.yggdrash.proto.Proto;

public interface TxPayload {
    void mappingClassToProto(Proto.Transaction.Builder builder);

    void mappingProtoToClass(Proto.Transaction txProto);

    boolean validate();
}
