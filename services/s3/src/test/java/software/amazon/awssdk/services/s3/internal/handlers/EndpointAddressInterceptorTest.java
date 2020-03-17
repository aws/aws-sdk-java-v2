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

package software.amazon.awssdk.services.s3.internal.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SIGNING_REGION;
import static software.amazon.awssdk.awscore.AwsExecutionAttribute.AWS_REGION;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.SERVICE_CONFIG;
import static software.amazon.awssdk.utils.http.SdkHttpUtils.urlEncode;

import java.net.URI;
import java.util.Optional;
import org.junit.Test;

import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class EndpointAddressInterceptorTest {

    private final EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();

    @Test
    public void traditionalEndpoint_shouldNotConvertEndpoint() {
        verifyEndpoint("http", "http://s3-test.com",
                       S3Configuration.builder());

        verifyEndpoint("https", "https://s3-test.com",
                       S3Configuration.builder());
    }

    @Test
    public void accelerateEnabled_shouldConvertToAccelerateEndpoint() {
        verifyEndpoint("http", "http://s3-accelerate.amazonaws.com",
                       S3Configuration.builder().accelerateModeEnabled(true));
        verifyEndpoint("https", "https://s3-accelerate.amazonaws.com",
                       S3Configuration.builder().accelerateModeEnabled(true));
    }

    @Test
    public void bothAccelerateDualstackEnabled_shouldConvertToAccelerateDualstackEndpoint() {
        S3Configuration.Builder configurationBuilder = S3Configuration.builder()
                                                                      .dualstackEnabled(true)
                                                                      .accelerateModeEnabled(true);
        verifyEndpoint("http",
                       "http://s3-accelerate.dualstack.amazonaws.com",
                       S3Configuration.builder()
                                                                    .accelerateModeEnabled(true)
                                                                    .dualstackEnabled(true)
        );
        verifyEndpoint("https",
                       "https://s3-accelerate.dualstack.amazonaws.com",
                       configurationBuilder);
    }

    @Test
    public void accelerateEnabled_ListBucketRequest_shouldNotConvertToAccelerateEndpoint() {
        verifyAccelerateDisabledOperationsEndpointNotConverted(ListBucketsRequest.builder().build());
    }

    @Test
    public void accelerateEnabled_CreateBucketsRequest_shouldNotConvertToAccelerateEndpoint() {
        verifyAccelerateDisabledOperationsEndpointNotConverted(CreateBucketRequest.builder().build());
    }

    @Test
    public void accelerateEnabled_DeleteBucketRequest_shouldNotConvertToAccelerateEndpoint() {
        verifyAccelerateDisabledOperationsEndpointNotConverted(DeleteBucketRequest.builder().build());
    }

    @Test
    public void dualstackEnabled_shouldConvertToDualstackEndpoint() {
        verifyEndpoint("http", "http://s3.dualstack.us-east-1.amazonaws.com",
                       S3Configuration.builder().dualstackEnabled(true));
        verifyEndpoint("https", "https://s3.dualstack.us-east-1.amazonaws.com",
                       S3Configuration.builder().dualstackEnabled(true));
    }

    @Test
    public void virtualStyle_shouldConvertToDnsEndpoint() {
        verifyVirtualStyleConvertDnsEndpoint("https");
        verifyVirtualStyleConvertDnsEndpoint("http");
    }

    @Test
    public void pathStyleAccessEnabled_shouldNotConvertToDnsEndpoint() {
        verifyEndpoint("http", "http://s3-test.com",
                       S3Configuration.builder().pathStyleAccessEnabled(true));
        verifyEndpoint("https", "https://s3-test.com",
                       S3Configuration.builder().pathStyleAccessEnabled(true));
    }

    @Test
    public void accesspointArn_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint:foobar",
                             "http://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                             S3Configuration.builder());
        verifyAccesspointArn("https",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint:foobar",
                             "https://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                             S3Configuration.builder());
    }

    @Test
    public void accesspointArn_futureUnknownRegion_US_correctlyInfersPartition() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:us-future-1:12345678910:accesspoint:foobar",
                             "http://foobar-12345678910.s3-accesspoint.us-future-1.amazonaws.com",
                             Region.of("us-future-1"),
                             S3Configuration.builder(),
                             Region.of("us-future-1"));
    }

    @Test
    public void accesspointArn_futureUnknownRegion_crossRegion_correctlyInfersPartition() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:us-future-2:12345678910:accesspoint:foobar",
                             "http://foobar-12345678910.s3-accesspoint.us-future-2.amazonaws.com",
                             Region.of("us-future-2"),
                             S3Configuration.builder().useArnRegionEnabled(true),
                             Region.of("us-future-1"));
    }

    @Test
    public void accesspointArn_futureUnknownRegion_CN_correctlyInfersPartition() {
        verifyAccesspointArn("http",
                             "arn:aws-cn:s3:cn-future-1:12345678910:accesspoint:foobar",
                             "http://foobar-12345678910.s3-accesspoint.cn-future-1.amazonaws.com.cn",
                             Region.of("cn-future-1"),
                             S3Configuration.builder(),
                             Region.of("cn-future-1"));
    }

    @Test
    public void accesspointArn_futureUnknownRegionAndPartition_defaultsToAws() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:unknown:12345678910:accesspoint:foobar",
                             "http://foobar-12345678910.s3-accesspoint.unknown.amazonaws.com",
                             Region.of("unknown"),
                             S3Configuration.builder(),
                             Region.of("unknown"));
    }

    @Test
    public void malformedArn_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                             "arn:foobar",
                             null,
                             S3Configuration.builder()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ARN");
    }

    @Test
    public void unsupportedArn_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3:us-east-1:12345678910:unsupported:foobar",
                                                      null,
                                                      S3Configuration.builder()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ARN");
    }

    @Test
    public void accesspointArn_invalidPartition_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:bar:s3:us-east-1:12345678910:accesspoint:foobar",
                                                      null,
                                                      S3Configuration.builder()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bar");
    }

    @Test
    public void bucketArn_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3:us-east-1:12345678910:bucket_name:foobar",
                                                      null,
                                                      S3Configuration.builder()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bucket parameter");
    }


    @Test
    public void accesspointArn_withSlashes_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "http://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                             S3Configuration.builder());
        verifyAccesspointArn("https",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "https://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                             S3Configuration.builder());
    }

    @Test
    public void accesspointArn_withDualStackEnabled_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "http://foobar-12345678910.s3-accesspoint.dualstack.us-east-1.amazonaws.com",
                             S3Configuration.builder().dualstackEnabled(true));
        verifyAccesspointArn("https",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "https://foobar-12345678910.s3-accesspoint.dualstack.us-east-1.amazonaws.com",
                             S3Configuration.builder().dualstackEnabled(true));
    }

    @Test
    public void accesspointArn_withCnPartition_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws-cn:s3:cn-north-1:12345678910:accesspoint:foobar",
                             "http://foobar-12345678910.s3-accesspoint.cn-north-1.amazonaws.com.cn",
                             Region.of("cn-north-1"),
                             S3Configuration.builder(),
                             Region.of("cn-north-1"));
        verifyAccesspointArn("https",
                             "arn:aws-cn:s3:cn-north-1:12345678910:accesspoint:foobar",
                             "https://foobar-12345678910.s3-accesspoint.cn-north-1.amazonaws.com.cn",
                             Region.of("cn-north-1"),
                             S3Configuration.builder(),
                             Region.of("cn-north-1"));
    }

    @Test
    public void accesspointArn_withDifferentPartition_useArnRegionEnabled_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws-cn:s3:cn-north-1:12345678910:accesspoint:foobar",
                                                      "http://foobar-12345678910.s3-accesspoint.cn-north-1.amazonaws.com.cn",
                                                      Region.of("cn-north-1"),
                                                      S3Configuration.builder().useArnRegionEnabled(true),
                                                      Region.of("us-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("partition");
    }

    @Test
    public void accesspointArn_withFipsRegionPrefix_useArnRegionEnabled_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "http://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                             Region.of("us-east-1"),
                             S3Configuration.builder().useArnRegionEnabled(true),
                             Region.of("fips-us-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FIPS");
        assertThatThrownBy(() -> verifyAccesspointArn("https",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "https://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                             Region.of("us-east-1"),
                             S3Configuration.builder().useArnRegionEnabled(true),
                             Region.of("fips-us-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FIPS");
    }

    @Test
    public void accesspointArn_withFipsRegionPrefix_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                                                      "http://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder(),
                                                      Region.of("fips-us-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FIPS");
        assertThatThrownBy(() -> verifyAccesspointArn("https",
                                                      "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                                                      "https://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder(),
                                                      Region.of("fips-us-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FIPS");
    }

    @Test
    public void accesspointArn_withFipsRegionSuffix_useArnRegionEnabled_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                                                      "http://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder().useArnRegionEnabled(true),
                                                      Region.of("us-east-1-fips")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FIPS");
        assertThatThrownBy(() -> verifyAccesspointArn("https",
                                                      "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                                                      "https://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder().useArnRegionEnabled(true),
                                                      Region.of("us-east-1-fips")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FIPS");
    }

    @Test
    public void accesspointArn_withFipsRegionSuffix_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                                                      "http://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder(),
                                                      Region.of("us-east-1-fips")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FIPS");
        assertThatThrownBy(() -> verifyAccesspointArn("https",
                                                      "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                                                      "https://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder(),
                                                      Region.of("us-east-1-fips")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FIPS");
    }

    @Test
    public void accesspointArn_withAccelerateEnabled_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                                                      "http://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder().accelerateModeEnabled(true),
                                                      Region.of("us-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("accelerate");
    }


    @Test
    public void accesspointArn_withPathStyleAddressingEnabled_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                                                      "http://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder().pathStyleAccessEnabled(true),
                                                      Region.of("us-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("path style");
    }

    private void verifyVirtualStyleConvertDnsEndpoint(String protocol) {
        String bucketName = "test-bucket";
        String key = "test-key";
        URI customUri = URI.create(String.format("%s://s3-test.com/%s/%s", protocol, bucketName, key));
        URI expectedUri = URI.create(String.format("%s://%s.s3.dualstack.us-east-1.amazonaws.com/%s", protocol,
                                                   bucketName, key));

        Context.ModifyHttpRequest ctx = context(ListObjectsV2Request.builder().bucket(bucketName).build(),
                                                sdkHttpRequest(customUri));
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        S3Configuration s3Configuration = S3Configuration.builder().dualstackEnabled(true).build();

        executionAttributes.putAttribute(SERVICE_CONFIG, s3Configuration);
        executionAttributes.putAttribute(AWS_REGION, Region.US_EAST_1);

        SdkHttpRequest sdkHttpFullRequest = interceptor.modifyHttpRequest(ctx, executionAttributes);

        assertThat(sdkHttpFullRequest.getUri()).isEqualTo(expectedUri);
    }

    private SdkHttpRequest sdkHttpRequest(URI customUri) {
        return SdkHttpFullRequest.builder()
                                 .protocol(customUri.getScheme())
                                 .host(customUri.getHost())
                                 .port(customUri.getPort())
                                 .method(SdkHttpMethod.GET)
                                 .encodedPath(customUri.getPath())
                                 .build();
    }

    private void verifyAccelerateDisabledOperationsEndpointNotConverted(SdkRequest request) {
        URI customUri = URI.create("http://s3-test.com");
        Context.ModifyHttpRequest ctx = context(request, sdkHttpRequest(customUri));
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        S3Configuration s3Configuration = S3Configuration.builder().accelerateModeEnabled(true).build();

        executionAttributes.putAttribute(SERVICE_CONFIG, s3Configuration);
        executionAttributes.putAttribute(AWS_REGION, Region.US_EAST_1);

        SdkHttpRequest sdkHttpFullRequest = interceptor.modifyHttpRequest(ctx, executionAttributes);

        assertThat(sdkHttpFullRequest.getUri()).isEqualTo(customUri);
    }

    private void verifyEndpoint(String protocol, String expectedEndpoint,
                                S3Configuration.Builder builder) {
        String bucket = "test-bucket";
        String key = "test-key";
        URI customUri = URI.create(String.format("%s://s3-test.com/%s/%s", protocol, bucket, key));
        URI expectedUri = URI.create(String.format("%s/%s/%s", expectedEndpoint, bucket, key));
        Context.ModifyHttpRequest ctx = context(PutObjectRequest.builder().build(), sdkHttpRequest(customUri));
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        S3Configuration s3Configuration = builder.build();

        executionAttributes.putAttribute(SERVICE_CONFIG, s3Configuration);
        executionAttributes.putAttribute(AWS_REGION, Region.US_EAST_1);

        SdkHttpRequest sdkHttpFullRequest = interceptor.modifyHttpRequest(ctx, executionAttributes);

        assertThat(sdkHttpFullRequest.getUri()).isEqualTo(expectedUri);
    }

    private void verifyAccesspointArn(String protocol, String accessPointArn, String expectedEndpoint,
                                      Region expectedSigningRegion,
                                      S3Configuration.Builder builder, Region region) {
        String key = "test-key";

        URI customUri = URI.create(String.format("%s://s3-test.com/%s/%s", protocol, urlEncode(accessPointArn), key));
        URI expectedUri = URI.create(String.format("%s/%s", expectedEndpoint, key));
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                            .bucket(accessPointArn)
                                                            .key(key)
                                                            .build();
        Context.ModifyHttpRequest ctx = context(putObjectRequest, sdkHttpRequest(customUri));
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        S3Configuration s3Configuration = builder.build();

        executionAttributes.putAttribute(SERVICE_CONFIG, s3Configuration);
        executionAttributes.putAttribute(AWS_REGION, region);
        executionAttributes.putAttribute(SIGNING_REGION, region);

        SdkHttpRequest sdkHttpFullRequest = interceptor.modifyHttpRequest(ctx, executionAttributes);

        assertThat(executionAttributes.getAttribute(SIGNING_REGION))
            .isEqualTo(expectedSigningRegion);
        assertThat(sdkHttpFullRequest.getUri()).isEqualTo(expectedUri);
    }


    private void verifyAccesspointArn(String protocol, String accessPointArn, String expectedEndpoint,
                                      S3Configuration.Builder builder) {
        verifyAccesspointArn(protocol, accessPointArn, expectedEndpoint, Region.US_EAST_1, builder, Region.US_EAST_1);
    }

    private Context.ModifyHttpRequest context(SdkRequest request, SdkHttpRequest sdkHttpRequest) {
        return new Context.ModifyHttpRequest() {
            @Override
            public SdkHttpRequest httpRequest() {
                return sdkHttpRequest;
            }

            @Override
            public Optional<RequestBody> requestBody() {
                return null;
            }

            @Override
            public Optional<AsyncRequestBody> asyncRequestBody() {
                return null;
            }

            @Override
            public SdkRequest request() {
                return request;
            }
        };
    }
}
