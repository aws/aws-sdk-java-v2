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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
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

    @Test
    public void unmodifiableMapOfListsIsUnmodifiable() {
        assertUnsupported(m -> m.clear());
        assertUnsupported(m -> m.compute(null, null));
        assertUnsupported(m -> m.computeIfAbsent(null, null));
        assertUnsupported(m -> m.computeIfPresent(null, null));
        assertUnsupported(m -> m.forEach((k, v) -> v.clear()));
        assertUnsupported(m -> m.get("foo").clear());
        assertUnsupported(m -> m.getOrDefault("", emptyList()).clear());
        assertUnsupported(m -> m.getOrDefault("foo", null).clear());
        assertUnsupported(m -> m.merge(null, null, null));
        assertUnsupported(m -> m.put(null, null));
        assertUnsupported(m -> m.putAll(null));
        assertUnsupported(m -> m.putIfAbsent(null, null));
        assertUnsupported(m -> m.remove(null));
        assertUnsupported(m -> m.remove(null, null));
        assertUnsupported(m -> m.replace(null, null));
        assertUnsupported(m -> m.replace(null, null, null));
        assertUnsupported(m -> m.replaceAll(null));
        assertUnsupported(m -> m.values().clear());

        assertUnsupported(m -> m.keySet().clear());
        assertUnsupported(m -> m.keySet().add(null));
        assertUnsupported(m -> m.keySet().addAll(null));
        assertUnsupported(m -> m.keySet().remove(null));
        assertUnsupported(m -> m.keySet().removeAll(null));
        assertUnsupported(m -> m.keySet().retainAll(null));

        assertUnsupported(m -> m.entrySet().clear());
        assertUnsupported(m -> m.entrySet().add(null));
        assertUnsupported(m -> m.entrySet().addAll(null));
        assertUnsupported(m -> m.entrySet().remove(null));
        assertUnsupported(m -> m.entrySet().removeAll(null));
        assertUnsupported(m -> m.entrySet().retainAll(null));
        assertUnsupported(m -> m.entrySet().iterator().next().setValue(emptyList()));

        assertUnsupported(m -> m.values().clear());
        assertUnsupported(m -> m.values().add(null));
        assertUnsupported(m -> m.values().addAll(null));
        assertUnsupported(m -> m.values().remove(null));
        assertUnsupported(m -> m.values().removeAll(null));
        assertUnsupported(m -> m.values().retainAll(null));

        assertUnsupported(m -> m.values().iterator().next().clear());

        assertUnsupported(m -> {
            Iterator<Map.Entry<String, List<String>>> i = m.entrySet().iterator();
            i.next();
            i.remove();
        });

        assertUnsupported(m -> {
            Iterator<List<String>> i = m.values().iterator();
            i.next();
            i.remove();
        });

        assertUnsupported(m -> {
            Iterator<String> i = m.keySet().iterator();
            i.next();
            i.remove();
        });
    }

    @Test
    public void unmodifiableMapOfListsIsReadable() {
        assertSupported(m -> m.containsKey("foo"));
        assertSupported(m -> m.containsValue("foo"));
        assertSupported(m -> m.equals(null));
        assertSupported(m -> m.forEach((k, v) -> {}));
        assertSupported(m -> m.get("foo"));
        assertSupported(m -> m.getOrDefault("foo", null));
        assertSupported(m -> m.hashCode());
        assertSupported(m -> m.isEmpty());
        assertSupported(m -> m.keySet());
        assertSupported(m -> m.size());

        assertSupported(m -> m.keySet().contains(null));
        assertSupported(m -> m.keySet().containsAll(emptyList()));
        assertSupported(m -> m.keySet().equals(null));
        assertSupported(m -> m.keySet().hashCode());
        assertSupported(m -> m.keySet().isEmpty());
        assertSupported(m -> m.keySet().size());
        assertSupported(m -> m.keySet().spliterator());
        assertSupported(m -> m.keySet().toArray());
        assertSupported(m -> m.keySet().toArray(new String[0]));
        assertSupported(m -> m.keySet().stream());

        assertSupported(m -> m.entrySet().contains(null));
        assertSupported(m -> m.entrySet().containsAll(emptyList()));
        assertSupported(m -> m.entrySet().equals(null));
        assertSupported(m -> m.entrySet().hashCode());
        assertSupported(m -> m.entrySet().isEmpty());
        assertSupported(m -> m.entrySet().size());
        assertSupported(m -> m.entrySet().spliterator());
        assertSupported(m -> m.entrySet().toArray());
        assertSupported(m -> m.entrySet().toArray(new Map.Entry[0]));
        assertSupported(m -> m.entrySet().stream());

        assertSupported(m -> m.values().contains(null));
        assertSupported(m -> m.values().containsAll(emptyList()));
        assertSupported(m -> m.values().equals(null));
        assertSupported(m -> m.values().hashCode());
        assertSupported(m -> m.values().isEmpty());
        assertSupported(m -> m.values().size());
        assertSupported(m -> m.values().spliterator());
        assertSupported(m -> m.values().toArray());
        assertSupported(m -> m.values().toArray(new Collection[0]));
        assertSupported(m -> m.values().stream());

        assertSupported(m -> m.entrySet().iterator().next());
        assertSupported(m -> m.entrySet().iterator().hasNext());
        assertSupported(m -> m.values().iterator().next());
        assertSupported(m -> m.values().iterator().hasNext());
        assertSupported(m -> m.keySet().iterator().next());
        assertSupported(m -> m.keySet().iterator().hasNext());
    }

    public void assertUnsupported(Consumer<Map<String, List<String>>> mutation) {
        Map<String, List<String>> map = new HashMap<>();
        map.put("foo", singletonList("bar"));

        assertThatThrownBy(() -> mutation.accept(CollectionUtils.unmodifiableMapOfLists(map)))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    public void assertSupported(Consumer<Map<String, List<String>>> mutation) {
        Map<String, List<String>> map = new HashMap<>();
        map.put("foo", singletonList("bar"));

        mutation.accept(map);
    }
}
