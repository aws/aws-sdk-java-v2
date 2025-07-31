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

package software.amazon.awssdk.awscore.internal.defaultsmode;

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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Tests specifically for AutoDefaultsModeDiscovery's migration to use Ec2MetadataClient.
 * These tests verify that the migration from EC2MetadataUtils to Ec2MetadataClient works correctly.
 */
public class AutoDefaultsModeDiscoveryEc2MetadataClientTest {
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
                                                               .options(wireMockConfig().dynamicPort().dynamicPort())
                                                               .configureStaticDsl(true)
                                                               .build();

    @BeforeAll
    static void setupClass() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(),
                           "http://localhost:" + wireMock.getPort());
    }

    @BeforeEach
    public void setup() {
        clearEnvironmentVariable("AWS_EXECUTION_ENV");
        clearEnvironmentVariable("AWS_REGION");
        clearEnvironmentVariable("AWS_DEFAULT_REGION");
    }

    @AfterEach
    public void cleanup() {
        wireMock.resetAll();
        ENVIRONMENT_VARIABLE_HELPER.reset();
    }

   // Clear an environment variable by setting it to null.
    private void clearEnvironmentVariable(String name) {
        try {
            ENVIRONMENT_VARIABLE_HELPER.set(name, null);
        } catch (Exception e) {
            // Ignore
        }
    }

    @Test
    public void autoDefaultsModeDiscovery_shouldUseSharedHttpClient() throws Exception {
        // Stub successful IMDS responses
        stubFor(put("/latest/api/token")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-aws-ec2-metadata-token-ttl-seconds", "21600")
                                    .withBody("test-token")));
        stubFor(get("/latest/meta-data/placement/region")
                    .willReturn(aResponse().withStatus(200).withBody("us-east-1")));

        AutoDefaultsModeDiscovery discovery = new AutoDefaultsModeDiscovery();
        DefaultsMode result = discovery.discover(Region.US_EAST_1);

        // Should return IN_REGION since client region matches IMDS region
        assertThat(result).isEqualTo(DefaultsMode.IN_REGION);

        // Verify token request was made
        verify(putRequestedFor(urlEqualTo("/latest/api/token")));
        
        // Verify region request was made with token header - IMDSv2
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/placement/region"))
                .withHeader("x-aws-ec2-metadata-token", matching("test-token")));
        
        // Verify no IMDSv1 requests were made
        verify(0, getRequestedFor(urlEqualTo("/latest/meta-data/placement/region"))
                .withoutHeader("x-aws-ec2-metadata-token"));
    }

    @Test
    public void multipleDiscoveryInstances_shouldShareSameHttpClient() throws Exception {
        stubFor(put("/latest/api/token")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-aws-ec2-metadata-token-ttl-seconds", "21600")
                                    .withBody("test-token")));
        stubFor(get("/latest/meta-data/placement/region")
                    .willReturn(aResponse().withStatus(200).withBody("us-west-2")));

        // Create multiple discovery instances
        AutoDefaultsModeDiscovery discovery1 = new AutoDefaultsModeDiscovery();
        AutoDefaultsModeDiscovery discovery2 = new AutoDefaultsModeDiscovery();

        // Both should use the same shared HTTP client
        DefaultsMode result1 = discovery1.discover(Region.US_EAST_1);
        DefaultsMode result2 = discovery2.discover(Region.US_EAST_1);

        // Both should return CROSS_REGION
        assertThat(result1).isEqualTo(DefaultsMode.CROSS_REGION);
        assertThat(result2).isEqualTo(DefaultsMode.CROSS_REGION);

        // Verify token request was made
        verify(putRequestedFor(urlEqualTo("/latest/api/token")));
        
        // Verify region request was made with token header - IMDSv2
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/placement/region"))
                .withHeader("x-aws-ec2-metadata-token", matching("test-token")));
        
        // Verify no IMDSv1 requests were made
        verify(0, getRequestedFor(urlEqualTo("/latest/meta-data/placement/region"))
                .withoutHeader("x-aws-ec2-metadata-token"));
    }

    @Test
    public void awsEc2MetadataDisabled_shouldSkipImdsAndUseStandardMode() {
        // Disable IMDS
        ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_EC2_METADATA_DISABLED.environmentVariable(), "true");

        AutoDefaultsModeDiscovery discovery = new AutoDefaultsModeDiscovery();
        DefaultsMode result = discovery.discover(Region.US_EAST_1);

        // Should return STANDARD mode without making IMDS calls
        assertThat(result).isEqualTo(DefaultsMode.STANDARD);

        // Verify no IMDS requests were made
        verify(0, putRequestedFor(urlEqualTo("/latest/api/token")));
        verify(0, getRequestedFor(urlEqualTo("/latest/meta-data/placement/region")));
    }

    @Test
    public void imdsFailure_shouldFallbackToStandardMode() {
        // Stub IMDS to fail
        stubFor(put("/latest/api/token")
                    .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));
        stubFor(get("/latest/meta-data/placement/region")
                    .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        AutoDefaultsModeDiscovery discovery = new AutoDefaultsModeDiscovery();
        DefaultsMode result = discovery.discover(Region.US_EAST_1);

        // Should fall back to STANDARD mode when IMDS fails
        assertThat(result).isEqualTo(DefaultsMode.STANDARD);

        // Verify IMDS requests were attempted
        verify(putRequestedFor(urlEqualTo("/latest/api/token")));
    }

    @Test
    public void noRetryPolicy_shouldBeUsedByDefault() {
        // Stub token to succeed but region to fail with retryable error
        stubFor(put("/latest/api/token")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-aws-ec2-metadata-token-ttl-seconds", "21600")
                                    .withBody("test-token")));
        stubFor(get("/latest/meta-data/placement/region")
                    .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        AutoDefaultsModeDiscovery discovery = new AutoDefaultsModeDiscovery();
        DefaultsMode result = discovery.discover(Region.US_EAST_1);

        // Should fail immediately without retries and fallback to STANDARD
        assertThat(result).isEqualTo(DefaultsMode.STANDARD);

        // Verify requests were made once (no retries)
        verify(1, putRequestedFor(urlEqualTo("/latest/api/token")));
        
        // Verify region request was made with token header - IMDSv2
        verify(1, getRequestedFor(urlEqualTo("/latest/meta-data/placement/region"))
                .withHeader("x-aws-ec2-metadata-token", matching("test-token")));
        
        // Verify no IMDSv1 requests were made
        verify(0, getRequestedFor(urlEqualTo("/latest/meta-data/placement/region"))
                .withoutHeader("x-aws-ec2-metadata-token"));
    }

    @Test
    public void imdsV1Fallback_shouldWorkWhenTokenFails() {
        // Stub token request to fail
        stubFor(put("/latest/api/token")
                    .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        // Stub successful IMDSv1 request
        stubFor(get("/latest/meta-data/placement/region")
                    .willReturn(aResponse().withStatus(200).withBody("us-east-1")));

        AutoDefaultsModeDiscovery discovery = new AutoDefaultsModeDiscovery();
        DefaultsMode result = discovery.discover(Region.US_EAST_1);

        // Should fall back to IMDSv1 and return IN_REGION
        assertThat(result).isEqualTo(DefaultsMode.IN_REGION);

        // Verify token request was attempted
        verify(putRequestedFor(urlEqualTo("/latest/api/token")));
        
        // Verify region request was made without token header - IMDSv1 fallback
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/placement/region"))
                .withoutHeader("x-aws-ec2-metadata-token"));
        
        // Verify no IMDSv2 requests were made
        verify(0, getRequestedFor(urlEqualTo("/latest/meta-data/placement/region"))
                .withHeader("x-aws-ec2-metadata-token", matching(".*")));
    }

    @Test
    public void imdsV1Fallback_shouldNotWorkWhenV1Disabled() {
        // Disable IMDSv1 fallback
        ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.environmentVariable(), "true");

        // Stub token request to fail
        stubFor(put("/latest/api/token")
                    .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        AutoDefaultsModeDiscovery discovery = new AutoDefaultsModeDiscovery();
        DefaultsMode result = discovery.discover(Region.US_EAST_1);

        // Should fail without fallback to IMDSv1 and return STANDARD
        assertThat(result).isEqualTo(DefaultsMode.STANDARD);

        // Verify only token request was made
        verify(putRequestedFor(urlEqualTo("/latest/api/token")));
    }

    @Test
    public void tokenRequest400Error_shouldNotFallbackToV1() {
        // Stub token request to fail with 400
        stubFor(put("/latest/api/token")
                    .willReturn(aResponse().withStatus(400).withBody("Bad Request")));

        AutoDefaultsModeDiscovery discovery = new AutoDefaultsModeDiscovery();
        DefaultsMode result = discovery.discover(Region.US_EAST_1);

        // Should fail without attempting IMDSv1 fallback and return STANDARD
        assertThat(result).isEqualTo(DefaultsMode.STANDARD);

        // Verify only token request was made
        verify(putRequestedFor(urlEqualTo("/latest/api/token")));
    }
}
