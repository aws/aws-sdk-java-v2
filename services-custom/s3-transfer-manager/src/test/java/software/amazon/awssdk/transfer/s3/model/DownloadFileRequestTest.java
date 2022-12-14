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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class DownloadFileRequestTest {

    @Test
    void noGetObjectRequest_throws() {
        assertThatThrownBy(() -> DownloadFileRequest.builder()
                                                    .destination(Paths.get("."))
                                                    .build()).isInstanceOf(NullPointerException.class).hasMessageContaining(
                                                        "getObjectRequest");
    }

    @Test
    public void pathMissing_throws() {
        assertThatThrownBy(() -> DownloadFileRequest.builder()
                                                    .getObjectRequest(b -> b.bucket("bucket").key("key"))
                                                    .build()).isInstanceOf(NullPointerException.class).hasMessageContaining(
            "destination");
    }

    @Test
    public void usingFile() {
        Path path = Paths.get(".");
        DownloadFileRequest requestUsingFile = DownloadFileRequest.builder()
                                                                  .getObjectRequest(b -> b.bucket("bucket").key("key"))
                                                                  .destination(path.toFile())
                                                                  .build();

        assertThat(requestUsingFile.destination()).isEqualTo(path);
    }

    @Test
    void usingFile_null_shouldThrowException() {
        File file = null;

        assertThatThrownBy(() -> DownloadFileRequest.builder()
                                                    .getObjectRequest(b -> b.bucket("bucket").key("key"))
                                                    .destination(file)
                                                    .build()).isInstanceOf(NullPointerException.class).hasMessageContaining(
            "destination");

    }

    @Test
    void equals_hashcode() {
        EqualsVerifier.forClass(DownloadFileRequest.class)
                      .withNonnullFields("destination", "getObjectRequest")
                      .verify();
    }
}
