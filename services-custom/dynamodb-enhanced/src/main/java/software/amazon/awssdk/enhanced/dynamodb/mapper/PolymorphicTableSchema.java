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

import java.util.Arrays;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Implementation of {@link TableSchema} that provides polymorphic mapping to and from various subtypes as denoted by
 * a single property of the object that represents the 'subtype name'. This implementation may only be used with a class
 * that is also a valid DynamoDb annotated class , and likewise every subtype class must also be a valid DynamoDb
 * annotated class.
 * <p>
 * Example:
 * <p><pre>
 * {@code
 * @DynamoDbBean
 * @DynamoDbSubtypes( {
 *   @Subtype(name = "CAT", subtypeClass = Cat.class),
 *   @Subtype(name = "DOG", subtypeClass = Dog.class) } )
 * public class Animal {
 *    @DynamoDbSubtypeName
 *    String getType() { ... }
 *
 *    ...
 * }
 * }
 * </pre>
 *
 * {@param T} The supertype class that is assignable from all the possible subtypes this schema maps.
 **/

@SdkPublicApi
public class PolymorphicTableSchema<T> extends WrappedTableSchema<T, StaticPolymorphicTableSchema<T>> {
    private final StaticPolymorphicTableSchema<T> staticPolymorphicTableSchema;

    private PolymorphicTableSchema(StaticPolymorphicTableSchema<T> staticPolymorphicTableSchema) {
        super(staticPolymorphicTableSchema);
        this.staticPolymorphicTableSchema = staticPolymorphicTableSchema;
    }

    /**
     * Scans a supertype class and builds a {@link PolymorphicTableSchema} from it that can be used with the
     * {@link DynamoDbEnhancedClient}.
     *
     * Creating a {@link PolymorphicTableSchema} is a moderately expensive operation, and should be performed sparingly.
     * This is usually done once at application startup.
     *
     * @param polymorphicClass The polymorphic supertype class to build the table schema from.
     * @param <T> The supertype class type.
     * @return An initialized {@link PolymorphicTableSchema}
     */
    public static <T> PolymorphicTableSchema<T> create(Class<T> polymorphicClass) {
        return create(polymorphicClass, new MetaTableSchemaCache());
    }

    @Override
    public TableSchema<? extends T> subtypeTableSchema(T itemContext) {
        return this.staticPolymorphicTableSchema.subtypeTableSchema(itemContext);
    }

    @Override
    public TableSchema<? extends T> subtypeTableSchema(Map<String, AttributeValue> itemContext) {
        return this.staticPolymorphicTableSchema.subtypeTableSchema(itemContext);
    }

    static <T> PolymorphicTableSchema<T> create(Class<T> polymorphicClass, MetaTableSchemaCache metaTableSchemaCache) {
        // Fetch or create a new reference to this yet-to-be-created TableSchema in the cache
        MetaTableSchema<T> metaTableSchema = metaTableSchemaCache.getOrCreate(polymorphicClass);

        // Get the monomorphic TableSchema form to wrap in the polymorphic TableSchema as the root
        TableSchema<T> rootTableSchema =
                TableSchemaFactory.fromMonomorphicClassWithoutUsingCache(polymorphicClass, metaTableSchemaCache);

        StaticPolymorphicTableSchema.Builder<T> staticBuilder =
                StaticPolymorphicTableSchema.builder(polymorphicClass).rootTableSchema(rootTableSchema);

        DynamoDbSubtypes dynamoDbSubtypes = polymorphicClass.getAnnotation(DynamoDbSubtypes.class);

        if (dynamoDbSubtypes == null) {
            throw new IllegalArgumentException("A DynamoDb polymorphic class [" + polymorphicClass.getSimpleName() +
                                                       "] must be annotated with @DynamoDbSubtypes");
        }

        Arrays.stream(dynamoDbSubtypes.value()).forEach(subtype -> {
            StaticSubtype<? extends T> staticSubtype = resolveSubtype(polymorphicClass, subtype, metaTableSchemaCache);
            staticBuilder.addStaticSubtype(staticSubtype);
        });

        PolymorphicTableSchema<T> newTableSchema = new PolymorphicTableSchema<>(staticBuilder.build());
        metaTableSchema.initialize(newTableSchema);
        return newTableSchema;
    }

    @SuppressWarnings("unchecked")
    private static <T> StaticSubtype<? extends T> resolveSubtype(Class<T> rootClass,
                                                                 DynamoDbSubtypes.Subtype subtype,
                                                                 MetaTableSchemaCache metaTableSchemaCache) {
        Class<?> subtypeClass = subtype.subtypeClass();

        if (!rootClass.isAssignableFrom(subtypeClass)) {
            throw new IllegalArgumentException("A subtype class [" + subtypeClass.getSimpleName() + "] listed in the " +
                                                       "@DynamoDbSubtypes annotation is not extending the root class.");
        }

        // This should be safe as we have explicitly verified the class is assignable
        Class<? extends T> typedSubtypeClass = (Class<? extends T>) subtypeClass;

        return resolveNamedSubType(typedSubtypeClass, subtype.name(), metaTableSchemaCache);
    }

    private static <T> StaticSubtype<T> resolveNamedSubType(Class<T> subtypeClass,
                                                            String[] names,
                                                            MetaTableSchemaCache metaTableSchemaCache) {
        return StaticSubtype.builder(subtypeClass)
                            .tableSchema(TableSchemaFactory.fromClass(subtypeClass, metaTableSchemaCache))
                            .names(names)
                            .build();
    }
}
