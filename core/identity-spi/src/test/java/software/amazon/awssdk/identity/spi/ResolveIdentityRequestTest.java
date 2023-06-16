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

package software.amazon.awssdk.identity.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.identity.spi.internal.DefaultResolveIdentityRequest;

public class ResolveIdentityRequestTest {
    @Test
    public void equalsHashcode() {
        EqualsVerifier.forClass(DefaultResolveIdentityRequest.class)
                      .withNonnullFields("properties")
                      .verify();
    }

    @Test
    public void emptyBuilder_isSuccessful() {
        assertNotNull(ResolveIdentityRequest.builder().build());
    }

    @Test
    public void build_withProperty_isSuccessful() {
        IdentityProperty<String> property = IdentityProperty.create(String.class, "key");
        ResolveIdentityRequest request = ResolveIdentityRequest.builder()
                                                               .putProperty(property, "value")
                                                               .build();
        assertEquals("value", request.property(property));
    }

    @Test
    public void putProperty_sameProperty_isReplaced() {
        IdentityProperty<String> property = IdentityProperty.create(String.class, "key");
        ResolveIdentityRequest request = ResolveIdentityRequest.builder()
                                                               .putProperty(property, "value")
                                                               .putProperty(property, "value2")
                                                               .build();
        assertEquals("value2", request.property(property));
    }

    @Test
    public void copyBuilder_updateAddProperty_works() {
        IdentityProperty<String> property1 = IdentityProperty.create(String.class, "key1");
        ResolveIdentityRequest request = ResolveIdentityRequest.builder()
                                                               .putProperty(property1, "key1value1")
                                                               .build();

        IdentityProperty<String> property2 = IdentityProperty.create(String.class, "key2");
        request = request.copy(builder -> builder.putProperty(property1, "key1value2").putProperty(property2, "key2value1"));
        assertEquals("key1value2", request.property(property1));
        assertEquals("key2value1", request.property(property2));
    }
}
