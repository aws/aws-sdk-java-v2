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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.SDK_HTTP_EXECUTION_ATTRIBUTES;
import static software.amazon.awssdk.services.s3.crt.S3CrtSdkHttpExecutionAttribute.CRT_PROGRESS_LISTENER;
import static software.amazon.awssdk.services.s3.crt.S3CrtSdkHttpExecutionAttribute.METAREQUEST_PAUSE_OBSERVABLE;
import static software.amazon.awssdk.services.s3.internal.crt.DefaultS3CrtAsyncClient.RESPONSE_FILE_OPTION;
import static software.amazon.awssdk.services.s3.internal.crt.DefaultS3CrtAsyncClient.RESPONSE_FILE_PATH;

import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.exceptions.verification.WantedButNotInvoked;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.listener.AsyncResponseTransformerListener.NotifyingAsyncResponseTransformer;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions.ResponseFileOption;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.CrtResponseFileResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

@ExtendWith(MockitoExtension.class)
public class CrtS3TransferManagerTest {

    @Mock
    private S3AsyncClient s3AsyncClient;

    private static Path localDirectory;
    private static FileSystem jimfs;
    private CrtS3TransferManager transferManager;

    @BeforeAll
    public static void setUp() throws IOException {
        jimfs = Jimfs.newFileSystem();
        localDirectory = jimfs.getPath("test");
        Files.createDirectory(localDirectory);
        Files.write(jimfs.getPath("test", "test.txt"), RandomStringUtils.randomAscii(1024).getBytes());
    }

    @BeforeEach
    public void setUpPerMethod() {
        transferManager = new CrtS3TransferManager(TransferManagerConfiguration.builder().build(),
                                                                        s3AsyncClient, false);
    }

    @AfterAll
    public static void tearDown() throws IOException {
        jimfs.close();
    }

    @Test
    void uploadDirectory_shouldUseCrtUploadFile() {
        when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(Path.class))).thenReturn(CompletableFuture.completedFuture(PutObjectResponse.builder().build()));
        transferManager.uploadDirectory(UploadDirectoryRequest.builder().bucket("TEST").source(localDirectory).build())
                       .completionFuture()
                       .join();

        verifyCrtInRequestAttributes(true);
    }

    @Test
    void uploadFile_shouldUseCrtUploadFile() {
        when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(Path.class))).thenReturn(CompletableFuture.completedFuture(PutObjectResponse.builder().build()));
        transferManager.uploadFile(UploadFileRequest.builder()
                                                    .putObjectRequest(PutObjectRequest.builder().bucket("test").key("test").build())
                                                    .source(localDirectory.resolve("test.txt"))
                                                    .build())
                       .completionFuture()
                       .join();

        verifyCrtInRequestAttributes(true);
    }


    @Test
    void downloadFile_shouldLetCrtWriteToTheDestinationFile() {
        when(s3AsyncClient.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(GetObjectResponse.builder().build()));
        Path destination = localDirectory.resolve("download.txt");

        transferManager.downloadFile(DownloadFileRequest.builder()
                                                        .getObjectRequest(r -> r.bucket("test").key("test"))
                                                        .destination(destination)
                                                        .build())
                       .completionFuture()
                       .join();

        ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        ArgumentCaptor<AsyncResponseTransformer> transformerCaptor =
            ArgumentCaptor.forClass(AsyncResponseTransformer.class);
        verify(s3AsyncClient).getObject(requestCaptor.capture(), transformerCaptor.capture());

        ExecutionAttributes executionAttributes = requestCaptor.getValue()
                                                               .overrideConfiguration()
                                                               .orElseThrow(AssertionError::new)
                                                               .executionAttributes();
        assertThat(executionAttributes.getAttribute(RESPONSE_FILE_PATH)).isEqualTo(destination);
        assertThat(executionAttributes.getAttribute(RESPONSE_FILE_OPTION)).isEqualTo(ResponseFileOption.CREATE_OR_REPLACE);

        SdkHttpExecutionAttributes httpExecutionAttributes =
            executionAttributes.getAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES);
        assertThat(httpExecutionAttributes).isNotNull();
        assertThat(httpExecutionAttributes.getAttribute(CRT_PROGRESS_LISTENER)).isNotNull();
        assertThat(httpExecutionAttributes.getAttribute(METAREQUEST_PAUSE_OBSERVABLE)).isNotNull();

        // The body is written by CRT, so the SDK must not be given a transformer that also tries to write the file.
        AsyncResponseTransformer<?, ?> transformer = transformerCaptor.getValue();
        assertThat(transformer).isInstanceOf(NotifyingAsyncResponseTransformer.class);
        assertThat(((NotifyingAsyncResponseTransformer<?, ?>) transformer).getDelegate())
            .isInstanceOf(CrtResponseFileResponseTransformer.class);
    }

    @Test
    void upload_shouldUseCrtUpload() {
        when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class))).thenReturn(CompletableFuture.completedFuture(PutObjectResponse.builder().build()));
        transferManager.upload(UploadRequest.builder()
                                            .putObjectRequest(PutObjectRequest.builder().bucket("test").key("test").build())
                                            .requestBody(AsyncRequestBody.fromString("test"))
                                            .build())
                       .completionFuture()
                       .join();

        verifyCrtInRequestAttributes(false);
    }

    private void verifyCrtInRequestAttributes(boolean verifyObservable) {
        ArgumentCaptor<PutObjectRequest> requestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        try {
            verify(s3AsyncClient).putObject(requestArgumentCaptor.capture(), ArgumentCaptor.forClass(Path.class).capture());
        } catch (WantedButNotInvoked e) {
            verify(s3AsyncClient).putObject(requestArgumentCaptor.capture(), ArgumentCaptor.forClass(AsyncRequestBody.class).capture());
        }
        PutObjectRequest actual = requestArgumentCaptor.getValue();
        assertThat(actual.overrideConfiguration()).isPresent();
        SdkHttpExecutionAttributes attribute = actual.overrideConfiguration().get().executionAttributes().getAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES);
        assertThat(attribute).isNotNull();
        assertThat(attribute.getAttribute(CRT_PROGRESS_LISTENER)).isNotNull();
        if (verifyObservable) {
            assertThat(attribute.getAttribute(METAREQUEST_PAUSE_OBSERVABLE)).isNotNull();
        }
    }
}
