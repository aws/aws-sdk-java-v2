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

package software.amazon.awssdk.services.s3.regression.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.assumeNotAccessPointWithPathStyle;
import static software.amazon.awssdk.services.s3.regression.S3ClientFlavor.STANDARD_ASYNC;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.regression.BucketType;
import software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils;
import software.amazon.awssdk.services.s3.regression.TestCallable;
import software.amazon.awssdk.utils.Logger;

public class UploadAsyncRegressionTesting extends UploadStreamingRegressionTesting {
    private static final Logger LOG = Logger.loggerFor(UploadAsyncRegressionTesting.class);

    public static List<FlattenUploadConfig> testConfigs() {
        return FlattenUploadConfig.testConfigs();
    }

    @ParameterizedTest
    @MethodSource("testConfigs")
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void putObject(FlattenUploadConfig config) throws Exception {
        assumeNotAccessPointWithPathStyle(config);

        // For testing purposes, ContentProvider is Publisher<ByteBuffer> for async clients
        // There is no way to create AsyncRequestBody with a Publisher<ByteBuffer> and also provide the content length
        Assumptions.assumeFalse(config.getBodyType() == BodyType.CONTENT_PROVIDER_WITH_LENGTH,
                                "No way to create AsyncRequestBody by giving both an Publisher and the content length");

        // Payload signing doesn't work correctly for async java based
        // TODO(sra-identity-auth) remove when chunked encoding support is added in async code path
        Assumptions.assumeFalse(config.isPayloadSigning()
                                // MRAP requires body signing
                                || config.getBucketType() == BucketType.MRAP,
                                "Async payload signing doesn't work with Java based clients");

        // For testing purposes, ContentProvider is Publisher<ByteBuffer> for async clients
        // Async java based clients don't currently support unknown content-length bodies
        Assumptions.assumeFalse(config.getBodyType() == BodyType.CONTENT_PROVIDER_NO_LENGTH
                                || config.getBodyType() == BodyType.INPUTSTREAM_NO_LENGTH,
                                "Async Java based support unknown content length");

        LOG.info(() -> "Running UploadAsyncRegressionTesting putObject with config: " + config);

        BucketType bucketType = config.getBucketType();

        String bucket = bucketForType(bucketType);
        String key = S3ChecksumsTestUtils.randomKey();

        PutObjectRequest request = PutObjectRequest.builder()
                                                   .bucket(bucket)
                                                   .key(key)
                                                   .build();


        RequestRecorder recorder = new RequestRecorder();

        ClientOverrideConfiguration.Builder overrideConfiguration =
            ClientOverrideConfiguration.builder()
                                       .addExecutionInterceptor(recorder)
                                       .apiCallTimeout(Duration.of(30, ChronoUnit.SECONDS));

        if (config.isPayloadSigning()) {
            overrideConfiguration.addExecutionInterceptor(new EnablePayloadSigningInterceptor());
        }

        TestCallable<PutObjectResponse> callable = null;
        try {

            Long actualContentLength = null;
            boolean requestBodyHasContentLength = false;
            String actualCrc32;

            TestAsyncBody body = getAsyncRequestBody(config.getBodyType(), config.getContentSize());
            callable = callPutObject(request, STANDARD_ASYNC, body, config, overrideConfiguration.build());
            actualContentLength = body.getActualContentLength();
            requestBodyHasContentLength = body.getAsyncRequestBody().contentLength().isPresent();
            actualCrc32 = body.getChecksum();

            PutObjectResponse response = callable.runnable().call();

            recordObjectToCleanup(bucketType, key);

            // We only validate when configured to WHEN_SUPPORTED since checksums are optional for PutObject
            // CRT switches to MPU under the hood which doesn't support checksums
            if (config.getRequestChecksumValidation() == RequestChecksumCalculation.WHEN_SUPPORTED) {
                assertThat(response.checksumCRC32()).isEqualTo(actualCrc32);
            }

            assertThat(recorder.getRequests()).isNotEmpty();

            for (SdkHttpRequest httpRequest : recorder.getRequests()) {
                // skip any non-PUT requests, e.g. GetSession for EOZ requests
                if (httpRequest.method() != SdkHttpMethod.PUT) {
                    continue;
                }

                String payloadSha = httpRequest.firstMatchingHeader("x-amz-content-sha256").get();
                if (payloadSha.startsWith("STREAMING")) {
                    String decodedContentLength = httpRequest.firstMatchingHeader("x-amz-decoded-content-length").get();
                    assertThat(Long.parseLong(decodedContentLength)).isEqualTo(actualContentLength);
                    verifyChecksumResponsePayload(config, key, actualCrc32);
                } else {
                    Optional<String> contentLength = httpRequest.firstMatchingHeader("Content-Length");
                    if (requestBodyHasContentLength) {
                        assertThat(Long.parseLong(contentLength.get())).isEqualTo(actualContentLength);
                    }
                }
            }
        } catch (Exception e) {
            LOG.info(() -> String.format("Error while executing %s. Error message: %s", config, e.getMessage()));
            throw e;
        } finally {
            if (callable != null) {
                callable.client().close();
            }
        }
    }

}
