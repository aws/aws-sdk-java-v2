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

package software.amazon.awssdk.testutils.hamcrest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static software.amazon.awssdk.testutils.hamcrest.Matchers.containsOnlyInOrder;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class CollectionContainsOnlyInOrderTest {

    private final List<String> list = Arrays.asList("hello", "world");
    private final List<String> listWithDuplicates = Arrays.asList("hello", "world", "hello");


    @Test
    public void matchesIfAllElementsArePresentInOrder() {
        assertThat(list, containsOnlyInOrder("hello", "world"));
    }

    @Test(expected = AssertionError.class)
    public void failsIfElementsAreOutOfOrder() {
        assertThat(list, containsOnlyInOrder("world", "hello"));
    }

    @Test(expected = AssertionError.class)
    public void failsIfElementIsMissing() {
        assertThat(list, containsOnlyInOrder("hello", "world", "yay"));
    }

    @Test(expected = AssertionError.class)
    public void failsIfElementCollectionHasUnexpectedElement() {
        assertThat(list, containsOnlyInOrder("hello"));
    }

    @Test
    public void worksWithMatchers() {
        assertThat(list, containsOnlyInOrder(endsWith("lo"), endsWith("ld")));
    }

    @Test
    public void canHandleDuplicateElementsInCollectionUnderTest() {
        assertThat(listWithDuplicates, containsOnlyInOrder("hello", "world", "hello"));
    }

    @Test(expected = AssertionError.class)
    public void missingDuplicatesAreConsideredAFailure() {
        assertThat(listWithDuplicates, containsOnlyInOrder("hello", "world"));
    }


    @Test(expected = AssertionError.class)
    public void orderIsImportantWithDuplicatesToo() {
        assertThat(listWithDuplicates, containsOnlyInOrder("hello", "hello", "world"));
    }
}
