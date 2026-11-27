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
import software.amazon.awssdk.services.dynamodb.model.DescribeTimeToLiveResponse;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveDescription;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveStatus;

public class DescribeTimeToLiveEnhancedResponseTest {
    @Test
    public void builder_populatesTimeToLiveDescription() {
        TimeToLiveDescription timeToLiveDescription = TimeToLiveDescription.builder()
                                                                          .attributeName("expirationDate")
                                                                          .timeToLiveStatus(TimeToLiveStatus.ENABLED)
                                                                          .build();
        DescribeTimeToLiveResponse response = DescribeTimeToLiveResponse.builder()
                                                                        .timeToLiveDescription(timeToLiveDescription)
                                                                        .build();

        DescribeTimeToLiveEnhancedResponse builtObject = DescribeTimeToLiveEnhancedResponse.builder()
                                                                                           .response(response)
                                                                                           .build();

        assertThat(builtObject.timeToLiveDescription()).isEqualTo(timeToLiveDescription);
    }

    @Test
    public void responseNull_shouldThrowException() {
        assertThatThrownBy(() -> DescribeTimeToLiveEnhancedResponse.builder().build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("response must not be null");
    }

    @Test
    public void equalsHashcode() {
        EqualsVerifier.forClass(DescribeTimeToLiveEnhancedResponse.class)
                      .withNonnullFields("response")
                      .verify();
    }

    @Test
    public void toString_containsTimeToLiveDescription() {
        DescribeTimeToLiveEnhancedResponse builtObject = DescribeTimeToLiveEnhancedResponse.builder()
                                                                                           .response(DescribeTimeToLiveResponse.builder()
                                                                                                                               .timeToLiveDescription(TimeToLiveDescription.builder()
                                                                                                                                                                           .attributeName("expirationDate")
                                                                                                                                                                           .timeToLiveStatus(TimeToLiveStatus.DISABLED)
                                                                                                                                                                           .build())
                                                                                                                               .build())
                                                                                           .build();

        assertThat(builtObject.toString()).contains("DescribeTimeToLiveEnhancedResponse")
                                          .contains("timeToLiveDescription")
                                          .contains("expirationDate")
                                          .contains("DISABLED");
    }
}

