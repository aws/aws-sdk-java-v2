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

package software.amazon.awssdk.enhanced.dynamodb.internal.immutable;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class ImmutableInfo<T> {
    private final Class<T> immutableClass;
    private final Class<?> builderClass;
    private final Method staticBuilderMethod;
    private final Method buildMethod;
    private final Collection<ImmutablePropertyDescriptor> propertyDescriptors;

    private ImmutableInfo(Builder<T> b) {
        this.immutableClass = b.immutableClass;
        this.builderClass = b.builderClass;
        this.staticBuilderMethod = b.staticBuilderMethod;
        this.buildMethod = b.buildMethod;
        this.propertyDescriptors = b.propertyDescriptors;
    }

    public Class<T> immutableClass() {
        return immutableClass;
    }

    public Class<?> builderClass() {
        return builderClass;
    }

    public Optional<Method> staticBuilderMethod() {
        return Optional.ofNullable(staticBuilderMethod);
    }

    public Method buildMethod() {
        return buildMethod;
    }

    public Collection<ImmutablePropertyDescriptor> propertyDescriptors() {
        return propertyDescriptors;
    }

    public static <T> Builder<T> builder(Class<T> immutableClass) {
        return new Builder<>(immutableClass);
    }

    public static final class Builder<T> {
        private final Class<T> immutableClass;
        private Class<?> builderClass;
        private Method staticBuilderMethod;
        private Method buildMethod;
        private Collection<ImmutablePropertyDescriptor> propertyDescriptors;

        private Builder(Class<T> immutableClass) {
            this.immutableClass = immutableClass;
        }

        public Builder<T> builderClass(Class<?> builderClass) {
            this.builderClass = builderClass;
            return this;
        }

        public Builder<T> staticBuilderMethod(Method builderMethod) {
            this.staticBuilderMethod = builderMethod;
            return this;
        }

        public Builder<T> buildMethod(Method buildMethod) {
            this.buildMethod = buildMethod;
            return this;
        }

        public Builder<T> propertyDescriptors(Collection<ImmutablePropertyDescriptor> propertyDescriptors) {
            this.propertyDescriptors = propertyDescriptors;
            return this;
        }

        public ImmutableInfo<T> build() {
            return new ImmutableInfo<>(this);
        }
    }
}
