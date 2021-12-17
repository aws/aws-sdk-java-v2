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

package software.amazon.awssdk.testutils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UnorderedCollectionComparatorTest {

    /**
     * Tests that UnorderedCollectionComparator.equalUnorderedCollections
     * correctly compares two unordered collections.
     */
    @Test
    public void testEqualUnorderedLists() {
        Assertions.assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(null, null));
        Assertions.assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(null, Collections.emptyList()));
        Assertions.assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(Collections.emptyList(), Collections.emptyList()));

        // Lists of Strings
        Assertions.assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("a", "b", "c")));
        Assertions.assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("a", "c", "b")));
        Assertions.assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("b", "a", "c")));
        Assertions.assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("b", "c", "a")));
        Assertions.assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("c", "a", "b")));
        Assertions.assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("c", "b", "a")));
        Assertions.assertFalse(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("a")));
        Assertions.assertFalse(UnorderedCollectionComparator.equalUnorderedCollections(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("a", "b", "d")));
    }
}
