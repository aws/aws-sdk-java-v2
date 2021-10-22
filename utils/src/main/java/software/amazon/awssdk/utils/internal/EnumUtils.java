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

package software.amazon.awssdk.utils.internal;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Utility class for working with {@link Enum}s.
 */
@SdkInternalApi
public final class EnumUtils {
    private EnumUtils() {
    }

    /**
     * Create a map that indexes all enum values by a given index function. This can offer a faster runtime complexity
     * compared to iterating an enum's {@code values()}.
     *
     * @see CollectionUtils#uniqueIndex(Iterable, Function)
     */
    public static <K, V extends Enum<V>> Map<K, V> uniqueIndex(Class<V> enumType, Function<? super V, K> indexFunction) {
        return CollectionUtils.uniqueIndex(EnumSet.allOf(enumType), indexFunction);
    }
}
