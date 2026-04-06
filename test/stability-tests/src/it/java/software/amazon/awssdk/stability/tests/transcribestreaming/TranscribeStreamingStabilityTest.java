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

package software.amazon.awssdk.stability.tests.transcribestreaming;

import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.ProtocolNegotiation;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.stability.tests.exceptions.StabilityTestsRetryableException;
import software.amazon.awssdk.testutils.retry.RetryableTest;

/**
 * Stability tests for Transcribe Streaming using Netty HTTP client.
 */
public class TranscribeStreamingStabilityTest extends TranscribeStreamingBaseStabilityTest {

    private static TranscribeStreamingAsyncClient asyncClient;
    private static TranscribeStreamingAsyncClient asyncClientAlpn;

    @BeforeAll
    public static void setup() {
        asyncClient = initClient(ProtocolNegotiation.ASSUME_PROTOCOL);
        asyncClientAlpn = initClient(ProtocolNegotiation.ALPN);

        audioFileInputStream = getInputStream();
        if (audioFileInputStream == null) {
            throw new RuntimeException("Failed to get audio input stream");
        }
    }

    private static TranscribeStreamingAsyncClient initClient(ProtocolNegotiation protocolNegotiation) {
        return TranscribeStreamingAsyncClient.builder()
                                             .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                             .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                                                                                       .connectionAcquisitionTimeout(Duration.ofSeconds(30))
                                                                                       .maxConcurrency(CONCURRENCY)
                                                                                       .protocol(Protocol.HTTP2)
                                                                                       .protocolNegotiation(protocolNegotiation))
                                             .build();
    }

    @AfterAll
    public static void tearDown() {
        asyncClient.close();
        asyncClientAlpn.close();
    }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void startTranscription() {
        runTranscriptionTest(asyncClient, "TranscribeStreamingStabilityTest.startTranscription");
    }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void startTranscription_alpnEnabled() {
        runTranscriptionTest(asyncClientAlpn, "TranscribeStreamingStabilityTest.startTranscription_alpn");
    }
}
