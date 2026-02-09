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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.stability.tests.exceptions.StabilityTestsRetryableException;
import software.amazon.awssdk.testutils.retry.RetryableTest;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Stability tests for Transcribe Streaming using CRT HTTP client with HTTP/2.
 */
public class TranscribeStreamingCrtStabilityTest extends TranscribeStreamingBaseStabilityTest {

    private static TranscribeStreamingAsyncClient asyncClient;

    @BeforeAll
    public static void setup() {
        asyncClient = TranscribeStreamingAsyncClient.builder()
                                                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                    .httpClientBuilder(AwsCrtAsyncHttpClient.builder())
                                                    .build();

        audioFileInputStream = getInputStream();
        if (audioFileInputStream == null) {
            throw new RuntimeException("Failed to get audio input stream");
        }
    }

    @AfterAll
    public static void tearDown() {
        asyncClient.close();
    }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void startTranscription_crtH2() {
        runTranscriptionTest(asyncClient, "TranscribeStreamingCrtStabilityTest.startTranscription");
    }
}
