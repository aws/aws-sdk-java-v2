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

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypes;

/**
 * This class is responsible for constructing {@link TableSchema} objects from annotated classes.
 */
@SdkPublicApi
public class TableSchemaFactory {
    private TableSchemaFactory() {
    }

    /**
     * Scans a class that has been annotated with DynamoDb enhanced client annotations and then returns an appropriate
     * {@link TableSchema} implementation that can map records to and from items of that class. Currently supported
     * top level annotations (see documentation on those classes for more information on how to use them):
     * <p>
     * {@link software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean}<br>
     * {@link software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable}
     *
     * This is a moderately expensive operation, and should be performed sparingly. This is usually done once at
     * application startup.
     *
     * @param annotatedClass A class that has been annotated with DynamoDb enhanced client annotations.
     * @param <T> The type of the item this {@link TableSchema} will map records to.
     * @return An initialized {@link TableSchema}
     */
    public static <T> TableSchema<T> fromClass(Class<T> annotatedClass) {
        return fromClass(annotatedClass, new MetaTableSchemaCache());
    }

    static <T> TableSchema<T> fromMonomorphicClassWithoutUsingCache(Class<T> annotatedClass,
                                                                    MetaTableSchemaCache metaTableSchemaCache) {
        if (isImmutableClass(annotatedClass)) {
            return ImmutableTableSchema.createWithoutUsingCache(annotatedClass, metaTableSchemaCache);
        }

        if (isBeanClass(annotatedClass)) {
            return BeanTableSchema.createWithoutUsingCache(annotatedClass, metaTableSchemaCache);
        }

        throw new IllegalArgumentException("Class does not appear to be a valid DynamoDb annotated class. [class = " +
                                                   "\"" + annotatedClass + "\"]");
    }

    static <T> TableSchema<T> fromClass(Class<T> annotatedClass,
                                        MetaTableSchemaCache metaTableSchemaCache) {
        Optional<MetaTableSchema<T>> metaTableSchema = metaTableSchemaCache.get(annotatedClass);

        // If we get a cache hit...
        if (metaTableSchema.isPresent()) {
            // Either: use the cached concrete TableSchema if we have one
            if (metaTableSchema.get().isInitialized()) {
                return metaTableSchema.get().concreteTableSchema();
            }

            // Or: return the uninitialized MetaTableSchema as this must be a recursive reference and it will be
            // initialized later as the chain completes
            return metaTableSchema.get();
        }

        // Otherwise: cache doesn't know about this class; create a new one from scratch
        if (isPolymorphicClass(annotatedClass)) {
            return PolymorphicTableSchema.create(annotatedClass, metaTableSchemaCache);
        }

        if (isImmutableClass(annotatedClass)) {
            return ImmutableTableSchema.create(annotatedClass, metaTableSchemaCache);
        }

        if (isBeanClass(annotatedClass)) {
            return BeanTableSchema.create(annotatedClass, metaTableSchemaCache);
        }

        throw new IllegalArgumentException("Class does not appear to be a valid DynamoDb annotated class. [class = " +
                                                   "\"" + annotatedClass + "\"]");
    }

    static boolean isDynamoDbAnnotatedClass(Class<?> clazz) {
        return isBeanClass(clazz) || isImmutableClass(clazz);
    }

    private static boolean isPolymorphicClass(Class<?> clazz) {
        return clazz.getAnnotation(DynamoDbSubtypes.class) != null;
    }

    private static boolean isBeanClass(Class<?> clazz) {
        return clazz.getAnnotation(DynamoDbBean.class) != null;
    }

    private static boolean isImmutableClass(Class<?> clazz) {
        return clazz.getAnnotation(DynamoDbImmutable.class) != null;
    }
}
