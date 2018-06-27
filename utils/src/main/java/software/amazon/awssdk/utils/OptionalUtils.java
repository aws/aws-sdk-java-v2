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

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Functions that make working with optionals easier.
 */
@SdkProtectedApi
public final class OptionalUtils {
    /**
     * This class should be used statically.
     */
    private OptionalUtils() {}

    /**
     * Attempt to find a present-valued optional in a list of optionals.
     *
     * @param firstValue The first value that should be checked.
     * @param fallbackValues The suppliers we should check in order for a present value.
     * @return The first present value (or Optional.empty() if none are present)
     */
    @SafeVarargs
    public static <T> Optional<T> firstPresent(Optional<T> firstValue, Supplier<Optional<T>>... fallbackValues) {
        if (firstValue.isPresent()) {
            return firstValue;
        }

        for (Supplier<Optional<T>> fallbackValueSupplier : fallbackValues) {
            Optional<T> fallbackValue = fallbackValueSupplier.get();
            if (fallbackValue.isPresent()) {
                return fallbackValue;
            }
        }

        return Optional.empty();
    }
}
