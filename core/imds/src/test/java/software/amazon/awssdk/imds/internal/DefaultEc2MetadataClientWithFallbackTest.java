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

package software.amazon.awssdk.imds.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.imds.Ec2MetadataClient;
import software.amazon.awssdk.imds.Ec2MetadataResponse;
import software.amazon.awssdk.imds.Ec2MetadataRetryPolicy;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Tests for DefaultEc2MetadataClientWithFallback to verify IMDSv1 fallback behavior.
 */
public class DefaultEc2MetadataClientWithFallbackTest {
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
                                                               .options(wireMockConfig().dynamicPort().dynamicPort())
                                                               .configureStaticDsl(true)
                                                               .build();

    private Ec2MetadataClient client;

    @BeforeEach
    public void setup() {
        clearEnvironmentVariable(SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.environmentVariable());
        
        // Create client with WireMock endpoint
        client = DefaultEc2MetadataClientWithFallback.builder()
                .endpoint(URI.create("http://localhost:" + wireMock.getPort()))
                .retryPolicy(Ec2MetadataRetryPolicy.builder().numRetries(0).build())
                .tokenTtl(Duration.ofSeconds(21600))
                .build();
    }

    @AfterEach
    public void cleanup() {
        if (client != null) {
            client.close();
        }
        wireMock.resetAll();
        ENVIRONMENT_VARIABLE_HELPER.reset();
    }

    private void clearEnvironmentVariable(String name) {
        try {
            ENVIRONMENT_VARIABLE_HELPER.set(name, null);
        } catch (Exception e) {
            //Ignore
        }
    }

    @Test
    public void imdsV2Success_shouldUseTokenAndReturnData() {
        // Stub successful IMDSv2 flow
        stubFor(put("/latest/api/token")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-aws-ec2-metadata-token-ttl-seconds", "21600")
                                    .withBody("test-token")));
        
        stubFor(get("/latest/meta-data/placement/region")
                    .withHeader("x-aws-ec2-metadata-token", matching("test-token"))
                    .willReturn(aResponse().withStatus(200).withBody("us-east-1")));

        Ec2MetadataResponse response = client.get("/latest/meta-data/placement/region");

        assertThat(response.asString()).isEqualTo("us-east-1");

        // Verify both token and data requests were made
        verify(putRequestedFor(urlEqualTo("/latest/api/token")));
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/placement/region"))
                .withHeader("x-aws-ec2-metadata-token", matching("test-token")));
    }

    @Test
    public void imdsV1Fallback_shouldWorkWhenTokenFails() {
        // Stub token request to fail with 500
        stubFor(put("/latest/api/token")
                    .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        // Stub successful IMDSv1 request
        stubFor(get("/latest/meta-data/placement/region")
                    .willReturn(aResponse().withStatus(200).withBody("us-west-2")));

        Ec2MetadataResponse response = client.get("/latest/meta-data/placement/region");

        assertThat(response.asString()).isEqualTo("us-west-2");

        verify(putRequestedFor(urlEqualTo("/latest/api/token")));
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/placement/region"))
                .withoutHeader("x-aws-ec2-metadata-token"));
        
        // Verify no IMDSv2 requests were made
        verify(0, getRequestedFor(urlEqualTo("/latest/meta-data/placement/region"))
                .withHeader("x-aws-ec2-metadata-token", matching(".*")));
    }

    @Test
    public void imdsV1Fallback_shouldNotWorkWith400Error() {
        // Stub token request to fail with 400
        stubFor(put("/latest/api/token")
                    .willReturn(aResponse().withStatus(400).withBody("Bad Request")));

        assertThatThrownBy(() -> client.get("/latest/meta-data/placement/region"))
                .isInstanceOf(SdkClientException.class)
                .hasMessageContaining("Unable to fetch metadata token");

        verify(putRequestedFor(urlEqualTo("/latest/api/token")));
        verify(0, getRequestedFor(urlEqualTo("/latest/meta-data/placement/region")));
    }

    @Test
    public void imdsV1Fallback_shouldNotWorkWhenDisabled() {
        // Disable IMDSv1 fallback
        ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.environmentVariable(), "true");

        // Recreate client to pick up environment variable
        client.close();
        client = DefaultEc2MetadataClientWithFallback.builder()
                .endpoint(URI.create("http://localhost:" + wireMock.getPort()))
                .retryPolicy(Ec2MetadataRetryPolicy.builder().numRetries(0).build())
                .tokenTtl(Duration.ofSeconds(21600))
                .build();

        stubFor(put("/latest/api/token")
                    .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        // Should throw exception without attempting IMDSv1 fallback
        assertThatThrownBy(() -> client.get("/latest/meta-data/placement/region"))
                .isInstanceOf(SdkClientException.class)
                .hasMessageContaining("fallback to IMDS v1 is disabled");


        verify(putRequestedFor(urlEqualTo("/latest/api/token")));
        verify(0, getRequestedFor(urlEqualTo("/latest/meta-data/placement/region")));
    }

    @Test
    public void tokenCaching_shouldReuseValidToken() {
        // Stub successful token request
        stubFor(put("/latest/api/token")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-aws-ec2-metadata-token-ttl-seconds", "21600")
                                    .withBody("cached-token")));
        
        // Stub successful data requests
        stubFor(get("/latest/meta-data/placement/region")
                    .withHeader("x-aws-ec2-metadata-token", matching("cached-token"))
                    .willReturn(aResponse().withStatus(200).withBody("us-east-1")));
        
        stubFor(get("/latest/meta-data/instance-id")
                    .withHeader("x-aws-ec2-metadata-token", matching("cached-token"))
                    .willReturn(aResponse().withStatus(200).withBody("i1234")));

        // Make two requests
        Ec2MetadataResponse response1 = client.get("/latest/meta-data/placement/region");
        Ec2MetadataResponse response2 = client.get("/latest/meta-data/instance-id");

        assertThat(response1.asString()).isEqualTo("us-east-1");
        assertThat(response2.asString()).isEqualTo("i1234");

        // Verify token was requested only once
        verify(1, putRequestedFor(urlEqualTo("/latest/api/token")));
        
        // Verify both data requests were made with the same token
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/placement/region"))
                .withHeader("x-aws-ec2-metadata-token", matching("cached-token")));
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/instance-id"))
                .withHeader("x-aws-ec2-metadata-token", matching("cached-token")));
    }

    @Test
    public void fallbackAfterTokenFailure_shouldUseImdsV1ForSubsequentRequests() {
        // Stub token request to fail
        stubFor(put("/latest/api/token")
                    .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        // Stub successful IMDSv1 requests
        stubFor(get("/latest/meta-data/placement/region")
                    .willReturn(aResponse().withStatus(200).withBody("us-west-2")));
        
        stubFor(get("/latest/meta-data/instance-id")
                    .willReturn(aResponse().withStatus(200).withBody("i-123")));

        // Make two requests - both should use IMDSv1 fallback
        Ec2MetadataResponse response1 = client.get("/latest/meta-data/placement/region");
        Ec2MetadataResponse response2 = client.get("/latest/meta-data/instance-id");

        assertThat(response1.asString()).isEqualTo("us-west-2");
        assertThat(response2.asString()).isEqualTo("i-123");

        // Verify token was requested only once
        verify(1, putRequestedFor(urlEqualTo("/latest/api/token")));
        

        verify(getRequestedFor(urlEqualTo("/latest/meta-data/placement/region"))
                .withoutHeader("x-aws-ec2-metadata-token"));
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/instance-id"))
                .withoutHeader("x-aws-ec2-metadata-token"));
    }
}
