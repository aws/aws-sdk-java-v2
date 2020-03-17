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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class CollectionUtilsTest {

    @Test
    public void isEmpty_NullCollection_ReturnsTrue() {
        assertTrue(CollectionUtils.isNullOrEmpty((Collection<?>) null));
    }

    @Test
    public void isEmpty_EmptyCollection_ReturnsTrue() {
        assertTrue(CollectionUtils.isNullOrEmpty(Collections.emptyList()));
    }

    @Test
    public void isEmpty_NonEmptyCollection_ReturnsFalse() {
        assertFalse(CollectionUtils.isNullOrEmpty(Arrays.asList("something")));
    }

    @Test
    public void firstIfPresent_NullList_ReturnsNull() {
        List<String> list = null;
        assertThat(CollectionUtils.firstIfPresent(list)).isNull();
    }

    @Test
    public void firstIfPresent_EmptyList_ReturnsNull() {
        List<String> list = Collections.emptyList();
        assertThat(CollectionUtils.firstIfPresent(list)).isNull();
    }

    @Test
    public void firstIfPresent_SingleElementList_ReturnsOnlyElement() {
        assertThat(CollectionUtils.firstIfPresent(singletonList("foo"))).isEqualTo("foo");
    }

    @Test
    public void firstIfPresent_MultipleElementList_ReturnsFirstElement() {
        assertThat(CollectionUtils.firstIfPresent(Arrays.asList("foo", "bar", "baz"))).isEqualTo("foo");
    }

    @Test
    public void firstIfPresent_FirstElementNull_ReturnsNull() {
        assertThat(CollectionUtils.firstIfPresent(Arrays.asList(null, "bar", "baz"))).isNull();
    }
}
