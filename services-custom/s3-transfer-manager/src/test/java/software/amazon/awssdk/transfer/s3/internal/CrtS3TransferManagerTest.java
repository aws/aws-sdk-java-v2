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
import static software.amazon.awssdk.services.s3.crt.S3CrtSdkHttpExecutionAttribute.METAREQUEST_PAUSE_OBSERVABLE;

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
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

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
        transferManager = new CrtS3TransferManager(new TransferManagerConfiguration(new TransferManagerFactory.DefaultBuilder()),
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

        verifyCrtInRequestAttributes();
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

        verifyCrtInRequestAttributes();
    }

    private void verifyCrtInRequestAttributes() {
        ArgumentCaptor<PutObjectRequest> requestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3AsyncClient).putObject(requestArgumentCaptor.capture(), ArgumentCaptor.forClass(Path.class).capture());

        PutObjectRequest actual = requestArgumentCaptor.getValue();
        assertThat(actual.overrideConfiguration()).isPresent();
        SdkHttpExecutionAttributes attribute = actual.overrideConfiguration().get().executionAttributes().getAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES);
        assertThat(attribute).isNotNull();
        assertThat(attribute.getAttribute(METAREQUEST_PAUSE_OBSERVABLE)).isNotNull();
    }
}
