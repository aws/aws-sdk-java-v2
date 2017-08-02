/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.unitils.util.ReflectionUtils;
import software.amazon.awssdk.util.Crc32ChecksumValidatingInputStream;
import software.amazon.awssdk.util.StringInputStream;

@RunWith(MockitoJUnitRunner.class)
public class SdkHttpFullResponseAdapterTest {

    private final SdkHttpFullRequest request = SdkHttpFullRequest.builder().build();

    @Test
    public void adapt_SingleHeaderValue_AdaptedCorrectly() throws Exception {
        SimpleSdkHttpFullResponse httpResponse = SimpleSdkHttpFullResponse.builder()
                                                                          .header("FooHeader", "headerValue")
                                                                          .build();

        HttpResponse adapted = adapt(httpResponse);

        assertThat(adapted.getHeader("FooHeader"), equalTo("headerValue"));
    }

    @Test
    public void adapt_StatusTextAndStatusCode_AdaptedCorrectly() throws Exception {
        SimpleSdkHttpFullResponse httpResponse = SimpleSdkHttpFullResponse.builder()
                                                                          .statusText("OK")
                                                                          .statusCode(200)
                                                                          .build();

        HttpResponse adapted = adapt(httpResponse);

        assertThat(adapted.getStatusText(), equalTo("OK"));
        assertThat(adapted.getStatusCode(), equalTo(200));
    }

    @Test
    public void adapt_InputStreamWithNoGzipOrCrc32_NotWrappedWhenAdapted() throws UnsupportedEncodingException {
        InputStream content = new StringInputStream("content");
        SimpleSdkHttpFullResponse httpResponse = SimpleSdkHttpFullResponse.builder()
                                                                          .content(content)
                                                                          .build();

        HttpResponse adapted = adapt(httpResponse);

        assertThat(getField(adapted.getContent(), "in"), equalTo(content));
    }

    @Test
    public void adapt_InputStreamWithCrc32Header_WrappedWithValidatingStream() throws UnsupportedEncodingException {
        InputStream content = new StringInputStream("content");
        SimpleSdkHttpFullResponse httpResponse = SimpleSdkHttpFullResponse.builder()
                                                                          .header("x-amz-crc32", "1234")
                                                                          .content(content)
                                                                          .build();

        HttpResponse adapted = adapt(httpResponse);

        assertThat(adapted.getContent(), instanceOf(Crc32ChecksumValidatingInputStream.class));
    }

    @Test
    public void adapt_InputStreamWithGzipEncoding_WrappedWithDecompressingStream() throws UnsupportedEncodingException {
        InputStream content = getClass().getResourceAsStream("/resources/compressed_json_body.gz");
        SimpleSdkHttpFullResponse httpResponse = SimpleSdkHttpFullResponse.builder()
                                                                          .header("Content-Encoding", "gzip")
                                                                          .content(content)
                                                                          .build();

        HttpResponse adapted = adapt(httpResponse);

        assertThat(adapted.getContent(), instanceOf(GZIPInputStream.class));
    }

    @Test
    public void adapt_CalculateCrcFromCompressed_WrapsWithCrc32ThenGzip() throws UnsupportedEncodingException {
        InputStream content = getClass().getResourceAsStream("/resources/compressed_json_body.gz");
        SimpleSdkHttpFullResponse httpResponse = SimpleSdkHttpFullResponse.builder()
                                                                          .header("Content-Encoding", "gzip")
                                                                          .header("x-amz-crc32", "1234")
                                                                          .content(content)
                                                                          .build();

        HttpResponse adapted = SdkHttpResponseAdapter.adapt(true, request, httpResponse);

        assertThat(adapted.getContent(), instanceOf(GZIPInputStream.class));
    }

    @Test(expected = UncheckedIOException.class)
    public void adapt_InvalidGzipContent_ThrowsException() throws UnsupportedEncodingException {
        InputStream content = new StringInputStream("this isn't GZIP");
        SimpleSdkHttpFullResponse httpResponse = SimpleSdkHttpFullResponse.builder()
                                                                          .header("Content-Encoding", "gzip")
                                                                          .content(content)
                                                                          .build();

        HttpResponse adapted = adapt(httpResponse);

        assertThat(adapted.getContent(), instanceOf(GZIPInputStream.class));
    }

    private HttpResponse adapt(SimpleSdkHttpFullResponse httpResponse) {
        return SdkHttpResponseAdapter.adapt(false, request, httpResponse);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object obj, String fieldName) {
        try {
            Field field = ReflectionUtils.getFieldWithName(obj.getClass(), fieldName, false);
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class SimpleSdkHttpFullResponse implements SdkHttpFullResponse {

        private final Map<String, List<String>> headers;
        private final AbortableInputStream content;
        private final String statusText;
        private final int statusCode;

        private SimpleSdkHttpFullResponse(Builder builder) {
            this.headers = builder.headers;
            this.content = builder.content;
            this.statusText = builder.statusText;
            this.statusCode = builder.statusCode;
        }

        @Override
        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        @Override
        public AbortableInputStream getContent() {
            return content;
        }

        @Override
        public String getStatusText() {
            return statusText;
        }

        @Override
        public int getStatusCode() {
            return statusCode;
        }


        /**
         * @return Builder instance to construct a {@link SimpleSdkHttpFullResponse}.
         */
        public static Builder builder() {
            return new Builder();
        }

        @Override
        public SdkHttpFullResponse.Builder toBuilder() {
            throw new UnsupportedOperationException();
        }

        /**
         * Builder for a {@link SimpleSdkHttpFullResponse}.
         */
        public static final class Builder {

            private final Map<String, List<String>> headers = new HashMap<>();
            private AbortableInputStream content;
            private String statusText;
            private int statusCode;

            private Builder() {
            }

            public Builder header(String headerName, String... values) {
                this.headers.put(headerName, Arrays.asList(values));
                return this;
            }

            public Builder content(InputStream content) {
                this.content = new AbortableInputStream(content, () -> {
                });
                return this;
            }

            public Builder statusText(String statusText) {
                this.statusText = statusText;
                return this;
            }

            public Builder statusCode(int statusCode) {
                this.statusCode = statusCode;
                return this;
            }

            /**
             * @return An immutable {@link SimpleSdkHttpFullResponse} object.
             */
            public SimpleSdkHttpFullResponse build() {
                return new SimpleSdkHttpFullResponse(this);
            }
        }
    }

}
