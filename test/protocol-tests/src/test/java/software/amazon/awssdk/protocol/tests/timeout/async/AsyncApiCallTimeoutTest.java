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

package software.amazon.awssdk.protocol.tests.timeout.async;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.protocol.tests.timeout.BaseApiCallTimeoutTest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockHttpClient;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Test apiCallTimeout feature for asynchronous operations.
 */
public class AsyncApiCallTimeoutTest extends BaseApiCallTimeoutTest {

    private ProtocolRestJsonAsyncClient client;
    private ProtocolRestJsonAsyncClient clientWithRetry;
    private MockAsyncHttpClient mockClient;

    @BeforeEach
    public void setup() {
        mockClient = new MockAsyncHttpClient();
        client = ProtocolRestJsonAsyncClient.builder()
                                            .region(Region.US_WEST_1)
                                            .httpClient(mockClient)
                                            .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                            .overrideConfiguration(b -> b.apiCallTimeout(TIMEOUT)
                                                                         .retryStrategy(AwsRetryStrategy.none()))
                                            .build();

        clientWithRetry = ProtocolRestJsonAsyncClient.builder()
                                                     .region(Region.US_WEST_1)
                                                     .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                                     .overrideConfiguration(b -> b.apiCallTimeout(TIMEOUT)
                                                                                 .retryStrategy(AwsRetryStrategy.standardRetryStrategy()
                                                                                                                .toBuilder()
                                                                                                                .backoffStrategy(BackoffStrategy.retryImmediately())
                                                                                                                .maxAttempts(2)
                                                                                                                .build()))
                                                     .httpClient(mockClient)
                                                     .build();
    }

    @AfterEach
    public void cleanUp() {
        mockClient.reset();
    }

    @Override
    protected Consumer<ThrowableAssert.ThrowingCallable> timeoutExceptionAssertion() {
        return c -> assertThatThrownBy(c).hasCauseInstanceOf(ApiCallTimeoutException.class);
    }

    @Override
    protected Consumer<ThrowableAssert.ThrowingCallable> serviceExceptionAssertion() {
        return c -> assertThatThrownBy(c).hasCauseInstanceOf(ProtocolRestJsonException.class);
    }

    @Override
    protected Callable callable() {
        return () -> client.allTypes().join();
    }

    @Override
    protected Callable retryableCallable() {
        return () -> clientWithRetry.allTypes().join();
    }

    @Override
    protected Callable streamingCallable() {
        return () -> client.streamingOutputOperation(SdkBuilder::build, AsyncResponseTransformer.toBytes()).join();
    }

    @Override
    protected void stubSuccessResponse(Duration delay) {
        mockClient.stubNextResponse(mockResponse(200), delay);
    }

    @Override
    protected void stubErrorResponse(Duration delay) {
        mockClient.stubNextResponse(mockResponse(500), delay);
    }

    @Override
    public MockHttpClient mockHttpClient() {
        return mockClient;
    }

    @Test
    public void increaseTimeoutInRequestOverrideConfig_shouldTakePrecedence() {

        ProtocolRestJsonAsyncClient asyncClient = createClientWithMockClient(mockClient);
        mockClient.stubNextResponse(mockResponse(200), DELAY_AFTER_TIMEOUT);

        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture =
            asyncClient.allTypes(b -> b.overrideConfiguration(c -> c.apiCallTimeout(DELAY_AFTER_TIMEOUT.plus(Duration.ofSeconds(1)))));

        AllTypesResponse response = allTypesResponseCompletableFuture.join();
        assertThat(response).isNotNull();
    }

    public ProtocolRestJsonAsyncClient createClientWithMockClient(MockAsyncHttpClient mockClient) {
        return ProtocolRestJsonAsyncClient.builder()
                                          .region(Region.US_WEST_1)
                                          .httpClient(mockClient)
                                          .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                          .overrideConfiguration(b -> b.apiCallTimeout(TIMEOUT)
                                                                       .retryStrategy(AwsRetryStrategy.none()))
                                          .build();
    }

}
