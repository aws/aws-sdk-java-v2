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

package software.amazon.awssdk.testutils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import software.amazon.awssdk.testutils.UnorderedCollectionComparator;

public class UnorderedCollectionComparatorTest {

    /**
     * Tests that UnorderedCollectionComparator.equalUnorderedCollections
     * correctly compares two unordered collections.
     */
    @Test
    public void testEqualUnorderedLists() {
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(null, null));
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(null, Collections.emptyList()));
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(Collections.emptyList(), Collections.emptyList()));

        // Lists of Strings
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("a", "b", "c")));
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("a", "c", "b")));
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("b", "a", "c")));
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("b", "c", "a")));
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("c", "a", "b")));
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("c", "b", "a")));
        assertFalse(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("a")));
        assertFalse(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("a", "b", "d")));
    }
}
