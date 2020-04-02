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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

/**
 * Currently RequestBody always require content length. So we always sent content-length header for all sync APIs.
 * So this test class only tests the case when content length is present
 */
@RunWith(MockitoJUnitRunner.class)
public class SyncStreamingRequestMarshallerTest {

    private static final Object object = new Object();

    @Mock
    private Marshaller delegate;

    private SdkHttpFullRequest request = generateBasicRequest();

    @Before
    public void setup() {
        when(delegate.marshall(any())).thenReturn(request);
    }

    @Test
    public void contentLengthHeaderIsSet_IfPresent() {
        String text = "foobar";
        StreamingRequestMarshaller marshaller = createMarshaller(RequestBody.fromString(text), true, true, true);
        SdkHttpFullRequest httpFullRequest = marshaller.marshall(object);

        assertThat(httpFullRequest.firstMatchingHeader(Header.CONTENT_LENGTH)).isPresent();
        assertContentLengthValue(httpFullRequest, text.length());
        assertThat(httpFullRequest.firstMatchingHeader(Header.TRANSFER_ENCODING)).isEmpty();
    }

    @Test
    public void contentLengthHeaderIsSet_forEmptyContent() {
        StreamingRequestMarshaller marshaller = createMarshaller(RequestBody.empty(), true, true, true);
        SdkHttpFullRequest httpFullRequest = marshaller.marshall(object);

        assertThat(httpFullRequest.firstMatchingHeader(Header.CONTENT_LENGTH)).isPresent();
        assertContentLengthValue(httpFullRequest, 0L);
        assertThat(httpFullRequest.firstMatchingHeader(Header.TRANSFER_ENCODING)).isEmpty();
    }

    private void assertContentLengthValue(SdkHttpFullRequest httpFullRequest, long value) {
        assertThat(httpFullRequest.firstMatchingHeader(Header.CONTENT_LENGTH).get())
            .contains(Long.toString(value));
    }

    private StreamingRequestMarshaller createMarshaller(RequestBody requestBody,
                                                        boolean requiresLength,
                                                        boolean transferEncoding,
                                                        boolean useHttp2) {
        return StreamingRequestMarshaller.builder()
                                         .delegateMarshaller(delegate)
                                         .requestBody(requestBody)
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
