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

package software.amazon.awssdk.auth.signer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.auth.signer.params.AwsS3V4SignerParams;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

class AwsS3V4SignerTest {
    private static final Clock UTC_EPOCH_CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

    @Test
    public void signWithParams_urlsAreNotNormalized() {
        byte[] bytes = new byte[1000];
        ThreadLocalRandom.current().nextBytes(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        URI target = URI.create("https://test.com/./foo");

        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .contentStreamProvider(RequestBody.fromByteBuffer(buffer)
                                                                                         .contentStreamProvider())
                                                       .method(SdkHttpMethod.GET)
                                                       .uri(target)
                                                       .encodedPath(target.getPath())
                                                       .build();
        AwsS3V4Signer signer = AwsS3V4Signer.create();
        SdkHttpFullRequest signedRequest =
            signer.sign(request,
                        AwsS3V4SignerParams.builder()
                                           .awsCredentials(AwsBasicCredentials.create("akid", "skid"))
                                           .signingRegion(Region.US_WEST_2)
                                           .signingName("s3")
                                           .signingClockOverride(UTC_EPOCH_CLOCK)
                                           .build());

        assertThat(signedRequest.firstMatchingHeader("Authorization"))
            .hasValue("AWS4-HMAC-SHA256 Credential=akid/19700101/us-west-2/s3/aws4_request, "
                      + "SignedHeaders=host;x-amz-content-sha256;x-amz-date, "
                      + "Signature=a3b97f9de337ab254f3b366c3d0b3c67016d2d8d8ba7e0e4ddab0ccebe84992a");
    }

    @Test
    public void signWithExecutionAttributes_urlsAreNotNormalized() {
        byte[] bytes = new byte[1000];
        ThreadLocalRandom.current().nextBytes(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        URI target = URI.create("https://test.com/./foo");

        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .contentStreamProvider(RequestBody.fromByteBuffer(buffer)
                                                                                         .contentStreamProvider())
                                                       .method(SdkHttpMethod.GET)
                                                       .uri(target)
                                                       .encodedPath(target.getPath())
                                                       .build();
        ExecutionAttributes attributes =
            ExecutionAttributes.builder()
                               .put(AwsSignerExecutionAttribute.AWS_CREDENTIALS,
                                    AwsBasicCredentials.create("akid", "skid"))
                               .put(AwsSignerExecutionAttribute.SIGNING_REGION, Region.US_WEST_2)
                               .put(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, "s3")
                               .put(AwsSignerExecutionAttribute.SIGNING_CLOCK, UTC_EPOCH_CLOCK)
                               .build();

        AwsS3V4Signer signer = AwsS3V4Signer.create();
        SdkHttpFullRequest signedRequest = signer.sign(request, attributes);

        assertThat(signedRequest.firstMatchingHeader("Authorization"))
            .hasValue("AWS4-HMAC-SHA256 Credential=akid/19700101/us-west-2/s3/aws4_request, "
                      + "SignedHeaders=host;x-amz-content-sha256;x-amz-date, "
                      + "Signature=a3b97f9de337ab254f3b366c3d0b3c67016d2d8d8ba7e0e4ddab0ccebe84992a");
    }

    @Test
    public void presignWithParams_urlsAreNotNormalized() {
        byte[] bytes = new byte[1000];
        ThreadLocalRandom.current().nextBytes(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        URI target = URI.create("https://test.com/./foo");

        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .contentStreamProvider(RequestBody.fromByteBuffer(buffer)
                                                                                         .contentStreamProvider())
                                                       .method(SdkHttpMethod.GET)
                                                       .uri(target)
                                                       .encodedPath(target.getPath())
                                                       .build();
        AwsS3V4Signer signer = AwsS3V4Signer.create();

        SdkHttpFullRequest signedRequest =
            signer.presign(request,
                           Aws4PresignerParams.builder()
                                              .awsCredentials(AwsBasicCredentials.create("akid", "skid"))
                                              .signingRegion(Region.US_WEST_2)
                                              .signingName("s3")
                                              .signingClockOverride(UTC_EPOCH_CLOCK)
                                              .build());

        assertThat(signedRequest.firstMatchingRawQueryParameter("X-Amz-Signature"))
            .hasValue("3a9d36d37e9a554b7a3803f58ee7539b5d1f52fdfe89ce6fd40fb25762a35ec3");
    }

    @Test
    public void presignWithExecutionAttributes_urlsAreNotNormalized() {
        byte[] bytes = new byte[1000];
        ThreadLocalRandom.current().nextBytes(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        URI target = URI.create("https://test.com/./foo");

        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .contentStreamProvider(RequestBody.fromByteBuffer(buffer)
                                                                                         .contentStreamProvider())
                                                       .method(SdkHttpMethod.GET)
                                                       .uri(target)
                                                       .encodedPath(target.getPath())
                                                       .build();
        ExecutionAttributes attributes =
            ExecutionAttributes.builder()
                               .put(AwsSignerExecutionAttribute.AWS_CREDENTIALS,
                                    AwsBasicCredentials.create("akid", "skid"))
                               .put(AwsSignerExecutionAttribute.SIGNING_REGION, Region.US_WEST_2)
                               .put(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, "s3")
                               .put(AwsSignerExecutionAttribute.SIGNING_CLOCK, UTC_EPOCH_CLOCK)
                               .build();

        AwsS3V4Signer signer = AwsS3V4Signer.create();
        SdkHttpFullRequest signedRequest = signer.presign(request, attributes);

        assertThat(signedRequest.firstMatchingRawQueryParameter("X-Amz-Signature"))
            .hasValue("3a9d36d37e9a554b7a3803f58ee7539b5d1f52fdfe89ce6fd40fb25762a35ec3");
    }

    @Test
    public void signWithParams_doesNotFailWithEncodedCharacters() {
        URI target = URI.create("https://test.com/%20foo");

        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.GET)
                                                       .uri(target)
                                                       .encodedPath(target.getPath())
                                                       .build();
        AwsS3V4Signer signer = AwsS3V4Signer.create();
        assertDoesNotThrow(() ->
            signer.sign(request,
                        AwsS3V4SignerParams.builder()
                                           .awsCredentials(AwsBasicCredentials.create("akid", "skid"))
                                           .signingRegion(Region.US_WEST_2)
                                           .signingName("s3")
                                           .build()));
    }

    @Test
    public void signWithExecutionAttributes_doesNotFailWithEncodedCharacters() {
        URI target = URI.create("https://test.com/%20foo");

        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.GET)
                                                       .uri(target)
                                                       .encodedPath(target.getPath())
                                                       .build();
        ExecutionAttributes attributes =
            ExecutionAttributes.builder()
                               .put(AwsSignerExecutionAttribute.AWS_CREDENTIALS,
                                    AwsBasicCredentials.create("akid", "skid"))
                               .put(AwsSignerExecutionAttribute.SIGNING_REGION, Region.US_WEST_2)
                               .put(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, "s3")
                               .build();

        AwsS3V4Signer signer = AwsS3V4Signer.create();
        assertDoesNotThrow(() -> signer.sign(request, attributes));
    }

    @Test
    public void presignWithParams_doesNotFailWithEncodedCharacters() {
        URI target = URI.create("https://test.com/%20foo");

        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.GET)
                                                       .uri(target)
                                                       .encodedPath(target.getPath())
                                                       .build();
        AwsS3V4Signer signer = AwsS3V4Signer.create();

        assertDoesNotThrow(() ->
            signer.presign(request,
                           Aws4PresignerParams.builder()
                                              .awsCredentials(AwsBasicCredentials.create("akid", "skid"))
                                              .signingRegion(Region.US_WEST_2)
                                              .signingName("s3")
                                              .build()));
    }

    @Test
    public void presignWithExecutionAttributes_doesNotFailWithEncodedCharacters() {
        URI target = URI.create("https://test.com/%20foo");

        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.GET)
                                                       .uri(target)
                                                       .encodedPath(target.getPath())
                                                       .build();
        ExecutionAttributes attributes =
            ExecutionAttributes.builder()
                               .put(AwsSignerExecutionAttribute.AWS_CREDENTIALS,
                                    AwsBasicCredentials.create("akid", "skid"))
                               .put(AwsSignerExecutionAttribute.SIGNING_REGION, Region.US_WEST_2)
                               .put(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, "s3")
                               .build();

        AwsS3V4Signer signer = AwsS3V4Signer.create();
        assertDoesNotThrow(() -> signer.presign(request, attributes));
    }
}