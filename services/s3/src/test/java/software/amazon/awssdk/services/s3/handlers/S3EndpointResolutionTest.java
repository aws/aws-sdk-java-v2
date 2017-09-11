/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3.handlers;


import static org.assertj.core.api.Assertions.assertThat;
import static utils.S3MockUtils.mockListBucketsResponse;
import static utils.S3MockUtils.mockListObjectsResponse;

import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.client.builder.ClientHttpConfiguration;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AdvancedConfiguration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.test.http.MockHttpClient;

/**
 * Functional tests for various endpoint related behavior in S3.
 */
@ReviewBeforeRelease("If we add back protocol (HTTP/HTTPS) to client builder we need to account for it in " +
                     "EndpointAddressInterceptor and add tests for it.")
public class S3EndpointResolutionTest {

    private static final String BUCKET = "some-bucket";
    private static final String NON_DNS_COMPATIBLE_BUCKET = "SOME.BUCKET";
    private static final String ENDPOINT_WITHOUT_BUCKET = "https://s3.ap-south-1.amazonaws.com";
    private static final String ENDPOINT_WITH_BUCKET = String.format("https://%s.s3.ap-south-1.amazonaws.com", BUCKET);

    private MockHttpClient mockHttpClient;

    @Before
    public void setup() {
        mockHttpClient = new MockHttpClient();
    }

    /**
     * Only APIs that operate on buckets uses virtual addressing. Service level operations like ListBuckets will use the normal
     * endpoint.
     */
    @Test
    public void serviceLevelOperation_UsesStandardEndpoint() throws Exception {
        mockHttpClient.stubNextResponse(mockListBucketsResponse());
        S3Client s3Client = buildClient(null);

        s3Client.listBuckets();

        assertThat(mockHttpClient.getLastRequest().getEndpoint())
                .as("Uses regional S3 endpoint without bucket")
                .isEqualTo(URI.create(ENDPOINT_WITHOUT_BUCKET));

        assertThat(mockHttpClient.getLastRequest().getResourcePath())
                .as("Bucket is not in resource path")
                .isEqualTo("/");
    }

    /**
     * Service level operations for dualstack mode should go to the dualstack endpoint (without virtual addressing).
     */
    @Test
    public void serviceLevelOperation_WithDualstackEnabled_UsesDualstackEndpoint() throws Exception {
        mockHttpClient.stubNextResponse(mockListBucketsResponse());
        S3Client s3Client = buildClient(withDualstackEnabled());

        s3Client.listBuckets();

        assertThat(mockHttpClient.getLastRequest().getEndpoint())
                .as("Uses regional S3 endpoint without bucket")
                .isEqualTo(URI.create("https://s3.dualstack.ap-south-1.amazonaws.com"));

        assertThat(mockHttpClient.getLastRequest().getResourcePath())
                .as("Bucket is not in resource path")
                .isEqualTo("/");
    }

    /**
     * When a custom endpoint is provided via the builder we should honor that instead of trying to re-resolve it in the
     * {@link EndpointAddressInterceptor}.
     */
    @Test
    public void customEndpointProvided_UsesCustomEndpoint() throws Exception {
        URI customEndpoint = URI.create("https://foobar.amazonaws.com");
        mockHttpClient.stubNextResponse(mockListBucketsResponse());
        S3Client s3Client = clientBuilder().endpointOverride(customEndpoint).build();

        s3Client.listBuckets();

        assertThat(mockHttpClient.getLastRequest().getEndpoint())
                .as("Uses custom endpoint")
                .isEqualTo(customEndpoint);
    }

    /**
     * If a custom, non-s3 endpoint is used we revert to path style addressing. This is useful for alternative S3 implementations
     * like Ceph that do not support virtual style addressing.
     */
    @Test
    public void nonS3EndpointProvided_DoesNotUseVirtualAddressing() throws Exception {
        URI customEndpoint = URI.create("https://foobar.amazonaws.com");
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilder().endpointOverride(customEndpoint).build();

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertUsesPathStyleAddressing(mockHttpClient.getLastRequest(), customEndpoint.toString());
    }

    /**
     * If a custom S3 endpoint is provided (like s3-external-1 or a FIPS endpoint) then we should still use virtual addressing
     * when possible.
     */
    @Test
    public void customS3EndpointProvided_UsesVirtualAddressing() throws Exception {
        URI customEndpoint = URI.create("https://s3-external-1.amazonaws.com");
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilder().endpointOverride(customEndpoint).build();

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertUsesVirtualAddressing(mockHttpClient.getLastRequest(),
                                    String.format("https://%s.s3-external-1.amazonaws.com", BUCKET));
    }

    /**
     * If customer is using HTTP we need to preserve that scheme when switching to virtual addressing.
     */
    @Test
    public void customHttpEndpoint_PreservesSchemeWhenSwitchingToVirtualAddressing() throws Exception {
        URI customEndpoint = URI.create("http://s3-external-1.amazonaws.com");
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilder().endpointOverride(customEndpoint).build();

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertUsesVirtualAddressing(mockHttpClient.getLastRequest(),
                                    String.format("http://%s.s3-external-1.amazonaws.com", BUCKET));
    }

    /**
     * In us-east-1 buckets can have non-DNS compliant names. For those buckets we must always use path style even when it
     * is disabled per the advanced configuration.
     */
    @Test
    public void pathStyleDisabled_NonDnsCompatibleBucket_StillUsesPathStyleAddressing() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = buildClient(null);

        s3Client.listObjects(ListObjectsRequest.builder().bucket(NON_DNS_COMPATIBLE_BUCKET).build());

        SdkHttpFullRequest capturedRequest = mockHttpClient.getLastRequest();
        assertThat(capturedRequest.getEndpoint())
                .as("Uses endpoint without bucket name prepended")
                .isEqualTo(URI.create(ENDPOINT_WITHOUT_BUCKET));

        assertThat(capturedRequest.getResourcePath())
                .as("Resource path is left as bucket name")
                .startsWith("/" + NON_DNS_COMPATIBLE_BUCKET);
    }

    /**
     * When path style is enabled in the advanced configuration we should always use it.
     */
    @Test
    public void pathStyleConfigured_UsesPathStyleAddressing() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = buildClient(withPathStyle());

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertUsesPathStyleAddressing(mockHttpClient.getLastRequest(), ENDPOINT_WITHOUT_BUCKET);
    }

    /**
     * By default we use virtual addressing when possible.
     */
    @Test
    public void noAdvancedConfigurationProvided_UsesVirtualAddressingWithStandardEndpoint() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = buildClient(null);

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertUsesVirtualAddressing(mockHttpClient.getLastRequest(), ENDPOINT_WITH_BUCKET);
    }

    /**
     * By default we use virtual addressing when possible.
     */
    @Test
    public void emptyAdvancedConfigurationProvided_UsesVirtualAddressingWithStandardEndpoint() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = buildClient(S3AdvancedConfiguration.builder().build());

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertUsesVirtualAddressing(mockHttpClient.getLastRequest(), ENDPOINT_WITH_BUCKET);
    }

    /**
     * S3 accelerate has a global endpoint, we use that when accelerate mode is enabled in the advanced configuration.
     */
    @Test
    public void accelerateEnabled_UsesVirtualAddressingWithAccelerateEndpoint() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = buildClient(withAccelerateEnabled());

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertUsesVirtualAddressing(mockHttpClient.getLastRequest(),
                                    String.format("https://%s.s3-accelerate.amazonaws.com", BUCKET));
    }

    /**
     * Dualstack uses regional endpoints that support virtual addressing.
     */
    @Test
    public void dualstackEnabled_UsesVirtualAddressingWithDualstackEndpoint() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = buildClient(withDualstackEnabled());

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertUsesVirtualAddressing(mockHttpClient.getLastRequest(),
                                    String.format("https://%s.s3.dualstack.ap-south-1.amazonaws.com", BUCKET));
    }

    /**
     * Dualstack also supports path style endpoints just like the normal endpoints.
     */
    @Test
    public void dualstackAndPathStyleEnabled_UsesPathStyleAddressingWithDualstackEndpoint() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = buildClient(withDualstackAndPathStyleEnabled());

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertUsesPathStyleAddressing(mockHttpClient.getLastRequest(),
                                      "https://s3.dualstack.ap-south-1.amazonaws.com");
    }

    /**
     * When dualstack and accelerate are both enabled there is a special, global dualstack endpoint we must use.
     */
    @Test
    public void dualstackAndAccelerateEnabled_UsesDualstackAccelerateEndpoint() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = buildClient(withDualstackAndAccelerateEnabled());

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertUsesVirtualAddressing(mockHttpClient.getLastRequest(),
                                    String.format("https://%s.s3-accelerate.dualstack.amazonaws.com", BUCKET));
    }

    /**
     * Accelerate is not supported for several operations. For those we should go to the normal, regional endpoint.
     */
    @Test
    public void unsupportedAccelerateOption_UsesStandardEndpoint() throws Exception {
        mockHttpClient.stubNextResponse(mockListBucketsResponse());
        S3Client s3Client = buildClient(withAccelerateEnabled());

        s3Client.listBuckets();

        assertThat(mockHttpClient.getLastRequest().getEndpoint())
                .as("Uses regional S3 endpoint")
                .isEqualTo(URI.create("https://s3.ap-south-1.amazonaws.com"));
    }

    /**
     * Accelerate only supports virtual addressing. Path style cannot be used with accelerate enabled.
     */
    @Test(expected = IllegalArgumentException.class)
    public void accelerateAndPathStyleEnabled_ThrowsIllegalArgumentException() {
        buildClient(S3AdvancedConfiguration.builder()
                                           .pathStyleAccessEnabled(true)
                                           .accelerateModeEnabled(true)
                                           .build());
    }

    /**
     * Assert that path style addressing is used.
     *
     * @param capturedRequest Request captured by mock HTTP client.
     * @param endpoint        Expected endpoint.
     */
    private void assertUsesPathStyleAddressing(SdkHttpFullRequest capturedRequest, String endpoint) {
        assertThat(capturedRequest.getEndpoint())
                .as("Uses endpoint without bucket name prepended")
                .isEqualTo(URI.create(endpoint));

        assertThat(capturedRequest.getResourcePath())
                .as("Resource path is left as bucket name")
                .startsWith("/" + BUCKET);
    }

    /**
     * Asserts that virtual addressing is used.
     *
     * @param capturedRequest Request captured by mock HTTP client.
     * @param endpoint        Expected endpoint.
     */
    private void assertUsesVirtualAddressing(SdkHttpFullRequest capturedRequest, String endpoint) {
        assertThat(capturedRequest.getEndpoint())
                .as("Uses virtual addressing")
                .isEqualTo(URI.create(endpoint));

        assertThat(capturedRequest.getResourcePath())
                .as("Resource path has bucket removed.")
                .isEmpty();
    }

    /**
     * @param s3AdvancedConfiguration Advanced configuration to use for this client.
     * @return A built client with the given advanced configuration.
     */
    private S3Client buildClient(S3AdvancedConfiguration s3AdvancedConfiguration) {
        return clientBuilder()
                .advancedConfiguration(s3AdvancedConfiguration)
                .build();
    }

    /**
     * @return Client builder instance preconfigured with credentials and region using the {@link #mockHttpClient} for transport.
     */
    private S3ClientBuilder clientBuilder() {
        return S3Client.builder()
                       .credentialsProvider(new StaticCredentialsProvider(new AwsCredentials("akid", "skid")))
                       .region(Region.AP_SOUTH_1)
                       .httpConfiguration(ClientHttpConfiguration.builder()
                                                                 .httpClient(mockHttpClient)
                                                                 .build());
    }

    /**
     * @return S3AdvancedConfiguration with path style enabled.
     */
    private S3AdvancedConfiguration withPathStyle() {
        return S3AdvancedConfiguration.builder()
                                      .pathStyleAccessEnabled(true)
                                      .build();
    }

    /**
     * @return S3AdvancedConfiguration with accelerate mode enabled.
     */
    private S3AdvancedConfiguration withAccelerateEnabled() {
        return S3AdvancedConfiguration.builder()
                                      .accelerateModeEnabled(true)
                                      .build();
    }

    /**
     * @return S3AdvancedConfiguration with dualstack mode enabled.
     */
    private S3AdvancedConfiguration withDualstackEnabled() {
        return S3AdvancedConfiguration.builder()
                                      .dualstackEnabled(true)
                                      .build();
    }

    /**
     * @return S3AdvancedConfiguration with dualstack mode and path style enabled.
     */
    private S3AdvancedConfiguration withDualstackAndPathStyleEnabled() {
        return S3AdvancedConfiguration.builder()
                                      .dualstackEnabled(true)
                                      .pathStyleAccessEnabled(true)
                                      .build();
    }

    /**
     * @return S3AdvancedConfiguration with dualstack mode and accelerate mode enabled.
     */
    private S3AdvancedConfiguration withDualstackAndAccelerateEnabled() {
        return S3AdvancedConfiguration.builder()
                                      .dualstackEnabled(true)
                                      .accelerateModeEnabled(true)
                                      .build();
    }

}