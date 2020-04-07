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

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Either;

@SdkInternalApi
public class LambdaToMethodBridgeBuilder<T> {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private final Class<T> lambdaType;
    private String lambdaMethodName;
    private Class<?> postEraseLambdaReturnType;
    private Class<?>[] postEraseLambdaParameters;
    private Class<?> preEraseLambdaReturnType;
    private Class<?>[] preEraseLambdaParameters;
    private Either<Method, Constructor<?>> targetMethod;

    private LambdaToMethodBridgeBuilder(Class<T> lambdaType) {
        this.lambdaType = lambdaType;
    }

    public static <T> LambdaToMethodBridgeBuilder<T> create(Class<T> lambdaType) {
        return new LambdaToMethodBridgeBuilder<>(lambdaType);
    }

    public LambdaToMethodBridgeBuilder<T> lambdaMethodName(String lambdaMethodName) {
        this.lambdaMethodName = lambdaMethodName;
        return this;
    }

    public LambdaToMethodBridgeBuilder<T> runtimeLambdaSignature(Class<?> returnType, Class<?>... parameters) {
        this.postEraseLambdaReturnType = returnType;
        this.postEraseLambdaParameters = parameters.clone();
        return this;
    }

    public LambdaToMethodBridgeBuilder<T> compileTimeLambdaSignature(Class<?> returnType, Class<?>... parameters) {
        this.preEraseLambdaReturnType = returnType;
        this.preEraseLambdaParameters = parameters.clone();
        return this;
    }

    public LambdaToMethodBridgeBuilder<T> targetMethod(Method method) {
        this.targetMethod = Either.left(method);
        return this;
    }

    public LambdaToMethodBridgeBuilder<T> targetMethod(Constructor<?> method) {
        this.targetMethod = Either.right(method);
        return this;
    }

    public T build() {
        try {
            MethodHandle targetMethodHandle = targetMethod.map(
                m -> invokeSafely(() -> LOOKUP.unreflect(m)),
                c -> invokeSafely(() -> LOOKUP.unreflectConstructor(c)));

            return lambdaType.cast(
                LambdaMetafactory.metafactory(LOOKUP,
                                              lambdaMethodName,
                                              MethodType.methodType(lambdaType),
                                              MethodType.methodType(postEraseLambdaReturnType, postEraseLambdaParameters),
                                              targetMethodHandle,
                                              MethodType.methodType(preEraseLambdaReturnType, preEraseLambdaParameters))
                                 .getTarget()
                                 .invoke());
        } catch (Throwable e) {
            throw new IllegalArgumentException("Failed to generate method handle.", e);
        }
    }
}
