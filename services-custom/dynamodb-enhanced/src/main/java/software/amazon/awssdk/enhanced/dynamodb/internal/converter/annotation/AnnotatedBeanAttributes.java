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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Converts a bean {@link Class} into a list of {@link AnnotatedBeanAttribute}, representing all attributes defined in the bean.
 *
 * <p>
 * Created with {@link #create(Class)}
 */
@SdkInternalApi
public class AnnotatedBeanAttributes<BeanT> {
    private final List<AnnotatedBeanAttribute<BeanT, ?, ?>> attributes;

    private AnnotatedBeanAttributes(Class<BeanT> beanType) {
        Method[] methods = beanType.getMethods();
        Map<String, PartialAnnotatedBeanAttribute> partialAttributes = new HashMap<>();

        for (Method method : methods) {
            AttributeUtils.getMethodAttributeName(method).ifPresent(methodAttributeName -> {
                PartialAnnotatedBeanAttribute partialAttribute =
                        partialAttributes.computeIfAbsent(methodAttributeName, x -> new PartialAnnotatedBeanAttribute());

                switch (AttributeUtils.getAttributeMethodType(method)) {
                    case GETTER:
                        partialAttribute.getter.add(method);
                        break;
                    case SETTER:
                        partialAttribute.setter.add(method);
                        break;
                    default:
                        throw new IllegalStateException();
                }
            });
        }

        List<AnnotatedBeanAttribute<BeanT, ?, ?>> attributes = new ArrayList<>();

        partialAttributes.forEach((methodAttributeName, attribute) -> {
            // Skip attributes that are missing a getter or setter
            if (attribute.getter.isEmpty() || attribute.setter.isEmpty()) {
                return;
            }

            Validate.isTrue(attribute.getter.size() == 1, "Attribute has multiple getters: %s", methodAttributeName);
            Validate.isTrue(attribute.setter.size() == 1,
                            "Attribute has multiple setters, which isn't supported yet: %s",
                            methodAttributeName);

            attributes.add(AnnotatedBeanAttribute.create(beanType,
                                                         methodAttributeName,
                                                         attribute.getter.get(0),
                                                         attribute.setter.get(0)));
        });

        this.attributes = Collections.unmodifiableList(attributes);
    }

    public static <BeanT> AnnotatedBeanAttributes<BeanT> create(Class<BeanT> beanType) {
        return new AnnotatedBeanAttributes<>(beanType);
    }

    public List<AnnotatedBeanAttribute<BeanT, ?, ?>> attributes() {
        return attributes;
    }

    private static final class PartialAnnotatedBeanAttribute {
        private final List<Method> getter = new ArrayList<>();
        private final List<Method> setter = new ArrayList<>();
    }
}
