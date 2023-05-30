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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.protocol.tests.timeout.BaseApiCallTimeoutTest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Test apiCallTimeout feature for asynchronous operations.
 */
public class AsyncApiCallTimeoutTest extends BaseApiCallTimeoutTest {
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolRestJsonAsyncClient client;
    private ProtocolRestJsonAsyncClient clientWithRetry;

    @Before
    public void setup() {
        client = ProtocolRestJsonAsyncClient.builder()
                                            .region(Region.US_WEST_1)
                                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                            .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                            .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMillis(TIMEOUT))
                                                                         .retryPolicy(RetryPolicy.none()))
                                            .build();

        clientWithRetry = ProtocolRestJsonAsyncClient.builder()
                                                     .region(Region.US_WEST_1)
                                                     .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                     .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                                     .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMillis(TIMEOUT))
                                                                                  .retryPolicy(RetryPolicy.builder().numRetries(1).build()))
                                                     .build();
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
    protected WireMockRule wireMock() {
        return wireMock;
    }

    @Test
    public void increaseTimeoutInRequestOverrideConfig_shouldTakePrecedence() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_AFTER_TIMEOUT)));
        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture =
            client.allTypes(b -> b.overrideConfiguration(c -> c.apiCallTimeout(Duration.ofMillis(DELAY_AFTER_TIMEOUT + 1000))));

        AllTypesResponse response = allTypesResponseCompletableFuture.join();
        assertThat(response).isNotNull();
    }

}
