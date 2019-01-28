package io.yggdrash.node.api.dto;

import com.google.gson.Gson;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.TransactionSignature;
import io.yggdrash.core.wallet.Wallet;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionDtoTest {
    private Wallet wallet;

    @Before
    public void setUp() throws Exception {
        wallet = new Wallet();
    }

    @Test
    public void signTx() {
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.branchId = "91b29a1453258d72ca6fbbcabb8dca10cca944fb";
        transactionDto.version = HexUtil.toHexString(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        transactionDto.type = HexUtil.toHexString(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        transactionDto.timestamp = 1514764800001L;

        Map<String, Object> param = new HashMap<>();
        param.put("to", "0x");
        param.put("amount", 1000);
        param.put("name", "ygg");

        Map<String, Object> paramsHusk = new HashMap<>();
        paramsHusk.put("method", "bonding");
        paramsHusk.put("params", param);

        List<Map<String, Object>> bodyData = new ArrayList<>();
        bodyData.add(paramsHusk);

        String body = new Gson().toJson(bodyData);
        TransactionSignature txSig = new TransactionSignature(wallet, HashUtil.sha3(body.getBytes()));

        transactionDto.bodyHash = HexUtil.toHexString(HashUtil.sha3(body.getBytes()));
        transactionDto.bodyLength = body.length();
        transactionDto.signature = HexUtil.toHexString(txSig.getSignature());
        transactionDto.body = JsonUtil.generateJsonToClass(body, List.class);

        Map<String, Object> api = new HashMap<>();
        api.put("jsonrpc", "2.0");
        api.put("id", "1");
        api.put("method", "sendTransaction");
        Map<String, Object> tx = new HashMap<>();
        tx.put("tx", transactionDto);
        api.put("params", tx);
        System.out.println(JsonUtil.convertObjToString(api));
    }
}
