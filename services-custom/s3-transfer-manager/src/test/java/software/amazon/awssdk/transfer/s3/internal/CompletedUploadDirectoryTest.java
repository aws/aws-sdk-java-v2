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
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.transfer.s3.CompletedUploadDirectory;
import software.amazon.awssdk.transfer.s3.FailedSingleFileUpload;
import software.amazon.awssdk.transfer.s3.UploadRequest;

public class CompletedUploadDirectoryTest {

    @Test
    public void equalsHashcode() {
        List<FailedSingleFileUpload> failedUploads = Arrays.asList(FailedSingleFileUpload.builder()
                                                                                         .request(UploadRequest.builder()
                                                                                                               .source(Paths.get("."))
                                                                                                               .putObjectRequest(b -> b.bucket("bucket").key("key")
                                                                                                               )
                                                                                                               .build())
                                                                                         .exception(SdkClientException.create(
                                                                                             "helloworld"))
                                                                                         .build());
        CompletedUploadDirectory completedUploadDirectory = CompletedUploadDirectory.builder()
                                                                                    .failedUploads(failedUploads)
                                                                                    .build();

        CompletedUploadDirectory completedUploadDirectory2 = CompletedUploadDirectory.builder()
                                                                                     .failedUploads(failedUploads)
                                                                                     .build();

        CompletedUploadDirectory completedUploadDirectory3 = CompletedUploadDirectory.builder()
                                                                                     .build();

        assertThat(completedUploadDirectory).isEqualTo(completedUploadDirectory2);
        assertThat(completedUploadDirectory.hashCode()).isEqualTo(completedUploadDirectory2.hashCode());
        assertThat(completedUploadDirectory2).isNotEqualTo(completedUploadDirectory3);
        assertThat(completedUploadDirectory2.hashCode()).isNotEqualTo(completedUploadDirectory3);
    }
}
