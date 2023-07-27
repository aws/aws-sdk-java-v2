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
    private final Class<T> clazz;
    private final String name;

    private SignerProperty(Class<T> clazz, String name) {
        Validate.paramNotNull(clazz, "clazz");
        Validate.paramNotBlank(name, "name");

        this.clazz = clazz;
        this.name = name;
    }

    /**
     * Create an instance of a property with a given type and name.
     */
    public static <T> SignerProperty<T> create(Class<T> clazz, String name) {
        return new SignerProperty<>(clazz, name);
    }

    /**
     * Validate that the {@link SignerProperty} is present in the {@link SignRequest}.
     * <p>
     * The value, {@link T}, is return when present, and an exception is thrown otherwise.
     */
    public static <T> T validatedProperty(SignRequest<?, ?> request, SignerProperty<T> property) {
        return Validate.notNull(request.property(property), property.toString() + " must not be null!");
    }

    /**
     * Validate that the {@link SignerProperty} is present in the {@link SignRequest}.
     * <p>
     * The value, {@link T}, is return when present, and the default is returned otherwise.
     */
    public static <T> T validatedProperty(SignRequest<?, ?> request, SignerProperty<T> property, T defaultValue) {
        return Validate.getOrDefault(request.property(property), () -> defaultValue);
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
