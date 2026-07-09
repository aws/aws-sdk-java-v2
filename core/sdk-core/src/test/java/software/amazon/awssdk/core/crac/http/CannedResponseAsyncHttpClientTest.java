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

package software.amazon.awssdk.core.crac.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SimpleSubscriber;

class CannedResponseAsyncHttpClientTest {

    private static final byte[] PAYLOAD = "{\"StringMember\":\"warmup\"}".getBytes(StandardCharsets.UTF_8);

    @Test
    void execute_whenNoStatusGiven_signalsDefault200() {
        CannedResponseAsyncHttpClient client = CannedResponseAsyncHttpClient.builder().responseBody(PAYLOAD).build();
        RecordingResponseHandler handler = new RecordingResponseHandler();

        client.execute(requestWith(handler)).join();

        assertThat(handler.headers.statusCode()).isEqualTo(200);
    }

    @Test
    void execute_whenStatusGiven_signalsConfiguredStatus() {
        CannedResponseAsyncHttpClient client =
            CannedResponseAsyncHttpClient.builder().responseBody(PAYLOAD).statusCode(500).build();
        RecordingResponseHandler handler = new RecordingResponseHandler();

        client.execute(requestWith(handler)).join();

        assertThat(handler.headers.statusCode()).isEqualTo(500);
    }

    @Test
    void execute_whenInvoked_streamsConfiguredBody() {
        CannedResponseAsyncHttpClient client = CannedResponseAsyncHttpClient.builder().responseBody(PAYLOAD).build();
        RecordingResponseHandler handler = new RecordingResponseHandler();

        client.execute(requestWith(handler)).join();

        assertThat(handler.drainBody()).isEqualTo(PAYLOAD);
    }

    @Test
    void execute_whenNoBodyGiven_signalsDefault200AndEmptyBody() {
        CannedResponseAsyncHttpClient client = CannedResponseAsyncHttpClient.builder().build();
        RecordingResponseHandler handler = new RecordingResponseHandler();

        client.execute(requestWith(handler)).join();

        assertThat(handler.headers.statusCode()).isEqualTo(200);
        assertThat(handler.drainBody()).isEmpty();
    }

    @Test
    void execute_whenInvoked_returnsCompletedFuture() {
        CannedResponseAsyncHttpClient client = CannedResponseAsyncHttpClient.builder().responseBody(PAYLOAD).build();

        CompletableFuture<Void> future = client.execute(requestWith(new RecordingResponseHandler()));

        assertThat(future).isCompleted();
    }

    @Test
    void close_isSafeNoOp() {
        CannedResponseAsyncHttpClient client = CannedResponseAsyncHttpClient.builder().build();

        assertThatCode(client::close).doesNotThrowAnyException();
    }

    private static AsyncExecuteRequest requestWith(SdkAsyncHttpResponseHandler handler) {
        return AsyncExecuteRequest.builder()
                                  .responseHandler(handler)
                                  .build();
    }

    private static final class RecordingResponseHandler implements SdkAsyncHttpResponseHandler {

        private SdkHttpResponse headers;
        private Publisher<ByteBuffer> stream;

        @Override
        public void onHeaders(SdkHttpResponse headers) {
            this.headers = headers;
        }

        @Override
        public void onStream(Publisher<ByteBuffer> stream) {
            this.stream = stream;
        }

        @Override
        public void onError(Throwable error) {
        }

        private byte[] drainBody() {
            ByteArrayOutputStream collected = new ByteArrayOutputStream();
            CompletableFuture<Void> done = new CompletableFuture<>();
            stream.subscribe(new SimpleSubscriber(byteBuffer -> {
                byte[] chunk = new byte[byteBuffer.remaining()];
                byteBuffer.get(chunk);
                collected.write(chunk, 0, chunk.length);
            }) {
                @Override
                public void onComplete() {
                    super.onComplete();
                    done.complete(null);
                }

                @Override
                public void onError(Throwable t) {
                    super.onError(t);
                    done.completeExceptionally(t);
                }
            });
            done.join();
            return collected.toByteArray();
        }
    }
}
