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

import java.lang.reflect.Constructor;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

@FunctionalInterface
@SdkInternalApi
@SuppressWarnings("unchecked")
public interface BeanConstructor<BeanT> extends Supplier<BeanT> {
    static <BeanT> BeanConstructor<BeanT> create(Class<BeanT> beanClass, Constructor<BeanT> noArgsConstructor) {
        Validate.isTrue(noArgsConstructor.getParameterCount() == 0,
                        "%s has no default constructor.",
                        beanClass);

        return LambdaToMethodBridgeBuilder.create(BeanConstructor.class)
                                          .lambdaMethodName("get")
                                          .runtimeLambdaSignature(Object.class)
                                          .compileTimeLambdaSignature(beanClass)
                                          .targetMethod(noArgsConstructor)
                                          .build();
    }
}
