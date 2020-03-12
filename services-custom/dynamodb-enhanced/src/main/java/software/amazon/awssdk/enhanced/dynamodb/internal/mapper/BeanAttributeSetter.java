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

package software.amazon.awssdk.enhanced.dynamodb.internal.mapper;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.internal.ReflectionUtils;

@FunctionalInterface
@SdkInternalApi
public interface BeanAttributeSetter<BeanT, GetterT> extends BiConsumer<BeanT, GetterT> {
    @SuppressWarnings("unchecked")
    static <BeanT, SetterT> BeanAttributeSetter<BeanT, SetterT> create(Class<BeanT> beanClass, Method setter) {
        Validate.isTrue(setter.getParameterCount() == 1,
                        "%s.%s doesn't have just 1 parameter, despite being named like a setter.",
                        beanClass, setter.getName());

        Class<?> setterInputClass = setter.getParameters()[0].getType();
        Class<?> boxedInputClass = ReflectionUtils.getWrappedClass(setterInputClass);

        return LambdaToMethodBridgeBuilder.create(BeanAttributeSetter.class)
                                          .lambdaMethodName("accept")
                                          .runtimeLambdaSignature(void.class, Object.class, Object.class)
                                          .compileTimeLambdaSignature(void.class, beanClass, boxedInputClass)
                                          .targetMethod(setter)
                                          .build();
    }
}
