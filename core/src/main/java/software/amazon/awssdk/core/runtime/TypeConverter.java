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

package software.amazon.awssdk.core.runtime;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * A utilities class used by generated clients to easily convert between nested types, such as lists and maps.
 */
@SdkProtectedApi
public final class TypeConverter {

    private TypeConverter() {
    }

    /**
     * Null-safely convert between types by applying a function.
     */
    public static <T, U> U convert(T toConvert, Function<? super T, ? extends U> converter) {
        if (toConvert == null) {
            return null;
        }

        return converter.apply(toConvert);
    }

    /**
     * Null-safely convert between two lists by applying a function to each value.
     */
    public static <T, U> List<U> convert(List<T> toConvert, Function<? super T, ? extends U> converter) {
        if (toConvert == null) {
            return null;
        }

        List<U> result = toConvert.stream().map(converter).collect(Collectors.toList());
        return Collections.unmodifiableList(result);
    }

    /**
     * Null-safely convert between two maps by applying a function to each key and value. A predicate is also specified to filter
     * the results, in case the mapping function were to generate duplicate keys, etc.
     */
    public static <T1, T2, U1, U2> Map<U1, U2> convert(Map<T1, T2> toConvert,
                                                       Function<? super T1, ? extends U1> keyConverter,
                                                       Function<? super T2, ? extends U2> valueConverter,
                                                       BiPredicate<U1, U2> resultFilter) {
        if (toConvert == null) {
            return null;
        }

        Map<U1, U2> result = toConvert.entrySet().stream()
                                      .map(e -> new SimpleImmutableEntry<>(keyConverter.apply(e.getKey()),
                                                                           valueConverter.apply(e.getValue())))
                                      .filter(p -> resultFilter.test(p.getKey(), p.getValue()))
                                      .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        return Collections.unmodifiableMap(result);
    }
}
