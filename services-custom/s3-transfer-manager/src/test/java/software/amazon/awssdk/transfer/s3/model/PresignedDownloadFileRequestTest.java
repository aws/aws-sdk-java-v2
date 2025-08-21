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
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;

class PresignedDownloadFileRequestTest {

    @Test
    void build_withoutPresignedUrlDownloadRequest_throwsNullPointerException() {
        assertThatThrownBy(() -> PresignedDownloadFileRequest.builder()
                                                            .destination(Paths.get("."))
                                                            .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("presignedUrlDownloadRequest");
    }

    @Test
    void build_withoutDestination_throwsNullPointerException() {
        assertThatThrownBy(() -> PresignedDownloadFileRequest.builder()
                                                            .presignedUrlDownloadRequest(createPresignedRequest())
                                                            .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("destination");
    }

    @Test
    void build_withPath_setsDestinationCorrectly() {
        Path path = Paths.get(".");
        PresignedUrlDownloadRequest presignedRequest = createPresignedRequest();

        PresignedDownloadFileRequest request = PresignedDownloadFileRequest.builder()
                                                                          .destination(path)
                                                                          .presignedUrlDownloadRequest(presignedRequest)
                                                                          .build();

        assertThat(request.destination()).isEqualTo(path);
        assertThat(request.presignedUrlDownloadRequest()).isEqualTo(presignedRequest);
    }

    @Test
    void build_withFile_convertsToPathCorrectly() {
        Path path = Paths.get(".");
        PresignedUrlDownloadRequest presignedRequest = createPresignedRequest();

        PresignedDownloadFileRequest request = PresignedDownloadFileRequest.builder()
                                                                          .destination(path.toFile())
                                                                          .presignedUrlDownloadRequest(presignedRequest)
                                                                          .build();

        assertThat(request.destination()).isEqualTo(path);
    }

    @Test
    void destination_withNullFile_throwsNullPointerException() {
        File file = null;

        assertThatThrownBy(() -> PresignedDownloadFileRequest.builder()
                                                            .destination(file))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("destination");
    }

    @Test
    void presignedUrlDownloadRequest_withConsumerBuilder_buildsCorrectly() {
        Path path = Paths.get(".");

        PresignedDownloadFileRequest request = PresignedDownloadFileRequest.builder()
                                                                          .destination(path)
                                                                          .presignedUrlDownloadRequest(b -> b.presignedUrl(createTestUrl()))
                                                                          .build();

        assertThat(request.presignedUrlDownloadRequest()).isNotNull();
        assertThat(request.presignedUrlDownloadRequest().presignedUrl()).isEqualTo(createTestUrl());
    }

    @Test
    void equalsHashCodeTest() {
        EqualsVerifier.forClass(PresignedDownloadFileRequest.class)
                      .withNonnullFields("destination", "presignedUrlDownloadRequest")
                      .verify();
    }

    private PresignedUrlDownloadRequest createPresignedRequest() {
        return PresignedUrlDownloadRequest.builder()
                                         .presignedUrl(createTestUrl())
                                         .build();
    }

    private URL createTestUrl() {
        try {
            return new URL("https://example.com/test");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}