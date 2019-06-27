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

import java.lang.reflect.Method;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.converter.bean.annotation.AttributeElementType;
import software.amazon.awssdk.enhanced.dynamodb.converter.bean.annotation.AttributeName;
import software.amazon.awssdk.enhanced.dynamodb.internal.model.DefaultParameterizedType;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;

/**
 * Converts a bean {@link Class}, attribute name, getter {@link Method} and setter {@link Method} into a
 * {@link AnnotatedBeanAttributeGetter} and {@link AnnotatedBeanAttributeSetter} that can be invoked to read and write attributes
 * to a bean.
 *
 * <p>
 * Created with {@link #create(Class, String, Method, Method)}
 */
@SdkInternalApi
public class AnnotatedBeanAttribute<BeanT, GetterT, SetterT> {
    private final String resolvedAttributeName;
    private final AnnotatedBeanAttributeGetter<BeanT, GetterT> getter;
    private final AnnotatedBeanAttributeSetter<BeanT, SetterT> setter;
    private final TypeToken<SetterT> resolvedSetterInputType;

    private AnnotatedBeanAttribute(Class<BeanT> beanClass, String methodAttributeName, Method getter, Method setter) {
        this.getter = AnnotatedBeanAttributeGetter.create(beanClass, getter);
        this.setter = AnnotatedBeanAttributeSetter.create(beanClass, setter);

        AnnotatedBeanAttributeAnnotations attributeAnnotations =
                this.getter.annotations()
                          .mergeWith(this.setter.annotations());

        this.resolvedAttributeName = resolveAttributeName(methodAttributeName, attributeAnnotations);
        this.resolvedSetterInputType = resolveSetterInputType(attributeAnnotations);
    }

    private String resolveAttributeName(String methodAttributeName, AnnotatedBeanAttributeAnnotations attributeAnnotations) {
        return attributeAnnotations.attributeName().map(AttributeName::value).orElse(methodAttributeName);
    }

    @SuppressWarnings("unchecked")
    private TypeToken<SetterT> resolveSetterInputType(AnnotatedBeanAttributeAnnotations attributeAnnotations) {
        Class<?>[] arguments = attributeAnnotations.attributeElementType()
                                                   .map(AttributeElementType::value)
                                                   .orElse(new Class<?>[0]);
        if (arguments.length == 0) {
            return TypeToken.of(this.setter.methodInputType());
        } else {
            return (TypeToken<SetterT>) TypeToken.of(
                    DefaultParameterizedType.parameterizedType(this.setter.methodInputType(), arguments));
        }
    }

    public static <BeanT, GetterT, SetterT> AnnotatedBeanAttribute<BeanT, GetterT, SetterT> create(Class<BeanT> beanClass,
                                                                                                   String methodAttributeName,
                                                                                                   Method getter,
                                                                                                   Method setter) {
        return new AnnotatedBeanAttribute<>(beanClass, methodAttributeName, getter, setter);
    }

    public String resolvedAttributeName() {
        return resolvedAttributeName;
    }

    public TypeToken<SetterT> resolvedSetterInputType() {
        return resolvedSetterInputType;
    }

    public AnnotatedBeanAttributeGetter<BeanT, GetterT> getter() {
        return getter;
    }

    public AnnotatedBeanAttributeSetter<BeanT, SetterT> setter() {
        return setter;
    }

    public enum Type {
        GETTER,
        SETTER
    }
}
