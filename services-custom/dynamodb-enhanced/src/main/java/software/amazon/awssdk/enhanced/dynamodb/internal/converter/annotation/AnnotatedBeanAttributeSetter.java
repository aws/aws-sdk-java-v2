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
import software.amazon.awssdk.enhanced.dynamodb.converter.bean.BeanAttributeSetter;
import software.amazon.awssdk.utils.Validate;

/**
 * Converts a bean {@link Class} and setter {@link Method} into a {@link BeanAttributeSetter} that can be invoked for writing
 * a property to a bean.
 *
 * <p>
 * Created with {@link #create(Class, Method)}.
 */
@SdkInternalApi
public class AnnotatedBeanAttributeSetter<BeanT, SetterT> {
    private final Class<SetterT> inputType;
    private final BeanAttributeSetter<BeanT, SetterT> setterFunction;
    private final AnnotatedBeanAttributeAnnotations annotations;

    private AnnotatedBeanAttributeSetter(Class<SetterT> inputType,
                                         BeanAttributeSetter<BeanT, SetterT> setterFunction,
                                         Method setter) {
        this.inputType = inputType;
        this.setterFunction = setterFunction;
        this.annotations = AnnotatedBeanAttributeAnnotations.create(setter);
    }

    @SuppressWarnings("unchecked")
    public static <BeanT, SetterT> AnnotatedBeanAttributeSetter<BeanT, SetterT> create(Class<BeanT> beanClass, Method setter) {
        Validate.isTrue(setter.getParameterCount() == 1,
                        "%s.%s doesn't have just 1 parameter, despite being named like a setter.",
                        beanClass, setter.getName());

        Class<?> setterInputClass = setter.getParameters()[0].getType();
        BeanAttributeSetter<BeanT, SetterT> setterFunction =
                LambdaToMethodBridgeBuilder.create(BeanAttributeSetter.class)
                                           .lambdaMethodName("accept")
                                           .runtimeLambdaSignature(void.class, Object.class, Object.class)
                                           .compileTimeLambdaSignature(void.class, beanClass, setterInputClass)
                                           .targetMethod(setter)
                                           .build();

        Class<SetterT> inputType = (Class<SetterT>) setterInputClass;
        return new AnnotatedBeanAttributeSetter<>(inputType, setterFunction, setter);
    }

    public Class<SetterT> methodInputType() {
        return inputType;
    }

    public BeanAttributeSetter<BeanT, SetterT> function() {
        return setterFunction;
    }

    public AnnotatedBeanAttributeAnnotations annotations() {
        return annotations;
    }
}
