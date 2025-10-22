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
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_DECODED_CONTENT_LENGTH;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;

public class SignerUtilsTest {

    @Test
    void moveContentLength_decodedContentLengthPresent_shouldNotInvokeNewStream() {
        SdkHttpRequest.Builder request = SdkHttpRequest.builder()
            .appendHeader(X_AMZ_DECODED_CONTENT_LENGTH, "10")
            .appendHeader(CONTENT_LENGTH, "10");

        ContentStreamProvider streamProvider = Mockito.mock(ContentStreamProvider.class);
        long contentLength = SignerUtils.moveContentLength(request, streamProvider);
        Mockito.verify(streamProvider, Mockito.never()).newStream();
        assertThat(contentLength).isEqualTo(10L);
        assertThat(request.firstMatchingHeader(CONTENT_LENGTH)).isEmpty();
        assertThat(request.firstMatchingHeader(X_AMZ_DECODED_CONTENT_LENGTH)).contains("10");
    }

    @Test
    void moveContentLength_contentLengthPresent_shouldNotInvokeNewStream() {
        SdkHttpRequest.Builder request = SdkHttpRequest.builder()
                                                       .appendHeader(CONTENT_LENGTH, "10");

        ContentStreamProvider streamProvider = Mockito.mock(ContentStreamProvider.class);
        long contentLength = SignerUtils.moveContentLength(request, streamProvider);
        Mockito.verify(streamProvider, Mockito.never()).newStream();
        assertThat(contentLength).isEqualTo(10L);
        assertThat(request.firstMatchingHeader(CONTENT_LENGTH)).isEmpty();
        assertThat(request.firstMatchingHeader(X_AMZ_DECODED_CONTENT_LENGTH)).contains("10");
    }

    public static Stream<Arguments> streams() {
        return Stream.of(Arguments.of(new ByteArrayInputStream("hello".getBytes()), 5),
                         Arguments.of(null, 0));
    }


    @ParameterizedTest
    @MethodSource("streams")
    void moveContentLength_contentLengthNotPresent_shouldInvokeNewStream(InputStream inputStream, long expectedLength) {
        SdkHttpRequest.Builder request = SdkHttpRequest.builder();

        ContentStreamProvider streamProvider = Mockito.mock(ContentStreamProvider.class);
        Mockito.when(streamProvider.newStream()).thenReturn(inputStream);

        long contentLength = SignerUtils.moveContentLength(request, streamProvider);
        Mockito.verify(streamProvider, Mockito.times(1)).newStream();
        assertThat(contentLength).isEqualTo(expectedLength);
        assertThat(request.firstMatchingHeader(CONTENT_LENGTH)).isEmpty();
        assertThat(request.firstMatchingHeader(X_AMZ_DECODED_CONTENT_LENGTH)).contains(String.valueOf(expectedLength));
    }
}
