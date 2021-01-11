/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.http.nio.netty.fault;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import software.amazon.awssdk.http.SdkAsyncHttpClientH1TestSuite;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.utils.AttributeMap;


/**
 * Testing the scenario where h1 server sends 5xx errors.
 */
public class H1ServerErrorTest extends SdkAsyncHttpClientH1TestSuite {

    @Override
    protected SdkAsyncHttpClient setupClient() {
        return NettyNioAsyncHttpClient.builder()
                                              .eventLoopGroup(SdkEventLoopGroup.builder().numberOfThreads(2).build())
                                              .protocol(Protocol.HTTP1_1)
                                              .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build());
    }
}
