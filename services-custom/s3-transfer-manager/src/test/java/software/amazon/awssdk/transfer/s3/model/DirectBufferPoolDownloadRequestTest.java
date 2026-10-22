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

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

class DirectBufferPoolDownloadRequestTest {

    @Test
    void noGetObjectRequest_throws() {
        assertThatThrownBy(() -> DirectBufferPoolDownloadRequest.builder().build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("getObjectRequest");
    }

    @Test
    void consumerGetObjectRequest_shouldBuildRequest() {
        DirectBufferPoolDownloadRequest request = DirectBufferPoolDownloadRequest.builder()
                                                                                 .getObjectRequest(b -> b.bucket("bucket")
                                                                                                         .key("key"))
                                                                                 .build();

        assertThat(request.getObjectRequest()).isEqualTo(GetObjectRequest.builder().bucket("bucket").key("key").build());
        assertThat(request.transferListeners()).isNull();
    }

    @Test
    void toBuilder_shouldCopyAllFields() {
        TransferListener listener = LoggingTransferListener.create();
        DirectBufferPoolDownloadRequest request = DirectBufferPoolDownloadRequest.builder()
                                                                                 .getObjectRequest(b -> b.bucket("bucket")
                                                                                                         .key("key"))
                                                                                 .addTransferListener(listener)
                                                                                 .build();

        DirectBufferPoolDownloadRequest copy = request.toBuilder().build();

        assertThat(copy).isEqualTo(request);
        assertThat(copy.transferListeners()).containsExactly(listener);
    }

    @Test
    void equals_hashcode() {
        EqualsVerifier.forClass(DirectBufferPoolDownloadRequest.class)
                      .withNonnullFields("getObjectRequest")
                      .verify();
    }
}
