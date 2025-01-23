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

package software.amazon.awssdk.services.retry;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.net.URI;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;


/**
 * A set of tests that verify the behavior of the SDK when attempts are added to the exception message.
 */
public abstract class ExceptionAttemptMessageBehaviorTest<ClientT, BuilderT extends AwsClientBuilder<BuilderT, ClientT>> {

    protected WireMockServer wireMock = new WireMockServer(0);

    protected abstract BuilderT newClientBuilder();

    protected abstract AllTypesResponse callAllTypes(ClientT client);

    private BuilderT clientBuilder() {
        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
        return newClientBuilder()
            .credentialsProvider(credentialsProvider)
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create("http://localhost:" + wireMock.port()));
    }

    @BeforeEach
    public void beforeEach() {
        wireMock.start();
    }

    @AfterEach
    public void afterEach() {
        wireMock.stop();
    }

    @Test
    public void exceptionMessage_ioException_includesMultipleAttempts() {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        ClientT client = clientBuilder()
            .overrideConfiguration(o -> o.retryStrategy(
                b -> b.retryOnException(AwsServiceException.class)))
            .build();

        callAllTypes(client);
        SdkClientException exception = assertThrows(SdkClientException.class,
                                                     () -> callAllTypes(client));

        assertThat(exception.getMessage()).contains("(Attempts: 4)");
        wireMock.verify(4, postRequestedFor(anyUrl()));
    }

    @Test
    public void exceptionMessage_whenNonRetryable_includesSingleAttempt() {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(403)));

        ClientT client = clientBuilder()
            .overrideConfiguration(o -> o.retryStrategy(
                b -> b.retryOnException(AwsServiceException.class)))
            .build();

        AwsServiceException exception = assertThrows(AwsServiceException.class,
                                                     () -> callAllTypes(client));

        assertThat(exception.getMessage()).contains("(Attempts: 1");
        wireMock.verify(1, postRequestedFor(anyUrl()));
    }

    @Test
    public void exceptionMessage_whenErrorTypeChanges_showsTotalAttempts() {
        wireMock.stubFor(post(anyUrl())
                             .inScenario("Mixed Errors")
                             .whenScenarioStateIs(Scenario.STARTED)
                             .willReturn(aResponse().withStatus(429))
                             .willSetStateTo("Second Request"));

        wireMock.stubFor(post(anyUrl())
                             .inScenario("Mixed Errors")
                             .whenScenarioStateIs("Second Request")
                             .willReturn(aResponse().withStatus(403)));

        ClientT client = clientBuilder()
            .overrideConfiguration(o -> o.retryStrategy(
                b -> b.retryOnException(AwsServiceException.class)))
            .build();

        AwsServiceException exception = assertThrows(AwsServiceException.class,
                                                     () -> callAllTypes(client));

        assertThat(exception.getMessage()).contains("(Attempts: 2)");
        wireMock.verify(2, postRequestedFor(anyUrl()));
    }


    static class RetryAttemptsMessageSyncTest extends ExceptionAttemptMessageBehaviorTest<ProtocolRestJsonClient, ProtocolRestJsonClientBuilder> {
        @Override
        protected ProtocolRestJsonClientBuilder newClientBuilder() {
            return ProtocolRestJsonClient.builder();
        }

        @Override
        protected AllTypesResponse callAllTypes(ProtocolRestJsonClient client) {
            AllTypesRequest.Builder requestBuilder = AllTypesRequest.builder();
            return client.allTypes(requestBuilder.build());
        }
    }

    static class RetryAttemptsMessageAsyncTest extends ExceptionAttemptMessageBehaviorTest<ProtocolRestJsonAsyncClient, ProtocolRestJsonAsyncClientBuilder> {

        @Override
        protected ProtocolRestJsonAsyncClientBuilder newClientBuilder() {
            return ProtocolRestJsonAsyncClient.builder();
        }

        @Override
        protected AllTypesResponse callAllTypes(ProtocolRestJsonAsyncClient client) {
            try {
                AllTypesRequest.Builder requestBuilder = AllTypesRequest.builder();
                return client.allTypes(requestBuilder.build()).join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw e;
            }
        }
    }
}
