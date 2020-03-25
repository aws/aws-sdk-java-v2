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

package software.amazon.awssdk.services.s3.internal;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Lazy;

/**
 * A helper class for setting a field's value to a default if it isn't specified, while still keeping track of whether the value
 * was from the default or from the field.
 *
 * For example, a "profile name" field-with-default might be set to "null" with a default of "foo". {@link #value()} returns
 * "foo", while {@link #isDefault()} can be used to keep track of the fact that the value was from the default.
 */
@SdkInternalApi
public abstract class FieldWithDefault<T> {
    private FieldWithDefault(){
    }

    /**
     * Create a {@link FieldWithDefault} using the provided field and its default value. If the field is null, the default value
     * will be returned by {@link #value()} and {@link #isDefault()} will return true. If the field is not null, the field value
     * will be returned by {@link #value()} and {@link #isDefault()} will return false.
     *
     * @see #createLazy(Object, Supplier)
     */
    public static <T> FieldWithDefault<T> create(T field, T defaultValue) {
        return new Impl<>(field, defaultValue);
    }

    /**
     * Create a {@link FieldWithDefault} using the provided field and its default value. If the field is null, the default value
     * will be returned by {@link #value()} and {@link #isDefault()} will return true. If the field is not null, the field value
     * will be returned by {@link #value()} and {@link #isDefault()} will return false.
     *
     * <p>This differs from {@link #create(Object, Object)} in that the default value won't be resolved if the provided field is
     * not null. The default value also won't be resolved until the first {@link #value()} call. This is useful for delaying
     * expensive calculations until right before they're needed.
     */
    public static <T> FieldWithDefault<T> createLazy(T field, Supplier<T> defaultValue) {
        return new LazyImpl<>(field, defaultValue);
    }

    /**
     * Retrieve the value of this field.
     */
    public abstract T value();

    /**
     * True, if the value returned by {@link #value()} is the default value (i.e. the field is null). False otherwise.
     */
    public abstract boolean isDefault();

    /**
     * Return the field exactly as it was specified when the field-with-default was created. If the field was null, this will
     * return null. This will not resolve the default if this is a field from {@link #createLazy(Object, Supplier)}.
     */
    public abstract T valueOrNullIfDefault();

    private static class Impl<T> extends FieldWithDefault<T> {
        private final T value;
        private final boolean isDefault;

        private Impl(T field, T defaultValue) {
            this.value = field != null ? field : defaultValue;
            this.isDefault = field == null;
        }

        @Override
        public T value() {
            return value;
        }

        @Override
        public boolean isDefault() {
            return isDefault;
        }

        @Override
        public T valueOrNullIfDefault() {
            return isDefault ? null : value;
        }
    }

    private static class LazyImpl<T> extends FieldWithDefault<T> {
        private final Lazy<T> value;
        private final boolean isDefault;

        private LazyImpl(T field, Supplier<T> defaultValue) {
            this.value = field != null ? new Lazy<>(() -> field) : new Lazy<>(defaultValue);
            this.isDefault = field == null;
        }

        @Override
        public T value() {
            return value.getValue();
        }

        @Override
        public boolean isDefault() {
            return isDefault;
        }

        @Override
        public T valueOrNullIfDefault() {
            return isDefault ? null : value.getValue();
        }
    }
}
