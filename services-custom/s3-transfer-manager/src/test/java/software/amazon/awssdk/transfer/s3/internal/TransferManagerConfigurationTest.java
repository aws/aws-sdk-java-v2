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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption.EXECUTOR;
import static software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption.UPLOAD_DIRECTORY_FOLLOW_SYMBOLIC_LINKS;
import static software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption.UPLOAD_DIRECTORY_MAX_DEPTH;
import static software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption.UPLOAD_DIRECTORY_RECURSIVE;

import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.transfer.s3.UploadDirectoryOverrideConfiguration;
import software.amazon.awssdk.transfer.s3.UploadDirectoryRequest;

public class TransferManagerConfigurationTest {
    private TransferManagerConfiguration transferManagerConfiguration;

    @Test
    public void resolveUploadDirectoryRecursive_requestOverride_requestOverrideShouldTakePrecedence() {
        transferManagerConfiguration = TransferManagerConfiguration.builder()
                                                                   .uploadDirectoryConfiguration(UploadDirectoryOverrideConfiguration.builder().recursive(true).build())
                                                                   .build();
        UploadDirectoryRequest uploadDirectoryRequest = UploadDirectoryRequest.builder()
                                                                              .bucket("bucket")
                                                                              .sourceDirectory(Paths.get("."))
                                                                              .overrideConfiguration(o -> o.recursive(false))
                                                                              .build();
        assertThat(transferManagerConfiguration.resolveUploadDirectoryRecursive(uploadDirectoryRequest)).isFalse();
    }

    @Test
    public void resolveMaxDepth_requestOverride_requestOverrideShouldTakePrecedence() {
        transferManagerConfiguration = TransferManagerConfiguration.builder()
                                                                   .uploadDirectoryConfiguration(UploadDirectoryOverrideConfiguration.builder()
                                                                                                                                     .maxDepth(1)
                                                                                                                                     .build())
                                                                   .build();
        UploadDirectoryRequest uploadDirectoryRequest = UploadDirectoryRequest.builder()
                                                                              .bucket("bucket")
                                                                              .sourceDirectory(Paths.get("."))
                                                                              .overrideConfiguration(o -> o.maxDepth(2))
                                                                              .build();
        assertThat(transferManagerConfiguration.resolveUploadDirectoryMaxDepth(uploadDirectoryRequest)).isEqualTo(2);
    }

    @Test
    public void resolveFollowSymlinks_requestOverride_requestOverrideShouldTakePrecedence() {
        transferManagerConfiguration = TransferManagerConfiguration.builder()
                                                                   .uploadDirectoryConfiguration(UploadDirectoryOverrideConfiguration.builder()
                                                                                                                                     .followSymbolicLinks(false)
                                                                                                                                     .build())
                                                                   .build();
        UploadDirectoryRequest uploadDirectoryRequest = UploadDirectoryRequest.builder()
                                                                              .bucket("bucket")
                                                                              .sourceDirectory(Paths.get("."))
                                                                              .overrideConfiguration(o -> o.followSymbolicLinks(true))
                                                                              .build();
        assertThat(transferManagerConfiguration.resolveUploadDirectoryFollowSymbolicLinks(uploadDirectoryRequest)).isTrue();
    }

    @Test
    public void noOverride_shouldUseDefaults() {
        transferManagerConfiguration = TransferManagerConfiguration.builder().build();
        assertThat(transferManagerConfiguration.option(UPLOAD_DIRECTORY_FOLLOW_SYMBOLIC_LINKS)).isFalse();
        assertThat(transferManagerConfiguration.option(UPLOAD_DIRECTORY_MAX_DEPTH)).isEqualTo(Integer.MAX_VALUE);
        assertThat(transferManagerConfiguration.option(UPLOAD_DIRECTORY_RECURSIVE)).isTrue();
        assertThat(transferManagerConfiguration.option(EXECUTOR)).isNotNull();
    }

    @Test
    public void close_noCustomExecutor_shouldCloseDefaultOne() {
        transferManagerConfiguration = TransferManagerConfiguration.builder().build();
        transferManagerConfiguration.close();
        ExecutorService executor = (ExecutorService) transferManagerConfiguration.option(EXECUTOR);
        assertThat(executor.isShutdown()).isTrue();
    }

    @Test
    public void close_customExecutor_shouldNotCloseCustomExecutor() {
        ExecutorService executorService = Mockito.mock(ExecutorService.class);
        transferManagerConfiguration = TransferManagerConfiguration.builder().executor(executorService).build();
        transferManagerConfiguration.close();
        verify(executorService, never()).shutdown();
    }
}
