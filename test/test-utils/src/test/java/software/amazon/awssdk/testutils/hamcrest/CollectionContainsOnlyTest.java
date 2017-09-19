/*
 * Copyright (c) 2017. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.testutils.hamcrest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static software.amazon.awssdk.testutils.hamcrest.Matchers.containsOnly;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class CollectionContainsOnlyTest {
    private final List<String> list = Arrays.asList("hello", "world");
    private final List<String> listWithDuplicates = Arrays.asList("hello", "world", "hello");


    @Test
    public void matchesIfAllElementsArePresent() {
        assertThat(list, containsOnly("hello", "world"));
    }

    @Test
    public void matchesIfAllElementsArePresentInAnyOrder() {
        assertThat(list, containsOnly("world", "hello"));
    }

    @Test(expected = AssertionError.class)
    public void failsIfElementIsMissing() {
        assertThat(list, containsOnly("hello", "world", "yay"));
    }

    @Test(expected = AssertionError.class)
    public void failsIfElementCollectionHasUnexpectedElement() {
        assertThat(list, containsOnly("hello"));
    }

    @Test
    public void worksWithMatchers() {
        assertThat(list, containsOnly(endsWith("lo"), endsWith("ld")));
    }

    @Test
    public void canHandleDuplicateElementsInCollectionUnderTest() {
        assertThat(listWithDuplicates, containsOnly("hello", "hello", "world"));
    }

    @Test(expected = AssertionError.class)
    public void missingDuplicatesAreConsideredAFailure() {
        assertThat(listWithDuplicates, containsOnly("hello", "world"));
    }
}
