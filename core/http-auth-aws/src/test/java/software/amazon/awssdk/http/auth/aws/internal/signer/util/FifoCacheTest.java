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

package software.amazon.awssdk.http.auth.aws.internal.signer.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class FifoCacheTest {

    @Test
    public void test() {
        FifoCache<String> cache = new FifoCache<>(3);
        assertEquals(0, cache.size());
        cache.add("k1", "v1");
        assertEquals(1, cache.size());
        cache.add("k1", "v11");
        assertEquals(1, cache.size());
        cache.add("k2", "v2");
        assertEquals(2, cache.size());
        cache.add("k3", "v3");
        assertEquals(3, cache.size());
        assertEquals("v11", cache.get("k1"));
        assertEquals("v2", cache.get("k2"));
        assertEquals("v3", cache.get("k3"));
        cache.add("k4", "v4");
        assertEquals(3, cache.size());
        assertNull(cache.get("k1"));
    }

    @Test
    public void testZeroSize() {
        assertThrows(IllegalArgumentException.class, () -> new FifoCache<>(0));
    }

    @Test
    public void testIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new FifoCache<>(0));
    }

    @Test
    public void testSingleEntry() {
        FifoCache<String> cache = new FifoCache<String>(1);
        assertEquals(0, cache.size());
        cache.add("k1", "v1");
        assertEquals(1, cache.size());
        cache.add("k1", "v11");
        assertEquals(1, cache.size());
        assertEquals("v11", cache.get("k1"));

        cache.add("k2", "v2");
        assertEquals(1, cache.size());
        assertEquals("v2", cache.get("k2"));
        assertNull(cache.get("k1"));

        cache.add("k3", "v3");
        assertEquals(1, cache.size());
        assertEquals("v3", cache.get("k3"));
        assertNull(cache.get("k2"));
    }
}
