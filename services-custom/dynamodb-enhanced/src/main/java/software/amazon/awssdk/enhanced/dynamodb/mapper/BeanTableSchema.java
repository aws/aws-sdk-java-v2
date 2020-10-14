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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
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
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanAttributeGetter;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanAttributeSetter;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.ObjectConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.BeanTableSchemaAttributeTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;

/**
 * Implementation of {@link TableSchema} that builds a table schema based on properties and annotations of a bean
 * class. Example:
 * <pre>
 * <code>
 * {@literal @}DynamoDbBean
 * public class Customer {
 *     private String accountId;
 *     private int subId;            // primitive types are supported
 *     private String name;
 *     private Instant createdDate;
 *
 *     {@literal @}DynamoDbPartitionKey
 *     public String getAccountId() { return this.accountId; }
 *     public void setAccountId(String accountId) { this.accountId = accountId; }
 *
 *     {@literal @}DynamoDbSortKey
 *     public int getSubId() { return this.subId; }
 *     public void setSubId(int subId) { this.subId = subId; }
 *
 *     // Defines a GSI (customers_by_name) with a partition key of 'name'
 *     {@literal @}DynamoDbSecondaryPartitionKey(indexNames = "customers_by_name")
 *     public String getName() { return this.name; }
 *     public void setName(String name) { this.name = name; }
 *
 *     // Defines an LSI (customers_by_date) with a sort key of 'createdDate' and also declares the
 *     // same attribute as a sort key for the GSI named 'customers_by_name'
 *     {@literal @}DynamoDbSecondarySortKey(indexNames = {"customers_by_date", "customers_by_name"})
 *     public Instant getCreatedDate() { return this.createdDate; }
 *     public void setCreatedDate(Instant createdDate) { this.createdDate = createdDate; }
 * }
 *
 * </pre>
 *
 * Creating an {@link BeanTableSchema} is a moderately expensive operation, and should be performed sparingly. This is
 * usually done once at application startup.
 *
 * @param <T> The type of object that this {@link TableSchema} maps to.
 */
@SdkPublicApi
public final class BeanTableSchema<T> extends WrappedTableSchema<T, StaticTableSchema<T>> {
    private static final String ATTRIBUTE_TAG_STATIC_SUPPLIER_NAME = "attributeTagFor";

    private BeanTableSchema(StaticTableSchema<T> staticTableSchema) {
        super(staticTableSchema);
    }

    /**
     * Scans a bean class and builds a {@link BeanTableSchema} from it that can be used with the
     * {@link DynamoDbEnhancedClient}.
     *
     * Creating an {@link BeanTableSchema} is a moderately expensive operation, and should be performed sparingly. This is
     * usually done once at application startup.
     *
     * @param beanClass The bean class to build the table schema from.
     * @param <T> The bean class type.
     * @return An initialized {@link BeanTableSchema}
     */
    public static <T> BeanTableSchema<T> create(Class<T> beanClass) {
        return create(beanClass, new MetaTableSchemaCache());
    }

    private static <T> BeanTableSchema<T> create(Class<T> beanClass, MetaTableSchemaCache metaTableSchemaCache) {
        // Fetch or create a new reference to this yet-to-be-created TableSchema in the cache
        MetaTableSchema<T> metaTableSchema = metaTableSchemaCache.getOrCreate(beanClass);

        BeanTableSchema<T> newTableSchema =
            new BeanTableSchema<>(createStaticTableSchema(beanClass, metaTableSchemaCache));
        metaTableSchema.initialize(newTableSchema);
        return newTableSchema;
    }

    // Called when creating an immutable TableSchema recursively. Utilizes the MetaTableSchema cache to stop infinite
    // recursion
    static <T> TableSchema<T> recursiveCreate(Class<T> beanClass, MetaTableSchemaCache metaTableSchemaCache) {
        Optional<MetaTableSchema<T>> metaTableSchema = metaTableSchemaCache.get(beanClass);

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
        return create(beanClass);

    }

    private static <T> StaticTableSchema<T> createStaticTableSchema(Class<T> beanClass,
                                                                    MetaTableSchemaCache metaTableSchemaCache) {
        DynamoDbBean dynamoDbBean = beanClass.getAnnotation(DynamoDbBean.class);

        if (dynamoDbBean == null) {
            throw new IllegalArgumentException("A DynamoDb bean class must be annotated with @DynamoDbBean");
        }

        BeanInfo beanInfo;

        try {
            beanInfo = Introspector.getBeanInfo(beanClass);
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException(e);
        }

        Supplier<T> newObjectSupplier = newObjectSupplierForClass(beanClass);

        StaticTableSchema.Builder<T> builder = StaticTableSchema.builder(beanClass)
                                                                .newItemSupplier(newObjectSupplier);

        builder.attributeConverterProviders(createConverterProvidersFromAnnotation(dynamoDbBean));

        List<StaticAttribute<T, ?>> attributes = new ArrayList<>();

        Arrays.stream(beanInfo.getPropertyDescriptors())
              .filter(BeanTableSchema::isMappableProperty)
              .forEach(propertyDescriptor -> {
                  DynamoDbFlatten dynamoDbFlatten = getPropertyAnnotation(propertyDescriptor, DynamoDbFlatten.class);

                  if (dynamoDbFlatten != null) {
                      builder.flatten(TableSchema.fromClass(propertyDescriptor.getReadMethod().getReturnType()),
                                      getterForProperty(propertyDescriptor, beanClass),
                                      setterForProperty(propertyDescriptor, beanClass));
                  } else {
                      StaticAttribute.Builder<T, ?> attributeBuilder =
                          staticAttributeBuilder(propertyDescriptor, beanClass, metaTableSchemaCache);

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

    private static List<AttributeConverterProvider> createConverterProvidersFromAnnotation(DynamoDbBean dynamoDbBean) {
        Class<? extends AttributeConverterProvider>[] providerClasses = dynamoDbBean.converterProviders();

        return Arrays.stream(providerClasses)
                .map(c -> (AttributeConverterProvider) newObjectSupplierForClass(c).get())
                .collect(Collectors.toList());
    }

    private static <T> StaticAttribute.Builder<T, ?> staticAttributeBuilder(PropertyDescriptor propertyDescriptor,
                                                                            Class<T> beanClass,
                                                                            MetaTableSchemaCache metaTableSchemaCache) {

        Type propertyType = propertyDescriptor.getReadMethod().getGenericReturnType();
        EnhancedType<?> propertyTypeToken = convertTypeToEnhancedType(propertyType, metaTableSchemaCache);
        return StaticAttribute.builder(beanClass, propertyTypeToken)
                              .name(attributeNameForProperty(propertyDescriptor))
                              .getter(getterForProperty(propertyDescriptor, beanClass))
                              .setter(setterForProperty(propertyDescriptor, beanClass));
    }

    /**
     * Converts a {@link Type} to an {@link EnhancedType}. Usually {@link EnhancedType#of} is capable of doing this all
     * by itself, but for the BeanTableSchema we want to detect if a parameterized class is being passed without a
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
            PropertyDescriptor propertyDescriptor) {
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
    private static void addTagsToAttribute(StaticAttribute.Builder<?, ?> attributeBuilder,
                                           PropertyDescriptor propertyDescriptor) {

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

    private static <R> Supplier<R> newObjectSupplierForClass(Class<R> clazz) {
        try {
            return ObjectConstructor.create(clazz, clazz.getConstructor());
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                String.format("Class '%s' appears to have no default constructor thus cannot be used with the " +
                                  "BeanTableSchema", clazz), e);
        }
    }

    private static <T, R> Function<T, R> getterForProperty(PropertyDescriptor propertyDescriptor, Class<T> beanClass) {
        Method readMethod = propertyDescriptor.getReadMethod();
        return BeanAttributeGetter.create(beanClass, readMethod);
    }

    private static <T, R> BiConsumer<T, R> setterForProperty(PropertyDescriptor propertyDescriptor,
                                                             Class<T> beanClass) {
        Method writeMethod = propertyDescriptor.getWriteMethod();
        return BeanAttributeSetter.create(beanClass, writeMethod);
    }

    private static String attributeNameForProperty(PropertyDescriptor propertyDescriptor) {
        DynamoDbAttribute dynamoDbAttribute = getPropertyAnnotation(propertyDescriptor, DynamoDbAttribute.class);
        if (dynamoDbAttribute != null) {
            return dynamoDbAttribute.value();
        }

        return propertyDescriptor.getName();
    }

    private static boolean isMappableProperty(PropertyDescriptor propertyDescriptor) {
        return propertyDescriptor.getReadMethod() != null
            && propertyDescriptor.getWriteMethod() != null
            && getPropertyAnnotation(propertyDescriptor, DynamoDbIgnore.class) == null;
    }

    private static <R extends Annotation> R getPropertyAnnotation(PropertyDescriptor propertyDescriptor,
                                                                  Class<R> annotationType) {
        R getterAnnotation = propertyDescriptor.getReadMethod().getAnnotation(annotationType);
        R setterAnnotation = propertyDescriptor.getWriteMethod().getAnnotation(annotationType);

        if (getterAnnotation != null) {
            return getterAnnotation;
        }

        return setterAnnotation;
    }

    private static List<? extends Annotation> propertyAnnotations(PropertyDescriptor propertyDescriptor) {
        return Stream.concat(Arrays.stream(propertyDescriptor.getReadMethod().getAnnotations()),
                             Arrays.stream(propertyDescriptor.getWriteMethod().getAnnotations()))
                     .collect(Collectors.toList());
    }
}

