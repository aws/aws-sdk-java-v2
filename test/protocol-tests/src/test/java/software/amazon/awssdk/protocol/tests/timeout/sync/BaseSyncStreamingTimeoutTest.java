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

package software.amazon.awssdk.protocol.tests.timeout.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static software.amazon.awssdk.protocol.tests.timeout.BaseTimeoutTest.mockResponse;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.protocol.tests.timeout.BaseTimeoutTest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

public abstract class BaseSyncStreamingTimeoutTest {

    private ProtocolRestJsonClient client;
    protected static final int TIMEOUT = 1000;
    protected static final Duration DELAY_BEFORE_TIMEOUT = Duration.ofMillis(100);
    private MockSyncHttpClient mockSyncHttpClient;

    @BeforeEach
    public void setup() {
        mockSyncHttpClient = new MockSyncHttpClient();
        client = ProtocolRestJsonClient.builder()
                                       .region(Region.US_WEST_1)
                                       .httpClient(mockSyncHttpClient)
                                       .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                       .overrideConfiguration(clientOverrideConfiguration())
                                       .build();
    }

    @Test
    public void slowFileTransformer_shouldThrowTimeoutException() {
        stubSuccessResponse(DELAY_BEFORE_TIMEOUT);

        assertThatThrownBy(() -> client
            .streamingOutputOperation(
                StreamingOutputOperationRequest.builder().build(), new BaseTimeoutTest.SlowFileResponseTransformer<>()))
            .isInstanceOf(expectedException());
        verifyInterruptStatusClear();
    }

    @Test
    public void slowBytesTransformer_shouldThrowTimeoutException() {
        stubSuccessResponse(DELAY_BEFORE_TIMEOUT);
        assertThatThrownBy(() -> client
            .streamingOutputOperation(
                StreamingOutputOperationRequest.builder().build(), new BaseTimeoutTest.SlowBytesResponseTransformer<>()))
            .isInstanceOf(expectedException());
        verifyInterruptStatusClear();
    }

    @Test
    public void slowInputTransformer_shouldThrowTimeoutException() {
        stubSuccessResponse(DELAY_BEFORE_TIMEOUT);

        assertThatThrownBy(() -> client
            .streamingOutputOperation(
                StreamingOutputOperationRequest.builder().build(), new BaseTimeoutTest.SlowInputStreamResponseTransformer<>()))
            .isInstanceOf(expectedException());
        verifyInterruptStatusClear();
    }

    @Test
    public void slowCustomResponseTransformer_shouldThrowTimeoutException() {
        stubSuccessResponse(DELAY_BEFORE_TIMEOUT);

        assertThatThrownBy(() -> client
            .streamingOutputOperation(
                StreamingOutputOperationRequest.builder().build(), new BaseTimeoutTest.SlowCustomResponseTransformer()))
            .isInstanceOf(expectedException());
        verifyInterruptStatusClear();
    }

    abstract Class<? extends Exception> expectedException();

    abstract ClientOverrideConfiguration clientOverrideConfiguration();

    private void verifyInterruptStatusClear() {
        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    private void stubSuccessResponse(Duration delay) {
        mockSyncHttpClient.stubNextResponse(mockResponse(200), delay);
    }
}
