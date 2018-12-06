/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static software.amazon.awssdk.services.s3.model.ServerSideEncryption.AES256;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.KeyGenerator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Md5Utils;

public class SyncServerSideEncryptionIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(GetObjectIntegrationTest.class);

    private static File file;

    @BeforeClass
    public static void setupFixture() throws IOException {
        createBucket(BUCKET);
        file = new RandomTempFile(10_000);
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
        file.delete();
    }

    @Test
    public void sse_AES256_succeeds() throws Exception {
        String key = UUID.randomUUID().toString();
        PutObjectRequest request = PutObjectRequest.builder()
                                                   .key(key)
                                                   .bucket(BUCKET)
                                                   .serverSideEncryption(AES256)
                                                   .build();

        s3.putObject(request, file.toPath());

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .key(key)
                                                            .bucket(BUCKET)
                                                            .build();

        InputStream response = s3.getObject(getObjectRequest);
        IoUtils.drainInputStream(response);
        response.close();
    }

    @Test
    public void sse_AWSKMS_succeeds() throws Exception {
        String key = UUID.randomUUID().toString();
        PutObjectRequest request = PutObjectRequest.builder()
                                                   .key(key)
                                                   .bucket(BUCKET)
                                                   .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                                                   .build();

        s3.putObject(request, file.toPath());

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .key(key)
                                                            .bucket(BUCKET)
                                                            .build();

        InputStream response = s3.getObject(getObjectRequest);
        IoUtils.drainInputStream(response);
        response.close();
    }

    @Test
    public void sse_customerManaged_succeeds() throws Exception {
        String key = UUID.randomUUID().toString();
        byte[] secretKey = generateSecretKey();
        String b64Key = Base64.getEncoder().encodeToString(secretKey);
        String b64KeyMd5 = Md5Utils.md5AsBase64(secretKey);

        PutObjectRequest request = PutObjectRequest.builder()
                                                   .key(key)
                                                   .bucket(BUCKET)
                                                   .sseCustomerKey(b64Key)
                                                   .sseCustomerAlgorithm(AES256.name())
                                                   .sseCustomerKeyMD5(b64KeyMd5)
                                                   .build();

        s3.putObject(request, file.toPath());

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .key(key)
                                                            .bucket(BUCKET)
                                                            .sseCustomerKey(b64Key)
                                                            .sseCustomerAlgorithm(AES256.name())
                                                            .sseCustomerKeyMD5(b64KeyMd5)
                                                            .build();

        InputStream response = s3.getObject(getObjectRequest);
        IoUtils.drainInputStream(response);
        response.close();
    }

    private static byte[] generateSecretKey() {
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
