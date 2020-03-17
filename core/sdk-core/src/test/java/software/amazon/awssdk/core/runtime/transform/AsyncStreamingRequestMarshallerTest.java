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

package software.amazon.awssdk.core.runtime.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

@RunWith(MockitoJUnitRunner.class)
public class AsyncStreamingRequestMarshallerTest {

    private static final Object object = new Object();

    @Mock
    private Marshaller delegate;

    @Mock
    private AsyncRequestBody requestBody;

    private SdkHttpFullRequest request = generateBasicRequest();

    @Before
    public void setup() {
        when(delegate.marshall(any())).thenReturn(request);
    }

    @Test
    public void contentLengthIsPresent_shouldNotOverride() {
        long contentLengthOnRequest = 1L;
        long contengLengthOnRequestBody = 5L;
        when(requestBody.contentLength()).thenReturn(Optional.of(contengLengthOnRequestBody));

        AsyncStreamingRequestMarshaller marshaller = createMarshaller(true, true, true);

        SdkHttpFullRequest requestWithContentLengthHeader = generateBasicRequest().toBuilder()
                                                                                  .appendHeader(Header.CONTENT_LENGTH,
                                                                                   String.valueOf(contentLengthOnRequest))
                                                                                  .build();


        when(delegate.marshall(any())).thenReturn(requestWithContentLengthHeader);
        SdkHttpFullRequest httpFullRequest = marshaller.marshall(object);

        assertThat(httpFullRequest.firstMatchingHeader(Header.CONTENT_LENGTH)).isPresent();
        assertContentLengthValue(httpFullRequest, contentLengthOnRequest);
    }


    @Test
    public void contentLengthOnRequestBody_shouldAddContentLengthHeader() {
        long value = 5L;
        when(requestBody.contentLength()).thenReturn(Optional.of(value));

        AsyncStreamingRequestMarshaller marshaller = createMarshaller(true, true, true);
        SdkHttpFullRequest httpFullRequest = marshaller.marshall(object);

        assertThat(httpFullRequest.firstMatchingHeader(Header.CONTENT_LENGTH)).isPresent();
        assertContentLengthValue(httpFullRequest, value);
        assertThat(httpFullRequest.firstMatchingHeader(Header.TRANSFER_ENCODING)).isEmpty();
    }

    @Test
    public void throwsException_contentLengthHeaderIsMissing_AndRequiresLengthIsPresent() {
        when(requestBody.contentLength()).thenReturn(Optional.empty());

        AsyncStreamingRequestMarshaller marshaller = createMarshaller(true, false, false);
        assertThatThrownBy(() -> marshaller.marshall(object)).isInstanceOf(SdkClientException.class);
    }

    @Test
    public void transferEncodingIsUsed_OverHttp1() {
        when(requestBody.contentLength()).thenReturn(Optional.empty());

        AsyncStreamingRequestMarshaller marshaller = createMarshaller(false, true, false);
        SdkHttpFullRequest httpFullRequest = marshaller.marshall(object);

        assertThat(httpFullRequest.firstMatchingHeader(Header.CONTENT_LENGTH)).isEmpty();
        assertThat(httpFullRequest.firstMatchingHeader(Header.TRANSFER_ENCODING)).isPresent();
    }

    @Test
    public void transferEncodingIsNotUsed_OverHttp2() {
        when(requestBody.contentLength()).thenReturn(Optional.empty());

        AsyncStreamingRequestMarshaller marshaller = createMarshaller(false, true, true);
        SdkHttpFullRequest httpFullRequest = marshaller.marshall(object);

        assertThat(httpFullRequest.firstMatchingHeader(Header.CONTENT_LENGTH)).isEmpty();
        assertThat(httpFullRequest.firstMatchingHeader(Header.TRANSFER_ENCODING)).isEmpty();
    }

    private void assertContentLengthValue(SdkHttpFullRequest httpFullRequest, long value) {
        assertThat(httpFullRequest.firstMatchingHeader(Header.CONTENT_LENGTH).get())
            .contains(Long.toString(value));
    }

    private AsyncStreamingRequestMarshaller createMarshaller(boolean requiresLength,
                                                             boolean transferEncoding,
                                                             boolean useHttp2) {
        return AsyncStreamingRequestMarshaller.builder()
                                              .delegateMarshaller(delegate)
                                              .asyncRequestBody(requestBody)
                                              .requiresLength(requiresLength)
                                              .transferEncoding(transferEncoding)
                                              .useHttp2(useHttp2)
                                              .build();
    }

    private SdkHttpFullRequest generateBasicRequest() {
        return SdkHttpFullRequest.builder()
                                 .contentStreamProvider(() -> new ByteArrayInputStream("{\"TableName\": \"foo\"}".getBytes()))
                                 .method(SdkHttpMethod.POST)
                                 .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                 .putHeader("x-amz-archive-description", "test  test")
                                 .encodedPath("/")
                                 .uri(URI.create("http://demo.us-east-1.amazonaws.com"))
                                 .build();
    }
}
