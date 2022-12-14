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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Paths;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FailedFileDownload;

class FailedFileDownloadTest {

    @Test
    void requestNull_mustThrowException() {
        assertThatThrownBy(() -> FailedFileDownload.builder()
                                                   .exception(SdkClientException.create("xxx")).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("request must not be null");
    }

    @Test
    void exceptionNull_mustThrowException() {
        DownloadFileRequest downloadFileRequest =
            DownloadFileRequest.builder().destination(Paths.get(".")).getObjectRequest(p -> p.bucket("bucket").key("key")).build();
        assertThatThrownBy(() -> FailedFileDownload.builder()
                                                   .request(downloadFileRequest).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("exception must not be null");
    }

    @Test
    void equalsHashcode() {
        EqualsVerifier.forClass(FailedFileDownload.class)
                      .withNonnullFields("exception", "request")
                      .verify();
    }
}
