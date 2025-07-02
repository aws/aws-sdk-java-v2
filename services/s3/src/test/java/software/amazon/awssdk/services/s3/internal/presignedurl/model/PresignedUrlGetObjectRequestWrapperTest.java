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


class PresignedUrlGetObjectRequestWrapperTest {
    @Test
    void equalsAndHashCode_shouldFollowContract() {
        EqualsVerifier.forClass(PresignedUrlGetObjectRequestWrapper.class)
                      .withRedefinedSuperclass()
                      .verify();
    }

    @Test
    void basicProperties_shouldWork() throws Exception {
        PresignedUrlGetObjectRequestWrapper request = PresignedUrlGetObjectRequestWrapper.builder()
                                                                                         .url(new URL("https://example.com"))
                                                                                         .range("bytes=0-100")
                                                                                         .build();

        assertThat(request.url()).isEqualTo(new URL("https://example.com"));
        assertThat(request.range()).isEqualTo("bytes=0-100");
    }

    @Test
    void sdkFields_shouldReturnExpectedFields() throws Exception {
        PresignedUrlGetObjectRequestWrapper request = PresignedUrlGetObjectRequestWrapper.builder()
                                                                                         .url(new URL("https://example.com"))
                                                                                         .range("bytes=0-100")
                                                                                         .build();

        List<SdkField<?>> fields = request.sdkFields();

        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).memberName()).isEqualTo("Range");
        assertThat(fields.get(0).location()).isEqualTo(MarshallLocation.HEADER);

        // Assert that range value is present
        Object rangeValue = fields.get(0).getValueOrDefault(request);
        assertThat(rangeValue).isNotNull();
        assertThat(rangeValue).isEqualTo("bytes=0-100");
    }

    @Test
    void sdkFieldNameToField_shouldReturnExpectedMapping() throws Exception {
        PresignedUrlGetObjectRequestWrapper request = PresignedUrlGetObjectRequestWrapper.builder()
                                                                                         .url(new URL("https://example.com"))
                                                                                         .build();

        Map<String, SdkField<?>> fieldMap = request.sdkFieldNameToField();

        assertThat(fieldMap)
            .hasSize(1)
            .containsKey("Range");
        assertThat(fieldMap.get("Range").memberName()).isEqualTo("Range");
    }

    @Test
    void rangeField_shouldMarshalCorrectly() throws Exception {
        PresignedUrlGetObjectRequestWrapper request = PresignedUrlGetObjectRequestWrapper.builder()
                                                                                         .url(new URL("https://example.com"))
                                                                                         .range("bytes=0-1023")
                                                                                         .build();

        // Test that the SdkField can extract the range value
        SdkField<?> rangeField = request.sdkFields().get(0);
        Object extractedValue = rangeField.getValueOrDefault(request);

        assertThat(extractedValue).isEqualTo("bytes=0-1023");
    }
}
