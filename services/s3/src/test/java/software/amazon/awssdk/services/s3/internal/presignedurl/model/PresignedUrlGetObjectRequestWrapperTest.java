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
import java.util.List;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlGetObjectRequest;

class PresignedUrlGetObjectRequestWrapperTest {

    @Test
    void equalsAndHashCode_shouldFollowContract() {
        EqualsVerifier.forClass(PresignedUrlGetObjectRequestWrapper.class)
                      .withIgnoredFields("sdkFields", "sdkFieldNameToField")
                      .verify();
    }

    @Test
    void sdkFields_shouldReturnExpectedFields() {
        PresignedUrlGetObjectRequest publicRequest = PresignedUrlGetObjectRequest.builder()
                                                                                 .presignedUrl("https://example.com")
                                                                                 .range("bytes=0-100")
                                                                                 .build();

        InternalPresignedUrlGetObjectRequest request = new InternalPresignedUrlGetObjectRequest(publicRequest);

        List<SdkField<?>> fields = request.sdkFields();

        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).memberName()).isEqualTo("Range");
        assertThat(fields.get(0).location()).isEqualTo(MarshallLocation.HEADER);
        assertThat(fields.get(0).locationName()).isEqualTo("Range");
    }

    @Test
    void sdkFieldNameToField_shouldReturnExpectedMapping() {
        PresignedUrlGetObjectRequest publicRequest = PresignedUrlGetObjectRequest.builder()
                                                                                 .presignedUrl("https://example.com")
                                                                                 .build();

        InternalPresignedUrlGetObjectRequest request = new InternalPresignedUrlGetObjectRequest(publicRequest);

        Map<String, SdkField<?>> fieldMap = request.sdkFieldNameToField();

        assertThat(fieldMap).hasSize(1);
        assertThat(fieldMap).containsKey("Range");
        assertThat(fieldMap.get("Range").memberName()).isEqualTo("Range");
    }

    @Test
    void rangeField_shouldMarshalCorrectly() {
        PresignedUrlGetObjectRequest publicRequest = PresignedUrlGetObjectRequest.builder()
                                                                                 .presignedUrl("https://example.com")
                                                                                 .range("bytes=0-1023")
                                                                                 .build();

        InternalPresignedUrlGetObjectRequest request = new InternalPresignedUrlGetObjectRequest(publicRequest);

        // Test that the SdkField can extract the range value
        SdkField<?> rangeField = request.sdkFields().get(0);
        Object extractedValue = rangeField.getValueOrDefault(request);

        assertThat(extractedValue).isEqualTo("bytes=0-1023");
    }
}