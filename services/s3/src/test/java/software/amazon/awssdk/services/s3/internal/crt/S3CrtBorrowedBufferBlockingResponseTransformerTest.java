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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class S3CrtBorrowedBufferBlockingResponseTransformerTest {

    @Test
    void borrowedStream_whenConsumed_shouldReturnResponseAndReleaseWithCredit() throws Exception {
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credit = new AtomicLong();
        S3CrtBorrowedBuffer buffer = new S3CrtBorrowedBuffer(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                             3,
                                                             releases::incrementAndGet,
                                                             credit::addAndGet);
        S3CrtBorrowedBufferBlockingResponseTransformer transformer =
            new S3CrtBorrowedBufferBlockingResponseTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        GetObjectResponse response = GetObjectResponse.builder().contentLength(3L).build();
        transformer.onResponse(response);

        transformer.onBorrowedStreamStart(() -> {
        });
        transformer.onBorrowedBuffer(buffer);
        transformer.onBorrowedStreamComplete();

        try (ResponseInputStream<GetObjectResponse> stream = future.join()) {
            byte[] bytes = new byte[3];
            assertThat(stream.response()).isSameAs(response);
            assertThat(stream.read(bytes)).isEqualTo(3);
            assertThat(bytes).isEqualTo("abc".getBytes(UTF_8));
            assertThat(stream.read()).isEqualTo(-1);
        }
        assertThat(releases).hasValue(1);
        assertThat(credit).hasValue(3);
    }

    @Test
    void timeout_shouldDiscardWithoutCreditAndRemainVisibleToReader() throws Exception {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credit = new AtomicLong();
        S3CrtBorrowedBuffer buffer = new S3CrtBorrowedBuffer(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                             3,
                                                             releases::incrementAndGet,
                                                             credit::addAndGet);
        S3CrtBorrowedBufferBlockingResponseTransformer transformer =
            new S3CrtBorrowedBufferBlockingResponseTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().contentLength(3L).build());
        transformer.onBorrowedBuffer(buffer);
        transformer.onBorrowedStreamStart(cancellations::incrementAndGet);
        ResponseInputStream<GetObjectResponse> stream = future.join();
        ApiCallTimeoutException timeout = ApiCallTimeoutException.create(1);

        transformer.onBorrowedStreamAbort(timeout);

        assertThat(releases).hasValue(1);
        assertThat(credit).hasValue(0);
        assertThat(cancellations).hasValue(1);
        assertThatThrownBy(stream::read)
            .isInstanceOf(IOException.class)
            .hasCause(timeout);
    }

    @Test
    void futureCancelledBeforeStreamStart_shouldRunCancellationWhenStarted() {
        AtomicInteger cancellations = new AtomicInteger();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer =
            new S3CrtBorrowedBufferBlockingResponseTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());

        future.cancel(true);
        transformer.onBorrowedStreamStart(cancellations::incrementAndGet);

        assertThat(cancellations).hasValue(1);
    }
}
