package io.yggdrash.contract.coin;

import com.google.gson.JsonObject;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CoinContractTest {
    private static final CoinContract.CoinService coinContract = new CoinContract.CoinService();
    private static final Logger log = LoggerFactory.getLogger(CoinContractTest.class);

    private static final String ADDRESS_1 = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
    private static final String ADDRESS_2 = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e";
    private static final String ADDRESS_FORMAT = "{\"address\" : \"%s\"}";
    private static final String ADDRESS_JSON_1 = String.format(ADDRESS_FORMAT, ADDRESS_1);
    private static final String ADDRESS_JSON_2 = String.format(ADDRESS_FORMAT, ADDRESS_2);
    private Field txReceiptField;

    @Before
    public void setUp() throws IllegalAccessException {
        StateStore coinContractStateStore = new StateStore(new HashMapDbSource());

        List<Field> txReceipt = ContractUtils.txReceiptFields(coinContract);
        if (txReceipt.size() == 1) {
            txReceiptField = txReceipt.get(0);
        }
        for (Field f : ContractUtils.contractFields(coinContract, ContractStateStore.class)) {
            f.setAccessible(true);
            f.set(coinContract, coinContractStateStore);
        }

        genesis();
    }

    private void genesis() {
        String genesisStr = "{\"alloc\": {\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\":"
                + " {\"balance\": \"1000000000\"},\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\":"
                + " {\"balance\": \"1000000000\"},\"cee3d4755e47055b530deeba062c5bd0c17eb00f\":"
                + " {\"balance\": \"998000000000\"}}}";

        TransactionReceipt result = new TransactionReceiptImpl();

        try {
            txReceiptField.set(coinContract, result);
            coinContract.init(createParams(genesisStr));
        } catch (IllegalAccessException e) {
            log.warn(e.getMessage());
        }

        assertTrue(result.isSuccess());
        assertEquals(4, result.getTxLog().size());
    }

    @Test
    public void totalSupply() {
        BigInteger res = coinContract.totalSupply();

        assertEquals(BigInteger.valueOf(1000000000000L), res);
    }

    @Test
    public void balanceOf() {
        BigInteger res = coinContract.balanceOf(createParams(ADDRESS_JSON_1));

        assertEquals(BigInteger.valueOf(1000000000), res);
    }

    @Test
    public void allowance() {
        String paramStr = "{\"owner\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\","
                + "\"spender\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\"}";

        BigInteger res = coinContract.allowance(createParams(paramStr));

        assertEquals(BigInteger.ZERO, res);
    }

    @Test
    public void transfer() {
        final String paramStr = "{\"to\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\",\"amount\" : \"10\"}";

        // tx 가 invoke 되지 않아 baseContract 에 sender 가 세팅되지 않아서 설정해줌
        log.debug("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94:{}", coinContract.balanceOf(createParams(ADDRESS_JSON_1)));
        log.debug("1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e:{}", coinContract.balanceOf(createParams(ADDRESS_JSON_2)));

        JsonObject param = createParams(paramStr);

        TransactionReceipt result = new TransactionReceiptImpl();
        result.setIssuer(ADDRESS_1);
        try {
            txReceiptField.set(coinContract, result);
            result = coinContract.transfer(param);
        } catch (IllegalAccessException e) {
            log.warn(e.getMessage());
        }

        assertTrue(result.isSuccess());

        assertEquals(BigInteger.valueOf(999999990), coinContract.balanceOf(createParams(ADDRESS_JSON_1)));
        assertEquals(BigInteger.valueOf(1000000010), coinContract.balanceOf(createParams(ADDRESS_JSON_2)));

        // To many amount
        addAmount(param, BigInteger.valueOf(1000000010));
        result = coinContract.transfer(param);
        assertFalse(result.isSuccess());

        // Same amount
        addAmount(param, BigInteger.valueOf(999999990));
        result = coinContract.transfer(param);
        assertTrue(result.isSuccess());
    }

    @Test
    public void transferFrom() {
        String owner = ADDRESS_1;
        String spender = ADDRESS_2;
        String to = "cee3d4755e47055b530deeba062c5bd0c17eb00f";

        approveByOwner(to, owner, spender, "1000");

        String transferParams = "{\"from\" : \"" + owner + "\", \"to\" : \"" + to + "\",\"amount\" : \"700\"}";

        JsonObject transferFromObject = createParams(transferParams);

        TransactionReceipt result = new TransactionReceiptImpl();
        result.setIssuer(spender);
        try {
            txReceiptField.set(coinContract, result);
            result = coinContract.transferFrom(transferFromObject);
        } catch (IllegalAccessException e) {
            log.warn(e.getMessage());
        }

        assertTrue(result.isSuccess());
        assertEquals(BigInteger.valueOf(300), getAllowance(owner, spender));
        String logFormat = "{}: {}";
        log.debug(logFormat, to, getBalance(to));
        log.debug(logFormat, owner, getBalance(owner));
        log.debug(logFormat, spender, getBalance(spender));
        log.debug("getAllowance : {}", getAllowance(owner, spender));

        TransactionReceipt result2 = new TransactionReceiptImpl();
        try {
            txReceiptField.set(coinContract, result);
            coinContract.transferFrom(transferFromObject);
        } catch (IllegalAccessException e) {
            log.warn(e.getMessage());
        }

        // not enough amount allowed
        assertFalse(result2.isSuccess());

        addAmount(transferFromObject, getAllowance(owner, spender));
        result2 = coinContract.transferFrom(transferFromObject);
        assertTrue(result2.isSuccess());
        // reset
        assertEquals(BigInteger.ZERO, getAllowance(owner, spender));
    }

    @Test
    public void metaCoinTest() {
        MetaCoinContract metaCoinContract = new MetaCoinContract();
        assertTrue(metaCoinContract.hello(new JsonObject()).isSuccess());
    }

    private void approveByOwner(String to, String owner, String spender, String amount) {
        String approveParams = "{\"spender\" : \"" + spender + "\","
                + "\"amount\" : \"" + amount + "\"}";

        TransactionReceipt result = new TransactionReceiptImpl();
        result.setIssuer(owner);
        try {
            txReceiptField.set(coinContract, result);
            coinContract.approve(createParams(approveParams));
        } catch (IllegalAccessException e) {
            log.warn(e.getMessage());
        }

        assertTrue(result.isSuccess());

        String spenderParams = String.format(ADDRESS_FORMAT, spender);
        String senderParams = String.format(ADDRESS_FORMAT, owner);

        assertEquals(BigInteger.valueOf(1000000000),
                coinContract.balanceOf(createParams(spenderParams)));
        assertEquals(BigInteger.valueOf(1000000000),
                coinContract.balanceOf(createParams(senderParams)));

        assertTransferFrom(to, owner, spender);
    }

    private void assertTransferFrom(String to, String owner, String spender) {

        String allowanceParams = "{\"owner\" : \"" + owner + "\", \"spender\" : \"" + spender + "\"}";
        assertEquals(BigInteger.valueOf(1000), coinContract.allowance(createParams(allowanceParams)));

        String toParams = String.format(ADDRESS_FORMAT, to);
        assertEquals(BigInteger.valueOf(998000000000L), coinContract.balanceOf(createParams(toParams)));

        String fromParams = String.format(ADDRESS_FORMAT, owner);
        assertEquals(BigInteger.valueOf(1000000000), coinContract.balanceOf(createParams(fromParams)));

        String spenderParams = String.format(ADDRESS_FORMAT, spender);
        assertEquals(BigInteger.valueOf(1000000000), coinContract.balanceOf(createParams(spenderParams)));
    }

    private void addAmount(JsonObject param, BigInteger amount) {
        param.addProperty("amount", amount);
    }

    private JsonObject createParams(String paramStr) {
        return JsonUtil.parseJsonObject(paramStr);
    }

    private class MetaCoinContract extends CoinContract {
        TransactionReceipt hello(JsonObject params) {
            TransactionReceipt txReceipt = new TransactionReceiptImpl();
            txReceipt.addLog(params.toString());
            txReceipt.setStatus(ExecuteStatus.SUCCESS);
            log.info("{}", txReceipt);
            return txReceipt;
        }
    }

    private BigInteger getBalance(String address) {
        JsonObject obj = new JsonObject();
        obj.addProperty("address", address);
        return coinContract.balanceOf(obj);
    }

    private BigInteger getAllowance(String owner, String spender) {
        JsonObject obj = new JsonObject();
        obj.addProperty("owner", owner);
        obj.addProperty("spender", spender);
        return coinContract.allowance(obj);
    }
}
