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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.Pair;
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
    private static final ConcurrentMap<Pair<String, String>, IdentityProperty<?>> NAME_HISTORY = new ConcurrentHashMap<>();

    private final String namespace;
    private final String name;

    private IdentityProperty(String namespace, String name) {
        Validate.paramNotBlank(namespace, "namespace");
        Validate.paramNotBlank(name, "name");

        this.namespace = namespace;
        this.name = name;
        ensureUnique();
    }

    /**
     * Create a property.
     *
     * @param <T> the type of the property.
     * @param namespace the class *where* the property is being defined
     * @param name the name for the property
     * @throws IllegalArgumentException if a property with this namespace and name already exist
     */
    public static <T> IdentityProperty<T> create(Class<?> namespace, String name) {
        return new IdentityProperty<>(namespace.getName(), name);
    }

    private void ensureUnique() {
        IdentityProperty<?> prev = NAME_HISTORY.putIfAbsent(Pair.of(namespace, name), this);
        Validate.isTrue(prev == null,
                        "No duplicate IdentityProperty names allowed but both IdentityProperties %s and %s have the same "
                        + "namespace (%s) and name (%s). IdentityProperty should be referenced from a shared static constant to "
                        + "protect against erroneous or unexpected collisions.",
                        Integer.toHexString(System.identityHashCode(prev)),
                        Integer.toHexString(System.identityHashCode(this)),
                        namespace,
                        name);
    }

    @Override
    public String toString() {
        return ToString.builder("IdentityProperty")
                       .add("namespace", namespace)
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

        return Objects.equals(namespace, that.namespace) &&
               Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(namespace);
        hashCode = 31 * hashCode + Objects.hashCode(name);
        return hashCode;
    }
}
