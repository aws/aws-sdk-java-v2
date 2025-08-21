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
import java.util.List;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.protocol.MarshallLocation;


class PresignedUrlDownloadRequestWrapperTest {
    @Test
    void equalsAndHashCode_shouldFollowContract() {
        EqualsVerifier.forClass(PresignedUrlDownloadRequestWrapper.class)
                      .withRedefinedSuperclass()
                      .verify();
    }

    @Test
    void basicProperties_shouldWork() throws Exception {
        PresignedUrlDownloadRequestWrapper request = PresignedUrlDownloadRequestWrapper.builder()
                                                                                         .url(new URL("https://example.com"))
                                                                                         .range("bytes=0-100")
                                                                                         .ifMatch("\"etag-123\"")
                                                                                         .build();

        assertThat(request.url()).isEqualTo(new URL("https://example.com"));
        assertThat(request.range()).isEqualTo("bytes=0-100");
        assertThat(request.ifMatch()).isEqualTo("\"etag-123\"");
    }

    @Test
    void sdkFields_shouldReturnExpectedFields() throws Exception {
        PresignedUrlDownloadRequestWrapper request = PresignedUrlDownloadRequestWrapper.builder()
                                                                                         .url(new URL("https://example.com"))
                                                                                         .range("bytes=0-100")
                                                                                         .build();

        List<SdkField<?>> fields = request.sdkFields();

        assertThat(fields).hasSize(2);
        assertThat(fields).extracting(SdkField::memberName)
                          .containsExactlyInAnyOrder("Range", "IfMatch");
        assertThat(fields).allMatch(field -> field.location() == MarshallLocation.HEADER);
        SdkField<?> rangeField = fields.stream()
                                       .filter(f -> "Range".equals(f.memberName()))
                                       .findFirst()
                                       .orElseThrow(() -> new AssertionError("Range field not found"));
        Object rangeValue = rangeField.getValueOrDefault(request);
        assertThat(rangeValue).isNotNull();
        assertThat(rangeValue).isEqualTo("bytes=0-100");
    }

    @Test
    void sdkFieldNameToField_shouldReturnExpectedMapping() throws Exception {
        PresignedUrlDownloadRequestWrapper request = PresignedUrlDownloadRequestWrapper.builder()
                                                                                         .url(new URL("https://example.com"))
                                                                                         .build();

        Map<String, SdkField<?>> fieldMap = request.sdkFieldNameToField();

        assertThat(fieldMap)
            .hasSize(2)
            .containsKeys("Range", "IfMatch");
        assertThat(fieldMap.get("Range").memberName()).isEqualTo("Range");
        assertThat(fieldMap.get("IfMatch").memberName()).isEqualTo("IfMatch");
    }

    @Test
    void rangeField_shouldMarshalCorrectly() throws Exception {
        PresignedUrlDownloadRequestWrapper request = PresignedUrlDownloadRequestWrapper.builder()
                                                                                         .url(new URL("https://example.com"))
                                                                                         .range("bytes=0-1023")
                                                                                         .build();

        SdkField<?> rangeField = request.sdkFields().stream()
                                        .filter(f -> "Range".equals(f.memberName()))
                                        .findFirst()
                                        .orElseThrow(() -> new AssertionError("Range field not found"));
        Object extractedValue = rangeField.getValueOrDefault(request);

        assertThat(extractedValue).isEqualTo("bytes=0-1023");
    }

    @Test
    void ifMatchField_shouldMarshalCorrectly() throws Exception {
        PresignedUrlDownloadRequestWrapper request = PresignedUrlDownloadRequestWrapper.builder()
                                                                                         .url(new URL("https://example.com"))
                                                                                         .ifMatch("\"etag-value\"")
                                                                                         .build();

        SdkField<?> ifMatchField = request.sdkFields().stream()
                                          .filter(f -> "IfMatch".equals(f.memberName()))
                                          .findFirst()
                                          .orElseThrow(() -> new AssertionError("IfMatch field not found"));
        Object extractedValue = ifMatchField.getValueOrDefault(request);

        assertThat(extractedValue).isEqualTo("\"etag-value\"");
    }
}
