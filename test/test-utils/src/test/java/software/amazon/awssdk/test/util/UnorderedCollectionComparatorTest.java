package software.amazon.awssdk.test.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

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
