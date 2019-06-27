/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter.annotation;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.DefaultAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.bean.AsymmetricBeanAttribute;
import software.amazon.awssdk.enhanced.dynamodb.converter.bean.BeanSchema;
import software.amazon.awssdk.enhanced.dynamodb.converter.bean.annotation.AnnotatedBeanAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.bean.annotation.Item;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.utils.Validate;

/**
 * Converts a {@link Class} into a {@link BeanSchema}, based on the rules described in {@link AnnotatedBeanAttributeConverter}.
 *
 * <p>
 * Uses {@link AnnotatedBeanConstructor} for calling bean constructors, and {@link AnnotatedBeanAttributes} for calling bean
 * getter and setter methods.
 *
 * <p>
 * Created with {@link #create(Class)}
 */
@SdkInternalApi
public class AnnotatedBean<T> {
    private final Class<T> beanType;
    private final AnnotatedBeanConstructor<T> constructor;
    private final AnnotatedBeanAttributes<T> attributes;

    private AnnotatedBean(Class<T> beanType) {
        Validate.isTrue(beanType.getAnnotation(Item.class) != null, "Provided type is not an annotated bean: %s", beanType);
        this.beanType = beanType;
        this.constructor = AnnotatedBeanConstructor.create(beanType);
        this.attributes = AnnotatedBeanAttributes.create(beanType);
    }

    /**
     * Create an annotated bean that can generate a schema for the provided bean type.
     */
    public static <T> AnnotatedBean<T> create(Class<T> beanType) {
        return new AnnotatedBean<>(beanType);
    }

    /**
     * Generate a {@link BeanSchema} that describes the annotated bean provided in {@link #create(Class)}.
     */
    public BeanSchema<T> toBeanSchema() {
        return BeanSchema.builder(beanType)
                         .constructor(constructor)
                         .addAsymmetricAttributes(convertAttributes())
                         .build();
    }

    /**
     * Convert each attribute in {@link #attributes} into a {@link AsymmetricBeanAttribute} so that they can be used in a
     * {@link BeanSchema}.
     */
    private Collection<AsymmetricBeanAttribute<T, ?, ?>> convertAttributes() {
        return attributes.attributes().stream()
                         .map(this::toAttributeSchema)
                         .collect(toList());
    }


    /**
     * Convert a specific {@link AnnotatedBeanAttribute} into a {@link AsymmetricBeanAttribute} so that it can be used in a
     * {@link BeanSchema}.
     */
    private <GetterT, SetterT> AsymmetricBeanAttribute<T, GetterT, SetterT> toAttributeSchema(
            AnnotatedBeanAttribute<T, GetterT, SetterT> attribute) {
        DefaultAttributeConverter defaultConverter = DefaultAttributeConverter.create();

        return AsymmetricBeanAttribute.builder(TypeToken.of(beanType),
                                               TypeToken.of(attribute.getter().methodReturnType()),
                                               attribute.resolvedSetterInputType())
                                      .attributeName(attribute.resolvedAttributeName())
                                      .getter(attribute.getter().function())
                                      .setter(attribute.setter().function())
                                      .getterConverter(defaultConverter)
                                      .setterConverter(defaultConverter)
                                      .build();
    }
}
