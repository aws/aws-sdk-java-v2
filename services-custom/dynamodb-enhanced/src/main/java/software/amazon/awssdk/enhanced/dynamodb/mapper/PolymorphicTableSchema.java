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

import java.util.Map;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSupertype;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A polymorphic {@link TableSchema} that routes items to subtypes based on a discriminator attribute
 * (see {@link DynamoDbSupertype}).
 * <p>
 * Typically constructed automatically via {@link TableSchemaFactory#fromClass(Class)}
 * when a class is annotated with {@link DynamoDbSupertype}. For manual assembly, use {@link #builder(Class)}.
 */
@SdkPublicApi
public final class PolymorphicTableSchema<T> extends WrappedTableSchema<T, StaticPolymorphicTableSchema<T>> {

    private PolymorphicTableSchema(Builder<T> builder) {
        super(builder.delegate.build());
    }

    /**
     * Returns a builder for manually creating a {@link PolymorphicTableSchema}.
     *
     * @param rootClass the root type that all subtypes must extend
     */
    public static <T> Builder<T> builder(Class<T> rootClass) {
        return new Builder<>(rootClass);
    }

    @Override
    public TableSchema<? extends T> subtypeTableSchema(T itemContext) {
        return delegateTableSchema().subtypeTableSchema(itemContext);
    }

    @Override
    public TableSchema<? extends T> subtypeTableSchema(Map<String, AttributeValue> itemContext) {
        return delegateTableSchema().subtypeTableSchema(itemContext);
    }

    @NotThreadSafe
    public static final class Builder<T> {
        private final StaticPolymorphicTableSchema.Builder<T> delegate;

        private Builder(Class<T> rootClass) {
            this.delegate = StaticPolymorphicTableSchema.builder(rootClass);
        }

        /**
         * Sets the schema for the root class.
         */
        public Builder<T> rootTableSchema(TableSchema<T> root) {
            delegate.rootTableSchema(root);
            return this;
        }

        /**
         * Sets the discriminator attribute name (defaults to {@code "type"}).
         */
        public Builder<T> discriminatorAttributeName(String name) {
            delegate.discriminatorAttributeName(name);
            return this;
        }

        /**
         * Adds a fully constructed static subtype.
         */
        public Builder<T> addStaticSubtype(StaticSubtype<? extends T> subtype) {
            delegate.addStaticSubtype(subtype);
            return this;
        }

        /**
         * Convenience for adding a subtype with its schema and discriminator value.
         *
         * @param subtypeClass       the Java class of the subtype
         * @param tableSchema        the schema for the subtype
         * @param discriminatorValue the discriminator value used in DynamoDB
         */
        public <S extends T> Builder<T> addSubtype(Class<S> subtypeClass,
                                                   TableSchema<S> tableSchema,
                                                   String discriminatorValue) {
            delegate.addStaticSubtype(
                StaticSubtype.builder(subtypeClass)
                             .tableSchema(tableSchema)
                             .name(discriminatorValue)
                             .build());
            return this;
        }

        /**
         * Builds the {@link PolymorphicTableSchema}.
         */
        public PolymorphicTableSchema<T> build() {
            return new PolymorphicTableSchema<>(this);
        }
    }
}
