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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.utils.CompletableFutureUtils;

class S3TransferManagerDownloadResumeSseCTest {
    private static final String SSE_CUSTOMER_KEY = "QEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaW1xdXl8=";
    private static final String SSE_CUSTOMER_KEY_MD5 = "bWQ1LW9mLXRoZS1rZXk=";

    private S3CrtAsyncClient mockS3Crt;
    private S3AsyncClient mockS3;
    private S3TransferManager tmCrt;
    private S3TransferManager tmJava;
    private File file;

    @BeforeEach
    public void methodSetup() throws IOException {
        file = RandomTempFile.createTempFile("test", UUID.randomUUID().toString());
        Files.write(file.toPath(), RandomStringUtils.randomAlphanumeric(1000).getBytes(StandardCharsets.UTF_8));
        TransferManagerConfiguration configuration = mock(TransferManagerConfiguration.class);
        mockS3Crt = mock(S3CrtAsyncClient.class);
        mockS3 = mock(S3AsyncClient.class);
        tmCrt = new CrtS3TransferManager(configuration, mockS3Crt, false);
        tmJava = new GenericS3TransferManager(mockS3, mock(UploadDirectoryHelper.class), configuration,
                                              mock(DownloadDirectoryHelper.class));
    }

    @AfterEach
    public void methodTeardown() {
        file.delete();
        tmCrt.close();
        tmJava.close();
    }

    enum TmType {
        JAVA, CRT
    }

    private static Stream<Arguments> transferManagers() {
        return Stream.of(Arguments.of(TmType.JAVA), Arguments.of(TmType.CRT));
    }

    private S3TransferManager tm(TmType tmType) {
        return tmType == TmType.JAVA ? tmJava : tmCrt;
    }

    private S3AsyncClient client(TmType tmType) {
        return tmType == TmType.JAVA ? mockS3 : mockS3Crt;
    }

    @ParameterizedTest
    @MethodSource("transferManagers")
    void resumeDownloadFile_deserializedSseCTokenWithoutKey_shouldThrowBeforeAnyS3Call(TmType tmType) {
        ResumableFileDownload deserialized = ResumableFileDownload.fromString(sseCustomerResumableDownload().serializeToString());

        assertThat(deserialized.downloadFileRequest().getObjectRequest().sseCustomerKey()).isNull();
        assertThatThrownBy(() -> tm(tmType).resumeDownloadFile(deserialized))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("GetObjectRequest")
            .hasMessageContaining("sseCustomerKey")
            .hasMessageNotContaining(SSE_CUSTOMER_KEY)
            .hasMessageNotContaining(SSE_CUSTOMER_KEY_MD5);

        verifyNoInteractions(mockS3, mockS3Crt);
    }

    @ParameterizedTest
    @MethodSource("transferManagers")
    @SuppressWarnings("unchecked")
    void resumeDownloadFile_deserializedSseCTokenWithKeySupplied_shouldPassValidation(TmType tmType) {
        SdkClientException headFailure = SdkClientException.create("head failed");
        when(client(tmType).headObject(any(Consumer.class))).thenReturn(CompletableFutureUtils.failedFuture(headFailure));
        ResumableFileDownload resumable = ResumableFileDownload.fromString(sseCustomerResumableDownload().serializeToString());

        ResumableFileDownload withKey = resumable.copy(b -> b.downloadFileRequest(resumable.downloadFileRequest().copy(
            r -> r.getObjectRequest(resumable.downloadFileRequest().getObjectRequest()
                                             .copy(g -> g.sseCustomerKey(SSE_CUSTOMER_KEY))))));

        assertThatThrownBy(() -> tm(tmType).resumeDownloadFile(withKey).completionFuture().join())
            .hasRootCause(headFailure);
        verify(client(tmType)).headObject(any(Consumer.class));
    }

    @ParameterizedTest
    @MethodSource("transferManagers")
    @SuppressWarnings("unchecked")
    void resumeDownloadFile_sseCKeyAsOverrideHeader_shouldPassValidation(TmType tmType) {
        SdkClientException headFailure = SdkClientException.create("head failed");
        when(client(tmType).headObject(any(Consumer.class))).thenReturn(CompletableFutureUtils.failedFuture(headFailure));
        ResumableFileDownload resumable = sseCustomerResumableDownload();
        AwsRequestOverrideConfiguration keyHeader =
            AwsRequestOverrideConfiguration.builder()
                                           .putHeader("X-Amz-Server-Side-Encryption-Customer-Key", SSE_CUSTOMER_KEY)
                                           .build();
        GetObjectRequest withHeader = resumable.downloadFileRequest().getObjectRequest()
                                               .copy(g -> g.sseCustomerKey(null).overrideConfiguration(keyHeader));
        ResumableFileDownload withHeaderToken = resumable.copy(
            b -> b.downloadFileRequest(resumable.downloadFileRequest().copy(r -> r.getObjectRequest(withHeader))));

        assertThatThrownBy(() -> tm(tmType).resumeDownloadFile(withHeaderToken).completionFuture().join())
            .hasRootCause(headFailure);
    }

    private ResumableFileDownload sseCustomerResumableDownload() {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket("bucket")
                                                            .key("key")
                                                            .sseCustomerAlgorithm("AES256")
                                                            .sseCustomerKey(SSE_CUSTOMER_KEY)
                                                            .sseCustomerKeyMD5(SSE_CUSTOMER_KEY_MD5)
                                                            .build();
        return ResumableFileDownload.builder()
                                    .downloadFileRequest(r -> r.getObjectRequest(getObjectRequest).destination(file))
                                    .bytesTransferred(file.length())
                                    .fileLastModified(Instant.ofEpochMilli(file.lastModified()))
                                    .s3ObjectLastModified(Instant.now())
                                    .build();
    }
}
