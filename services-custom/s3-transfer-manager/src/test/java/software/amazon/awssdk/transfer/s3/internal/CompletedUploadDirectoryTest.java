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

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedUpload;
import software.amazon.awssdk.transfer.s3.CompletedUploadDirectory;
import software.amazon.awssdk.transfer.s3.FailedUpload;

public class CompletedUploadDirectoryTest {

    @Test
    public void equalsHashcode() {
        List<FailedUpload> failedUploads = Arrays.asList(DefaultFailedUpload.builder().path(Paths.get(".")).build());
        List<CompletedUpload> completedUploads = Arrays.asList(DefaultCompletedUpload.builder().response(PutObjectResponse.builder().build()).build());
        CompletedUploadDirectory completedUploadDirectory = DefaultCompletedUploadDirectory.builder()
                                                                                           .failedUploads(failedUploads)
                                                                                           .successfulUploads(completedUploads)
                                                                                           .build();

        CompletedUploadDirectory completedUploadDirectory2 = DefaultCompletedUploadDirectory.builder()
                                                                                           .failedUploads(failedUploads)
                                                                                           .successfulUploads(completedUploads)
                                                                                           .build();

        CompletedUploadDirectory completedUploadDirectory3 = DefaultCompletedUploadDirectory.builder()
                                                                                           .build();

        assertThat(completedUploadDirectory).isEqualTo(completedUploadDirectory2);
        assertThat(completedUploadDirectory.hashCode()).isEqualTo(completedUploadDirectory2.hashCode());
        assertThat(completedUploadDirectory2).isNotEqualTo(completedUploadDirectory3);
        assertThat(completedUploadDirectory2.hashCode()).isNotEqualTo(completedUploadDirectory3);
    }
}
