package io.yggdrash.core.types.tx;

import io.yggdrash.core.types.TxPayload;
import io.yggdrash.proto.Proto;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigInteger;

@Data
public class TxBonding implements Serializable, TxPayload {
    private BigInteger amount;

    private String name;
    private String desc;
    private String url;
    private String logoUrl;
    private String lat;
    private String lon;


    @Override
    public void mappingClassToProto(Proto.Transaction.Builder builder) {
        Proto.TxBonding.Builder txBuilder = Proto.TxBonding.newBuilder();
        if (amount != null) {
            txBuilder.setAmount(amount.toString());
        }
        if (!StringUtils.isEmpty(name)) {
            txBuilder.setName(name);
        }
        if (!StringUtils.isEmpty(desc)) {
            txBuilder.setDesc(desc);
        }
        if (!StringUtils.isEmpty(url)) {
            txBuilder.setUrl(url);
        }
        if (!StringUtils.isEmpty(logoUrl)) {
            txBuilder.setLogoUrl(logoUrl);
        }
        if (!StringUtils.isEmpty(lat)) {
            txBuilder.setLat(lat);
        }
        if (!StringUtils.isEmpty(lon)) {
            txBuilder.setLon(lon);
        }
        builder.setBonding(txBuilder.build());
    }

    @Override
    public void mappingProtoToClass(Proto.Transaction txProto) {
        Proto.TxBonding tx = txProto.getBonding();
        if (!StringUtils.isEmpty(tx.getAmount())) {
            amount = new BigInteger(tx.getAmount());
        }
        name = tx.getName();
        desc = tx.getDesc();
        url = tx.getUrl();
        logoUrl = tx.getLogoUrl();
        lat = tx.getLat();
        lon = tx.getLon();
    }

    @Override
    public boolean validate() {
        if (amount == null || "0".equals(amount.toString())) {
            return false;
        }
        return true;
    }
}
