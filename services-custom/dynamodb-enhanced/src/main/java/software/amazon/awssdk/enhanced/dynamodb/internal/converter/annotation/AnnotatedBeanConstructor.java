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

import java.lang.reflect.Constructor;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Converts a bean {@link Class} into a {@link Supplier}, that can be invoked to create an instance of the bean.
 *
 * <p>
 * Uses {@link LambdaToMethodBridgeBuilder} for invoking the constructor, which means that security checks are performed when
 * the {@code AnnotatedBeanConstructor} is created, and not when the constructor is invoked.
 *
 * <p>
 * Requires a bean to have a zero-argument public constructor.
 *
 * <p>
 * Created with {@link #create(Class)}
 */
@SdkInternalApi
public class AnnotatedBeanConstructor<BeanT> implements Supplier<BeanT> {
    private final Supplier<BeanT> delegate;

    @SuppressWarnings("unchecked")
    private AnnotatedBeanConstructor(Class<BeanT> beanClass, Constructor<BeanT> constructor) {
        this.delegate = LambdaToMethodBridgeBuilder.create(Supplier.class)
                                                   .lambdaMethodName("get")
                                                   .compileTimeLambdaSignature(beanClass)
                                                   .runtimeLambdaSignature(Object.class)
                                                   .targetMethod(constructor)
                                                   .build();
    }

    /**
     * Create a {@code AnnotatedBeanConstructor} that can be used to create instances of the provided bean class.
     */
    public static <BeanT> AnnotatedBeanConstructor<BeanT> create(Class<BeanT> beanClass) {
        try {
            return new AnnotatedBeanConstructor<>(beanClass, beanClass.getDeclaredConstructor());
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(beanClass + " must contain zero-arg constructor", e);
        }
    }

    @Override
    public BeanT get() {
        return delegate.get();
    }
}
