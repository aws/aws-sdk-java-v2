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

package software.amazon.awssdk.services.s3.presigner.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.time.Duration;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class GetObjectPresignRequestTest {
    private static final GetObjectRequest GET_OBJECT_REQUEST = GetObjectRequest.builder()
                                                                               .bucket("some-bucket")
                                                                               .key("some-key")
                                                                               .build();

    @Test
    public void build_minimal_maximal() {
        GetObjectPresignRequest getObjectPresignRequest =
            GetObjectPresignRequest.builder()
                                   .getObjectRequest(GET_OBJECT_REQUEST)
                                   .signatureDuration(Duration.ofSeconds(123L))
                                   .build();

        assertThat(getObjectPresignRequest.getObjectRequest()).isEqualTo(GET_OBJECT_REQUEST);
        assertThat(getObjectPresignRequest.signatureDuration()).isEqualTo(Duration.ofSeconds(123L));
    }

    @Test
    public void build_missingProperty_getObjectRequest() {
        assertThatThrownBy(() -> GetObjectPresignRequest.builder()
                                                        .signatureDuration(Duration.ofSeconds(123L))
                                                        .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("getObjectRequest");
    }

    @Test
    public void build_missingProperty_signatureDuration() {
        assertThatThrownBy(() -> GetObjectPresignRequest.builder()
                                                        .getObjectRequest(GET_OBJECT_REQUEST)
                                                        .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("signatureDuration");
    }

    @Test
    public void toBuilder() {
        GetObjectPresignRequest getObjectPresignRequest =
            GetObjectPresignRequest.builder()
                                   .getObjectRequest(GET_OBJECT_REQUEST)
                                   .signatureDuration(Duration.ofSeconds(123L))
                                   .build();

        GetObjectPresignRequest otherGetObjectPresignRequest = getObjectPresignRequest.toBuilder().build();

        assertThat(otherGetObjectPresignRequest.getObjectRequest()).isEqualTo(GET_OBJECT_REQUEST);
        assertThat(otherGetObjectPresignRequest.signatureDuration()).isEqualTo(Duration.ofSeconds(123L));
    }

    @Test
    public void equalsAndHashCode_allPropertiesEqual() {
        GetObjectPresignRequest getObjectPresignRequest =
            GetObjectPresignRequest.builder()
                                   .getObjectRequest(GET_OBJECT_REQUEST)
                                   .signatureDuration(Duration.ofSeconds(123L))
                                   .build();

        GetObjectPresignRequest otherGetObjectPresignRequest =
            GetObjectPresignRequest.builder()
                                   .getObjectRequest(GET_OBJECT_REQUEST)
                                   .signatureDuration(Duration.ofSeconds(123L))
                                   .build();

        assertThat(otherGetObjectPresignRequest).isEqualTo(getObjectPresignRequest);
        assertThat(otherGetObjectPresignRequest.hashCode()).isEqualTo(getObjectPresignRequest.hashCode());
    }

    @Test
    public void equalsAndHashCode_differentProperty_getObjectRequest() {
        GetObjectRequest otherGetObjectRequest = GetObjectRequest.builder()
                                                                 .bucket("other-bucket")
                                                                 .key("other-key")
                                                                 .build();
        GetObjectPresignRequest getObjectPresignRequest =
            GetObjectPresignRequest.builder()
                                   .getObjectRequest(GET_OBJECT_REQUEST)
                                   .signatureDuration(Duration.ofSeconds(123L))
                                   .build();

        GetObjectPresignRequest otherGetObjectPresignRequest =
            GetObjectPresignRequest.builder()
                                   .getObjectRequest(otherGetObjectRequest)
                                   .signatureDuration(Duration.ofSeconds(123L))
                                   .build();

        assertThat(otherGetObjectPresignRequest).isNotEqualTo(getObjectPresignRequest);
        assertThat(otherGetObjectPresignRequest.hashCode()).isNotEqualTo(getObjectPresignRequest.hashCode());
    }

    @Test
    public void equalsAndHashCode_differentProperty_signatureDuration() {
        GetObjectPresignRequest getObjectPresignRequest =
            GetObjectPresignRequest.builder()
                                   .getObjectRequest(GET_OBJECT_REQUEST)
                                   .signatureDuration(Duration.ofSeconds(123L))
                                   .build();

        GetObjectPresignRequest otherGetObjectPresignRequest =
            GetObjectPresignRequest.builder()
                                   .getObjectRequest(GET_OBJECT_REQUEST)
                                   .signatureDuration(Duration.ofSeconds(321L))
                                   .build();

        assertThat(otherGetObjectPresignRequest).isNotEqualTo(getObjectPresignRequest);
        assertThat(otherGetObjectPresignRequest.hashCode()).isNotEqualTo(getObjectPresignRequest.hashCode());
    }
}