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

package software.amazon.awssdk.http.auth.aws.internal.signer.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.X_AMZ_DECODED_CONTENT_LENGTH;

import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;

public class SignerUtilsTest {

    @Test
    void computeAndMoveContentLength_decodedContentLengthPresent_shouldNotInvokeNewStream() {
        SdkHttpRequest.Builder request = SdkHttpRequest.builder()
            .appendHeader(X_AMZ_DECODED_CONTENT_LENGTH, "10")
            .appendHeader(CONTENT_LENGTH, "10");

        ContentStreamProvider streamProvider = Mockito.mock(ContentStreamProvider.class);
        long contentLength = SignerUtils.computeAndMoveContentLength(request, streamProvider);
        Mockito.verify(streamProvider, Mockito.never()).newStream();
        assertThat(contentLength).isEqualTo(10L);
        assertThat(request.firstMatchingHeader(CONTENT_LENGTH)).isEmpty();
        assertThat(request.firstMatchingHeader(X_AMZ_DECODED_CONTENT_LENGTH)).contains("10");
    }

    @Test
    void computeAndMoveContentLength_contentLengthPresent_shouldNotInvokeNewStream() {
        SdkHttpRequest.Builder request = SdkHttpRequest.builder()
                                                       .appendHeader(CONTENT_LENGTH, "10");

        ContentStreamProvider streamProvider = Mockito.mock(ContentStreamProvider.class);
        long contentLength = SignerUtils.computeAndMoveContentLength(request, streamProvider);
        Mockito.verify(streamProvider, Mockito.never()).newStream();
        assertThat(contentLength).isEqualTo(10L);
        assertThat(request.firstMatchingHeader(CONTENT_LENGTH)).isEmpty();
        assertThat(request.firstMatchingHeader(X_AMZ_DECODED_CONTENT_LENGTH)).contains("10");
    }

    @ParameterizedTest
    @MethodSource("streams")
    void computeAndMoveContentLength_contentLengthNotPresent_shouldInvokeNewStream(InputStream inputStream, long expectedLength) {
        SdkHttpRequest.Builder request = SdkHttpRequest.builder();

        ContentStreamProvider streamProvider = Mockito.mock(ContentStreamProvider.class);
        Mockito.when(streamProvider.newStream()).thenReturn(inputStream);

        long contentLength = SignerUtils.computeAndMoveContentLength(request, streamProvider);
        Mockito.verify(streamProvider, Mockito.times(1)).newStream();
        assertThat(contentLength).isEqualTo(expectedLength);
        assertThat(request.firstMatchingHeader(CONTENT_LENGTH)).isEmpty();
        assertThat(request.firstMatchingHeader(X_AMZ_DECODED_CONTENT_LENGTH)).contains(String.valueOf(expectedLength));
    }

    @Test
    void moveContentLength_async_decodedContentLengthPresent_shouldNotSubscribeToPublisher() {

        SdkHttpRequest.Builder request = SdkHttpRequest.builder()
                                                       .appendHeader(X_AMZ_DECODED_CONTENT_LENGTH, "10")
                                                       .appendHeader(CONTENT_LENGTH, "10");

        Publisher<ByteBuffer> contentPublisher = Mockito.spy(Flowable.empty());

        SignerUtils.moveContentLength(request, contentPublisher).join();
        Mockito.verify(contentPublisher, Mockito.never()).subscribe(Mockito.any(Subscriber.class));

        assertThat(request.firstMatchingHeader(CONTENT_LENGTH)).isEmpty();
        assertThat(request.firstMatchingHeader(X_AMZ_DECODED_CONTENT_LENGTH)).contains("10");
    }

    @Test
    void moveContentLength_async_contentLengthPresent_shouldNotSubscribeToPublisher() {
        SdkHttpRequest.Builder request = SdkHttpRequest.builder()
                                                       .appendHeader(CONTENT_LENGTH, "10");

        Publisher<ByteBuffer> contentPublisher = Mockito.spy(Flowable.empty());

        SignerUtils.moveContentLength(request, contentPublisher).join();
        Mockito.verify(contentPublisher, Mockito.never()).subscribe(Mockito.any(Subscriber.class));

        assertThat(request.firstMatchingHeader(CONTENT_LENGTH)).isEmpty();
        assertThat(request.firstMatchingHeader(X_AMZ_DECODED_CONTENT_LENGTH)).contains("10");
    }

    @Test
    void moveContentLength_contentLengthNotPresent_throws() {
        SdkHttpRequest.Builder request = SdkHttpRequest.builder();

        Publisher<ByteBuffer> contentPublisher = Flowable.just(ByteBuffer.wrap("content".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> SignerUtils.moveContentLength(request, contentPublisher).join())
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Content-Length header must be specified");

    }

    public static Stream<Arguments> streams() {
        return Stream.of(Arguments.of(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), 5),
                         Arguments.of(null, 0));
    }

    public static Stream<Arguments> publishers() {
        return Stream.of(Arguments.of(Flowable.just(ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8))), 5),
                         Arguments.of(null, 0));
    }
}
