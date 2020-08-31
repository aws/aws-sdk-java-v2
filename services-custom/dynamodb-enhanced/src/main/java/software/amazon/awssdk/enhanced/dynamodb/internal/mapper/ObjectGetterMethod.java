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
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;

@FunctionalInterface
@SdkInternalApi
@SuppressWarnings("unchecked")
public interface ObjectGetterMethod<BeanT, GetterT> extends Function<BeanT, GetterT> {
    static <BeanT, GetterT> ObjectGetterMethod<BeanT, GetterT> create(Class<BeanT> beanClass, Method buildMethod) {
        return LambdaToMethodBridgeBuilder.create(ObjectGetterMethod.class)
                                          .lambdaMethodName("apply")
                                          .runtimeLambdaSignature(Object.class, Object.class)
                                          .compileTimeLambdaSignature(buildMethod.getReturnType(), beanClass)
                                          .targetMethod(buildMethod)
                                          .build();
    }
}
