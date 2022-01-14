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

package software.amazon.awssdk.services.defaultsmode;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;

/**
 * Tests suites to verify {@link DefaultsMode} behavior. We currently just test SDK default configuration such as
 * {@link RetryMode}; there is no easy way to test HTTP timeout option.
 *
 */
public abstract class ClientDefaultsModeTestSuite<ClientT, BuilderT extends AwsClientBuilder<BuilderT, ClientT>> {
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    @Test
    public void legacyDefaultsMode_shouldUseLegacySetting() {
        stubResponse();
        ClientT client = clientBuilder().overrideConfiguration(o -> o.retryPolicy(RetryMode.LEGACY)).build();
        callAllTypes(client);

        WireMock.verify(postRequestedFor(anyUrl()).withHeader("User-Agent", containing("cfg/retry-mode/legacy")));
    }

    @Test
    public void standardDefaultsMode_shouldApplyStandardDefaults() {
        stubResponse();
        ClientT client = clientBuilder().defaultsMode(DefaultsMode.STANDARD).build();
        callAllTypes(client);

        WireMock.verify(postRequestedFor(anyUrl()).withHeader("User-Agent", containing("cfg/retry-mode/standard")));
    }

    @Test
    public void retryModeOverridden_shouldTakePrecedence() {
        stubResponse();
        ClientT client =
            clientBuilder().defaultsMode(DefaultsMode.STANDARD).overrideConfiguration(o -> o.retryPolicy(RetryMode.LEGACY)).build();
        callAllTypes(client);

        WireMock.verify(postRequestedFor(anyUrl()).withHeader("User-Agent", containing("cfg/retry-mode/legacy")));
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

    private void stubResponse() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse()));
    }
}
