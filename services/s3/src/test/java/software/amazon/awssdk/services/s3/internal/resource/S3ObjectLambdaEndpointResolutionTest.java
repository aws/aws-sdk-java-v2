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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

public class S3ObjectLambdaEndpointResolutionTest {

    private MockSyncHttpClient mockHttpClient;

    @BeforeEach
    public void setup() throws UnsupportedEncodingException {
        mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
    }

    // Invalid endpoints tests

    @Test
    public void objectLambdaArn_crossRegionArn_throwsIllegalArgumentException() {
        S3Client s3Client = clientBuilder().build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-east-1:123456789012:accesspoint/myol";

        assertThatThrownBy(() -> s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid configuration: region from ARN `us-east-1` does not match client region `us-west-2` and UseArnRegion is `false`");

    }

    @Test
    public void objectLambdaArn_dualstackEnabled_throwsIllegalArgumentException() {
        S3Client s3Client = clientBuilder().serviceConfiguration(S3Configuration.builder()
                                                                                .dualstackEnabled(true)
                                                                                .useArnRegionEnabled(true)
                                                                                .build()).build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint/myol";

        assertThatThrownBy(() -> s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("S3 Object Lambda does not support Dual-stack");
    }

    @Test
    public void objectLambdaArn_crossPartition_throwsIllegalArgumentException() {
        S3Client s3Client = clientBuilder().serviceConfiguration(S3Configuration.builder()
                                                                                .useArnRegionEnabled(true)
                                                                                .build()).build();
        String objectLambdaArn = "arn:aws-cn:s3-object-lambda:cn-north-1:123456789012:accesspoint/myol";

        assertThatThrownBy(() -> s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Client was configured for partition `aws` but ARN (`arn:aws-cn:s3-object-lambda:cn-north-1:123456789012:accesspoint/myol`) has `aws-cn`");
    }

    @Test
    public void objectLambdaArn_accelerateEnabled_throwsIllegalArgumentException() {
        S3Client s3Client = clientBuilder().serviceConfiguration(S3Configuration.builder()
                                                                                .accelerateModeEnabled(true)
                                                                                .build()).build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint/myol";

        assertThatThrownBy(() -> s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("S3 Object Lambda does not support S3 Accelerate");
    }

    @Test
    public void objectLambdaArn_nonS3Arn_throwsIllegalArgumentException() {
        S3Client s3Client = clientBuilder().build();
        String objectLambdaArn = "arn:aws:sqs:us-west-2:123456789012:someresource";

        assertThatThrownBy(() -> s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid ARN: Unrecognized format: arn:aws:sqs:us-west-2:123456789012:someresource (type: someresource)");
    }

    @Test
    public void objectLambdaArn_nonAccessPointArn_throwsIllegalArgumentException() {
        S3Client s3Client = clientBuilder().build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-west-2:123456789012:bucket_name:mybucket";

        assertThatThrownBy(() -> s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid ARN: Object Lambda ARNs only support `accesspoint` arn types, but found: `bucket_name`");
    }

    @Test
    public void objectLambdaArn_missingRegion_throwsNullPointerException() {
        S3Client s3Client = clientBuilder().build();
        String objectLambdaArn = "arn:aws:s3-object-lambda::123456789012:accesspoint/myol";

        assertThatThrownBy(() -> s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid ARN: bucket ARN is missing a region");
    }

    @Test
    public void objectLambdaArn_missingAccountId_throwsNullPointerException() {
        S3Client s3Client = clientBuilder().build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-west-2::accesspoint/myol";

        assertThatThrownBy(() -> s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid ARN: Missing account id");
    }

    @Test
    public void objectLambdaArn_accoutIdContainsInvalidCharacters_throwsIllegalArgumentException() {
        S3Client s3Client = clientBuilder().build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-west-2:123.45678.9012:accesspoint:mybucket";

        assertThatThrownBy(() -> s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid ARN: The account id may only contain a-z, A-Z, 0-9 and `-`. Found: `123.45678.9012`");
    }

    @Test
    public void objectLambdaArn_missingAccessPointName_throwsIllegalArgumentException() {
        S3Client s3Client = clientBuilder().build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint";

        assertThatThrownBy(() -> s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid ARN: Expected a resource of the format `accesspoint:<accesspoint name>` but no name was provided");
    }

    @Test
    public void objectLambdaArn_accessPointNameContainsInvalidCharacters_star_throwsIllegalArgumentException() {
        S3Client s3Client = clientBuilder().build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint:*";

        assertThatThrownBy(() -> s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `*`");
    }

    @Test
    public void objectLambdaArn_accessPointNameContainsInvalidCharacters_dot_throwsIllegalArgumentException() {
        S3Client s3Client = clientBuilder().build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint:my.bucket";

        assertThatThrownBy(() -> s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `my.bucket`");
    }

    @Test
    public void objectLambdaArn_accessPointNameContainsSubResources_throwsIllegalArgumentException() {
        S3Client s3Client = clientBuilder().build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint:mybucket:object:foo";

        assertThatThrownBy(() -> s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid ARN: The ARN may only contain a single resource component after `accesspoint`.");
    }

    // Valid endpoint tests

    @Test
    public void objectLambdaArn_normalCase_slashDelimiter_resolveEndpointCorrectly() {
        URI expectedEndpoint = URI.create("myol-123456789012.s3-object-lambda.us-west-2.amazonaws.com");
        S3Client s3Client = clientBuilder().build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint/myol";

        s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build());
        assertEndpointMatches(mockHttpClient.getLastRequest(), expectedEndpoint);
    }

    @Test
    public void objectLambdaArn_normalCase_colonDelimiter_resolveEndpointCorrectly() {
        URI expectedEndpoint = URI.create("myol-123456789012.s3-object-lambda.us-west-2.amazonaws.com");
        S3Client s3Client = clientBuilder().build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint:myol";

        s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build());
        assertEndpointMatches(mockHttpClient.getLastRequest(), expectedEndpoint);
    }

    @Test
    public void objectLambdaArn_fips_resolveEndpointCorrectly() {
        URI expectedEndpoint = URI.create("myol-123456789012.s3-object-lambda-fips.us-west-2.amazonaws.com");
        S3Client s3Client = clientBuilder().fipsEnabled(true).build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint/myol";

        s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build());
        assertEndpointMatches(mockHttpClient.getLastRequest(), expectedEndpoint);
    }

    @Test
    public void objectLambdaArn_crossRegion_useArnRegionTrue_resolveEndpointCorrectly() {
        URI expectedEndpoint = URI.create("myol-123456789012.s3-object-lambda.us-east-1.amazonaws.com");
        S3Client s3Client = clientBuilder().serviceConfiguration(S3Configuration.builder()
                                                                                .useArnRegionEnabled(true)
                                                                                .build()).build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-east-1:123456789012:accesspoint/myol";

        s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build());
        assertEndpointMatches(mockHttpClient.getLastRequest(), expectedEndpoint);
    }

    @Test
    public void objectLambdaArn_externalRegion_useArnRegionTrue_resolveEndpointCorrectly() {
        URI expectedEndpoint = URI.create("myol-123456789012.s3-object-lambda.us-east-1.amazonaws.com");
        S3Client s3Client = clientBuilder().region(Region.of("s3-external-1"))
                                           .serviceConfiguration(S3Configuration.builder()
                                                                                .useArnRegionEnabled(true)
                                                                                .build())
                                           .build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-east-1:123456789012:accesspoint/myol";

        s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build());
        assertEndpointMatches(mockHttpClient.getLastRequest(), expectedEndpoint);
    }

    @Test
    public void objectLambdaArn_globalRegion_useArnRegionTrue_resolveEndpointCorrectly() {
        URI expectedEndpoint = URI.create("myol-123456789012.s3-object-lambda.us-east-1.amazonaws.com");
        S3Client s3Client = clientBuilder().region(Region.of("aws-global"))
                                           .serviceConfiguration(S3Configuration.builder()
                                                                                .useArnRegionEnabled(true)
                                                                                .build())
                                           .build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-east-1:123456789012:accesspoint/myol";

        s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build());
        assertEndpointMatches(mockHttpClient.getLastRequest(), expectedEndpoint);
    }

    @Test
    public void objectLambdaArn_cnPartitionSameRegion_useArnRegionTrue_resolveEndpointCorrectly() {
        URI expectedEndpoint = URI.create("myol-123456789012.s3-object-lambda.cn-north-1.amazonaws.com.cn");
        S3Client s3Client = clientBuilder().region(Region.of("cn-north-1"))
                                           .serviceConfiguration(S3Configuration.builder()
                                                                                .useArnRegionEnabled(true)
                                                                                .build())
                                           .build();
        String objectLambdaArn = "arn:aws-cn:s3-object-lambda:cn-north-1:123456789012:accesspoint/myol";

        s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build());
        assertEndpointMatches(mockHttpClient.getLastRequest(), expectedEndpoint);
    }

    @Test
    public void objectLambdaArn_cnPartitionSameRegion_useArnRegionFalse_resolveEndpointCorrectly() {
        URI expectedEndpoint = URI.create("myol-123456789012.s3-object-lambda.cn-north-1.amazonaws.com.cn");
        S3Client s3Client = clientBuilder().region(Region.of("cn-north-1")).build();
        String objectLambdaArn = "arn:aws-cn:s3-object-lambda:cn-north-1:123456789012:accesspoint/myol";

        s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build());
        assertEndpointMatches(mockHttpClient.getLastRequest(), expectedEndpoint);
    }

    @Test
    public void objectLambdaArn_cnPartitionCrossRegion_useArnRegionTrue_resolveEndpointCorrectly() {
        URI expectedEndpoint = URI.create("myol-123456789012.s3-object-lambda.cn-northwest-1.amazonaws.com.cn");
        S3Client s3Client = clientBuilder().region(Region.of("cn-north-1"))
                                           .serviceConfiguration(S3Configuration.builder()
                                                                                .useArnRegionEnabled(true)
                                                                                .build())
                                           .build();
        String objectLambdaArn = "arn:aws-cn:s3-object-lambda:cn-northwest-1:123456789012:accesspoint/myol";

        s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build());
        assertEndpointMatches(mockHttpClient.getLastRequest(), expectedEndpoint);
    }

    @Test
    public void objectLambdaArn_govRegion_useArnRegionTrue_resolveEndpointCorrectly() {
        URI expectedEndpoint = URI.create("myol-123456789012.s3-object-lambda.us-gov-east-1.amazonaws.com");
        S3Client s3Client = clientBuilder().region(Region.of("us-gov-east-1"))
                                           .serviceConfiguration(S3Configuration.builder()
                                                                                .useArnRegionEnabled(true)
                                                                                .build())
                                           .build();
        String objectLambdaArn = "arn:aws-us-gov:s3-object-lambda:us-gov-east-1:123456789012:accesspoint/myol";

        s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build());
        assertEndpointMatches(mockHttpClient.getLastRequest(), expectedEndpoint);
    }

    @Test
    public void objectLambdaArn_customizeEndpoint_resolveEndpointCorrectly() throws Exception {
        URI customEndpoint = URI.create("myol-123456789012.my-endpoint.com");
        S3Client s3Client = clientBuilder().endpointOverride(URI.create("http://my-endpoint.com")).build();
        String objectLambdaArn = "arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint/myol";

        s3Client.getObject(GetObjectRequest.builder().bucket(objectLambdaArn).key("obj").build());
        assertEndpointMatches(mockHttpClient.getLastRequest(), customEndpoint);
    }

    /**
     * Assert that the provided request would have gone to the given endpoint.
     *
     * @param capturedRequest Request captured by mock HTTP client.
     * @param endpoint        Expected endpoint.
     */
    private void assertEndpointMatches(SdkHttpRequest capturedRequest, URI endpoint) {
        assertThat(capturedRequest.host()).isEqualTo(endpoint.toString());
    }

    /**
     * @return Client builder instance preconfigured with credentials and region using the {@link #mockHttpClient} for transport.
     */
    private S3ClientBuilder clientBuilder() {
        return S3Client.builder()
                       .credentialsProvider(StaticCredentialsProvider
                                                .create(AwsBasicCredentials.create("akid", "skid")))
                       .region(Region.US_WEST_2)
                       .httpClient(mockHttpClient);
    }

}
