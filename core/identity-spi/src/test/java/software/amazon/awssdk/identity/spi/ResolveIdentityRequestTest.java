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
    private static final IdentityProperty<String> PROPERTY_1 =
        IdentityProperty.create(ResolveIdentityRequestTest.class, "key_1");
    private static final IdentityProperty<String> PROPERTY_2 =
        IdentityProperty.create(ResolveIdentityRequestTest.class, "key_2");

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
        ResolveIdentityRequest request = ResolveIdentityRequest.builder()
                                                               .putProperty(PROPERTY_1, "value")
                                                               .build();
        assertEquals("value", request.property(PROPERTY_1));
    }

    @Test
    public void putProperty_sameProperty_isReplaced() {
        ResolveIdentityRequest request = ResolveIdentityRequest.builder()
                                                               .putProperty(PROPERTY_1, "value")
                                                               .putProperty(PROPERTY_1, "value2")
                                                               .build();
        assertEquals("value2", request.property(PROPERTY_1));
    }

    @Test
    public void copyBuilder_addProperty_retains() {
        ResolveIdentityRequest request = ResolveIdentityRequest.builder()
                                                               .putProperty(PROPERTY_1, "key1value1")
                                                               .build();

        request = request.copy(builder -> builder.putProperty(PROPERTY_2, "key2value1"));
        assertEquals("key1value1", request.property(PROPERTY_1));
        assertEquals("key2value1", request.property(PROPERTY_2));
    }

    @Test
    public void copyBuilder_updateAddProperty_works() {
        ResolveIdentityRequest request = ResolveIdentityRequest.builder()
                                                               .putProperty(PROPERTY_1, "key1value1")
                                                               .build();
        request = request.copy(builder -> builder.putProperty(PROPERTY_1, "key1value2").putProperty(PROPERTY_2, "key2value1"));
        assertEquals("key1value2", request.property(PROPERTY_1));
        assertEquals("key2value1", request.property(PROPERTY_2));
    }
}
