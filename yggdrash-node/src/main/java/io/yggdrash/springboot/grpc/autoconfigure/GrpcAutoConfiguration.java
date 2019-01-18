/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.springboot.grpc.autoconfigure;

import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.yggdrash.node.grpc.interceptor.IpBlockInterceptor;
import io.yggdrash.springboot.grpc.GrpcGlobalInterceptor;
import io.yggdrash.springboot.grpc.GrpcServerBuilderConfigurer;
import io.yggdrash.springboot.grpc.GrpcServerRunner;
import io.yggdrash.springboot.grpc.GrpcService;
import io.yggdrash.springboot.grpc.context.LocalRunningGrpcPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfigureOrder
@ConditionalOnBean(annotation = GrpcService.class)
@EnableConfigurationProperties(GrpcServerProperties.class)
public class GrpcAutoConfiguration {
    @LocalRunningGrpcPort
    int runningPort;

    @Autowired
    GrpcServerProperties grpcServerProperties;

    @Bean
    public GrpcServerRunner grpcServerRunner(GrpcServerBuilderConfigurer configurer) {
        return new GrpcServerRunner(configurer,
                ServerBuilder.forPort(runningPort));
    }

    @Bean
    @ConditionalOnMissingBean(GrpcServerBuilderConfigurer.class)
    public GrpcServerBuilderConfigurer serverBuilderConfigurer() {
        return new GrpcServerBuilderConfigurer();
    }

    @Bean(name = "ipBlockFilter")
    @ConditionalOnProperty("yggdrash.node.grpc.black-list")
    @GrpcGlobalInterceptor
    public ServerInterceptor ipBlockFilter() {
        String[] blackList = grpcServerProperties.getBlackList();
        IpBlockInterceptor ipBlockInterceptor = new IpBlockInterceptor();
        ipBlockInterceptor.setBlackIps(blackList);
        return ipBlockInterceptor;
    }
}
