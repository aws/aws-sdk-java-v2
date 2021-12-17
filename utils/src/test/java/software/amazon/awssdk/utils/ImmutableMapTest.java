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

package software.amazon.awssdk.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the ImmutableMapTest class.
 */
public class ImmutableMapTest {

    @Test
    public void testMapBuilder() {
        Map<Integer, String> builtMap = new ImmutableMap.Builder<Integer, String>()
                .put(1, "one")
                .put(2, "two")
                .put(3, "three")
                .build();
        Assertions.assertEquals(3, builtMap.size());
        Assertions.assertEquals("one", builtMap.get(1));
        Assertions.assertEquals("two", builtMap.get(2));
        Assertions.assertEquals("three", builtMap.get(3));
    }

    @Test
    public void testOfBuilder() {
        Map<Integer, String> builtMap = ImmutableMap.of(1, "one");
        Assertions.assertEquals(1, builtMap.size());
        Assertions.assertEquals("one", builtMap.get(1));
        builtMap = ImmutableMap.of(1, "one", 2, "two");
        Assertions.assertEquals(2, builtMap.size());
        Assertions.assertEquals("one", builtMap.get(1));
        Assertions.assertEquals("two", builtMap.get(2));
        builtMap = ImmutableMap.of(1, "one", 2, "two", 3, "three");
        Assertions.assertEquals(3, builtMap.size());
        Assertions.assertEquals("one", builtMap.get(1));
        Assertions.assertEquals("two", builtMap.get(2));
        Assertions.assertEquals("three", builtMap.get(3));
        builtMap = ImmutableMap.of(1, "one", 2, "two", 3, "three", 4, "four");
        Assertions.assertEquals(4, builtMap.size());
        Assertions.assertEquals("one", builtMap.get(1));
        Assertions.assertEquals("two", builtMap.get(2));
        Assertions.assertEquals("three", builtMap.get(3));
        Assertions.assertEquals("four", builtMap.get(4));
        builtMap = ImmutableMap.of(1, "one", 2, "two", 3, "three", 4, "four", 5, "five");
        Assertions.assertEquals(5, builtMap.size());
        Assertions.assertEquals("one", builtMap.get(1));
        Assertions.assertEquals("two", builtMap.get(2));
        Assertions.assertEquals("three", builtMap.get(3));
        Assertions.assertEquals("four", builtMap.get(4));
        Assertions.assertEquals("five", builtMap.get(5));
    }

    @Test
    public void testErrorOnDuplicateKeys() {
        try {
            Map<Integer, String> builtMap = new ImmutableMap.Builder<Integer, String>()
                    .put(1, "one")
                    .put(1, "two")
                    .build();
            Assertions.fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
            // Ignored or expected.
        } catch (Exception e) {
            Assertions.fail("IllegalArgumentException expected.");
        }
    }

    @Test
    public void testMapOperations() {
        Map<Integer, String> builtMap = new ImmutableMap.Builder<Integer, String>()
                .put(1, "one")
                .put(2, "two")
                .put(3, "three")
                .build();
        Assertions.assertTrue(builtMap.containsKey(1));
        Assertions.assertTrue(builtMap.containsValue("one"));
        Assertions.assertTrue(builtMap.values().contains("one"));
        Assertions.assertEquals("one", builtMap.get(1));
        Assertions.assertEquals(3, builtMap.entrySet().size());
        Assertions.assertEquals(3, builtMap.values().size());

        Assertions.assertEquals(3, builtMap.size());

        /** Unsupported methods **/
        try {
            builtMap.clear();
            Assertions.fail("UnsupportedOperationException expected.");
        } catch (UnsupportedOperationException iae) {
            // Ignored or expected.
        } catch (Exception e) {
            Assertions.fail("UnsupportedOperationException expected.");
        }
        try {
            builtMap.put(4, "four");
            Assertions.fail("UnsupportedOperationException expected.");
        } catch (UnsupportedOperationException iae) {
            // Ignored or expected.
        } catch (Exception e) {
            Assertions.fail("UnsupportedOperationException expected.");
        }
        try {
            builtMap.putAll(Collections.singletonMap(4, "four"));
            Assertions.fail("UnsupportedOperationException expected.");
        } catch (UnsupportedOperationException iae) {
            // Ignored or expected.
        } catch (Exception e) {
            Assertions.fail("UnsupportedOperationException expected.");
        }
        try {
            builtMap.remove(1);
            Assertions.fail("UnsupportedOperationException expected.");
        } catch (UnsupportedOperationException iae) {
            // Ignored or expected.
        } catch (Exception e) {
            Assertions.fail("UnsupportedOperationException expected.");
        }
    }
}
