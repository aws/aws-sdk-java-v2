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
import java.util.Arrays;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSupertype;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.StringUtils;

/**
 * A polymorphic wrapper that reads the {@link DynamoDbSupertype#discriminatorAttributeName()} and wires up each declared
 * subtype.
 */
@SdkPublicApi
public final class PolymorphicTableSchema<T> extends WrappedTableSchema<T, StaticPolymorphicTableSchema<T>> {
    private final StaticPolymorphicTableSchema<T> staticPolymorphicTableSchema;

    private PolymorphicTableSchema(StaticPolymorphicTableSchema<T> staticPolymorphicTableSchema) {
        super(staticPolymorphicTableSchema);
        this.staticPolymorphicTableSchema = staticPolymorphicTableSchema;
    }

    public static <T> PolymorphicTableSchema<T> create(Class<T> polymorphicClass, MethodHandles.Lookup lookup) {
        return create(polymorphicClass, lookup, new MetaTableSchemaCache());
    }

    static <T> PolymorphicTableSchema<T> create(Class<T> polymorphicClass,
                                                MethodHandles.Lookup lookup,
                                                MetaTableSchemaCache cache) {

        MetaTableSchema<T> metaTableSchema = cache.getOrCreate(polymorphicClass);
        TableSchema<T> root = TableSchemaFactory.fromMonomorphicClassWithoutUsingCache(polymorphicClass, lookup, cache);

        DynamoDbSupertype dynamoDbSupertype = polymorphicClass.getAnnotation(DynamoDbSupertype.class);
        if (dynamoDbSupertype == null) {
            throw new IllegalArgumentException("A DynamoDb polymorphic class [" + polymorphicClass.getSimpleName()
                                               + "] must be annotated with @DynamoDbSupertype");
        }

        StaticPolymorphicTableSchema.Builder<T> staticBuilder =
            StaticPolymorphicTableSchema.builder(polymorphicClass)
                                        .rootTableSchema(root)
                                        .discriminatorAttributeName(dynamoDbSupertype.discriminatorAttributeName());

        Arrays.stream(dynamoDbSupertype.value())
              .forEach(sub -> staticBuilder.addStaticSubtype(resolveSubtype(polymorphicClass, lookup, sub, cache)));

        PolymorphicTableSchema<T> result = new PolymorphicTableSchema<>(staticBuilder.build());
        metaTableSchema.initialize(result);
        return result;
    }

    private static <T> StaticSubtype<? extends T> resolveSubtype(Class<T> rootClass,
                                                                 MethodHandles.Lookup lookup,
                                                                 DynamoDbSupertype.Subtype subtype,
                                                                 MetaTableSchemaCache cache) {
        Class<?> subtypeClass = subtype.subtypeClass();
        if (!rootClass.isAssignableFrom(subtypeClass)) {
            throw new IllegalArgumentException("A subtype class [" + subtypeClass.getSimpleName() + "] listed in the "
                                               + "@DynamoDbSupertype annotation is not extending the root class.");
        }
        Class<T> typed = (Class<T>) subtypeClass;

        //if the discriminator values is provided, it will be used; if not, we'll use the name of the class
        String subtypeName = StringUtils.isEmpty(subtype.discriminatorValue())
                             ? subtype.subtypeClass().getSimpleName()
                             : subtype.discriminatorValue();

        return StaticSubtype.builder(typed)
                            .tableSchema(TableSchemaFactory.fromClass(typed, lookup, cache))
                            .name(subtypeName)
                            .build();
    }

    @Override
    public TableSchema<? extends T> subtypeTableSchema(T itemContext) {
        return staticPolymorphicTableSchema.subtypeTableSchema(itemContext);
    }

    @Override
    public TableSchema<? extends T> subtypeTableSchema(Map<String, AttributeValue> itemContext) {
        return staticPolymorphicTableSchema.subtypeTableSchema(itemContext);
    }
}
