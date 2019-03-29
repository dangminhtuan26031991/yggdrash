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

package io.yggdrash.core.blockchain.osgi;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;

public class ContractPolicyLoaderTest {
    private static final Logger log = LoggerFactory.getLogger(ContractPolicyLoaderTest.class);

    @Test
    public void contractPolicyLoader() {
        ContractPolicyLoader loader = new ContractPolicyLoader();

        Assert.assertNotNull(loader.getFrameworkFactory());
    }

    @Test
    public void loadFramework() throws BundleException {
        ContractPolicyLoader loader = new ContractPolicyLoader();

        FrameworkFactory fa = loader.getFrameworkFactory();
        Map config = loader.getContainerConfig();

        if (System.getSecurityManager() != null) {
            config.remove("org.osgi.framework.security");
        }


        Framework osgi = fa.newFramework(config);
        log.debug(osgi.getLocation());
        log.debug(osgi.getSymbolicName());

        osgi.start();

        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("contracts/96206ff28aead93a49272379a85191c54f7b33c0.jar");

        BundleContext context = osgi.getBundleContext();
        log.debug(context.getClass().getName());

        // Test unsigned Jar
        for (Bundle b : context.getBundles()) {
            log.debug("BID {} , State : {}", b.getBundleId(), b.getState());
            log.debug(b.getSymbolicName());
            if (b.getSymbolicName().startsWith("io.yggdrash")) {
                b.uninstall();
            }
        }

        Bundle bd = context
                .installBundle("contracts/96206ff28aead93a49272379a85191c54f7b33c0.jar", stream);

        bd.start();
        log.debug("BID {} , State : {}", bd.getBundleId(), bd.getState());
        log.debug(Long.toString(bd.getBundleId()));
        log.debug(bd.getSymbolicName());
        log.debug(bd.getVersion().toString());
        Assert.assertEquals(osgi.ACTIVE, osgi.getState());

        osgi.stop();
    }

}