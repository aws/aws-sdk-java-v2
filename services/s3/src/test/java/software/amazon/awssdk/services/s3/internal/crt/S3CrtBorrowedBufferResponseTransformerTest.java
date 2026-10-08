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

package software.amazon.awssdk.services.s3.internal.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class S3CrtBorrowedBufferResponseTransformerTest {

    @Test
    void factory_shouldReturnBorrowedStreamTransformer() {
        AsyncResponseTransformer<GetObjectResponse, ?> transformer =
            S3AsyncResponseTransformer.toBlockingInputStreamWithBorrowedBuffers();

        assertThat(transformer).isInstanceOf(S3CrtBorrowedBufferResponseTransformer.class);
        assertThat(transformer.name()).isEqualTo(AsyncResponseTransformer.TransformerType.STREAM.getName());
    }

    @Test
    void ordinaryCallbacks_shouldFailClearlyWithoutMaskingOriginalException() {
        S3CrtBorrowedBufferResponseTransformer transformer = new S3CrtBorrowedBufferResponseTransformer();

        assertThatThrownBy(transformer::prepare).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> transformer.onResponse(GetObjectResponse.builder().build()))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> transformer.onStream(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatCode(() -> transformer.exceptionOccurred(new RuntimeException("original"))).doesNotThrowAnyException();
    }
}
