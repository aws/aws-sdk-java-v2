/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import software.amazon.awssdk.annotation.SdkProtectedApi;

@SdkProtectedApi
public class CollectionUtils {

    public static <T> boolean isNullOrEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
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
     * Joins a collection of strings with the given separator into a single string.
     *
     * @param toJoin    Collection containing items to join.
     * @param separator String to join items with.
     * @return Empty string if collection is null or empty. Otherwise joins all strings in the collection with the separator.
     */
    public static String join(Collection<String> toJoin, String separator) {
        if (isNullOrEmpty(toJoin)) {
            return "";
        }

        StringBuilder joinedString = new StringBuilder();
        int currentIndex = 0;
        for (String s : toJoin) {
            if (s != null) {
                joinedString.append(s);
            }
            if (currentIndex++ != toJoin.size() - 1) {
                joinedString.append(separator);
            }
        }
        return joinedString.toString();
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
                  .collect(toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue()),
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
                                  .collect(toMap(Map.Entry::getKey, e -> unmodifiableList(new ArrayList<>(e.getValue())),
                                                 CollectionUtils::throwIllegalStateException, mapConstructor)));
    }

    /**
     * Dummy merger since there can't be a conflict when collecting from a map.
     */
    private static <T> T throwIllegalStateException(T left, T right) {
        throw new IllegalStateException("Duplicate keys are impossible when collecting from a map");
    }
}
