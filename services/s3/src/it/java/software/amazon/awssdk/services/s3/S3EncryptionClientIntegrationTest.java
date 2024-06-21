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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.AliasListEntry;
import software.amazon.awssdk.services.kms.model.CreateAliasRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;
import software.amazon.awssdk.services.kms.model.KeySpec;
import software.amazon.awssdk.services.kms.model.KeyUsageType;
import software.amazon.awssdk.services.kms.model.ListAliasesRequest;
import software.amazon.awssdk.services.kms.model.ListAliasesResponse;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.encryption.s3.S3AsyncEncryptionClient;
import software.amazon.encryption.s3.materials.Keyring;
import software.amazon.encryption.s3.materials.KmsKeyring;

public class S3EncryptionClientIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(S3EncryptionClientIntegrationTest.class);
    private static final String KEY = "key";
    private static final String KMS_KEY_ALIAS = "alias/do-not-delete-encryption-client-integ-test-key";

    private static KmsClient kmsClient;
    private static S3AsyncEncryptionClient encryptionClient;


    @BeforeAll
    public static void init() throws Exception {
        setUp();
        createBucket(BUCKET);
        kmsClient = KmsClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
        String kmsKeyId = getOrCreateKmsKey();
        Keyring keyring = KmsKeyring.builder().wrappingKeyId(kmsKeyId).kmsClient(kmsClient).build();
        encryptionClient = S3AsyncEncryptionClient.builder()
                                                  .keyring(keyring)
                                                  .enableLegacyUnauthenticatedModes(true)
                                                  .enableDelayedAuthenticationMode(true)
                                                  .wrappedClient(s3Async)
                                                  .build();
    }

    @AfterAll
    public static void cleanUp() {
        deleteBucketAndAllContents(BUCKET);
        kmsClient.close();
        encryptionClient.close();
    }

    private static Stream<Arguments> publishers() throws IOException {
        return Stream.of(
            Arguments.of(AsyncRequestBody.fromFile(new RandomTempFile(15))),
            Arguments.of(AsyncRequestBody.fromFile(new RandomTempFile(0))),
            Arguments.of(AsyncRequestBody.fromString("a")),
            Arguments.of(AsyncRequestBody.fromString("")),
            Arguments.of(AsyncRequestBody.fromBytes(new byte[15])),
            Arguments.of(AsyncRequestBody.fromBytes(new byte[0]))
            );
    }

    @ParameterizedTest
    @MethodSource("publishers")
    void putObject_withEncryptionClient_withChecksumAlgorithm_withFilesLessThan16Bytes_uploadsSuccessfully(AsyncRequestBody asyncRequestBody) {
        PutObjectRequest request = PutObjectRequest.builder()
                                                   .bucket(BUCKET).key(KEY)
                                                   .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                                   .build();

        PutObjectResponse response = encryptionClient.putObject(request, asyncRequestBody).join();
        assertThat(response).isNotNull();
    }

    private static Optional<AliasListEntry> checkForExistingKey() {
        ListAliasesResponse listAliasesResponse = kmsClient.listAliases(ListAliasesRequest.builder().build());

        return listAliasesResponse.aliases().stream().filter(alias -> alias.aliasName().equals(KMS_KEY_ALIAS)).findAny();
    }

    private static String getOrCreateKmsKey() {
        Optional<AliasListEntry> existingKey = checkForExistingKey();
        if (existingKey.isPresent()) {
            return existingKey.get().targetKeyId();
        }

        CreateKeyResponse response =
            kmsClient.createKey(c -> c.keySpec(KeySpec.SYMMETRIC_DEFAULT).keyUsage(KeyUsageType.ENCRYPT_DECRYPT));
        String keyId =  response.keyMetadata().keyId();

        CreateAliasRequest createAliasRequest = CreateAliasRequest.builder()
                                                                  .aliasName(KMS_KEY_ALIAS)
                                                                  .targetKeyId(keyId)
                                                                  .build();
        kmsClient.createAlias(createAliasRequest);

        return keyId;
    }
}
