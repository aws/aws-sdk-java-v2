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

import static software.amazon.awssdk.enhanced.dynamodb.internal.DynamoDbEnhancedLogger.BEAN_LOGGER;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.Transient;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedTypeDocumentConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeConfiguration;
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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPreserveEmptyObject;
import software.amazon.awssdk.utils.StringUtils;

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
 * If this table schema is not behaving as you expect, enable debug logging for 'software.amazon.awssdk.enhanced.dynamodb.beans'.
 *
 * @param <T> The type of object that this {@link TableSchema} maps to.
 */
@SdkPublicApi
@ThreadSafe
public final class BeanTableSchema<T> extends WrappedTableSchema<T, StaticTableSchema<T>> {
    private static final Map<Class<?>, BeanTableSchema<?>> BEAN_TABLE_SCHEMA_CACHE =
        Collections.synchronizedMap(new WeakHashMap<>());
    private static final String ATTRIBUTE_TAG_STATIC_SUPPLIER_NAME = "attributeTagFor";

    private BeanTableSchema(StaticTableSchema<T> staticTableSchema) {
        super(staticTableSchema);
    }

    /**
     * Scans a bean class and builds a {@link BeanTableSchema} from it that can be used with the
     * {@link DynamoDbEnhancedClient}.
     *
     * <p>
     * It's recommended to only create a {@link BeanTableSchema} once for a single bean class, usually at application start up,
     * because it's a moderately expensive operation.
     * <p>
     * If you are running your application in an environment where {@code beanClass} and the SDK are loaded by different
     * classloaders, you should consider using the {@link #create(BeanTableSchemaParams)} overload instead, and provided a
     * custom {@link MethodHandles.Lookup} object to ensure that the SDK has access to the {@code beanClass} and its properties
     * at runtime.
     *
     * @param beanClass The bean class to build the table schema from.
     * @param <T> The bean class type.
     * @return An initialized {@link BeanTableSchema}
     */
    @SuppressWarnings("unchecked")
    public static <T> BeanTableSchema<T> create(Class<T> beanClass) {
        BeanTableSchemaParams<T> params = BeanTableSchemaParams.builder(beanClass).build();
        return create(params);
    }

    /**
     * Scans a bean class and builds a {@link BeanTableSchema} from it that can be used with the
     * {@link DynamoDbEnhancedClient}.
     *
     * <p>
     * It's recommended to only create a {@link BeanTableSchema} once for a single bean class, usually at application start up,
     * because it's a moderately expensive operation.
     * <p>
     * Generally, this method should be preferred over {@link #create(Class)} because it allows you to use a custom
     * {@link MethodHandles.Lookup} instance, which is necessary when your application runs in an environment where your
     * application code and dependencies like the AWS SDK for Java are loaded by different classloaders.
     *
     * @param params The parameters object.
     * @param <T> The bean class type.
     * @return An initialized {@link BeanTableSchema}
     */
    @SuppressWarnings("unchecked")
    public static <T> BeanTableSchema<T> create(BeanTableSchemaParams<T> params) {
        return (BeanTableSchema<T>) BEAN_TABLE_SCHEMA_CACHE.computeIfAbsent(params.beanClass(),
                                                                            clz -> create(params,
                                                                                          new MetaTableSchemaCache()));
    }

    private static <T> BeanTableSchema<T> create(BeanTableSchemaParams<T> params, MetaTableSchemaCache metaTableSchemaCache) {
        Class<T> beanClass = params.beanClass();
        debugLog(beanClass, () -> "Creating bean schema");
        // Fetch or create a new reference to this yet-to-be-created TableSchema in the cache
        MetaTableSchema<T> metaTableSchema = metaTableSchemaCache.getOrCreate(beanClass);

        BeanTableSchema<T> newTableSchema =
            new BeanTableSchema<>(createStaticTableSchema(params.beanClass(), params.lookup(), metaTableSchemaCache));
        metaTableSchema.initialize(newTableSchema);
        return newTableSchema;
    }

    // Called when creating an immutable TableSchema recursively. Utilizes the MetaTableSchema cache to stop infinite
    // recursion
    static <T> TableSchema<T> recursiveCreate(Class<T> beanClass, MethodHandles.Lookup lookup,
                                              MetaTableSchemaCache metaTableSchemaCache) {
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
        return create(BeanTableSchemaParams.builder(beanClass).lookup(lookup).build());

    }

    private static <T> StaticTableSchema<T> createStaticTableSchema(Class<T> beanClass,
                                                                    MethodHandles.Lookup lookup,
                                                                    MetaTableSchemaCache metaTableSchemaCache) {

        DynamoDbBean dynamoDbBean = beanClass.getAnnotation(DynamoDbBean.class);

        if (dynamoDbBean == null) {
            throw new IllegalArgumentException("A DynamoDb bean class must be annotated with @DynamoDbBean, but " +
                                               beanClass.getTypeName() + " was not.");
        }

        BeanInfo beanInfo;

        try {
            beanInfo = Introspector.getBeanInfo(beanClass);
            enhanceDescriptorsWithFluentSetters(beanClass, beanInfo);
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException(e);
        }

        Supplier<T> newObjectSupplier = newObjectSupplierForClass(beanClass, lookup);

        StaticTableSchema.Builder<T> builder = StaticTableSchema.builder(beanClass)
                                                                .newItemSupplier(newObjectSupplier);

        builder.attributeConverterProviders(createConverterProvidersFromAnnotation(beanClass, lookup, dynamoDbBean));

        List<StaticAttribute<T, ?>> attributes = new ArrayList<>();

        Arrays.stream(beanInfo.getPropertyDescriptors())
              .filter(p -> isMappableProperty(beanClass, p))
              .forEach(propertyDescriptor -> {
                  DynamoDbFlatten dynamoDbFlatten = getPropertyAnnotation(propertyDescriptor, DynamoDbFlatten.class);

                  if (dynamoDbFlatten != null) {
                      builder.flatten(TableSchema.fromClass(propertyDescriptor.getReadMethod().getReturnType()),
                                      getterForProperty(propertyDescriptor, beanClass, lookup),
                                      setterForProperty(propertyDescriptor, beanClass, lookup));
                  } else {
                      AttributeConfiguration attributeConfiguration =
                          resolveAttributeConfiguration(propertyDescriptor);

                      StaticAttribute.Builder<T, ?> attributeBuilder =
                          staticAttributeBuilder(propertyDescriptor, beanClass, lookup, metaTableSchemaCache,
                                                 attributeConfiguration);

                      Optional<AttributeConverter> attributeConverter =
                              createAttributeConverterFromAnnotation(propertyDescriptor, lookup);
                      attributeConverter.ifPresent(attributeBuilder::attributeConverter);

                      addTagsToAttribute(attributeBuilder, propertyDescriptor);
                      attributes.add(attributeBuilder.build());
                  }
              });

        builder.attributes(attributes);

        return builder.build();
    }

    // Enhance beanInfo descriptors with fluent setter when the default set method is absent
    private static <T> void enhanceDescriptorsWithFluentSetters(Class<T> beanClass, BeanInfo beanInfo) {
        Arrays.stream(beanInfo.getPropertyDescriptors())
              .filter(descriptor -> descriptor.getWriteMethod() == null)
              .forEach(descriptor -> findFluentSetter(beanClass, descriptor.getName())
                  .ifPresent(method -> {
                      try {
                          descriptor.setWriteMethod(method);
                      } catch (IntrospectionException e) {
                          throw new RuntimeException("Failed to set write method for " + descriptor.getName(), e);
                      }
                  }));
    }

    private static Optional<Method> findFluentSetter(Class<?> beanClass, String propertyName) {
        String setterName = "set" + StringUtils.capitalize(propertyName);

        return Arrays.stream(beanClass.getMethods())
                     .filter(m -> m.getName().equals(setterName)
                                  && m.getParameterCount() == 1
                                  && m.getReturnType().equals(beanClass))
                     .findFirst();
    }

    private static AttributeConfiguration resolveAttributeConfiguration(PropertyDescriptor propertyDescriptor) {
        boolean shouldPreserveEmptyObject = getPropertyAnnotation(propertyDescriptor,
                                                                  DynamoDbPreserveEmptyObject.class) != null;

        boolean shouldIgnoreNulls = getPropertyAnnotation(propertyDescriptor,
                                                          DynamoDbIgnoreNulls.class) != null;

        return AttributeConfiguration.builder()
                                     .preserveEmptyObject(shouldPreserveEmptyObject)
                                     .ignoreNulls(shouldIgnoreNulls)
                                     .build();
    }

    private static List<AttributeConverterProvider> createConverterProvidersFromAnnotation(Class<?> beanClass,
                                                                                           MethodHandles.Lookup lookup,
                                                                                           DynamoDbBean dynamoDbBean) {
        Class<? extends AttributeConverterProvider>[] providerClasses = dynamoDbBean.converterProviders();

        return Arrays.stream(providerClasses)
                     .peek(c -> debugLog(beanClass, () -> "Adding Converter: " + c.getTypeName()))
                     .map(c -> (AttributeConverterProvider) newObjectSupplierForClass(c, lookup).get())
                     .collect(Collectors.toList());
    }

    private static <T> StaticAttribute.Builder<T, ?> staticAttributeBuilder(PropertyDescriptor propertyDescriptor,
                                                                            Class<T> beanClass,
                                                                            MethodHandles.Lookup lookup,
                                                                            MetaTableSchemaCache metaTableSchemaCache,
                                                                            AttributeConfiguration attributeConfiguration) {

        Type propertyType = propertyDescriptor.getReadMethod().getGenericReturnType();
        EnhancedType<?> propertyTypeToken = convertTypeToEnhancedType(propertyType, lookup, metaTableSchemaCache,
                                                                      attributeConfiguration);
        return StaticAttribute.builder(beanClass, propertyTypeToken)
                              .name(attributeNameForProperty(propertyDescriptor))
                              .getter(getterForProperty(propertyDescriptor, beanClass, lookup))
                              .setter(setterForProperty(propertyDescriptor, beanClass, lookup));
    }

    /**
     * Converts a {@link Type} to an {@link EnhancedType}. Usually {@link EnhancedType#of} is capable of doing this all
     * by itself, but for the BeanTableSchema we want to detect if a parameterized class is being passed without a
     * converter that is actually another annotated class in which case we want to capture its schema and add it to the
     * EnhancedType. Unfortunately this means we have to duplicate some of the recursive Type parsing that
     * EnhancedClient otherwise does all by itself.
     */
    @SuppressWarnings("unchecked")
    private static EnhancedType<?> convertTypeToEnhancedType(Type type,
                                                             MethodHandles.Lookup lookup,
                                                             MetaTableSchemaCache metaTableSchemaCache,
                                                             AttributeConfiguration attributeConfiguration) {
        Class<?> clazz = null;

        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();

            if (List.class.equals(rawType)) {
                EnhancedType<?> enhancedType = convertTypeToEnhancedType(parameterizedType.getActualTypeArguments()[0], lookup,
                                                                         metaTableSchemaCache, attributeConfiguration);
                return EnhancedType.listOf(enhancedType);
            }

            if (Map.class.equals(rawType)) {
                EnhancedType<?> enhancedType = convertTypeToEnhancedType(parameterizedType.getActualTypeArguments()[1], lookup,
                                                                         metaTableSchemaCache, attributeConfiguration);
                return EnhancedType.mapOf(EnhancedType.of(parameterizedType.getActualTypeArguments()[0]),
                                          enhancedType);
            }

            if (rawType instanceof Class) {
                clazz = (Class<?>) rawType;
            }
        } else if (type instanceof Class) {
            clazz = (Class<?>) type;
        }

        if (clazz != null) {
            Consumer<EnhancedTypeDocumentConfiguration.Builder> attrConfiguration =
                b -> b.preserveEmptyObject(attributeConfiguration.preserveEmptyObject())
                      .ignoreNulls(attributeConfiguration.ignoreNulls());

            if (clazz.getAnnotation(DynamoDbImmutable.class) != null) {
                return EnhancedType.documentOf(
                    (Class<Object>) clazz,
                    (TableSchema<Object>) ImmutableTableSchema.recursiveCreate(clazz, lookup, metaTableSchemaCache),
                    attrConfiguration);
            } else if (clazz.getAnnotation(DynamoDbBean.class) != null) {
                return EnhancedType.documentOf(
                    (Class<Object>) clazz,
                    (TableSchema<Object>) BeanTableSchema.recursiveCreate(clazz, lookup, metaTableSchemaCache),
                    attrConfiguration);
            }
        }

        return EnhancedType.of(type);
    }

    private static Optional<AttributeConverter> createAttributeConverterFromAnnotation(
            PropertyDescriptor propertyDescriptor, MethodHandles.Lookup lookup) {
        DynamoDbConvertedBy attributeConverterBean =
                getPropertyAnnotation(propertyDescriptor, DynamoDbConvertedBy.class);
        Optional<Class<?>> optionalClass = Optional.ofNullable(attributeConverterBean)
                                                   .map(DynamoDbConvertedBy::value);
        return optionalClass.map(clazz -> (AttributeConverter) newObjectSupplierForClass(clazz, lookup).get());
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

    private static <R> Supplier<R> newObjectSupplierForClass(Class<R> clazz, MethodHandles.Lookup lookup) {
        try {
            Constructor<R> constructor = clazz.getConstructor();
            debugLog(clazz, () -> "Constructor: " + constructor);
            return ObjectConstructor.create(clazz, constructor, lookup);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                String.format("Class '%s' appears to have no default constructor thus cannot be used with the " +
                                  "BeanTableSchema", clazz), e);
        }
    }

    private static <T, R> Function<T, R> getterForProperty(PropertyDescriptor propertyDescriptor,
                                                           Class<T> beanClass,
                                                           MethodHandles.Lookup lookup) {
        Method readMethod = propertyDescriptor.getReadMethod();
        debugLog(beanClass, () -> "Property " + propertyDescriptor.getDisplayName() + " read method: " + readMethod);
        return BeanAttributeGetter.create(beanClass, readMethod, lookup);
    }

    private static <T, R> BiConsumer<T, R> setterForProperty(PropertyDescriptor propertyDescriptor,
                                                             Class<T> beanClass,
                                                             MethodHandles.Lookup lookup) {
        Method writeMethod = propertyDescriptor.getWriteMethod();
        debugLog(beanClass, () -> "Property " + propertyDescriptor.getDisplayName() + " write method: " + writeMethod);
        return BeanAttributeSetter.create(beanClass, writeMethod, lookup);
    }

    private static String attributeNameForProperty(PropertyDescriptor propertyDescriptor) {
        DynamoDbAttribute dynamoDbAttribute = getPropertyAnnotation(propertyDescriptor, DynamoDbAttribute.class);
        if (dynamoDbAttribute != null) {
            return dynamoDbAttribute.value();
        }

        return propertyDescriptor.getName();
    }

    private static boolean isMappableProperty(Class<?> beanClass, PropertyDescriptor propertyDescriptor) {

        if (propertyDescriptor.getReadMethod() == null) {
            debugLog(beanClass, () -> "Ignoring bean property " + propertyDescriptor.getDisplayName() + " because it has no "
                                      + "read (get/is) method.");
            return false;
        }

        if (propertyDescriptor.getWriteMethod() == null) {
            debugLog(beanClass, () -> "Ignoring bean property " + propertyDescriptor.getDisplayName() + " because it has no "
                                      + "write (set) method.");
            return false;
        }

        if (getPropertyAnnotation(propertyDescriptor, DynamoDbIgnore.class) != null ||
            getPropertyAnnotation(propertyDescriptor, Transient.class) != null) {
            debugLog(beanClass, () -> "Ignoring bean property " + propertyDescriptor.getDisplayName() + " because it has "
                                      + "@DynamoDbIgnore or @Transient.");
            return false;
        }

        return true;
    }

    private static <R extends Annotation> R getPropertyAnnotation(PropertyDescriptor propertyDescriptor,
                                                                  Class<R> annotationType) {
        R getterAnnotation = propertyDescriptor.getReadMethod().getAnnotation(annotationType);
        R setterAnnotation = propertyDescriptor.getWriteMethod().getAnnotation(annotationType);

        if (getterAnnotation != null) {
            return getterAnnotation;
        }

        // TODO: It's a common mistake that superclasses might have annotations that the child classes do not inherit, but the
        // customer expects them to be inherited. We should either allow inheriting those annotations, allow specifying an
        // annotation to inherit them, or log when this situation happens.
        return setterAnnotation;
    }

    private static List<? extends Annotation> propertyAnnotations(PropertyDescriptor propertyDescriptor) {
        return Stream.concat(Arrays.stream(propertyDescriptor.getReadMethod().getAnnotations()),
                             Arrays.stream(propertyDescriptor.getWriteMethod().getAnnotations()))
                     .collect(Collectors.toList());
    }

    private static void debugLog(Class<?> beanClass, Supplier<String> logMessage) {
        BEAN_LOGGER.debug(() -> beanClass.getTypeName() + " - " + logMessage.get());
    }

    @SdkTestInternalApi
    static void clearSchemaCache() {
        BEAN_TABLE_SCHEMA_CACHE.clear();
    }
}

