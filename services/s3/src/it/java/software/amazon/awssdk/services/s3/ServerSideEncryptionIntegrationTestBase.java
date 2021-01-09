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

import static org.assertj.core.api.Fail.fail;
import static software.amazon.awssdk.services.s3.S3IntegrationTestBase.createBucket;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.testutils.RandomTempFile;

public class ServerSideEncryptionIntegrationTestBase extends S3IntegrationTestBase {

    protected static final String BUCKET = temporaryBucketName(ServerSideEncryptionIntegrationTestBase.class);
    protected static final String BUCKET_WITH_SSE = temporaryBucketName(ServerSideEncryptionIntegrationTestBase.class);

    private static final KmsClient KMS = KmsClient.builder()
                                                  .region(DEFAULT_REGION)
                                                  .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                  .build();

    protected static File file;

    private static String keyId;

    @BeforeClass
    public static void setupFixture() throws IOException {
        createBucket(BUCKET);
        createBucket(BUCKET_WITH_SSE);
        keyId = KMS.createKey().keyMetadata().keyId();

        s3.putBucketEncryption(r -> r
            .bucket(BUCKET_WITH_SSE)
            .serverSideEncryptionConfiguration(ssec -> ssec
                .rules(rule -> rule
                    .applyServerSideEncryptionByDefault(d -> d.kmsMasterKeyID(keyId)
                                                              .sseAlgorithm(ServerSideEncryption.AWS_KMS)))));
        file = new RandomTempFile(10_000);
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
        deleteBucketAndAllContents(BUCKET_WITH_SSE);
        file.delete();
        KMS.scheduleKeyDeletion(r -> r.keyId(keyId));
    }

    protected static byte[] generateSecretKey() {
        KeyGenerator generator;
        try {
            generator = KeyGenerator.getInstance("AES");
            generator.init(256, new SecureRandom());
            return generator.generateKey().getEncoded();
        } catch (Exception e) {
            fail("Unable to generate symmetric key: " + e.getMessage());
            return null;
        }
    }
}
