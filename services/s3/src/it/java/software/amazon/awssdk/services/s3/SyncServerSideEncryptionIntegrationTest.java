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

import static software.amazon.awssdk.services.s3.model.ServerSideEncryption.AES256;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;
import org.junit.Test;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.testutils.SdkAsserts;
import software.amazon.awssdk.utils.Md5Utils;

public class SyncServerSideEncryptionIntegrationTest extends ServerSideEncryptionIntegrationTestBase {
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
        SdkAsserts.assertFileEqualsStream(file, response);
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

        String response = s3.getObject(getObjectRequest, ResponseTransformer.toBytes()).asUtf8String();
        SdkAsserts.assertStringEqualsStream(response, new FileInputStream(file));
    }

    @Test
    public void sse_customerManaged_succeeds() {
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
        SdkAsserts.assertFileEqualsStream(file, response);
    }

    @Test
    public void sse_onBucket_succeeds() throws FileNotFoundException {
        String key = UUID.randomUUID().toString();

        PutObjectRequest request = PutObjectRequest.builder()
                                                   .key(key)
                                                   .bucket(BUCKET_WITH_SSE)
                                                   .build();

        s3.putObject(request, file.toPath());

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .key(key)
                                                            .bucket(BUCKET_WITH_SSE)
                                                            .build();

        String response = s3.getObject(getObjectRequest, ResponseTransformer.toBytes()).asUtf8String();
        SdkAsserts.assertStringEqualsStream(response, new FileInputStream(file));
    }
}
