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

package software.amazon.awssdk.services.oldclient;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class S3PutGetIntegrationTest extends AwsTestBase {
    private static final S3Client S3 =
        s3ClientBuilder().build();
    private static final S3Client S3_CUSTOM_SIGNER =
        s3ClientBuilder().overrideConfiguration(c -> c.putAdvancedOption(SdkAdvancedClientOption.SIGNER,
                                                                         AwsS3V4Signer.create()))
                         .build();
    private static final S3Client S3_NO_CREDS =
        s3ClientBuilder().credentialsProvider(AnonymousCredentialsProvider.create())
                         .build();

    private static final S3AsyncClient S3_ASYNC =
        getS3AsyncClientBuilder().build();
    private static final S3AsyncClient S3_ASYNC_CUSTOM_SIGNER =
        getS3AsyncClientBuilder().overrideConfiguration(c -> c.putAdvancedOption(SdkAdvancedClientOption.SIGNER,
                                                                                 AwsS3V4Signer.create()))
                                 .build();
    private static final S3AsyncClient S3_ASYNC_NO_CREDS =
        getS3AsyncClientBuilder().credentialsProvider(AnonymousCredentialsProvider.create())
                                 .build();

    private static final String BUCKET = "sra-get-put-integ-" + System.currentTimeMillis();
    private static final String BODY = "foo";
    private static final String BODY_CRC32 = "jHNlIQ==";

    private String key;

    @BeforeAll
    public static void setup() {
        S3.createBucket(r -> r.bucket(BUCKET));
    }

    @BeforeEach
    public void setupTest() {
        key = UUID.randomUUID().toString();
    }

    @AfterAll
    public static void teardown() {
        TestUtils.deleteBucketAndAllContents(S3, BUCKET);
    }

    @Test
    public void putGet() {
        S3.putObject(r -> r.bucket(BUCKET).key(key), RequestBody.fromString(BODY));
        assertThat(S3.getObjectAsBytes(r -> r.bucket(BUCKET).key(key)).asUtf8String()).isEqualTo(BODY);
    }

    @Test
    public void putGet_async() {
        S3_ASYNC.putObject(r -> r.bucket(BUCKET).key(key), AsyncRequestBody.fromString(BODY)).join();
        assertThat(S3_ASYNC.getObject(r -> r.bucket(BUCKET).key(key),
                                      AsyncResponseTransformer.toBytes())
                           .join()
                           .asUtf8String()).isEqualTo(BODY);
    }

    @Test
    public void putGet_requestLevelCreds() {
        S3_NO_CREDS.putObject(r -> r.bucket(BUCKET)
                                    .key(key)
                                    .overrideConfiguration(c -> c.credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)),
                              RequestBody.fromString(BODY));
        assertThat(S3_NO_CREDS.getObjectAsBytes(r -> r.bucket(BUCKET)
                                                      .key(key)
                                                      .overrideConfiguration(c -> c.credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)))
                              .asUtf8String()).isEqualTo(BODY);
    }

    @Test
    public void putGet_async_requestLevelCreds() {
        S3_ASYNC_NO_CREDS.putObject(r -> r.bucket(BUCKET)
                                          .key(key)
                                          .overrideConfiguration(c -> c.credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)),
                           AsyncRequestBody.fromString(BODY))
                .join();
        assertThat(S3_ASYNC_NO_CREDS.getObject(r -> r.bucket(BUCKET)
                                                     .key(key)
                                                     .overrideConfiguration(c -> c.credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)),
                                               AsyncResponseTransformer.toBytes())
                                    .join()
                                    .asUtf8String()).isEqualTo(BODY);
    }

    @Test
    public void putGet_flexibleChecksums() {
        S3.putObject(r -> r.bucket(BUCKET).key(key).checksumAlgorithm(ChecksumAlgorithm.CRC32),
                     RequestBody.fromString(BODY));
        ResponseBytes<GetObjectResponse> response =
            S3.getObjectAsBytes(r -> r.bucket(BUCKET).key(key).checksumMode(ChecksumMode.ENABLED));
        assertThat(response.asUtf8String()).isEqualTo(BODY);
        assertThat(response.response().checksumCRC32()).isEqualTo(BODY_CRC32);
    }

    @Test
    public void putGet_async_flexibleChecksums() {
        S3_ASYNC.putObject(r -> r.bucket(BUCKET).key(key).checksumAlgorithm(ChecksumAlgorithm.CRC32),
                           AsyncRequestBody.fromString(BODY)).join();
        ResponseBytes<GetObjectResponse> response = S3_ASYNC.getObject(r -> r.bucket(BUCKET).key(key).checksumMode(ChecksumMode.ENABLED),
                                                                       AsyncResponseTransformer.toBytes())
                                                            .join();
        assertThat(response.asUtf8String()).isEqualTo(BODY);
        assertThat(response.response().checksumCRC32()).isEqualTo(BODY_CRC32);
    }

    @Test
    public void putGet_customSigner_flexibleChecksums() {
        S3_CUSTOM_SIGNER.putObject(r -> r.bucket(BUCKET).key(key).checksumAlgorithm(ChecksumAlgorithm.CRC32),
                                   RequestBody.fromString(BODY));
        ResponseBytes<GetObjectResponse> response =
            S3_CUSTOM_SIGNER.getObjectAsBytes(r -> r.bucket(BUCKET).key(key).checksumMode(ChecksumMode.ENABLED));
        assertThat(response.asUtf8String()).isEqualTo(BODY);
        assertThat(response.response().checksumCRC32()).isEqualTo(BODY_CRC32);
    }

    @Test
    public void putGet_async_customSigner_flexibleChecksums() {
        S3_ASYNC_CUSTOM_SIGNER.putObject(r -> r.bucket(BUCKET).key(key).checksumAlgorithm(ChecksumAlgorithm.CRC32),
                                         AsyncRequestBody.fromString(BODY))
                              .join();
        ResponseBytes<GetObjectResponse> response =
            S3_ASYNC_CUSTOM_SIGNER.getObject(r -> r.bucket(BUCKET).key(key).checksumMode(ChecksumMode.ENABLED),
                                             AsyncResponseTransformer.toBytes())
                                  .join();
        assertThat(response.asUtf8String()).isEqualTo(BODY);
        assertThat(response.response().checksumCRC32()).isEqualTo(BODY_CRC32);
    }

    private static S3ClientBuilder s3ClientBuilder() {
        return S3Client.builder()
                       .region(Region.US_WEST_2)
                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN);
    }

    private static S3AsyncClientBuilder getS3AsyncClientBuilder() {
        return S3AsyncClient.builder()
                            .region(Region.US_WEST_2)
                            .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN);
    }
}
