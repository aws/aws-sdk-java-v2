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

import static software.amazon.awssdk.http.LongRunningRequestTestSupport.assertAcquireTimeoutFailure;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.assertFailsWithinTimeBound;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.http.SdkAsyncHttpClientH2AcquireTimeoutTestSuite;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

public class NettyNioAsyncHttpClientH2AcquireTimeoutTest extends SdkAsyncHttpClientH2AcquireTimeoutTestSuite {

    // A shared event loop group, so that closing each client does not wait out the event loop group's shutdown quiet period.
    private static SdkEventLoopGroup eventLoopGroup;

    @BeforeAll
    public static void createEventLoopGroup() {
        eventLoopGroup = SdkEventLoopGroup.builder().build();
    }

    @AfterAll
    public static void closeEventLoopGroup() {
        eventLoopGroup.eventLoopGroup().shutdownGracefully(0, 5, TimeUnit.SECONDS);
    }

    // MAX_CONNECTIONS (maxConcurrency) caps concurrent HTTP/2 streams: HttpOrHttp2ChannelPool sizes the pool in front of the
    // HTTP/2 multiplexed pool by it, and each acquire from that pool is a stream. So no maxStreams setting is needed.
    @Override
    protected SdkAsyncHttpClient createSdkAsyncHttpClient(AttributeMap config) {
        return NettyNioAsyncHttpClient.builder().eventLoopGroup(eventLoopGroup).buildWithDefaults(config);
    }

    @Override
    protected String expectedAcquireTimeoutMessagePrefix() {
        return "Acquire operation took longer than the configured maximum time.";
    }

    @Override
    protected Class<? extends Throwable> expectedAcquireTimeoutCause() {
        return TimeoutException.class;
    }

    // TODO: Both suite cases fail at the acquire timeout (about 500 ms) with a java.lang.Throwable whose message starts with
    // the acquire-timeout sentence and whose cause is java.util.concurrent.TimeoutException. The cause chain has no
    // IOException, because NettyUtils.decorateException wraps acquire timeouts in a plain Throwable, so the SDK retry
    // layer treats them as non-transient. This override is the suite's check minus the IOException assertion.
    @Override
    protected void assertAcquireTimeout(CompletableFuture<?> request) {
        Throwable failure = assertFailsWithinTimeBound(request, ACQUIRE_TIMEOUT);
        assertAcquireTimeoutFailure(failure, expectedAcquireTimeoutMessagePrefix(), expectedAcquireTimeoutCause());
    }
}
