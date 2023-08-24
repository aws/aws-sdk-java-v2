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

package software.amazon.awssdk.http.auth.spi;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A strongly-typed property for input to an {@link HttpSigner}.
 * @param <T> The type of the property.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class SignerProperty<T> {
    private static final ConcurrentMap<String, SignerProperty<?>> NAME_HISTORY = new ConcurrentHashMap<>();

    private final Class<T> clazz;
    private final String name;

    private SignerProperty(Class<T> clazz, String name) {
        Validate.paramNotNull(clazz, "clazz");
        Validate.paramNotBlank(name, "name");

        this.clazz = clazz;
        this.name = name;
        ensureUnique();
    }

    /**
     * Create an instance of a property with a given type and name.
     */
    public static <T> SignerProperty<T> create(Class<T> clazz, String name) {
        return new SignerProperty<>(clazz, name);
    }

    private void ensureUnique() {
        SignerProperty<?> prev = NAME_HISTORY.putIfAbsent(name, this);
        if (prev != null) {
            throw new IllegalArgumentException(String.format("No duplicate SignerProperty names allowed but both "
                                                             + "SignerProperty %s and %s have the same name: %s. "
                                                             + "SignerProperty should be referenced from a shared static "
                                                             + "constant to protect against erroneous or unexpected collisions.",
                                                             Integer.toHexString(System.identityHashCode(prev)),
                                                             Integer.toHexString(System.identityHashCode(this)),
                                                             name));
        }
    }

    @Override
    public String toString() {
        return ToString.builder("SignerProperty")
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

        SignerProperty<?> that = (SignerProperty<?>) o;

        return Objects.equals(clazz, that.clazz) &&
               Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(clazz);
        hashCode = 31 * hashCode + Objects.hashCode(name);
        return hashCode;
    }
}
