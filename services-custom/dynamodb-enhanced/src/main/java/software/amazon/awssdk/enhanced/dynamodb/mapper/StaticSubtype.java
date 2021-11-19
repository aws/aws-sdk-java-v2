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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.utils.Validate;

/**
 * A structure that represents a mappable subtype to be used when constructing a {@link StaticPolymorphicTableSchema}.
 * @param <T> the subtype
 */
@SdkPublicApi
public class StaticSubtype<T> {
    private final TableSchema<T> tableSchema;
    private final List<String> names;

    private StaticSubtype(Builder<T> builder) {
        this.tableSchema = Validate.notNull(builder.tableSchema, "A subtype must have a tableSchema associated with " +
                "it. [subtypeClass = \"%s\"]", builder.subtypeClass.getName());
        this.names = Collections.unmodifiableList(Validate.notEmpty(builder.names,
                                       "A subtype must have one or more names associated with it. [subtypeClass = \"" +
                                               builder.subtypeClass.getName() + "\"]"));

        if (this.tableSchema.isAbstract()) {
            throw new IllegalArgumentException(
                    "A subtype may not be constructed with an abstract TableSchema. An abstract TableSchema is a " +
                            "TableSchema that does not know how to construct new objects of its type. " +
                            "[subtypeClass = \"" + builder.subtypeClass.getName() + "\"]");
        }
    }

    /**
     * Returns the {@link TableSchema} that can be used to map objects of this subtype.
     */
    public TableSchema<T> tableSchema() {
        return this.tableSchema;
    }

    /**
     * Returns the list of names that would designate an object with a matching subtype name to be of this particular
     * subtype.
     */
    public List<String> names() {
        return this.names;
    }

    /**
     * Create a newly initialized builder for a {@link StaticSubtype}.
     * @param subtypeClass The subtype class.
     * @param <T> The subtype.
     */
    public static <T> Builder<T> builder(Class<T> subtypeClass) {
        return new Builder<>(subtypeClass);
    }

    /**
     * Builder class for a {@link StaticSubtype}.
     * @param <T> the subtype.
     */
    public static class Builder<T> {
        private final Class<T> subtypeClass;
        private TableSchema<T> tableSchema;
        private List<String> names;

        private Builder(Class<T> subtypeClass) {
            this.subtypeClass = subtypeClass;
        }

        /**
         * Sets the {@link TableSchema} that can be used to map objects of this subtype.
         */
        public Builder<T> tableSchema(TableSchema<T> tableSchema) {
            this.tableSchema = tableSchema;
            return this;
        }

        /**
         * Sets the list of names that would designate an object with a matching subtype name to be of this particular
         * subtype.
         */
        public Builder<T> names(List<String> names) {
            this.names = new ArrayList<>(names);
            return this;
        }

        /**
         * Sets the list of names that would designate an object with a matching subtype name to be of this particular
         * subtype.
         */
        public Builder<T> names(String ...names) {
            this.names = Arrays.asList(names);
            return this;
        }

        /**
         * Builds a {@link StaticSubtype} based on the properties of this builder.
         */
        public StaticSubtype<T> build() {
            return new StaticSubtype<>(this);
        }
    }
}
