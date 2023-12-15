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

package software.amazon.awssdk.core.client.config;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.utils.AttributeMap;

public class SdkClientConfigurationTest {

    @Test
    public void equalsHashcode() {
        AttributeMap one = AttributeMap.empty();
        AttributeMap two = AttributeMap.builder().put(SdkClientOption.CLIENT_TYPE, ClientType.SYNC).build();

        EqualsVerifier.forClass(SdkClientConfiguration.class)
                      .withNonnullFields("attributes")
                      .withPrefabValues(AttributeMap.class, one, two)
                      .verify();
    }
}
