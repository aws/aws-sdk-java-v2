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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.protocol.tests.timeout.BaseApiCallAttemptTimeoutTest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;
import software.amazon.awssdk.testutils.service.http.MockHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Test apiCallAttemptTimeout feature for synchronous calls.
 */
public class SyncApiCallAttemptTimeoutTest extends BaseApiCallAttemptTimeoutTest {

    private ProtocolRestJsonClient client;
    private ProtocolRestJsonClient clientWithRetry;
    private MockSyncHttpClient mockSyncHttpClient;

    @BeforeEach
    public void setup() {
        mockSyncHttpClient = new MockSyncHttpClient();
        client = ProtocolRestJsonClient.builder()
                                       .region(Region.US_WEST_1)
                                       .httpClient(mockSyncHttpClient)
                                       .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                       .overrideConfiguration(
                                           b -> b.apiCallAttemptTimeout(API_CALL_ATTEMPT_TIMEOUT)
                                                 .retryStrategy(AwsRetryStrategy.none()))
                                       .build();

        clientWithRetry = ProtocolRestJsonClient.builder()
                                                .region(Region.US_WEST_1)
                                                .httpClient(mockSyncHttpClient)
                                                .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                                .overrideConfiguration(
                                                    b -> b.apiCallAttemptTimeout(API_CALL_ATTEMPT_TIMEOUT)
                                                          .retryStrategy(AwsRetryStrategy.standardRetryStrategy()
                                                                                         .toBuilder()
                                                                                         .backoffStrategy(BackoffStrategy.retryImmediately())
                                                                                         .maxAttempts(2)
                                                                                         .build()))
                                                .build();

    }

    @AfterEach
    public void cleanUp() {
        mockSyncHttpClient.reset();
    }

    @Override
    protected Consumer<ThrowableAssert.ThrowingCallable> timeoutExceptionAssertion() {
        return c -> assertThatThrownBy(c).isInstanceOf(ApiCallAttemptTimeoutException.class);
    }

    @Override
    protected Consumer<ThrowableAssert.ThrowingCallable> serviceExceptionAssertion() {
        return c -> assertThatThrownBy(c).isInstanceOf(ProtocolRestJsonException.class);
    }

    @Override
    protected Callable callable() {
        return () -> client.allTypes();
    }

    @Override
    protected Callable retryableCallable() {
        return () -> clientWithRetry.allTypes();
    }

    @Override
    protected Callable streamingCallable() {
        return () -> client.streamingOutputOperation(SdkBuilder::build, ResponseTransformer.toBytes());
    }

    @Override
    protected void stubSuccessResponse(Duration delay) {
        mockSyncHttpClient.stubNextResponse(mockResponse(200), delay);
    }

    @Override
    protected void stubErrorResponse(Duration delay) {
        mockSyncHttpClient.stubNextResponse(mockResponse(500), delay);
    }

    @Override
    public MockHttpClient mockHttpClient() {
        return mockSyncHttpClient;
    }
}
