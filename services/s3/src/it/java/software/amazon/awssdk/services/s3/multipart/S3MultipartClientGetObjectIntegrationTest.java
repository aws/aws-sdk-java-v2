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

package software.amazon.awssdk.services.s3.multipart;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.utils.BinaryUtils;

public class S3MultipartClientGetObjectIntegrationTest extends S3IntegrationTestBase {
    private static final String TEST_BUCKET = temporaryBucketName(S3MultipartClientPutObjectIntegrationTest.class);
    private static final String TEST_KEY = "testfile.dat";
    private static final int PART_SIZE = 8 * 1024 * 1024;
    private static final int N_PARTS = 16;

    private static byte[] partData;
    private static String testObjectChecksum;
    private static S3AsyncClient mpuS3Client;

    @BeforeAll
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();
        S3IntegrationTestBase.createBucket(TEST_BUCKET);

        partData = new byte[PART_SIZE];
        new Random().nextBytes(partData);

        uploadTestObject(partData, N_PARTS);
        testObjectChecksum = calculateSha256(partData, N_PARTS);

        mpuS3Client = S3AsyncClient
            .builder()
            .region(DEFAULT_REGION)
            .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
            .overrideConfiguration(o -> o.addExecutionInterceptor(
                new UserAgentVerifyingExecutionInterceptor("NettyNio", ClientType.ASYNC)))
            .multipartEnabled(true)
            .build();
    }

    @AfterAll
    public static void teardown() throws IOException {
        mpuS3Client.close();
        deleteBucketAndAllContents(TEST_BUCKET);
    }

    @Test
    public void getObject_objectReceivedCorrectly() {
        ResponseBytes<GetObjectResponse> getObjectResponse = mpuS3Client.getObject(get -> get.bucket(TEST_BUCKET).key(TEST_KEY),
                                                                                   AsyncResponseTransformer.toBytes())
                                                                        .join();
        String objectChecksum = calculateSha256(getObjectResponse.asByteArray(), 1);
        assertThat(objectChecksum).isEqualTo(testObjectChecksum);
    }

    private static void uploadTestObject(byte[] partData, int nParts) {
        CreateMultipartUploadResponse multipartUpload = s3.createMultipartUpload(r -> r.bucket(TEST_BUCKET).key(TEST_KEY));

        List<CompletedPart> completedParts = new ArrayList<>();
        for (int i = 0; i < nParts; ++i) {
            int partNum = i + 1;
            UploadPartResponse uploadPartResponse = s3.uploadPart(
                r -> r.bucket(TEST_BUCKET)
                      .key(TEST_KEY)
                      .partNumber(partNum)
                      .uploadId(multipartUpload.uploadId()),
                RequestBody.fromBytes(partData)
            );

            completedParts.add(
                CompletedPart.builder()
                             .partNumber(partNum)
                             .eTag(uploadPartResponse.eTag())
                             .build()
            );
        }

        s3.completeMultipartUpload(r -> r.multipartUpload(mpu -> mpu.parts(completedParts))
                                         .bucket(TEST_BUCKET)
                                         .key(TEST_KEY)
                                         .uploadId(multipartUpload.uploadId()));
    }

    private static String calculateSha256(byte[] data, int repetitions) {
        SdkChecksum sha256 = SdkChecksum.forAlgorithm(Algorithm.SHA256);
        for (int i = 0; i < repetitions; ++i) {
            sha256.update(data);
        }
        return BinaryUtils.toHex(sha256.getChecksumBytes());
    }
}
