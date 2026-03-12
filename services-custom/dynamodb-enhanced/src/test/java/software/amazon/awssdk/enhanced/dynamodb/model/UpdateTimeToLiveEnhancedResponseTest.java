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

package software.amazon.awssdk.enhanced.dynamodb.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveSpecification;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveResponse;

public class UpdateTimeToLiveEnhancedResponseTest {
    @Test
    public void builder_populatesTimeToLiveSpecification() {
        TimeToLiveSpecification timeToLiveSpecification = TimeToLiveSpecification.builder()
                                                                                 .attributeName("expirationDate")
                                                                                 .enabled(true)
                                                                                 .build();
        UpdateTimeToLiveResponse response = UpdateTimeToLiveResponse.builder()
                                                                    .timeToLiveSpecification(timeToLiveSpecification)
                                                                    .build();

        UpdateTimeToLiveEnhancedResponse builtObject = UpdateTimeToLiveEnhancedResponse.builder()
                                                                                       .response(response)
                                                                                       .build();

        assertThat(builtObject.timeToLiveSpecification()).isEqualTo(timeToLiveSpecification);
    }

    @Test
    public void responseNull_shouldThrowException() {
        assertThatThrownBy(() -> UpdateTimeToLiveEnhancedResponse.builder().build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("response must not be null");
    }

    @Test
    public void equalsHashcode() {
        EqualsVerifier.forClass(UpdateTimeToLiveEnhancedResponse.class)
                      .withNonnullFields("response")
                      .verify();
    }

    @Test
    public void toString_containsTimeToLiveSpecification() {
        UpdateTimeToLiveEnhancedResponse builtObject = UpdateTimeToLiveEnhancedResponse.builder()
                                                                                       .response(UpdateTimeToLiveResponse.builder()
                                                                                                                         .timeToLiveSpecification(TimeToLiveSpecification.builder()
                                                                                                                                                                   .attributeName("expirationDate")
                                                                                                                                                                   .enabled(false)
                                                                                                                                                                   .build())
                                                                                                                         .build())
                                                                                       .build();

        assertThat(builtObject.toString()).contains("UpdateTimeToLiveEnhancedResponse")
                                          .contains("timeToLiveSpecification")
                                          .contains("expirationDate")
                                          .contains("false");
    }
}

