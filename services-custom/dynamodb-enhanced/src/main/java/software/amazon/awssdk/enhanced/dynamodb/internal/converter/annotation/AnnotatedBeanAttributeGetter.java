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
import software.amazon.awssdk.enhanced.dynamodb.converter.bean.BeanAttributeGetter;
import software.amazon.awssdk.utils.Validate;

/**
 * Converts a bean {@link Class} and getter {@link Method} into a {@link BeanAttributeGetter} that can be invoked for reading
 * a property from a bean.
 *
 * <p>
 * Created with {@link #create(Class, Method)}.
 */
@SdkInternalApi
public class AnnotatedBeanAttributeGetter<BeanT, GetterT> {
    private final Class<GetterT> methodReturnType;
    private final BeanAttributeGetter<BeanT, GetterT> getterFunction;
    private final AnnotatedBeanAttributeAnnotations annotations;

    private AnnotatedBeanAttributeGetter(Class<GetterT> returnType,
                                         BeanAttributeGetter<BeanT, GetterT> getterFunction,
                                         Method getter) {
        this.methodReturnType = returnType;
        this.getterFunction = getterFunction;
        this.annotations = AnnotatedBeanAttributeAnnotations.create(getter);
    }

    @SuppressWarnings("unchecked")
    public static <BeanT, GetterT> AnnotatedBeanAttributeGetter<BeanT, GetterT> create(Class<BeanT> beanClass, Method getter) {
        Validate.isTrue(getter.getParameterCount() == 0,
                        "%s.%s has parameters, despite being named like a getter.",
                        beanClass, getter.getName());

        BeanAttributeGetter<BeanT, GetterT> getterFunction =
                LambdaToMethodBridgeBuilder.create(BeanAttributeGetter.class)
                                           .lambdaMethodName("apply")
                                           .runtimeLambdaSignature(Object.class, Object.class)
                                           .compileTimeLambdaSignature(getter.getReturnType(), beanClass)
                                           .targetMethod(getter)
                                           .build();

        Class<GetterT> returnType = (Class<GetterT>) getter.getReturnType();
        return new AnnotatedBeanAttributeGetter<>(returnType, getterFunction, getter);
    }

    public Class<GetterT> methodReturnType() {
        return methodReturnType;
    }

    public BeanAttributeGetter<BeanT, GetterT> function() {
        return getterFunction;
    }

    public AnnotatedBeanAttributeAnnotations annotations() {
        return annotations;
    }
}
