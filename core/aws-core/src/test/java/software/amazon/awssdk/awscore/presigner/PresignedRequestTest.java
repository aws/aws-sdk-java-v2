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

package software.amazon.awssdk.awscore.presigner;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

class PresignedRequestTest {
    @Test
    void standardPortIsOmitted() throws Exception {
        SdkHttpRequest httpRequest = requestBuilder()
            .protocol("https")
            .host("example.com")
            .port(443)
            .encodedPath("/resource")
            .build();

        assertEquivalentToUriConversion(httpRequest);
        assertThat(presignedRequest(httpRequest).url().getPort()).isEqualTo(-1);
        assertThat(presignedRequest(httpRequest).url()).hasToString("https://example.com/resource");
    }

    @Test
    void standardHttpPortIsOmitted() throws Exception {
        SdkHttpRequest httpRequest = requestBuilder()
            .protocol("http")
            .host("example.com")
            .port(80)
            .encodedPath("/resource")
            .build();

        assertEquivalentToUriConversion(httpRequest);
        assertThat(presignedRequest(httpRequest).url().getPort()).isEqualTo(-1);
        assertThat(presignedRequest(httpRequest).url()).hasToString("http://example.com/resource");
    }

    @Test
    void implicitDefaultPortIsOmitted() throws Exception {
        SdkHttpRequest httpRequest = requestBuilder()
            .protocol("https")
            .host("example.com")
            .encodedPath("/resource")
            .build();

        assertThat(httpRequest.port()).isEqualTo(443);
        assertEquivalentToUriConversion(httpRequest);
        assertThat(presignedRequest(httpRequest).url().getPort()).isEqualTo(-1);
    }

    @Test
    void customPortIsPreserved() throws Exception {
        SdkHttpRequest httpRequest = requestBuilder()
            .protocol("https")
            .host("example.com")
            .port(8443)
            .encodedPath("/resource")
            .build();

        assertEquivalentToUriConversion(httpRequest);
        assertThat(presignedRequest(httpRequest).url().getPort()).isEqualTo(8443);
        assertThat(presignedRequest(httpRequest).url()).hasToString("https://example.com:8443/resource");
    }

    @Test
    void encodedPathAndQueryValuesArePreserved() throws Exception {
        SdkHttpRequest httpRequest = requestBuilder()
            .protocol("https")
            .host("example.com")
            .encodedPath("/a%20path/%2Fvalue%3Fquery%23fragment%25percent")
            .putRawQueryParameter("key with space", "value/with?#%characters")
            .build();

        assertEquivalentToUriConversion(httpRequest);
        assertThat(presignedRequest(httpRequest).url())
            .hasToString("https://example.com/a%20path/%2Fvalue%3Fquery%23fragment%25percent"
                         + "?key%20with%20space=value%2Fwith%3F%23%25characters");
    }

    @Test
    void emptyPathIsPreserved() throws Exception {
        SdkHttpRequest httpRequest = requestBuilder()
            .protocol("https")
            .host("example.com")
            .build();

        assertEquivalentToUriConversion(httpRequest);
        assertThat(presignedRequest(httpRequest).url()).hasToString("https://example.com");
    }

    @Test
    void ipv6HostIsPreserved() throws Exception {
        SdkHttpRequest httpRequest = requestBuilder()
            .uri(URI.create("https://[2001:db8::1]:8443/resource"))
            .build();

        assertEquivalentToUriConversion(httpRequest);
        assertThat(presignedRequest(httpRequest).url())
            .hasToString("https://[2001:db8::1]:8443/resource");
    }

    private static void assertEquivalentToUriConversion(SdkHttpRequest httpRequest) throws Exception {
        assertThat(presignedRequest(httpRequest).url().toExternalForm())
            .isEqualTo(httpRequest.getUri().toURL().toExternalForm());
    }

    private static SdkHttpFullRequest.Builder requestBuilder() {
        return SdkHttpFullRequest.builder().method(SdkHttpMethod.GET);
    }

    private static TestPresignedRequest presignedRequest(SdkHttpRequest httpRequest) {
        return new TestBuilder()
            .expiration(Instant.EPOCH)
            .isBrowserExecutable(true)
            .signedHeaders(Collections.singletonMap("host", Collections.singletonList(httpRequest.host())))
            .httpRequest(httpRequest)
            .build();
    }

    private static final class TestPresignedRequest extends PresignedRequest {
        private TestPresignedRequest(TestBuilder builder) {
            super(builder);
        }
    }

    private static final class TestBuilder extends PresignedRequest.DefaultBuilder<TestBuilder> {
        @Override
        public TestPresignedRequest build() {
            return new TestPresignedRequest(this);
        }
    }
}
