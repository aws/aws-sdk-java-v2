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
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSupertype;

/**
 * Constructs {@link TableSchema} instances from annotated classes.
 */
@SdkPublicApi
public class TableSchemaFactory {
    private TableSchemaFactory() {
    }

    /**
     * Build a {@link TableSchema} by inspecting annotations on the given class.
     *
     * <p>Supported top-level annotations:
     * <ul>
     *   <li>{@link DynamoDbBean}</li>
     *   <li>{@link DynamoDbImmutable}</li>
     *   <li>{@link DynamoDbSupertype}</li>
     * </ul>
     *
     * @param annotatedClass the annotated class
     * @param <T>            item type
     * @return initialized {@link TableSchema}
     */
    public static <T> TableSchema<T> fromClass(Class<T> annotatedClass) {
        return fromClass(annotatedClass, MethodHandles.lookup(), new MetaTableSchemaCache());
    }

    static <T> TableSchema<T> fromMonomorphicClassWithoutUsingCache(Class<T> annotatedClass,
                                                                    MethodHandles.Lookup lookup,
                                                                    MetaTableSchemaCache metaTableSchemaCache) {
        if (isImmutableClass(annotatedClass)) {
            return ImmutableTableSchema.createWithoutUsingCache(annotatedClass, lookup, metaTableSchemaCache);
        }
        if (isBeanClass(annotatedClass)) {
            return BeanTableSchema.createWithoutUsingCache(annotatedClass, lookup, metaTableSchemaCache);
        }
        throw new IllegalArgumentException("Class does not appear to be a valid DynamoDb annotated class. " +
                                           "[class = \"" + annotatedClass + "\"]");
    }

    static <T> TableSchema<T> fromClass(Class<T> annotatedClass,
                                        MethodHandles.Lookup lookup,
                                        MetaTableSchemaCache metaTableSchemaCache) {
        Optional<MetaTableSchema<T>> metaTableSchema = metaTableSchemaCache.get(annotatedClass);

        if (metaTableSchema.isPresent()) {
            if (metaTableSchema.get().isInitialized()) {
                return metaTableSchema.get().concreteTableSchema();
            }
            return metaTableSchema.get();
        }

        if (isPolymorphicClass(annotatedClass)) {
            return buildPolymorphicFromAnnotations(annotatedClass, lookup, metaTableSchemaCache);
        }

        if (isImmutableClass(annotatedClass)) {
            ImmutableTableSchemaParams<T> immutableTableSchemaParams =
                ImmutableTableSchemaParams.builder(annotatedClass).lookup(lookup).build();
            return ImmutableTableSchema.create(immutableTableSchemaParams, metaTableSchemaCache);
        }

        if (isBeanClass(annotatedClass)) {
            BeanTableSchemaParams<T> beanTableSchemaParams =
                BeanTableSchemaParams.builder(annotatedClass).lookup(lookup).build();
            return BeanTableSchema.create(beanTableSchemaParams, metaTableSchemaCache);
        }

        throw new IllegalArgumentException("Class does not appear to be a valid DynamoDb annotated class. " +
                                           "[class = \"" + annotatedClass + "\"]");
    }

    // -----------------------------
    // Polymorphic builder
    // -----------------------------
    private static <T> TableSchema<T> buildPolymorphicFromAnnotations(Class<T> polymorphicClass,
                                                                      MethodHandles.Lookup lookup,
                                                                      MetaTableSchemaCache cache) {
        MetaTableSchema<T> meta = cache.getOrCreate(polymorphicClass);

        // Root must be a valid bean/immutable schema (not polymorphic)
        TableSchema<T> root = fromMonomorphicClassWithoutUsingCache(polymorphicClass, lookup, cache);

        DynamoDbSupertype supertypeAnnotation = polymorphicClass.getAnnotation(DynamoDbSupertype.class);
        validateSupertypeAnnotationUsage(polymorphicClass, supertypeAnnotation);

        PolymorphicTableSchema.Builder<T> builder =
            PolymorphicTableSchema.builder(polymorphicClass)
                                  .rootTableSchema(root)
                                  .discriminatorAttributeName(supertypeAnnotation.discriminatorAttributeName());

        Arrays.stream(supertypeAnnotation.value())
              .forEach(sub -> builder.addStaticSubtype(
                  resolvePolymorphicSubtype(polymorphicClass, lookup, sub, cache)));

        PolymorphicTableSchema<T> result = builder.build();
        meta.initialize(result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> StaticSubtype<? extends T> resolvePolymorphicSubtype(Class<T> rootClass,
                                                                            MethodHandles.Lookup lookup,
                                                                            DynamoDbSupertype.Subtype sub,
                                                                            MetaTableSchemaCache cache) {
        Class<?> subtypeClass = sub.subtypeClass();

        // VALIDATION: subtype must be assignable to root
        if (!rootClass.isAssignableFrom(subtypeClass)) {
            throw new IllegalArgumentException(
                "A subtype class [" + subtypeClass.getSimpleName()
                + "] listed in the @DynamoDbSupertype annotation is not extending the root class.");
        }

        Class<T> typed = (Class<T>) subtypeClass;

        // The subtype may itself be bean/immutable or polymorphic; reuse the factory path.
        TableSchema<T> subtypeSchema = fromClass(typed, lookup, cache);

        return StaticSubtype.builder(typed)
                            .tableSchema(subtypeSchema)
                            .name(sub.discriminatorValue())
                            .build();
    }

    private static <T> void validateSupertypeAnnotationUsage(Class<T> polymorphicClass,
                                                             DynamoDbSupertype supertypeAnnotation) {
        if (supertypeAnnotation == null) {
            throw new IllegalArgumentException("A DynamoDb polymorphic class [" + polymorphicClass.getSimpleName()
                                               + "] must be annotated with @DynamoDbSupertype");
        }
        if (supertypeAnnotation.value().length == 0) {
            throw new IllegalArgumentException("A DynamoDb polymorphic class [" + polymorphicClass.getSimpleName()
                                               + "] must declare at least one subtype in @DynamoDbSupertype");
        }
    }

    // -----------------------------
    // Annotation detection helpers
    // -----------------------------
    static boolean isDynamoDbAnnotatedClass(Class<?> clazz) {
        return isBeanClass(clazz) || isImmutableClass(clazz);
    }

    private static boolean isPolymorphicClass(Class<?> clazz) {
        return clazz.getAnnotation(DynamoDbSupertype.class) != null;
    }

    private static boolean isBeanClass(Class<?> clazz) {
        return clazz.getAnnotation(DynamoDbBean.class) != null;
    }

    private static boolean isImmutableClass(Class<?> clazz) {
        return clazz.getAnnotation(DynamoDbImmutable.class) != null;
    }
}
