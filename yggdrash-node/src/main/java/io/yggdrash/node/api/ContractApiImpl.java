package io.yggdrash.node.api;

import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.exception.NonExistObjectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static io.yggdrash.common.config.Constants.BRANCH_ID;

@Service
@AutoJsonRpcServiceImpl
public class ContractApiImpl implements ContractApi {

    private final BranchGroup branchGroup;

    @Autowired
    public ContractApiImpl(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @Override
    public String query(String data) {
        JsonObject query = Utils.parseJsonObject(data);
        if (!query.has(BRANCH_ID)) {
            throw new NonExistObjectException("BranchId is required");
        }
        return branchGroup.query(query).toString();
    }
}
