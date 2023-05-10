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
    private final Class<T> clazz;
    private final String name;

    private IdentityProperty(Class<T> clazz, String name) {
        Validate.paramNotNull(clazz, "clazz");
        Validate.paramNotBlank(name, "name");

        this.clazz = clazz;
        this.name = name;
    }

    public static <T> IdentityProperty<T> create(Class<T> clazz, String name) {
        return new IdentityProperty<>(clazz, name);
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
