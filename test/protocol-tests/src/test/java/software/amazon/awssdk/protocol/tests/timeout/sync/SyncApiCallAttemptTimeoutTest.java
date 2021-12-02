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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Before;
import org.junit.Rule;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.protocol.tests.timeout.BaseApiCallAttemptTimeoutTest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Test apiCallAttemptTimeout feature for synchronous calls.
 */
public class SyncApiCallAttemptTimeoutTest extends BaseApiCallAttemptTimeoutTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolRestJsonClient client;
    private ProtocolRestJsonClient clientWithRetry;
    private static final int API_CALL_ATTEMPT_TIMEOUT = 800;

    @Before
    public void setup() {
        client = ProtocolRestJsonClient.builder()
                                       .region(Region.US_WEST_1)
                                       .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                       .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                       .overrideConfiguration(
                                           b -> b.apiCallAttemptTimeout(Duration.ofMillis(API_CALL_ATTEMPT_TIMEOUT))
                                                 .retryPolicy(RetryPolicy.none()))
                                       .build();

        clientWithRetry = ProtocolRestJsonClient.builder()
                                                .region(Region.US_WEST_1)
                                                .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                                .overrideConfiguration(
                                                    b -> b.apiCallAttemptTimeout(Duration.ofMillis(API_CALL_ATTEMPT_TIMEOUT))
                                                          .retryPolicy(RetryPolicy.builder()
                                                                                  .numRetries(1)
                                                                                  .build()))
                                                .build();

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
    protected WireMockRule wireMock() {
        return wireMock;
    }
}
