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
import static software.amazon.awssdk.services.s3.regression.S3ClientFlavor.MULTIPART_ENABLED;
import static software.amazon.awssdk.services.s3.regression.S3ClientFlavor.STANDARD_ASYNC;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.regression.BaseS3RegressionTest;
import software.amazon.awssdk.services.s3.regression.BucketType;
import software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils;
import software.amazon.awssdk.services.s3.regression.S3ClientFlavor;
import software.amazon.awssdk.services.s3.regression.TestCallable;
import software.amazon.awssdk.utils.Logger;

public class UploadTransferManagerRegressionTesting extends UploadStreamingRegressionTesting {
    private static final Logger LOG = Logger.loggerFor(UploadTransferManagerRegressionTesting.class);

    public static List<FlattenUploadConfig> testConfigs() {
        return FlattenUploadConfig.testConfigs();
    }

    @ParameterizedTest
    @MethodSource("uploadConfigs")
    void putObject(FlattenUploadConfig config) throws Exception {

        assumeNotAccessPointWithPathStyle(config);

        // For testing purposes, ContentProvider is Publisher<ByteBuffer> for async clients
        // There is no way to create AsyncRequestBody with a Publisher<ByteBuffer> and also provide the content length
        S3ClientFlavor flavor = MULTIPART_ENABLED;

        Assumptions.assumeFalse(config.getBodyType() == BodyType.CONTENT_PROVIDER_WITH_LENGTH,
                                "No way to create AsyncRequestBody by giving both an Publisher and the content length");

        // Payload signing doesn't work correctly for async java based
        // TODO(sra-identity-auth) remove when chunked encoding support is added in async code path
        // MRAP requires body signing
        Assumptions.assumeFalse(
            config.isPayloadSigning() || config.getBucketType() == BucketType.MRAP,
            "Async payload signing doesn't work with Java based clients");

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
                                       .addExecutionInterceptor(recorder);

        if (config.isPayloadSigning()) {
            overrideConfiguration.addExecutionInterceptor(new EnablePayloadSigningInterceptor());
        }

        TestCallable<PutObjectResponse> callable = null;
        try {

            TestAsyncBody body = getAsyncRequestBody(config.getBodyType(), config.getContentSize());
            callable = callTmUpload(request, flavor, body, config, overrideConfiguration.build());
            String actualCrc32 = body.getChecksum();

            PutObjectResponse response = callable.runnable().call();

            recordObjectToCleanup(bucketType, key);

            if (response.checksumCRC32() != null && !response.checksumCRC32().isEmpty()) {
                assertThat(actualCrc32).isEqualTo(response.checksumCRC32());
            } else {
                LOG.info(() -> "Skipping checksum for config " + config);
            }

        } finally {
            if (callable != null) {
                callable.client().close();
            }
        }
    }


}
