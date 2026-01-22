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

package software.amazon.awssdk.core.useragent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class AdditionalMetadataTest {

    @Test
    public void toString_formatsCorrectly() {
        AdditionalMetadata metadata = AdditionalMetadata.builder()
            .name("name")
            .value("value")
                                                        .build();
        assertEquals("md/name#value", metadata.toString());
    }
    @Test
    public void equalsHashCode() {
        EqualsVerifier.forClass(AdditionalMetadata.class)
                      .withNonnullFields("name", "value")
                      .verify();
    }
}


