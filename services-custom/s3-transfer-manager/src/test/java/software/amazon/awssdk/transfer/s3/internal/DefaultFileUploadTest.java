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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultFileUpload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;

class DefaultFileUploadTest {
    @Test
    void equals_hashcode() {
        EqualsVerifier.forClass(DefaultFileUpload.class)
                      .withNonnullFields("completionFuture", "progress", "request")
                      .verify();
    }

    @Test
    void pause_shouldThrowUnsupportedOperation() {
        TransferProgress transferProgress = Mockito.mock(TransferProgress.class);
        UploadFileRequest request = UploadFileRequest.builder()
                                                   .source(Paths.get("test"))
                                                   .putObjectRequest(p -> p.key("test").bucket("bucket"))
                                                   .build();
        FileUpload fileUpload = new DefaultFileUpload(new CompletableFuture<>(),
                                                      transferProgress,
                                                      request);

        assertThatThrownBy(() -> fileUpload.pause()).isInstanceOf(UnsupportedOperationException.class);
    }
}