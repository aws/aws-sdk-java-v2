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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import java.lang.invoke.MethodHandles;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Container object for the parameters used to construct a {@link ImmutableTableSchema}.
 *
 * @param <T> The type of the immutable item.
 * @see TableSchema#fromImmutableClass(ImmutableTableSchemaParams)
 * @see ImmutableTableSchema#fromImmutableClass(ImmutableTableSchemaParams)
 */
@SdkPublicApi
@Immutable
public final class ImmutableTableSchemaParams<T>
    implements ToCopyableBuilder<ImmutableTableSchemaParams.Builder<T>, ImmutableTableSchemaParams<T>> {
    private final Class<T> immutableClass;
    private final MethodHandles.Lookup lookup;

    private ImmutableTableSchemaParams(BuilderImpl<T> builder) {
        this.immutableClass = builder.immutableClass;
        this.lookup = builder.lookup;
    }

    public Class<T> immutableClass() {
        return immutableClass;
    }

    public MethodHandles.Lookup lookup() {
        return lookup;
    }

    @Override
    public Builder<T> toBuilder() {
        return new BuilderImpl<>(this);
    }

    public static <T> Builder<T> builder(Class<T> beanClass) {
        return new BuilderImpl<>(beanClass);
    }

    public interface Builder<T> extends CopyableBuilder<Builder<T>, ImmutableTableSchemaParams<T>> {

        /**
         * Set the class of the immutable item.
         *
         * @return This builder for method chaining.
         */
        Builder<T> immutableClass(Class<T> immutableClass);

        /**
         * Set the {@link MethodHandles.Lookup} that will be used for reflection and unreflection purposes on the provided item
         * class, including security and access checking. A lookup object is created by calling {@link MethodHandles#lookup()}.
         * Note that this method is caller-sensitive, which means that the return value of {@code lookup()} relies on the class
         * that invoked it. When providing a custom lookup object, you should ensure that the lookup is created from a class
         * that belongs to your application to ensure it has the correct access to the item.
         * <p>
         * In practical terms, calling {@code MethodHandles.lookup()} when creating this {@code ReflectiveSchemaParams} is
         * normally sufficient:
         *
         * {@snippet lang="java" :
         *     ImmutableTableSchemaParams.builder(MyBean.class)
         *                           .lookup(MethodHandles.lookup())
         *                           .build();
         * }
         * @return This builder for method chaining.
         */
        Builder<T> lookup(MethodHandles.Lookup lookup);
    }

    private static class BuilderImpl<T> implements Builder<T> {
        private Class<T> immutableClass;
        private MethodHandles.Lookup lookup;

        private BuilderImpl(Class<T> beanClass) {
            this.immutableClass = beanClass;
        }

        private BuilderImpl(ImmutableTableSchemaParams<T> info) {
            this.immutableClass = info.immutableClass;
            this.lookup = info.lookup;
        }

        public BuilderImpl<T> immutableClass(Class<T> beanClass) {
            this.immutableClass = beanClass;
            return this;
        }

        public BuilderImpl<T> lookup(MethodHandles.Lookup lookup) {
            this.lookup = lookup;
            return this;
        }

        @Override
        public ImmutableTableSchemaParams<T> build() {
            return new ImmutableTableSchemaParams<>(this);
        }
    }
}
