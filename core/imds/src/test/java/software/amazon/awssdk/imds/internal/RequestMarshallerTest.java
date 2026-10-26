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

package software.amazon.awssdk.imds.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpFullRequest;

class RequestMarshallerTest {

    private static final Duration TTL = Duration.ofSeconds(21600);
    private static final String TOKEN = "some-token";
    private static final String IPV4_ENDPOINT = "http://169.254.169.254";
    private static final String IPV6_ENDPOINT = "http://[fd00:ec2::254]";

    private static Stream<Arguments> validPaths() {
        return Stream.of(
            Arguments.of(IPV4_ENDPOINT, "/latest/meta-data/ami-id", "169.254.169.254", "/latest/meta-data/ami-id"),
            Arguments.of(IPV4_ENDPOINT + "/", "latest/meta-data", "169.254.169.254", "/latest/meta-data"),
            Arguments.of(IPV4_ENDPOINT, "//evil/x", "169.254.169.254", "//evil/x"),
            Arguments.of(IPV4_ENDPOINT, "/latest/meta-data?a=b", "169.254.169.254", "/latest/meta-data"),
            Arguments.of(IPV6_ENDPOINT, "/latest/meta-data/ami-id", "[fd00:ec2::254]", "/latest/meta-data/ami-id"));
    }

    private static Stream<Arguments> invalidPaths() {
        return Stream.of(
            Arguments.of(IPV4_ENDPOINT, "@evil.example/latest/meta-data"),
            Arguments.of(IPV4_ENDPOINT, ":80@evil/x"),
            Arguments.of(IPV4_ENDPOINT, ".evil.com/x"),
            Arguments.of(IPV4_ENDPOINT, "evil"),
            Arguments.of(IPV4_ENDPOINT, ":8080/x"),
            Arguments.of(IPV4_ENDPOINT, null),
            Arguments.of(IPV6_ENDPOINT, "@evil/x"),
            Arguments.of(IPV6_ENDPOINT, ":8080/x"));
    }

    @ParameterizedTest
    @MethodSource("validPaths")
    void createDataRequest_validPath_keepsEndpointHost(String endpoint, String path, String expectedHost,
                                                       String expectedPath) {
        RequestMarshaller marshaller = new RequestMarshaller(URI.create(endpoint));
        SdkHttpFullRequest request = marshaller.createDataRequest(path, TOKEN, TTL);
        assertThat(request.host()).isEqualTo(expectedHost);
        assertThat(request.protocol()).isEqualTo("http");
        assertThat(request.encodedPath()).isEqualTo(expectedPath);
        assertThat(request.firstMatchingHeader(RequestMarshaller.TOKEN_HEADER)).hasValue(TOKEN);
    }

    @Test
    void createDataRequest_pathWithQuery_keepsQuery() {
        RequestMarshaller marshaller = new RequestMarshaller(URI.create(IPV4_ENDPOINT));
        SdkHttpFullRequest request = marshaller.createDataRequest("/latest/meta-data?a=b", TOKEN, TTL);
        assertThat(request.firstMatchingRawQueryParameter("a")).hasValue("b");
    }

    @ParameterizedTest
    @MethodSource("invalidPaths")
    void createDataRequest_invalidPath_throwsSdkClientException(String endpoint, String path) {
        RequestMarshaller marshaller = new RequestMarshaller(URI.create(endpoint));
        assertThatThrownBy(() -> marshaller.createDataRequest(path, TOKEN, TTL))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Invalid metadata path: the path must not change the scheme, host, or port "
                        + "of the configured IMDS endpoint " + endpoint)
            .hasMessageNotContaining(TOKEN);
    }
}
