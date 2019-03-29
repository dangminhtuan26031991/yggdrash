/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.blockchain;

import io.yggdrash.common.contract.ContractVersion;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PrepareBlockchain {
    private static final Logger log = LoggerFactory.getLogger(PrepareBlockchain.class);

    String contractPath;
    List<BranchContract> contractList;


    public PrepareBlockchain(String contractPath) {
        this.contractPath = contractPath;
        log.debug("Contract Path : {} ", contractPath);
    }

    public File loadContractFile(ContractVersion version) {
        File contractFile = new File(String.format("%s/%s.jar", contractPath,
                version));
        if (!(contractFile.exists() && contractFile.canRead())) {
            log.debug("Contract file not Exist");
            return null;
        }
        return contractFile;
    }


    public boolean checkBlockChainIsReady(BlockChain blockChain) {
        // Get BranchContract
        contractList = blockChain.getBranchContracts();
        if (blockChain.getLastIndex() == 0 && contractList.isEmpty()) {
            // is Genesis Blockchain
            contractList = blockChain.getBranch().getBranchContracts();
        }

        // TODO check branch contract file exist
        if (contractList == null) {
            contractList = blockChain.getBranch().getBranchContracts();
        }
        if (contractList.isEmpty()) {
            log.error("Branch Contract is Null");
        }

        for (BranchContract bc : contractList) {
            ContractVersion contractVersion = bc.getContractVersion();

            File contractFile = loadContractFile(contractVersion);

            if (contractFile == null && !findContractFile(contractVersion)) {
                log.error("Contract {} is not exists", contractVersion);
                return false;
            }

            // verify contract file
            if (!verifyContractFile(contractFile, contractVersion)) {
                // TODO findContractFile
                log.error("Contract {} is not verify", contractVersion);
                return false;
            }
        }
        return true;
    }

    public boolean findContractFile(ContractVersion contractVersion) {
        // TODO contract file download
        return false;
    }

    public boolean verifyContractFile(File contractFile, ContractVersion contractVersion) {
        // Contract Path + contract Version + .jar
        // check contractVersion Hex
        try (InputStream is = new FileInputStream(contractFile)) {
            byte[] contractBinary = IOUtils.toByteArray(is);
            ContractVersion checkVersion = ContractVersion.of(contractBinary);
            return contractVersion.toString().equals(checkVersion.toString());

        } catch (IOException e) {
            log.warn(e.getMessage());
        }

        return false;

    }

    public List<BranchContract> getContractList() {
        return this.contractList;
    }

}
