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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.immutable.ImmutableInfo;
import software.amazon.awssdk.enhanced.dynamodb.internal.immutable.ImmutableIntrospector;
import software.amazon.awssdk.enhanced.dynamodb.internal.immutable.ImmutablePropertyDescriptor;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanAttributeGetter;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanAttributeSetter;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.ObjectConstructor;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.ObjectGetterMethod;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticGetterMethod;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.BeanTableSchemaAttributeTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;

/**
 * Implementation of {@link TableSchema} that builds a table schema based on properties and annotations of an immutable
 * class with an associated builder class. Example:
 * <pre>
 * <code>
 * {@literal @}DynamoDbImmutable(builder = Customer.Builder.class)
 * public class Customer {
 *     {@literal @}DynamoDbPartitionKey
 *     public String accountId() { ... }
 *
 *     {@literal @}DynamoDbSortKey
 *     public int subId() { ... }
 *
 *     // Defines a GSI (customers_by_name) with a partition key of 'name'
 *     {@literal @}DynamoDbSecondaryPartitionKey(indexNames = "customers_by_name")
 *     public String name() { ... }
 *
 *     // Defines an LSI (customers_by_date) with a sort key of 'createdDate' and also declares the
 *     // same attribute as a sort key for the GSI named 'customers_by_name'
 *     {@literal @}DynamoDbSecondarySortKey(indexNames = {"customers_by_date", "customers_by_name"})
 *     public Instant createdDate() { ... }
 *
 *     // Not required to be an inner-class, but builders often are
 *     public static final class Builder {
 *         public Builder accountId(String accountId) { ... };
 *         public Builder subId(int subId) { ... };
 *         public Builder name(String name) { ... };
 *         public Builder createdDate(Instant createdDate) { ... };
 *
 *         public Customer build() { ... };
 *     }
 * }
 * </pre>
 *
 * Creating an {@link ImmutableTableSchema} is a moderately expensive operation, and should be performed sparingly. This is
 * usually done once at application startup.
 *
 * @param <T> The type of object that this {@link TableSchema} maps to.
 */
@SdkPublicApi
public final class ImmutableTableSchema<T> extends WrappedTableSchema<T, StaticImmutableTableSchema<T, ?>> {
    private static final String ATTRIBUTE_TAG_STATIC_SUPPLIER_NAME = "attributeTagFor";

    private ImmutableTableSchema(StaticImmutableTableSchema<T, ?> wrappedTableSchema) {
        super(wrappedTableSchema);
    }

    /**
     * Scans an immutable class and builds an {@link ImmutableTableSchema} from it that can be used with the
     * {@link DynamoDbEnhancedClient}.
     *
     * Creating an {@link ImmutableTableSchema} is a moderately expensive operation, and should be performed sparingly. This is
     * usually done once at application startup.
     *
     * @param immutableClass The annotated immutable class to build the table schema from.
     * @param <T> The immutable class type.
     * @return An initialized {@link ImmutableTableSchema}
     */
    public static <T> ImmutableTableSchema<T> create(Class<T> immutableClass) {
        return create(immutableClass, new MetaTableSchemaCache());
    }

    private static <T> ImmutableTableSchema<T> create(Class<T> immutableClass,
                                                      MetaTableSchemaCache metaTableSchemaCache) {
        // Fetch or create a new reference to this yet-to-be-created TableSchema in the cache
        MetaTableSchema<T> metaTableSchema = metaTableSchemaCache.getOrCreate(immutableClass);

        ImmutableTableSchema<T> newTableSchema =
            new ImmutableTableSchema<>(createStaticImmutableTableSchema(immutableClass, metaTableSchemaCache));
        metaTableSchema.initialize(newTableSchema);
        return newTableSchema;
    }

    // Called when creating an immutable TableSchema recursively. Utilizes the MetaTableSchema cache to stop infinite
    // recursion
    static <T> TableSchema<T> recursiveCreate(Class<T> immutableClass, MetaTableSchemaCache metaTableSchemaCache) {
        Optional<MetaTableSchema<T>> metaTableSchema = metaTableSchemaCache.get(immutableClass);

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
        return create(immutableClass, metaTableSchemaCache);

    }

    private static <T> StaticImmutableTableSchema<T, ?> createStaticImmutableTableSchema(
            Class<T> immutableClass, MetaTableSchemaCache metaTableSchemaCache) {
        ImmutableInfo<T> immutableInfo = ImmutableIntrospector.getImmutableInfo(immutableClass);
        Class<?> builderClass = immutableInfo.builderClass();
        return createStaticImmutableTableSchema(immutableClass, builderClass, immutableInfo, metaTableSchemaCache);
    }

    private static <T, B> StaticImmutableTableSchema<T, B>  createStaticImmutableTableSchema(
        Class<T> immutableClass,
        Class<B> builderClass,
        ImmutableInfo<T> immutableInfo,
        MetaTableSchemaCache metaTableSchemaCache) {

        Supplier<B> newBuilderSupplier = newObjectSupplier(immutableInfo, builderClass);
        Function<B, T> buildFunction = ObjectGetterMethod.create(builderClass, immutableInfo.buildMethod());

        StaticImmutableTableSchema.Builder<T, B> builder =
            StaticImmutableTableSchema.builder(immutableClass, builderClass)
                                      .newItemBuilder(newBuilderSupplier, buildFunction);

        builder.attributeConverterProviders(
            createConverterProvidersFromAnnotation(immutableClass.getAnnotation(DynamoDbImmutable.class)));

        List<ImmutableAttribute<T, B, ?>> attributes = new ArrayList<>();

        immutableInfo.propertyDescriptors()
              .forEach(propertyDescriptor -> {
                  DynamoDbFlatten dynamoDbFlatten = getPropertyAnnotation(propertyDescriptor, DynamoDbFlatten.class);

                  if (dynamoDbFlatten != null) {
                      builder.flatten(TableSchema.fromClass(propertyDescriptor.getter().getReturnType()),
                                      getterForProperty(propertyDescriptor, immutableClass),
                                      setterForProperty(propertyDescriptor, builderClass));
                  } else {
                      ImmutableAttribute.Builder<T, B, ?> attributeBuilder =
                          immutableAttributeBuilder(propertyDescriptor,
                                                    immutableClass,
                                                    builderClass,
                                                    metaTableSchemaCache);

                      Optional<AttributeConverter> attributeConverter =
                              createAttributeConverterFromAnnotation(propertyDescriptor);
                      attributeConverter.ifPresent(attributeBuilder::attributeConverter);

                      addTagsToAttribute(attributeBuilder, propertyDescriptor);
                      attributes.add(attributeBuilder.build());
                  }
              });

        builder.attributes(attributes);

        return builder.build();
    }

    private static List<AttributeConverterProvider> createConverterProvidersFromAnnotation(
        DynamoDbImmutable dynamoDbImmutable) {

        Class<? extends AttributeConverterProvider>[] providerClasses = dynamoDbImmutable.converterProviders();

        return Arrays.stream(providerClasses)
                .map(c -> (AttributeConverterProvider) newObjectSupplierForClass(c).get())
                .collect(Collectors.toList());
    }

    private static <T, B> ImmutableAttribute.Builder<T, B, ?> immutableAttributeBuilder(
        ImmutablePropertyDescriptor propertyDescriptor,
        Class<T> immutableClass, Class<B> builderClass,
        MetaTableSchemaCache metaTableSchemaCache) {

        Type propertyType = propertyDescriptor.getter().getGenericReturnType();
        EnhancedType<?> propertyTypeToken = convertTypeToEnhancedType(propertyType, metaTableSchemaCache);
        return ImmutableAttribute.builder(immutableClass, builderClass, propertyTypeToken)
                                 .name(attributeNameForProperty(propertyDescriptor))
                                 .getter(getterForProperty(propertyDescriptor, immutableClass))
                                 .setter(setterForProperty(propertyDescriptor, builderClass));
    }

    /**
     * Converts a {@link Type} to an {@link EnhancedType}. Usually {@link EnhancedType#of} is capable of doing this all
     * by itself, but for the ImmutableTableSchema we want to detect if a parameterized class is being passed without a
     * converter that is actually another annotated class in which case we want to capture its schema and add it to the
     * EnhancedType. Unfortunately this means we have to duplicate some of the recursive Type parsing that
     * EnhancedClient otherwise does all by itself.
     */
    @SuppressWarnings("unchecked")
    private static EnhancedType<?> convertTypeToEnhancedType(Type type, MetaTableSchemaCache metaTableSchemaCache) {
        Class<?> clazz = null;

        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();

            if (List.class.equals(rawType)) {
                return EnhancedType.listOf(convertTypeToEnhancedType(parameterizedType.getActualTypeArguments()[0],
                                                                     metaTableSchemaCache));
            }

            if (Map.class.equals(rawType)) {
                return EnhancedType.mapOf(EnhancedType.of(parameterizedType.getActualTypeArguments()[0]),
                                          convertTypeToEnhancedType(parameterizedType.getActualTypeArguments()[1],
                                                                    metaTableSchemaCache));
            }

            if (rawType instanceof Class) {
                clazz = (Class<?>) rawType;
            }
        } else if (type instanceof Class) {
            clazz = (Class<?>) type;
        }

        if (clazz != null) {
            if (clazz.getAnnotation(DynamoDbImmutable.class) != null) {
                return EnhancedType.documentOf(
                    (Class<Object>) clazz,
                    (TableSchema<Object>) ImmutableTableSchema.recursiveCreate(clazz, metaTableSchemaCache));
            } else if (clazz.getAnnotation(DynamoDbBean.class) != null) {
                return EnhancedType.documentOf(
                    (Class<Object>) clazz,
                    (TableSchema<Object>) BeanTableSchema.recursiveCreate(clazz, metaTableSchemaCache));
            }
        }

        return EnhancedType.of(type);
    }

    private static Optional<AttributeConverter> createAttributeConverterFromAnnotation(
            ImmutablePropertyDescriptor propertyDescriptor) {
        DynamoDbConvertedBy attributeConverterBean =
                getPropertyAnnotation(propertyDescriptor, DynamoDbConvertedBy.class);
        Optional<Class<?>> optionalClass = Optional.ofNullable(attributeConverterBean)
                                                   .map(DynamoDbConvertedBy::value);
        return optionalClass.map(clazz -> (AttributeConverter) newObjectSupplierForClass(clazz).get());
    }

    /**
     * This method scans all the annotations on a property and looks for a meta-annotation of
     * {@link BeanTableSchemaAttributeTag}. If the meta-annotation is found, it attempts to create
     * an annotation tag based on a standard named static method
     * of the class that tag has been annotated with passing in the original property annotation as an argument.
     */
    private static void addTagsToAttribute(ImmutableAttribute.Builder<?, ?, ?> attributeBuilder,
                                           ImmutablePropertyDescriptor propertyDescriptor) {

        propertyAnnotations(propertyDescriptor).forEach(annotation -> {
            BeanTableSchemaAttributeTag beanTableSchemaAttributeTag =
                annotation.annotationType().getAnnotation(BeanTableSchemaAttributeTag.class);

            if (beanTableSchemaAttributeTag != null) {
                Class<?> tagClass = beanTableSchemaAttributeTag.value();

                Method tagMethod;
                try {
                    tagMethod = tagClass.getDeclaredMethod(ATTRIBUTE_TAG_STATIC_SUPPLIER_NAME,
                                                           annotation.annotationType());
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(
                        String.format("Could not find a static method named '%s' on class '%s' that returns " +
                                          "an AttributeTag for annotation '%s'", ATTRIBUTE_TAG_STATIC_SUPPLIER_NAME,
                                      tagClass, annotation.annotationType()), e);
                }

                if (!Modifier.isStatic(tagMethod.getModifiers())) {
                    throw new RuntimeException(
                        String.format("Could not find a static method named '%s' on class '%s' that returns " +
                                          "an AttributeTag for annotation '%s'", ATTRIBUTE_TAG_STATIC_SUPPLIER_NAME,
                                      tagClass, annotation.annotationType()));
                }

                StaticAttributeTag staticAttributeTag;
                try {
                    staticAttributeTag = (StaticAttributeTag) tagMethod.invoke(null, annotation);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(
                        String.format("Could not invoke method to create AttributeTag for annotation '%s' on class " +
                                          "'%s'.", annotation.annotationType(), tagClass), e);
                }

                attributeBuilder.addTag(staticAttributeTag);
            }
        });
    }

    private static <T, R> Supplier<R> newObjectSupplier(ImmutableInfo<T> immutableInfo, Class<R> builderClass) {
        if (immutableInfo.staticBuilderMethod().isPresent()) {
            return StaticGetterMethod.create(immutableInfo.staticBuilderMethod().get());
        }

        return newObjectSupplierForClass(builderClass);
    }

    private static <R> Supplier<R> newObjectSupplierForClass(Class<R> clazz) {
        try {
            return ObjectConstructor.create(clazz, clazz.getConstructor());
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                String.format("Builder class '%s' appears to have no default constructor thus cannot be used with " +
                                  "the ImmutableTableSchema", clazz), e);
        }
    }

    private static <T, R> Function<T, R> getterForProperty(ImmutablePropertyDescriptor propertyDescriptor,
                                                           Class<T> immutableClass) {
        Method readMethod = propertyDescriptor.getter();
        return BeanAttributeGetter.create(immutableClass, readMethod);
    }

    private static <T, R> BiConsumer<T, R> setterForProperty(ImmutablePropertyDescriptor propertyDescriptor,
                                                             Class<T> builderClass) {
        Method writeMethod = propertyDescriptor.setter();
        return BeanAttributeSetter.create(builderClass, writeMethod);
    }

    private static String attributeNameForProperty(ImmutablePropertyDescriptor propertyDescriptor) {
        DynamoDbAttribute dynamoDbAttribute = getPropertyAnnotation(propertyDescriptor, DynamoDbAttribute.class);
        if (dynamoDbAttribute != null) {
            return dynamoDbAttribute.value();
        }

        return propertyDescriptor.name();
    }

    private static <R extends Annotation> R getPropertyAnnotation(ImmutablePropertyDescriptor propertyDescriptor,
                                                                  Class<R> annotationType) {
        R getterAnnotation = propertyDescriptor.getter().getAnnotation(annotationType);
        R setterAnnotation = propertyDescriptor.setter().getAnnotation(annotationType);

        if (getterAnnotation != null) {
            return getterAnnotation;
        }

        return setterAnnotation;
    }

    private static List<? extends Annotation> propertyAnnotations(ImmutablePropertyDescriptor propertyDescriptor) {
        return Stream.concat(Arrays.stream(propertyDescriptor.getter().getAnnotations()),
                             Arrays.stream(propertyDescriptor.setter().getAnnotations()))
                     .collect(Collectors.toList());
    }
}

