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

package software.amazon.awssdk.identity.spi;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A strongly-typed property for input to an {@link IdentityProvider}.
 * @param <T> The type of the attribute.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class IdentityProperty<T> {
    private static final ConcurrentMap<String, IdentityProperty<?>> NAME_HISTORY = new ConcurrentHashMap<>();

    private final Class<T> clazz;
    private final String name;

    private IdentityProperty(Class<T> clazz, String name) {
        Validate.paramNotNull(clazz, "clazz");
        Validate.paramNotBlank(name, "name");

        this.clazz = clazz;
        this.name = name;
        ensureUnique();
    }

    public static <T> IdentityProperty<T> create(Class<T> clazz, String name) {
        return new IdentityProperty<>(clazz, name);
    }

    private void ensureUnique() {
        IdentityProperty<?> prev = NAME_HISTORY.putIfAbsent(name, this);
        if (prev != null) {
            throw new IllegalArgumentException(String.format("No duplicate IdentityProperty names allowed but both "
                                                             + "IdentityProperty %s and %s have the same name: %s. "
                                                             + "IdentityProperty should be referenced from a shared static "
                                                             + "constant to protect against erroneous or unexpected collisions.",
                                                             Integer.toHexString(System.identityHashCode(prev)),
                                                             Integer.toHexString(System.identityHashCode(this)),
                                                             name));
        }
    }

    @Override
    public String toString() {
        return ToString.builder("IdentityProperty")
                       .add("clazz", clazz)
                       .add("name", name)
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IdentityProperty<?> that = (IdentityProperty<?>) o;

        if (!clazz.equals(that.clazz)) {
            return false;
        }
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = clazz.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}
