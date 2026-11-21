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

package software.amazon.awssdk.http.nio.netty;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import io.netty.handler.timeout.ReadTimeoutException;
import java.time.Duration;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.ProtocolNegotiation;
import software.amazon.awssdk.http.SdkAsyncHttpClientH2StreamErrorTestSuite;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

public class NettyNioAsyncHttpClientH2StreamErrorTest extends SdkAsyncHttpClientH2StreamErrorTestSuite {
    @Override
    protected SdkAsyncHttpClient createH2Client(int eventLoopThreads, Duration readTimeout) {
        return NettyNioAsyncHttpClient.builder()
                                      .protocol(Protocol.HTTP2)
                                      .protocolNegotiation(ProtocolNegotiation.ALPN)
                                      .readTimeout(readTimeout)
                                      .http2Configuration(b -> b.maxStreams(10L))
                                      .eventLoopGroupBuilder(SdkEventLoopGroup.builder().numberOfThreads(eventLoopThreads))
                                      .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build());
    }

    @Override
    protected Class<? extends Exception> expectedReadTimeoutCause() {
        return ReadTimeoutException.class;
    }
}
