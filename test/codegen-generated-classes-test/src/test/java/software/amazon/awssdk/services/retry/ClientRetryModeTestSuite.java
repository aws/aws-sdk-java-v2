/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;
import software.amazon.awssdk.utils.StringInputStream;

public abstract class ClientRetryModeTestSuite<ClientT, BuilderT extends AwsClientBuilder<BuilderT, ClientT>> {
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    @Test
    public void legacyRetryModeIsFourAttempts() {
        stubThrottlingResponse();
        ClientT client = clientBuilder().overrideConfiguration(o -> o.retryStrategy(RetryMode.LEGACY))
                                        .build();
        assertThatThrownBy(() -> callAllTypes(client)).isInstanceOf(SdkException.class);
        verifyRequestCount(4);
    }

    @Test
    public void standardRetryModeIsThreeAttempts() {
        stubThrottlingResponse();
        ClientT client = clientBuilder().overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD)).build();
        assertThatThrownBy(() -> callAllTypes(client)).isInstanceOf(SdkException.class);
        verifyRequestCount(3);
    }

    @Test
    public void retryModeCanBeSetByProfileFile() {
        ProfileFile profileFile = ProfileFile.builder()
                                             .content(new StringInputStream("[profile foo]\n" +
                                                                            "retry_mode = standard"))
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();
        stubThrottlingResponse();
        ClientT client = clientBuilder().overrideConfiguration(o -> o.defaultProfileFile(profileFile)
                                                                     .defaultProfileName("foo")).build();
        assertThatThrownBy(() -> callAllTypes(client)).isInstanceOf(SdkException.class);
        verifyRequestCount(3);
    }

    @Test
    public void legacyRetryModeExcludesThrottlingExceptions() throws InterruptedException {
        stubThrottlingResponse();

        ExecutorService executor = Executors.newFixedThreadPool(51);
        ClientT client = clientBuilder().overrideConfiguration(o -> o.retryStrategy(RetryMode.LEGACY)).build();
        for (int i = 0; i < 51; ++i) {
            executor.execute(() -> assertThatThrownBy(() -> callAllTypes(client)).isInstanceOf(SdkException.class));
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // 51 requests * 4 attempts = 204 requests
        verifyRequestCount(204);
    }

    @Test
    public void standardRetryModeIncludesThrottlingExceptions() throws InterruptedException {
        stubThrottlingResponse();

        ExecutorService executor = Executors.newFixedThreadPool(51);
        ClientT client = clientBuilder().overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD)).build();

        for (int i = 0; i < 51; ++i) {
            executor.execute(() -> assertThatThrownBy(() -> callAllTypes(client)).isInstanceOf(SdkException.class));
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // Would receive 153 without throttling (51 requests * 3 attempts = 153 requests)
        verifyRequestCount(151);
    }

    private BuilderT clientBuilder() {
        return newClientBuilder().credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                 .region(Region.US_EAST_1)
                                 .endpointOverride(URI.create("http://localhost:" + wireMock.port()));
    }

    protected abstract BuilderT newClientBuilder();

    protected abstract AllTypesResponse callAllTypes(ClientT client);

    private void verifyRequestCount(int count) {
        verify(count, anyRequestedFor(anyUrl()));
    }

    private void stubThrottlingResponse() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(429)));
    }
}
