/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.gateway.config;

import io.yggdrash.contract.core.store.OutputStore;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.gateway.store.es.EsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("es.host")
public class ElasticSearchConfiguration {

    @Value("${es.host}")
    private String esHost;

    @Value("${es.transport}")
    private int esTransport;

    @Value("${event.store:#{null}}")
    private String[] eventStore;

    @Bean
    SystemProperties systemProperties() {
        return SystemProperties.SystemPropertiesBuilder.aSystemProperties()
                .withEsHost(esHost)
                .withEsTransport(esTransport)
                .withEventStore(eventStore)
                .build();
    }

    @Bean
    OutputStore outputStore(SystemProperties systemProperties) {
        return EsClient.newInstance(systemProperties.getEsPrefixHost(), systemProperties.getEsTransport());
    }
}
