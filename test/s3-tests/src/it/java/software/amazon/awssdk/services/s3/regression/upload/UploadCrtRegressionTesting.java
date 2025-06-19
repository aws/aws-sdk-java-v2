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
import static software.amazon.awssdk.services.s3.regression.S3ClientFlavor.CRT_BASED;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.regression.BucketType;
import software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils;
import software.amazon.awssdk.services.s3.regression.TestCallable;
import software.amazon.awssdk.utils.Logger;

public class UploadCrtRegressionTesting extends UploadStreamingRegressionTesting {
    private static final Logger LOG = Logger.loggerFor(UploadCrtRegressionTesting.class);

    public static List<FlattenUploadConfig> testConfigs() {
        return FlattenUploadConfig.testConfigs();
    }

    @ParameterizedTest
    @MethodSource("testConfigs")
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void putObject(FlattenUploadConfig config) throws Exception {
        assumeNotAccessPointWithPathStyle(config);

        Assumptions.assumeFalse(config.getBodyType() == BodyType.CONTENT_PROVIDER_WITH_LENGTH,
                                "No way to create AsyncRequestBody by giving both an Publisher and the content length");

        LOG.info(() -> "Running UploadCrtRegressionTesting putObject with config: " + config);

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


            TestAsyncBody body = getAsyncRequestBody(config.getBodyType(), config.getContentSize());
            callable = callPutObject(request, CRT_BASED, body, config, overrideConfiguration.build());
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
