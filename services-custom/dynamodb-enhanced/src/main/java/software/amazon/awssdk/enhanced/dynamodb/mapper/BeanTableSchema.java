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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanAttributeGetter;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanAttributeSetter;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.BeanTableSchemaAttributeTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Implementation of {@link TableSchema} that builds a table schema based on properties and annotations of a bean
 * class. Example:
 * {@code
 * @literal @DynamoDbBean
 * public class CustomerAccount {
 *     private String unencryptedBillingKey;
 *
 *     @literal @DynamoDbPartitionKey
 *     @literal @DynamoDbSecondarySortKey(indexName = "accounts_by_customer")
 *     public String accountId;
 *
 *     @literal @DynamoDbSortKey
 *     @literal @DynamoDbSecondaryPartitionKey(indexName = "accounts_by_customer")
 *     public String customerId;
 *
 *     @literal @DynamoDbAttribute("account_status")
 *     public CustomerAccountStatus status;
 *
 *     @literal @DynamoDbFlatten(dynamoDbBeanClass = Customer.class)
 *     public Customer customer;
 *
 *     public Instant createdOn;
 *
 *     // All public fields must be opted out to not participate in mapping
 *     @literal @DynamoDbIgnore
 *     public String internalKey;
 *
 *     public enum CustomerAccountStatus {
 *         ACTIVE,
 *         CLOSED
 *     }
 * }
 *
 * @literal @DynamoDbBean
 * public class Customer {
 *     public String name;
 *
 *     public List<String> address;
 * }
 * }
 * @param <T> The type of object that this {@link TableSchema} maps to.
 */
@SdkPublicApi
public final class BeanTableSchema<T> implements TableSchema<T> {
    private static final String ATTRIBUTE_TAG_STATIC_SUPPLIER_NAME = "attributeTagFor";

    private final StaticTableSchema<T> wrappedTableSchema;
    private final Class<T> beanClass;

    private BeanTableSchema(StaticTableSchema<T> staticTableSchema, Class<T> beanClass) {
        this.wrappedTableSchema = staticTableSchema;
        this.beanClass = beanClass;
    }

    /**
     * Scans a bean class and builds a {@link BeanTableSchema} from it that can be used with the
     * {@link DynamoDbEnhancedClient}.
     * @param beanClass The bean class to build the table schema from.
     * @param <T> The bean class type.
     * @return An initialized {@link BeanTableSchema}
     */
    public static <T> BeanTableSchema<T> create(Class<T> beanClass) {
        return new BeanTableSchema<>(createStaticTableSchema(beanClass), beanClass);
    }

    /**
     * Returns the bean class this object was created with.
     * @return The bean class that was supplied to the static constructor of this object.
     */
    public Class<T> beanClass() {
        return this.beanClass;
    }

    /**
     * {@inheritDoc}
     * @param attributeMap A map of String to {@link AttributeValue} that contains all the raw attributes to map.
     */
    @Override
    public T mapToItem(Map<String, AttributeValue> attributeMap) {
        return wrappedTableSchema.mapToItem(attributeMap);
    }

    /**
     * {@inheritDoc}
     * @param item The modelled Java object to convert into a map of attributes.
     * @param ignoreNulls If set to true; any null values in the Java object will not be added to the output map.
     *                    If set to false; null values in the Java object will be added as {@link AttributeValue} of
     *                    type 'nul' to the output map.
     */
    @Override
    public Map<String, AttributeValue> itemToMap(T item, boolean ignoreNulls) {
        return wrappedTableSchema.itemToMap(item, ignoreNulls);
    }

    /**
     * {@inheritDoc}
     * @param item The modelled Java object to extract the map of attributes from.
     * @param attributes A collection of attribute names to extract into the output map.
     */
    @Override
    public Map<String, AttributeValue> itemToMap(T item, Collection<String> attributes) {
        return wrappedTableSchema.itemToMap(item, attributes);
    }

    /**
     * {@inheritDoc}
     * @param item The modelled Java object to extract the attribute from.
     * @param key The attribute name describing which attribute to extract.
     */
    @Override
    public AttributeValue attributeValue(T item, String key) {
        return wrappedTableSchema.attributeValue(item, key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TableMetadata tableMetadata() {
        return wrappedTableSchema.tableMetadata();
    }

    private static <T> StaticTableSchema<T> createStaticTableSchema(Class<T> beanClass) {
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

        List<StaticAttribute<T, ?>> attributes = new ArrayList<>();

        Arrays.stream(beanInfo.getPropertyDescriptors())
              .filter(BeanTableSchema::isMappableProperty)
              .forEach(propertyDescriptor -> {
                  DynamoDbFlatten dynamoDbFlatten = propertyAnnotation(propertyDescriptor, DynamoDbFlatten.class);

                  if (dynamoDbFlatten != null) {
                      builder.flatten(createStaticTableSchema(dynamoDbFlatten.dynamoDbBeanClass()),
                                      getterForProperty(propertyDescriptor, beanClass),
                                      setterForProperty(propertyDescriptor, beanClass));
                  } else {
                      StaticAttribute.Builder<T, ?> attributeBuilder =
                          staticAttributeBuilder(propertyDescriptor, beanClass);

                      addTagsToAttribute(attributeBuilder, propertyDescriptor);
                      attributes.add(attributeBuilder.build());
                  }
              });

        builder.attributes(attributes);
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static <T> StaticAttribute.Builder<T, ?> staticAttributeBuilder(PropertyDescriptor propertyDescriptor,
                                                                            Class<T> beanClass) {

        Type propertyType = propertyDescriptor.getReadMethod().getGenericReturnType();
        EnhancedType<?> propertyTypeToken = null;

        if (propertyType instanceof Class) {
            Class<?> clazz = (Class<?>) propertyType;
            if (clazz.getAnnotation(DynamoDbBean.class) != null) {
                propertyTypeToken = EnhancedType.documentOf((Class<Object>) clazz,
                                                         (TableSchema<Object>) createStaticTableSchema(clazz));
            }
        }

        if (propertyTypeToken == null) {
            propertyTypeToken = EnhancedType.of(propertyDescriptor.getReadMethod().getGenericReturnType());
        }

        return StaticAttribute.builder(beanClass, propertyTypeToken)
                              .name(attributeNameForProperty(propertyDescriptor))
                              .getter(getterForProperty(propertyDescriptor, beanClass))
                              .setter(setterForProperty(propertyDescriptor, beanClass));
    }

    /**
     * This method scans all the annotations on a property and looks for a meta-annotation of {@link BeanTableSchemaAttributeTag}.
     * If the meta-annotation is found, it attempts to create an annotation tag based on a standard named static method
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
            return BeanConstructor.create(clazz, clazz.getConstructor());
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
        DynamoDbAttribute dynamoDbAttribute = propertyAnnotation(propertyDescriptor, DynamoDbAttribute.class);
        if (dynamoDbAttribute != null) {
            return dynamoDbAttribute.value();
        }

        return propertyDescriptor.getName();
    }

    private static boolean isMappableProperty(PropertyDescriptor propertyDescriptor) {
        return propertyDescriptor.getReadMethod() != null
            && propertyDescriptor.getWriteMethod() != null
            && propertyAnnotation(propertyDescriptor, DynamoDbIgnore.class) == null;
    }

    private static <R extends Annotation> R propertyAnnotation(PropertyDescriptor propertyDescriptor,
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

