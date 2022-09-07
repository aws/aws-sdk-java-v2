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

package software.amazon.awssdk.transfer.s3.model;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.time.Instant;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ResumableFileUploadTest {

    private static FileSystem jimfs;

    @Test
    void equalsHashcode() {
        EqualsVerifier.forClass(ResumableFileUpload.class)
                      .withNonnullFields("fileLength", "uploadFileRequest", "fileLastModified")
                      .verify();
    }

    @Test
    void toBuilder() {
        ResumableFileUpload fileUpload =
            ResumableFileUpload.builder()
                               .multipartUploadId("1234")
                               .uploadFileRequest(UploadFileRequest.builder().putObjectRequest(p -> p.bucket("bucket").key("key"
                               )).source(Paths.get("test")).build())
                               .fileLastModified(Instant.now())
                               .fileLength(10L)
                               .partSizeInBytes(10 * MB)
                               .build();

        assertThat(fileUpload.toBuilder().build()).isEqualTo(fileUpload);
        assertThat(fileUpload.toBuilder().multipartUploadId("5678").build()).isNotEqualTo(fileUpload);
    }

}
