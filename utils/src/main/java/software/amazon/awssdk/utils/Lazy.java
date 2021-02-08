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

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A class that lazily constructs a value the first time {@link #getValue()} is invoked.
 *
 * This should be {@link #close()}d if the initializer returns value that needs to be {@link AutoCloseable#close()}d.
 */
@SdkPublicApi
public class Lazy<T> implements SdkAutoCloseable {
    private final Supplier<T> initializer;

    private volatile T value;

    public Lazy(Supplier<T> initializer) {
        this.initializer = initializer;
    }

    public T getValue() {
        T result = value;
        if (result == null) {
            synchronized (this) {
                result = value;
                if (result == null) {
                    result = initializer.get();
                    value = result;
                }
            }
        }

        return result;
    }

    @Override
    public String toString() {
        T value = this.value;
        return ToString.builder("Lazy")
                       .add("value", value == null ? "Uninitialized" : value)
                       .build();
    }

    @Override
    public void close() {
        try {
            // Make sure the value has been initialized before we attempt to close it
            getValue();
        } catch (RuntimeException e) {
            // Failed to initialize the value.
        }

        IoUtils.closeIfCloseable(initializer, null);
        IoUtils.closeIfCloseable(value, null);
    }
}
