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

package software.amazon.awssdk.s3.mockserver.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class Immutable {

    private Immutable() {
    }

    @SafeVarargs
    public static <E> List<E> listOf(E... elements) {
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(requireNoNulls(elements))));
    }

    public static <E> List<E> copyOfList(Collection<? extends E> source) {
        return Collections.unmodifiableList(new ArrayList<>(requireNoNulls(source)));
    }

    @SafeVarargs
    public static <E> Set<E> setOf(E... elements) {
        Set<E> set = new LinkedHashSet<>();
        for (E e : requireNoNulls(elements)) {
            if (!set.add(e)) {
                throw new IllegalArgumentException("duplicate element: " + e);
            }
        }
        return Collections.unmodifiableSet(set);
    }

    public static <E> Set<E> copyOfSet(Collection<? extends E> source) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(requireNoNulls(source)));
    }

    public static <K, V> Map<K, V> mapOf() {
        return Collections.emptyMap();
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1) {
        return build(k1, v1);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
        return build(k1, v1, k2, v2);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        return build(k1, v1, k2, v2, k3, v3);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return build(k1, v1, k2, v2, k3, v3, k4, v4);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return build(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                                         K k6, V v6) {
        return build(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                                         K k6, V v6, K k7, V v7) {
        return build(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                                         K k6, V v6, K k7, V v7, K k8, V v8) {
        return build(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                                         K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
        return build(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                                         K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
        return build(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
    }

    public static <K, V> Map<K, V> copyOfMap(Map<? extends K, ? extends V> source) {
        Map<K, V> map = new LinkedHashMap<>();
        for (Map.Entry<? extends K, ? extends V> e : source.entrySet()) {
            map.put(Objects.requireNonNull(e.getKey()), Objects.requireNonNull(e.getValue()));
        }
        return Collections.unmodifiableMap(map);
    }

    public static <E> Collector<E, ?, List<E>> toList() {
        return Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> build(Object... pairs) {
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            K key = (K) Objects.requireNonNull(pairs[i]);
            V value = (V) Objects.requireNonNull(pairs[i + 1]);
            if (map.put(key, value) != null) {
                throw new IllegalArgumentException("duplicate key: " + key);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static <E> E[] requireNoNulls(E[] elements) {
        for (E e : elements) {
            Objects.requireNonNull(e);
        }
        return elements;
    }

    private static <C extends Collection<?>> C requireNoNulls(C elements) {
        for (Object e : elements) {
            Objects.requireNonNull(e);
        }
        return elements;
    }
}
