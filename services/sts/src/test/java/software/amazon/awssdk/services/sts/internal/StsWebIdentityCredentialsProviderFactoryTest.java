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

package software.amazon.awssdk.services.sts.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenCredentialsProviderFactory;
import software.amazon.awssdk.auth.credentials.internal.WebIdentityCredentialsUtils;
import software.amazon.awssdk.auth.credentials.internal.WebIdentityTokenCredentialProperties;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.IoUtils;

class StsWebIdentityCredentialsProviderFactoryTest {
    private static final String REGION_PROPERTY = "aws.region";
    private static final String STS_ENDPOINT_PROPERTY = "aws.endpointUrlSts";

    private WireMockServer stsServer;
    private Path tokenFile;
    private String originalRegion;
    private String originalStsEndpoint;

    @BeforeEach
    void setup() throws IOException {
        stsServer = new WireMockServer(wireMockConfig().dynamicPort());
        stsServer.start();

        tokenFile = Files.createTempFile("StsWebIdentityCredentialsProviderFactoryTest", ".token");
        Files.write(tokenFile, "web-identity-token".getBytes(StandardCharsets.UTF_8));

        originalRegion = System.getProperty(REGION_PROPERTY);
        originalStsEndpoint = System.getProperty(STS_ENDPOINT_PROPERTY);
        System.setProperty(REGION_PROPERTY, "us-east-1");
        System.setProperty(STS_ENDPOINT_PROPERTY, "http://localhost:" + stsServer.port());
    }

    @AfterEach
    void teardown() throws IOException {
        restoreProperty(REGION_PROPERTY, originalRegion);
        restoreProperty(STS_ENDPOINT_PROPERTY, originalStsEndpoint);
        stsServer.stop();
        Files.deleteIfExists(tokenFile);
    }

    @Test
    void stsWebIdentityCredentialsProviderFactory_with_webIdentityCredentialsUtils() {
        WebIdentityTokenCredentialsProviderFactory factory = WebIdentityCredentialsUtils.factory();
        assertNotNull(factory);
    }

    @Test
    void stsWebIdentityCredentialsProviderFactory_withWebIdentityTokenCredentialProperties() {
        WebIdentityTokenCredentialsProviderFactory factory = new StsWebIdentityCredentialsProviderFactory();
        AwsCredentialsProvider provider = factory.create(
            WebIdentityTokenCredentialProperties.builder()
                                                .asyncCredentialUpdateEnabled(true)
                                                .prefetchTime(Duration.ofMinutes(15))
                                                .staleTime(Duration.ofMinutes(5))
                                                .roleArn("role-arn")
                                                .webIdentityTokenFile(Paths.get("/path/to/file"))
                                                .roleSessionName("session-name")
                                                .roleSessionDuration(Duration.ofMinutes(60))
                                                .build());
        assertNotNull(provider);
    }

    @Test
    void create_invalidateMatchingAccessKey_nextResolveRefreshes() {
        stubAssumeRoleWithWebIdentity("AKID-1", "AKID-2");
        AwsCredentialsProvider provider = createProvider();
        try {
            assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("AKID-1");

            provider.invalidate(AwsBasicCredentials.create("AKID-1", "secret")).join();

            assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("AKID-2");
            stsServer.verify(2, postRequestedFor(urlPathEqualTo("/")));
        } finally {
            IoUtils.closeIfCloseable(provider, null);
        }
    }

    @Test
    void create_invalidateNonMatchingAccessKey_doesNotCallSts() {
        stubAssumeRoleWithWebIdentity("AKID-1", "AKID-2");
        AwsCredentialsProvider provider = createProvider();
        try {
            assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("AKID-1");

            provider.invalidate(AwsBasicCredentials.create("AKID-X", "secret")).join();

            assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("AKID-1");
            stsServer.verify(1, postRequestedFor(urlPathEqualTo("/")));
        } finally {
            IoUtils.closeIfCloseable(provider, null);
        }
    }

    private AwsCredentialsProvider createProvider() {
        return new StsWebIdentityCredentialsProviderFactory().create(
            WebIdentityTokenCredentialProperties.builder()
                                                .roleArn("arn:aws:iam::123456789012:role/test")
                                                .webIdentityTokenFile(tokenFile)
                                                .roleSessionName("session-name")
                                                .build());
    }

    /**
     * Stub STS so that the first AssumeRoleWithWebIdentity call returns {@code firstAccessKeyId} and every later call returns
     * {@code secondAccessKeyId}.
     */
    private void stubAssumeRoleWithWebIdentity(String firstAccessKeyId, String secondAccessKeyId) {
        String scenario = "AssumeRoleWithWebIdentity";
        String secondCall = "second call";
        stsServer.stubFor(post(urlPathEqualTo("/"))
                              .inScenario(scenario)
                              .whenScenarioStateIs(Scenario.STARTED)
                              .willReturn(assumeRoleWithWebIdentityResponse(firstAccessKeyId))
                              .willSetStateTo(secondCall));
        stsServer.stubFor(post(urlPathEqualTo("/"))
                              .inScenario(scenario)
                              .whenScenarioStateIs(secondCall)
                              .willReturn(assumeRoleWithWebIdentityResponse(secondAccessKeyId)));
    }

    private static ResponseDefinitionBuilder assumeRoleWithWebIdentityResponse(String accessKeyId) {
        String expiration = DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofHours(1)));
        String body = "<AssumeRoleWithWebIdentityResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\">"
                      + "<AssumeRoleWithWebIdentityResult>"
                      + "<Credentials>"
                      + "<AccessKeyId>" + accessKeyId + "</AccessKeyId>"
                      + "<SecretAccessKey>secret</SecretAccessKey>"
                      + "<SessionToken>session-token</SessionToken>"
                      + "<Expiration>" + expiration + "</Expiration>"
                      + "</Credentials>"
                      + "</AssumeRoleWithWebIdentityResult>"
                      + "<ResponseMetadata><RequestId>request-id</RequestId></ResponseMetadata>"
                      + "</AssumeRoleWithWebIdentityResponse>";
        return aResponse().withStatus(200)
                          .withHeader("Content-Type", "text/xml")
                          .withBody(body);
    }

    private static void restoreProperty(String name, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, originalValue);
        }
    }
}
