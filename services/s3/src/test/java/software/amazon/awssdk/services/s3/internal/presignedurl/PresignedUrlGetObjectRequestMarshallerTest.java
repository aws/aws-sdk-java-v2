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

package software.amazon.awssdk.services.s3.internal.presignedurl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import java.net.URL;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.xml.AwsXmlProtocolFactory;
import software.amazon.awssdk.services.s3.internal.presignedurl.model.PresignedUrlGetObjectRequestWrapper;

class PresignedUrlGetObjectRequestMarshallerTest {

    private PresignedUrlGetObjectRequestMarshaller marshaller;
    private AwsXmlProtocolFactory mockProtocolFactory;
    private URL testUrl;

    @BeforeEach
    void setUp() throws Exception {
        mockProtocolFactory = mock(AwsXmlProtocolFactory.class);
        marshaller = new PresignedUrlGetObjectRequestMarshaller(mockProtocolFactory) {
            @Override
            public SdkHttpFullRequest marshall(PresignedUrlGetObjectRequestWrapper request) {
                try {
                    if (request == null) {
                        throw SdkClientException.builder()
                                                .message("presignedUrlGetObjectRequestWrapper must not be null")
                                                .build();
                    }

                    SdkHttpFullRequest.Builder requestBuilder = SdkHttpFullRequest.builder()
                                                                                  .method(SdkHttpMethod.GET)
                                                                                  .uri(request.url().toURI());

                    String range = request.range();
                    if (range != null && !range.isEmpty()) {
                        requestBuilder.putHeader("Range", range);
                    }

                    return requestBuilder.build();
                } catch (SdkClientException e) {
                    throw e;
                } catch (Exception e) {
                    throw SdkClientException.builder()
                                            .message("Unable to marshall pre-signed URL Request: " + e.getMessage())
                                            .cause(e).build();
                }
            }
        };

        testUrl = new URL("https://test-bucket.s3.us-east-1.amazonaws.com/test-key?" +
                          "X-Amz-Date=20231215T000000Z&" +
                          "X-Amz-Signature=example-signature&" +
                          "X-Amz-Algorithm=AWS4-HMAC-SHA256&" +
                          "X-Amz-SignedHeaders=host&" +
                          "X-Amz-Security-Token=xxx&" +
                          "X-Amz-Credential=EXAMPLE12345678901234%2F20231215%2Fus-east-1%2Fs3%2Faws4_request&" +
                          "X-Amz-Expires=3600");
    }

    @Test
    void marshall_withBasicRequest_shouldCreateCorrectHttpRequest() {
        PresignedUrlGetObjectRequestWrapper request = PresignedUrlGetObjectRequestWrapper.builder()
                                                                                         .url(testUrl)
                                                                                         .build();
        SdkHttpFullRequest result = marshaller.marshall(request);

        // Verify HTTP method and URI components
        assertThat(result.method()).isEqualTo(SdkHttpMethod.GET);
        assertThat(result.getUri())
            .satisfies(uri -> {
                assertThat(uri.getScheme()).isEqualTo("https");
                assertThat(uri.getHost()).isEqualTo("test-bucket.s3.us-east-1.amazonaws.com");
                assertThat(uri.getPath()).isEqualTo("/test-key");
            });

        // Verify query parameters are preserved
        assertThat(result.getUri().getQuery())
            .contains("X-Amz-Date=20231215T000000Z")
            .contains("X-Amz-Signature=example-signature")
            .contains("X-Amz-Algorithm=AWS4-HMAC-SHA256")
            .contains("X-Amz-SignedHeaders=host")
            .contains("X-Amz-Security-Token=xxx")
            .contains("X-Amz-Credential=EXAMPLE12345678901234")
            .contains("X-Amz-Expires=3600");

        assertThat(result.headers()).doesNotContainKey("Range");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "bytes=0-100",      // First 101 bytes
        "bytes=100-",       // From byte 100 to end
        "bytes=-100",       // Last 100 bytes
        "bytes=0-0",        // Single byte
        "bytes=100-200"     // Specific range
    })
    void marshall_withValidRangeFormats_shouldAddRangeHeader(String rangeValue) {
        PresignedUrlGetObjectRequestWrapper request = PresignedUrlGetObjectRequestWrapper.builder()
                                                                                         .url(testUrl)
                                                                                         .range(rangeValue)
                                                                                         .build();
        SdkHttpFullRequest result = marshaller.marshall(request);

        assertThat(result.headers())
            .containsKey("Range")
            .satisfies(headers -> assertThat(headers.get("Range")).contains(rangeValue));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void marshall_withNullOrEmptyRange_shouldNotAddRangeHeader(String rangeValue) {
        PresignedUrlGetObjectRequestWrapper request = PresignedUrlGetObjectRequestWrapper.builder()
                                                                                         .url(testUrl)
                                                                                         .range(rangeValue)
                                                                                         .build();
        SdkHttpFullRequest result = marshaller.marshall(request);

        assertThat(result.headers()).doesNotContainKey("Range");
    }

    @Test
    void marshall_withNullRequest_shouldThrowException() {
        assertThatThrownBy(() -> marshaller.marshall(null))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("presignedUrlGetObjectRequestWrapper must not be null");
    }

    @Test
    void marshall_withMalformedUrl_shouldThrowSdkClientException() throws Exception {
        URL malformedUrl = new URL("https", "test-bucket.s3.us-east-1.amazonaws.com", -1, "/test key with spaces");
        PresignedUrlGetObjectRequestWrapper request = PresignedUrlGetObjectRequestWrapper.builder()
                                                                                         .url(malformedUrl)
                                                                                         .build();

        assertThatThrownBy(() -> marshaller.marshall(request))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unable to marshall pre-signed URL Request");
    }

    private static Stream<Arguments> provideComplexUrls() throws Exception {
        return Stream.of(
            Arguments.of(
                new URL("https://my-bucket.s3.amazonaws.com/path/to/object.txt?" +
                        "X-Amz-Date=20231215T120000Z&" +
                        "X-Amz-Signature=example-signature-hash&" +
                        "X-Amz-Algorithm=AWS4-HMAC-SHA256&" +
                        "X-Amz-SignedHeaders=host%3Bx-amz-content-sha256&" +
                        "X-Amz-Security-Token=xxx&" +
                        "X-Amz-Credential=EXAMPLE12345678901234%2F20231215%2Fus-east-1%2Fs3%2Faws4_request&" +
                        "X-Amz-Expires=86400&" +
                        "response-content-disposition=attachment%3B%20filename%3D%22download.txt%22"),
                new String[] {
                    "X-Amz-Algorithm=AWS4-HMAC-SHA256",
                    "X-Amz-Credential=EXAMPLE12345678901234",
                    "X-Amz-Date=20231215T120000Z",
                    "X-Amz-Expires=86400",
                    "X-Amz-SignedHeaders=host",
                    "X-Amz-Security-Token=xxx",
                    "X-Amz-Signature=example-signature-hash",
                    "response-content-disposition=attachment"
                }
            ),
            Arguments.of(
                new URL("https://test-bucket.s3.us-west-2.amazonaws.com/folder/file.pdf?" +
                        "X-Amz-Date=20231215T180000Z&" +
                        "X-Amz-Signature=different-signature&" +
                        "X-Amz-Algorithm=AWS4-HMAC-SHA256&" +
                        "X-Amz-SignedHeaders=host&" +
                        "X-Amz-Credential=EXAMPLE12345678901234%2F20231215%2Fus-west-2%2Fs3%2Faws4_request&" +
                        "X-Amz-Expires=7200"),
                new String[] {
                    "X-Amz-Algorithm=AWS4-HMAC-SHA256",
                    "X-Amz-Credential=EXAMPLE12345678901234",
                    "X-Amz-Date=20231215T180000Z",
                    "X-Amz-Expires=7200",
                    "X-Amz-SignedHeaders=host",
                    "X-Amz-Signature=different-signature"
                }
            )
        );
    }
}