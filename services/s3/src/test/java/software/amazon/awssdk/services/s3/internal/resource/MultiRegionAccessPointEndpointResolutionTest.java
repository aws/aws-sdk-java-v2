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

package software.amazon.awssdk.services.s3.internal.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.services.s3.S3MockUtils.mockListObjectsResponse;

import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.testutils.service.http.MockHttpClient;

/**
 * Functional tests for multi-region access point ARN
 */
public class MultiRegionAccessPointEndpointResolutionTest {

    private final static String MULTI_REGION_ARN = "arn:aws:s3::123456789012:accesspoint:mfzwi23gnjvgw.mrap";
    private final static URI MULTI_REGION_ENDPOINT =
        URI.create("https://mfzwi23gnjvgw.mrap.accesspoint.s3-global.amazonaws.com");
    private MockHttpClient mockHttpClient;

    @Before
    public void setup() {
        mockHttpClient = new MockHttpClient();
    }

    @Test
    public void multiRegionArn_correctlyRewritesEndpoint() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilder().serviceConfiguration(S3Configuration.builder().build()).build();
        s3Client.listObjects(ListObjectsRequest.builder().bucket(MULTI_REGION_ARN).build());
        assertEndpointMatches(mockHttpClient.getLastRequest(), MULTI_REGION_ENDPOINT.toString());
    }

    @Test
    public void multiRegionArn_useArnRegionEnabled_correctlyRewritesEndpoint() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilder().serviceConfiguration(S3Configuration.builder()
                                                                                .useArnRegionEnabled(true)
                                                                                .build())
                                           .build();
        s3Client.listObjects(ListObjectsRequest.builder().bucket(MULTI_REGION_ARN).build());
        assertEndpointMatches(mockHttpClient.getLastRequest(), MULTI_REGION_ENDPOINT.toString());
    }

    @Test
    public void multiRegionArn_customEndpoint_throwsIllegalArgumentException() throws Exception {
        URI customEndpoint = URI.create("https://foobar.amazonaws.com");
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilder().endpointOverride(customEndpoint).build();

        assertThatThrownBy(() -> s3Client.listObjects(ListObjectsRequest.builder().bucket(MULTI_REGION_ARN).build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("endpoint override");
    }

    @Test
    public void multiRegionArn_dualstackEnabled_throwsIllegalArgumentException() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilder().serviceConfiguration(S3Configuration.builder()
                                                                                .dualstackEnabled(true)
                                                                                .build())
                                           .build();

        assertThatThrownBy(() -> s3Client.listObjects(ListObjectsRequest.builder().bucket(MULTI_REGION_ARN).build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dualstack");
    }

    @Test
    public void multiRegionArn_fipsRegion_throwsIllegalArgumentException() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilder().region(Region.of("fips-us-east-1"))
                                           .serviceConfiguration(S3Configuration.builder()
                                                                                .dualstackEnabled(false)
                                                                                .build())
                                           .build();

        assertThatThrownBy(() -> s3Client.listObjects(ListObjectsRequest.builder().bucket(MULTI_REGION_ARN).build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FIPS");
    }

    @Test
    public void multiRegionArn_accelerateEnabled_throwsIllegalArgumentException() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilder().serviceConfiguration(S3Configuration.builder()
                                                                                .accelerateModeEnabled(true)
                                                                                .build())
                                           .build();

        assertThatThrownBy(() -> s3Client.listObjects(ListObjectsRequest.builder().bucket(MULTI_REGION_ARN).build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("accelerate");
    }

    @Test
    public void multiRegionArn_pathStyle_throwsIllegalArgumentException() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilder().serviceConfiguration(S3Configuration.builder()
                                                                                .pathStyleAccessEnabled(true)
                                                                                .build())
                                           .build();

        assertThatThrownBy(() -> s3Client.listObjects(ListObjectsRequest.builder().bucket(MULTI_REGION_ARN).build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("path style addressing");
    }

    @Test
    public void multiRegionArn_differentRegion_useArnRegionTrue() throws Exception {
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        S3Client s3Client = clientBuilder().build();
        s3Client.listObjects(ListObjectsRequest.builder().bucket(MULTI_REGION_ARN).build());

        assertEndpointMatches(mockHttpClient.getLastRequest(), MULTI_REGION_ENDPOINT.toString());
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
     * @return Client builder instance preconfigured with credentials and region using the {@link #mockHttpClient} for transport.
     */
    private S3ClientBuilder clientBuilder() {
        return S3Client.builder()
                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                       .region(Region.AP_SOUTH_1)
                       .httpClient(mockHttpClient);
    }
}
