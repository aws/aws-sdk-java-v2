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

package software.amazon.awssdk.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;

/**
 * Tests for the marshaller cache on {@link SdkField}.
 *
 * <p><b>Validates: Requirements 7.1, 7.2</b></p>
 * <p><b>Property 2: Marshaller cache round-trip</b></p>
 */
public class SdkFieldCacheMarshallerTest {

    private static SdkField<String> newStringField() {
        return SdkField.<String>builder(MarshallingType.STRING)
                       .memberName("testField")
                       .getter(obj -> null)
                       .setter((obj, val) -> { })
                       .traits(LocationTrait.builder()
                                            .location(MarshallLocation.PAYLOAD)
                                            .locationName("testField")
                                            .build())
                       .build();
    }

    /**
     * cachedMarshaller returns null when nothing has been cached yet.
     */
    @Test
    public void cachedMarshaller_beforeAnyCaching_returnsNull() {
        SdkField<String> field = newStringField();
        Object registryKey = new Object();

        Object cached = field.cachedMarshaller(registryKey);
        assertThat(cached).isNull();
    }

    /**
     * Round-trip: cacheMarshaller(key, m) then cachedMarshaller(key) returns the same instance.
     */
    @Test
    public void cachedMarshaller_afterCaching_returnsSameInstance() {
        SdkField<String> field = newStringField();
        Object registryKey = new Object();
        Object marshaller = new Object();

        field.cacheMarshaller(registryKey, marshaller);

        Object cached = field.cachedMarshaller(registryKey);
        assertThat(cached).isSameAs(marshaller);
    }

    /**
     * A different registry key reference returns null, even if both keys are "equal" by value.
     * The cache uses reference identity (==), not equals().
     */
    @Test
    public void cachedMarshaller_differentKeyReference_returnsNull() {
        SdkField<String> field = newStringField();
        // Use strings constructed so they are .equals() but not ==
        String key1 = new String("registry");
        String key2 = new String("registry");
        Object marshaller = new Object();

        field.cacheMarshaller(key1, marshaller);

        // key2.equals(key1) is true, but key2 != key1
        Object cached = field.cachedMarshaller(key2);
        assertThat(cached).isNull();
    }

    /**
     * Overwriting the cache with a new registry key replaces the old entry.
     * The old key no longer returns the old marshaller (single-slot replacement).
     */
    @Test
    public void cacheMarshaller_overwrite_replacesOldEntry() {
        SdkField<String> field = newStringField();
        Object oldKey = new Object();
        Object oldMarshaller = new Object();
        Object newKey = new Object();
        Object newMarshaller = new Object();

        field.cacheMarshaller(oldKey, oldMarshaller);
        Object cachedOld = field.cachedMarshaller(oldKey);
        assertThat(cachedOld).isSameAs(oldMarshaller);

        // Overwrite with a new key
        field.cacheMarshaller(newKey, newMarshaller);

        // New key returns the new marshaller
        Object cachedNew = field.cachedMarshaller(newKey);
        assertThat(cachedNew).isSameAs(newMarshaller);
        // Old key no longer returns anything
        Object cachedOldAfter = field.cachedMarshaller(oldKey);
        assertThat(cachedOldAfter).isNull();
    }
}
