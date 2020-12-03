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

package software.amazon.awssdk.services.s3;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.services.s3.S3MockUtils.mockListBucketsResponse;
import static software.amazon.awssdk.services.s3.S3MockUtils.mockListObjectsResponse;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.handlers.EndpointAddressInterceptor;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.testutils.service.http.MockHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Functional tests for various endpoint related behavior in S3.
 */
public class S3EndpointResolutionTest {

    private static final String BUCKET = "some-bucket";
    private static final String NON_DNS_COMPATIBLE_BUCKET = "SOME.BUCKET";
    private static final String ENDPOINT_WITHOUT_BUCKET = "https://s3.ap-south-1.amazonaws.com";
    private static final String ENDPOINT_WITH_BUCKET = String.format("https://%s.s3.ap-south-1.amazonaws.com", BUCKET);

    private MockHttpClient mockHttpClient;
    private Signer mockSigner;

    @Before
    public void setup() {
        mockHttpClient = new MockHttpClient();
        mockSigner = (request, executionAttributes) -> request;
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

        assertThat(mockHttpClient.getLastRequest().getUri())
                .as("Uses regional S3 endpoint without bucket")
                .isEqualTo(URI.create(ENDPOINT_WITHOUT_BUCKET + "/"));

        assertThat(mockHttpClient.getLastRequest().encodedPath())
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

        assertThat(mockHttpClient.getLastRequest().getUri())
                .as("Uses regional S3 endpoint without bucket")
                .isEqualTo(URI.create("https://s3.dualstack.ap-south-1.amazonaws.com/"));
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

        assertThat(mockHttpClient.getLastRequest().getUri())
                .as("Uses custom endpoint")
                .isEqualTo(URI.create(customEndpoint + "/"));
    }

    @Test
    public void accessPointArn_correctlyRewritesEndpoint() throws Exception {
        URI customEndpoint = URI.create("https://foobar-12345678910.s3-accesspoint.ap-south-1.amazonaws.com");
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilder().build();
        String accessPointArn = "arn:aws:s3:ap-south-1:12345678910:accesspoint:foobar";

        s3Client.listObjects(ListObjectsRequest.builder().bucket(accessPointArn).build());

        assertEndpointMatches(mockHttpClient.getLastRequest(), customEndpoint.toString());
    }

    @Test
    public void accessPointArn_customEndpoint_throwsIllegalArgumentException() throws Exception {
        URI customEndpoint = URI.create("https://foobar.amazonaws.com");
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilder().endpointOverride(customEndpoint).build();
        String accessPointArn = "arn:aws:s3:ap-south-1:12345678910:accesspoint:foobar";

        assertThatThrownBy(() -> s3Client.listObjects(ListObjectsRequest.builder().bucket(accessPointArn).build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("endpoint override");
    }

    @Test
    public void accessPointArn_differentRegion_useArnRegionFalse_throwsIllegalArgumentException() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilder().build();
        String accessPointArn = "arn:aws:s3:us-west-2:12345678910:accesspoint:foobar";

        assertThatThrownBy(() -> s3Client.listObjects(ListObjectsRequest.builder().bucket(accessPointArn).build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("region");
    }

    @Test
    public void accessPointArn_differentRegion_useArnRegionTrue() throws Exception {
        URI customEndpoint = URI.create("https://foobar-12345678910.s3-accesspoint.us-west-2.amazonaws.com");
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilder().serviceConfiguration(b -> b.useArnRegionEnabled(true)).build();
        String accessPointArn = "arn:aws:s3:us-west-2:12345678910:accesspoint:foobar";

        s3Client.listObjects(ListObjectsRequest.builder().bucket(accessPointArn).build());

        assertEndpointMatches(mockHttpClient.getLastRequest(), customEndpoint.toString());
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

        assertEndpointMatches(mockHttpClient.getLastRequest(), customEndpoint.toString() + "/" + BUCKET);
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

        assertEndpointMatches(mockHttpClient.getLastRequest(),
                              String.format("https://%s.s3-external-1.amazonaws.com", BUCKET));
    }

    /**
     * If customer is using HTTP we need to preserve that scheme when switching to virtual addressing.
     */
    @Test
    public void customHttpEndpoint_PreservesSchemeWhenSwitchingToVirtualAddressing() throws Exception {
        URI customEndpoint = URI.create("http://s3-external-1.amazonaws.com");
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilderWithMockSigner().endpointOverride(customEndpoint).build();

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertEndpointMatches(mockHttpClient.getLastRequest(),
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

        assertEndpointMatches(mockHttpClient.getLastRequest(), ENDPOINT_WITHOUT_BUCKET + "/" + NON_DNS_COMPATIBLE_BUCKET);
    }

    /**
     * When path style is enabled in the advanced configuration we should always use it.
     */
    @Test
    public void pathStyleConfigured_UsesPathStyleAddressing() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = buildClient(withPathStyle());

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertEndpointMatches(mockHttpClient.getLastRequest(), ENDPOINT_WITHOUT_BUCKET + "/" + BUCKET);
    }

    /**
     * By default we use virtual addressing when possible.
     */
    @Test
    public void noServiceConfigurationProvided_UsesVirtualAddressingWithStandardEndpoint() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = buildClient(null);

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertEndpointMatches(mockHttpClient.getLastRequest(), ENDPOINT_WITH_BUCKET);
    }

    /**
     * By default we use virtual addressing when possible.
     */
    @Test
    public void emptyServiceConfigurationProvided_UsesVirtualAddressingWithStandardEndpoint() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = buildClient(S3Configuration.builder().build());

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertEndpointMatches(mockHttpClient.getLastRequest(), ENDPOINT_WITH_BUCKET);
    }

    /**
     * S3 accelerate has a global endpoint, we use that when accelerate mode is enabled in the advanced configuration.
     */
    @Test
    public void accelerateEnabled_UsesVirtualAddressingWithAccelerateEndpoint() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = buildClient(withAccelerateEnabled());

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertEndpointMatches(mockHttpClient.getLastRequest(),
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

        assertEndpointMatches(mockHttpClient.getLastRequest(),
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

        assertEndpointMatches(mockHttpClient.getLastRequest(), "https://s3.dualstack.ap-south-1.amazonaws.com/" + BUCKET);
    }

    /**
     * When dualstack and accelerate are both enabled there is a special, global dualstack endpoint we must use.
     */
    @Test
    public void dualstackAndAccelerateEnabled_UsesDualstackAccelerateEndpoint() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = buildClient(withDualstackAndAccelerateEnabled());

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());

        assertEndpointMatches(mockHttpClient.getLastRequest(),
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

        assertThat(mockHttpClient.getLastRequest().getUri())
                .as("Uses regional S3 endpoint")
                .isEqualTo(URI.create("https://s3.ap-south-1.amazonaws.com/"));
    }

    /**
     * Accelerate only supports virtual addressing. Path style cannot be used with accelerate enabled.
     */
    @Test(expected = IllegalArgumentException.class)
    public void accelerateAndPathStyleEnabled_ThrowsIllegalArgumentException() {
        buildClient(S3Configuration.builder()
                                   .pathStyleAccessEnabled(true)
                                   .accelerateModeEnabled(true)
                                   .build());
    }

    @Test
    public void regionalSettingEnabled_usesRegionalIadEndpoint() throws UnsupportedEncodingException {
        EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();
        environmentVariableHelper.set(SdkSystemSetting.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.environmentVariable(), "regional");

        mockHttpClient.stubNextResponse(mockListObjectsResponse());

        S3Client s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                .httpClient(mockHttpClient)
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
        try {
            s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());
            assertThat(mockHttpClient.getLastRequest().getUri().getHost()).isEqualTo("s3.us-east-1.amazonaws.com");
        } finally {
            environmentVariableHelper.reset();
        }
    }

    @Test
    public void regionalSettingEnabledViaProfile_usesRegionalIadEndpoint() throws UnsupportedEncodingException {
        String profile =
            "[profile test]\n" +
            "s3_us_east_1_regional_endpoint = regional";

        ProfileFile profileFile = ProfileFile.builder()
                                             .content(new StringInputStream(profile))
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();

        mockHttpClient.stubNextResponse(mockListObjectsResponse());

        S3Client s3Client = S3Client.builder()
                                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                    .httpClient(mockHttpClient)
                                    .region(Region.US_EAST_1)
                                    .overrideConfiguration(c -> c.defaultProfileFile(profileFile)
                                                                 .defaultProfileName("test"))
                                    .serviceConfiguration(c -> c.pathStyleAccessEnabled(true))
                                    .build();

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());
        assertThat(mockHttpClient.getLastRequest().getUri().getHost()).isEqualTo("s3.us-east-1.amazonaws.com");
    }

    @Test
    public void regionalSettingDisabled_usesGlobalEndpoint() throws UnsupportedEncodingException {
        EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();
        environmentVariableHelper.set(SdkSystemSetting.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.environmentVariable(), "nonregional");

        mockHttpClient.stubNextResponse(mockListObjectsResponse());

        S3Client s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                .httpClient(mockHttpClient)
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
        try {
            s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());
            assertThat(mockHttpClient.getLastRequest().getUri().getHost()).isEqualTo("s3.amazonaws.com");
        } finally {
            environmentVariableHelper.reset();
        }
    }

    @Test
    public void regionalSettingUnset_usesGlobalEndpoint() throws UnsupportedEncodingException {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());

        S3Client s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                .httpClient(mockHttpClient)
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();

        s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET).build());
        assertThat(mockHttpClient.getLastRequest().getUri().getHost()).isEqualTo("s3.amazonaws.com");
    }

    /**
     * Assert that the provided request would have gone to the given endpoint.
     *
     * @param capturedRequest Request captured by mock HTTP client.
     * @param endpoint        Expected endpoint.
     */
    private void assertEndpointMatches(SdkHttpRequest capturedRequest, String endpoint) {
        assertThat(capturedRequest.getUri()).isEqualTo(URI.create(endpoint));
    }

    /**
     * @param s3ServiceConfiguration Advanced configuration to use for this client.
     * @return A built client with the given advanced configuration.
     */
    private S3Client buildClient(S3Configuration s3ServiceConfiguration) {
        return clientBuilder()
                .serviceConfiguration(s3ServiceConfiguration)
                .build();
    }

    /**
     * @return Client builder instance preconfigured with credentials and region using the {@link #mockHttpClient} for transport.
     */
    private S3ClientBuilder clientBuilder() {
        return S3Client.builder()
                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                       .region(Region.AP_SOUTH_1)
                       .httpClient(mockHttpClient);
    }

    /**
     * @return Client builder instance preconfigured with credentials and region using the {@link #mockHttpClient} for transport
     * and {@link #mockSigner} for signing. Using actual AwsS3V4Signer results in NPE as the execution goes into payload signing
     * due to "http" protocol and input stream is not mark supported.
     */
    private S3ClientBuilder clientBuilderWithMockSigner() {
        return S3Client.builder()
                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                       .region(Region.AP_SOUTH_1)
                       .overrideConfiguration(ClientOverrideConfiguration.builder()
                                                                         .putAdvancedOption(SdkAdvancedClientOption.SIGNER,
                                                                                            mockSigner)
                                                                         .build())
                       .httpClient(mockHttpClient);
    }

    /**
     * @return S3Configuration with path style enabled.
     */
    private S3Configuration withPathStyle() {
        return S3Configuration.builder()
                              .pathStyleAccessEnabled(true)
                              .build();
    }

    /**
     * @return S3Configuration with accelerate mode enabled.
     */
    private S3Configuration withAccelerateEnabled() {
        return S3Configuration.builder()
                              .accelerateModeEnabled(true)
                              .build();
    }

    /**
     * @return S3Configuration with dualstack mode enabled.
     */
    private S3Configuration withDualstackEnabled() {
        return S3Configuration.builder()
                              .dualstackEnabled(true)
                              .build();
    }

    /**
     * @return S3Configuration with dualstack mode and path style enabled.
     */
    private S3Configuration withDualstackAndPathStyleEnabled() {
        return S3Configuration.builder()
                              .dualstackEnabled(true)
                              .pathStyleAccessEnabled(true)
                              .build();
    }

    /**
     * @return S3Configuration with dualstack mode and accelerate mode enabled.
     */
    private S3Configuration withDualstackAndAccelerateEnabled() {
        return S3Configuration.builder()
                              .dualstackEnabled(true)
                              .accelerateModeEnabled(true)
                              .build();
    }

}
