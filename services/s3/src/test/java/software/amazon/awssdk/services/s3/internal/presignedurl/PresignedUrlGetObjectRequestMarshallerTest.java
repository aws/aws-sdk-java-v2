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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.xml.AwsXmlProtocolFactory;
import software.amazon.awssdk.services.s3.internal.presignedurl.model.PresignedUrlGetObjectRequestWrapper;

class PresignedUrlGetObjectRequestMarshallerTest {

    private PresignedUrlGetObjectRequestMarshaller marshaller;
    private AwsXmlProtocolFactory mockProtocolFactory;
    private ProtocolMarshaller<SdkHttpFullRequest> mockProtocolMarshaller;
    private URL testUrl;

    @BeforeEach
    void setUp() throws Exception {
        mockProtocolFactory = mock(AwsXmlProtocolFactory.class);
        mockProtocolMarshaller = mock(ProtocolMarshaller.class);
        when(mockProtocolFactory.createProtocolMarshaller(any(OperationInfo.class)))
            .thenReturn(mockProtocolMarshaller);
        marshaller = new PresignedUrlGetObjectRequestMarshaller(mockProtocolFactory);

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
    void marshall_withBasicRequest_shouldCreateCorrectHttpRequest() throws Exception {
        // Setup the mock marshaller to return a properly configured request
        SdkHttpFullRequest baseRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .protocol("https")
                                                           .host("example.com")
                                                           .build();
        when(mockProtocolMarshaller.marshall(any(PresignedUrlGetObjectRequestWrapper.class)))
            .thenReturn(baseRequest);

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
    void marshall_withValidRangeFormats_shouldAddRangeHeader(String rangeValue) throws Exception {
        // Setup the mock marshaller to return a request with the Range header already set
        SdkHttpFullRequest baseRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .protocol("https")
                                                           .host("example.com")
                                                           .putHeader("Range", rangeValue)  // Add the Range header to the mock response
                                                           .build();

        when(mockProtocolMarshaller.marshall(any(PresignedUrlGetObjectRequestWrapper.class)))
            .thenReturn(baseRequest);

        PresignedUrlGetObjectRequestWrapper request = PresignedUrlGetObjectRequestWrapper.builder()
                                                                                         .url(testUrl)
                                                                                         .range(rangeValue)
                                                                                         .build();

        SdkHttpFullRequest result = marshaller.marshall(request);

        // Verify the Range header is preserved
        assertThat(result.headers())
            .containsKey("Range")
            .satisfies(headers -> assertThat(headers.get("Range")).contains(rangeValue));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void marshall_withNullOrEmptyRange_shouldNotAddRangeHeader(String rangeValue) throws Exception {
        // Setup the mock marshaller to return a properly configured request
        SdkHttpFullRequest baseRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .protocol("https")
                                                           .host("example.com")
                                                           .build();
        when(mockProtocolMarshaller.marshall(any(PresignedUrlGetObjectRequestWrapper.class)))
            .thenReturn(baseRequest);

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
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("presignedUrlGetObjectRequestWrapper must not be null");
    }

    @Test
    void marshall_withMalformedUrl_shouldThrowSdkClientException() throws Exception {
        // Setup the mock marshaller to return a properly configured request
        SdkHttpFullRequest baseRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .protocol("https")
                                                           .host("example.com")
                                                           .build();
        when(mockProtocolMarshaller.marshall(any(PresignedUrlGetObjectRequestWrapper.class)))
            .thenReturn(baseRequest);

        URL malformedUrl = new URL("https", "test-bucket.s3.us-east-1.amazonaws.com", -1, "/test key with spaces");
        PresignedUrlGetObjectRequestWrapper request = PresignedUrlGetObjectRequestWrapper.builder()
                                                                                         .url(malformedUrl)
                                                                                         .build();

        assertThatThrownBy(() -> marshaller.marshall(request))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unable to marshall pre-signed URL Request");
    }
}