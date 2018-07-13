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

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public final class CollectionUtils {

    private CollectionUtils() {
    }

    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNullOrEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Returns a new list containing the second list appended to the first list.
     */
    public static <T> List<T> mergeLists(List<T> list1, List<T> list2) {
        List<T> merged = new LinkedList<T>();
        if (list1 != null) {
            merged.addAll(list1);
        }
        if (list2 != null) {
            merged.addAll(list2);
        }
        return merged;
    }

    /**
     * Perform a deep copy of the provided map of lists. This only performs a deep copy of the map and lists. Entries are not
     * copied, so care should be taken to ensure that entries are immutable if preventing unwanted mutations of the elements is
     * desired.
     */
    public static <T, U> Map<T, List<U>> deepCopyMap(Map<T, ? extends List<U>> map) {
        return deepCopyMap(map, () -> new HashMap<>());
    }

    /**
     * Perform a deep copy of the provided map of lists. This only performs a deep copy of the map and lists. Entries are not
     * copied, so care should be taken to ensure that entries are immutable if preventing unwanted mutations of the elements is
     * desired.
     */
    public static <T, U> Map<T, List<U>> deepCopyMap(Map<T, ? extends List<U>> map, Supplier<Map<T, List<U>>> mapConstructor) {
        return map.entrySet().stream()
                  .collect(Collectors.toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue()),
                                 CollectionUtils::throwIllegalStateException, mapConstructor));
    }

    /**
     * Perform a deep copy of the provided map of lists, and make the result unmodifiable.
     */
    public static <T, U> Map<T, List<U>> deepUnmodifiableMap(Map<T, ? extends List<U>> map) {
        return deepUnmodifiableMap(map, () -> new HashMap<>());
    }

    /**
     * Perform a deep copy of the provided map of lists, and make the result unmodifiable.
     */
    public static <T, U> Map<T, List<U>> deepUnmodifiableMap(Map<T, ? extends List<U>> map,
                                                             Supplier<Map<T, List<U>>> mapConstructor) {
        return unmodifiableMap(map.entrySet().stream()
                                  .collect(Collectors.toMap(
                                      Map.Entry::getKey,
                                      e -> unmodifiableList(new ArrayList<>(e.getValue())),
                                      CollectionUtils::throwIllegalStateException,
                                      mapConstructor)));
    }


    /**
     * Collect a stream of {@link Map.Entry} to a {@link Map} with the same key/value types
     * @param <K> the key type
     * @param <V> the value type
     * @return a map
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> toMap() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    public static <K, VInT, VOutT> Map<K, VOutT> mapValues(Map<K, VInT> inputMap, Function<VInT, VOutT> mapper) {
        return inputMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> mapper.apply(e.getValue())));
    }

    /**
     * Dummy merger since there can't be a conflict when collecting from a map.
     */
    private static <T> T throwIllegalStateException(T left, T right) {
        throw new IllegalStateException("Duplicate keys are impossible when collecting from a map");
    }
}
