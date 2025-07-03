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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
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

public class UploadSyncRegressionTesting extends UploadStreamingRegressionTesting {
    private static final Logger LOG = Logger.loggerFor(UploadSyncRegressionTesting.class);

    public static List<UploadConfig> testConfigs() {
        return UploadConfig.testConfigs();
    }

    @ParameterizedTest
    @MethodSource("testConfigs")
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void putObject(UploadConfig config) throws Exception {
        assumeNotAccessPointWithPathStyle(config);

        // TODO connection acquire timeout when RequestBody.fromRemainingByteBuffer is used with RequestChecksumCalculation
        //  .WHEN_SUPPORTED
        Assumptions.assumeFalse(config.getBodyType() == BodyType.REMAINING_BYTE_BUFFER
                                && config.getRequestChecksumValidation() == RequestChecksumCalculation.WHEN_SUPPORTED,
                                "TODO: investigate connection acquire timeout when using RequestBody.fromRemainingByteBuffer"
                                + " with RequestChecksumCalculation.WHEN_SUPPORTED");

        LOG.info(() -> "Running putObject with config: " + config);

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
            TestRequestBody body = getRequestBody(config.getBodyType(), config.getContentSize());
            callable = callPutObject(request, body, config, overrideConfiguration.build());
            Long actualContentLength = body.getActualContentLength();
            boolean requestBodyHasContentLength = body.optionalContentLength().isPresent();
            String actualCrc32 = body.getChecksum();
            PutObjectResponse response = callable.runnable().call();
            recordObjectToCleanup(bucketType, key);

            // We only validate when configured to WHEN_SUPPORTED since checksums are optional for PutObject
            if (config.getRequestChecksumValidation() == RequestChecksumCalculation.WHEN_SUPPORTED
                && response.checksumCRC32() != null) {
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
