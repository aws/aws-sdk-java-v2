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
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.crt.S3CrtResponseTransformerFactory;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

class DirectBufferPoolDownloadRequestTest {

    @Test
    void noGetObjectRequest_throws() {
        assertThatThrownBy(() -> DirectBufferPoolDownloadRequest.builder()
                                                                .responseTransformer(pooledTransformer())
                                                                .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("getObjectRequest");
    }

    @Test
    void noResponseTransformer_throws() {
        assertThatThrownBy(() -> DirectBufferPoolDownloadRequest.builder()
                                                                .responseTransformer(pooledTransformer())
                                                                .responseTransformer(null)
                                                                .getObjectRequest(b -> b.bucket("bucket").key("key"))
                                                                .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("responseTransformer");
    }

    @Test
    void typedBuilder_shouldKeepFieldsSetBeforeTransformer() {
        TransferListener listener = LoggingTransferListener.create();
        AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>> transformer = pooledTransformer();

        DirectBufferPoolDownloadRequest<ResponseInputStream<GetObjectResponse>> request =
            DirectBufferPoolDownloadRequest.builder()
                                           .getObjectRequest(b -> b.bucket("bucket").key("key"))
                                           .addTransferListener(listener)
                                           .responseTransformer(transformer)
                                           .build();

        assertThat(request.getObjectRequest()).isEqualTo(GetObjectRequest.builder().bucket("bucket").key("key").build());
        assertThat(request.transferListeners()).containsExactly(listener);
        assertThat(request.responseTransformer()).isSameAs(transformer);
    }

    @Test
    void toBuilder_shouldCopyAllFields() {
        DirectBufferPoolDownloadRequest<ResponseInputStream<GetObjectResponse>> request =
            DirectBufferPoolDownloadRequest.builder()
                                           .getObjectRequest(b -> b.bucket("bucket").key("key"))
                                           .addTransferListener(LoggingTransferListener.create())
                                           .responseTransformer(pooledTransformer())
                                           .build();

        DirectBufferPoolDownloadRequest<ResponseInputStream<GetObjectResponse>> copy = request.toBuilder().build();

        assertThat(copy).isEqualTo(request);
        assertThat(copy.responseTransformer()).isSameAs(request.responseTransformer());
    }

    @Test
    void equals_hashcode() {
        EqualsVerifier.forClass(DirectBufferPoolDownloadRequest.class)
                      .withNonnullFields("getObjectRequest", "responseTransformer")
                      .verify();
    }

    private static AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>> pooledTransformer() {
        return S3CrtResponseTransformerFactory.toBlockingInputStreamFromDirectBufferPool();
    }
}
