/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit tests {@link ArrayUtils}.
 *
 * Adapted from https://github.com/apache/commons-lang.
 */
public class ArrayUtilsTest {

    //-----------------------------------------------------------------------

    @Test
    public void testLastIndexOf() {
        final Object[] array = new Object[] {"0", "1", "2", "3", null, "0"};
        assertEquals(-1, ArrayUtils.lastIndexOf(null, null));
        assertEquals(-1, ArrayUtils.lastIndexOf(null, "0"));
        assertEquals(5, ArrayUtils.lastIndexOf(array, "0"));
        assertEquals(1, ArrayUtils.lastIndexOf(array, "1"));
        assertEquals(2, ArrayUtils.lastIndexOf(array, "2"));
        assertEquals(3, ArrayUtils.lastIndexOf(array, "3"));
        assertEquals(4, ArrayUtils.lastIndexOf(array, null));
        assertEquals(-1, ArrayUtils.lastIndexOf(array, "notInArray"));
    }

    @Test
    public void testContains() {
        final Object[] array = new Object[] {"0", "1", "2", "3", null, "0"};
        assertFalse(ArrayUtils.contains(null, null));
        assertFalse(ArrayUtils.contains(null, "1"));
        assertTrue(ArrayUtils.contains(array, "0"));
        assertTrue(ArrayUtils.contains(array, "1"));
        assertTrue(ArrayUtils.contains(array, "2"));
        assertTrue(ArrayUtils.contains(array, "3"));
        assertTrue(ArrayUtils.contains(array, null));
        assertFalse(ArrayUtils.contains(array, "notInArray"));
    }

    @Test
    public void testContains_LANG_1261() {
        class LANG1261ParentObject {
            @Override
            public boolean equals(final Object o) {
                return true;
            }
        }
        class LANG1261ChildObject extends LANG1261ParentObject {
        }

        final Object[] array = new LANG1261ChildObject[] {new LANG1261ChildObject()};

        assertTrue(ArrayUtils.contains(array, new LANG1261ParentObject()));
    }

    //-----------------------------------------------------------------------

    /**
     * Test for {@link ArrayUtils#isEmpty(java.lang.Object[])}.
     */
    @Test
    public void testIsEmptyObject() {
        final Object[] emptyArray = new Object[] {};
        final Object[] notEmptyArray = new Object[] {"Value"};
        assertTrue(ArrayUtils.isEmpty(null));
        assertTrue(ArrayUtils.isEmpty(emptyArray));
        assertFalse(ArrayUtils.isEmpty(notEmptyArray));
    }

    // ------------------------------------------------------------------------
    @Test
    public void testGetLength() {
        assertEquals(0, ArrayUtils.getLength(null));

        final Object[] emptyObjectArray = new Object[0];
        final Object[] notEmptyObjectArray = new Object[] {"aValue"};
        assertEquals(0, ArrayUtils.getLength(null));
        assertEquals(0, ArrayUtils.getLength(emptyObjectArray));
        assertEquals(1, ArrayUtils.getLength(notEmptyObjectArray));

        try {
            ArrayUtils.getLength("notAnArray");
            fail("IllegalArgumentException should have been thrown");
        } catch (final IllegalArgumentException e) {
            // Expected
        }
    }
}
