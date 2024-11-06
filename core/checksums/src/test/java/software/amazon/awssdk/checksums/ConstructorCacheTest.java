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

package software.amazon.awssdk.checksums;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.util.Optional;
import software.amazon.awssdk.checksums.internal.ConstructorCache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstructorCacheTest {

    private ConstructorCache constructorCache;

    @BeforeEach
    void setUp() {
        constructorCache = new ConstructorCache();
    }

    @Test
    void testGetConstructor_existingClassWithNoArgsConstructor() {
        // Test with a known class that has a no-args constructor, e.g., String
        Optional<Constructor<?>> constructor = constructorCache.getConstructor("java.lang.String");
        assertTrue(constructor.isPresent());
        assertEquals(String.class, constructor.get().getDeclaringClass());
    }

    @Test
    void testGetConstructor_existingClassWithoutNoArgsConstructor() {
        // Test with a class that doesn't have a no-args constructor
        Optional<Constructor<?>> constructor = constructorCache.getConstructor("java.util.Scanner");
        assertFalse(constructor.isPresent());
    }

    @Test
    void testGetConstructor_nonExistingClass() {
        // Test with a non-existing class name
        Optional<Constructor<?>> constructor = constructorCache.getConstructor("non.existent.ClassName");
        assertFalse(constructor.isPresent());
    }
}
