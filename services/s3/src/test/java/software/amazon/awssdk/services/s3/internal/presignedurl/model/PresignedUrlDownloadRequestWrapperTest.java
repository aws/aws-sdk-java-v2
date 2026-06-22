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

package software.amazon.awssdk.services.s3.internal.presignedurl.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkField;


class PresignedUrlDownloadRequestWrapperTest {
    @Test
    void equalsAndHashCode_shouldFollowContract() {
        EqualsVerifier.forClass(PresignedUrlDownloadRequestWrapper.class)
                      .withRedefinedSuperclass()
                      .verify();
    }

    @Test
    void basicProperties_shouldWork() throws Exception {
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("Range", Collections.singletonList("bytes=0-100"));
        headers.put("If-Match", Collections.singletonList("\"etag-123\""));

        PresignedUrlDownloadRequestWrapper request = PresignedUrlDownloadRequestWrapper.builder()
                                                                                         .url(new URL("https://example.com"))
                                                                                         .headers(headers)
                                                                                         .build();

        assertThat(request.url()).isEqualTo(new URL("https://example.com"));
        assertThat(request.headers().get("Range")).isEqualTo(Collections.singletonList("bytes=0-100"));
        assertThat(request.headers().get("If-Match")).isEqualTo(Collections.singletonList("\"etag-123\""));
    }

    @Test
    void sdkFields_shouldReturnEmptyList() throws Exception {
        PresignedUrlDownloadRequestWrapper request = PresignedUrlDownloadRequestWrapper.builder()
                                                                                         .url(new URL("https://example.com"))
                                                                                         .headers(Collections.emptyMap())
                                                                                         .build();

        List<SdkField<?>> fields = request.sdkFields();

        assertThat(fields).isEmpty();
    }

    @Test
    void sdkFieldNameToField_shouldReturnEmptyMapping() throws Exception {
        PresignedUrlDownloadRequestWrapper request = PresignedUrlDownloadRequestWrapper.builder()
                                                                                         .url(new URL("https://example.com"))
                                                                                         .build();

        Map<String, SdkField<?>> fieldMap = request.sdkFieldNameToField();

        assertThat(fieldMap).isEmpty();
    }

    @Test
    void headers_shouldBeCaseInsensitive() throws Exception {
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("Range", Collections.singletonList("bytes=0-1023"));

        PresignedUrlDownloadRequestWrapper request = PresignedUrlDownloadRequestWrapper.builder()
                                                                                         .url(new URL("https://example.com"))
                                                                                         .headers(headers)
                                                                                         .build();

        assertThat(request.headers().get("range")).isEqualTo(Collections.singletonList("bytes=0-1023"));
        assertThat(request.headers().get("RANGE")).isEqualTo(Collections.singletonList("bytes=0-1023"));
    }

    @Test
    void toBuilder_shouldPreserveHeaders() throws Exception {
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("If-Match", Collections.singletonList("\"etag-value\""));

        PresignedUrlDownloadRequestWrapper request = PresignedUrlDownloadRequestWrapper.builder()
                                                                                         .url(new URL("https://example.com"))
                                                                                         .headers(headers)
                                                                                         .build();

        PresignedUrlDownloadRequestWrapper copy = request.toBuilder().build();

        assertThat(copy.headers().get("If-Match")).isEqualTo(Collections.singletonList("\"etag-value\""));
    }
}
