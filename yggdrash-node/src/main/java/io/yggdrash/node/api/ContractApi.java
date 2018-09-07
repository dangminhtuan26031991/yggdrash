package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.exception.NonExistObjectException;

@JsonRpcService("/api/contract")
public interface ContractApi {

    /**
     * Handles all queries that are dispatched to the contract
     * @param data query string
     * @return result of query
     * @throws Exception exception
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    String query(String data) throws Exception;
}