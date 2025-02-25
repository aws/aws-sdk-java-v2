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
 * Container object for the parameters used to construct a {@link BeanTableSchema}.
 *
 * @param <T> The type of the immutable item.
 * @see TableSchema#fromBean(BeanTableSchemaParams)
 * @see BeanTableSchema#fromBean(BeanTableSchemaParams)
 */
@SdkPublicApi
@Immutable
public final class BeanTableSchemaParams<T>
    implements ToCopyableBuilder<BeanTableSchemaParams.Builder<T>, BeanTableSchemaParams<T>> {
    private final Class<T> beanClass;
    private final MethodHandles.Lookup lookup;

    private BeanTableSchemaParams(BuilderImpl<T> builder) {
        this.beanClass = builder.beanClass;
        this.lookup = builder.lookup;
    }

    public Class<T> beanClass() {
        return beanClass;
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

    public interface Builder<T> extends CopyableBuilder<Builder<T>, BeanTableSchemaParams<T>> {

        /**
         * Set the class of the item.
         *
         * @return This builder for method chaining.
         */
        Builder<T> beanClass(Class<T> beanClass);

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
         * {@snippet lang = "java":
         *     BeanTableSchemaParams.builder(MyBean.class)
         *                           .lookup(MethodHandles.lookup())
         *                           .build();
         *}
         *
         * @return This builder for method chaining.
         */
        Builder<T> lookup(MethodHandles.Lookup lookup);
    }

    private static class BuilderImpl<T> implements Builder<T> {
        private Class<T> beanClass;
        private MethodHandles.Lookup lookup;

        private BuilderImpl(Class<T> beanClass) {
            this.beanClass = beanClass;
        }

        private BuilderImpl(BeanTableSchemaParams<T> info) {
            this.beanClass = info.beanClass;
            this.lookup = info.lookup;
        }

        public BuilderImpl<T> beanClass(Class<T> beanClass) {
            this.beanClass = beanClass;
            return this;
        }

        public BuilderImpl<T> lookup(MethodHandles.Lookup lookup) {
            this.lookup = lookup;
            return this;
        }

        @Override
        public BeanTableSchemaParams<T> build() {
            return new BeanTableSchemaParams<>(this);
        }
    }
}
