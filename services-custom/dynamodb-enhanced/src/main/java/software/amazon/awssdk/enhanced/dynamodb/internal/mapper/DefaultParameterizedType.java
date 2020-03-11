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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link ParameterizedType} that guarantees its raw type is always a {@link Class}.
 */
@SdkInternalApi
@ThreadSafe
public final class DefaultParameterizedType implements ParameterizedType {
    private final Class<?> rawType;
    private final Type[] arguments;

    private DefaultParameterizedType(Class<?> rawType, Type... arguments) {
        Validate.notEmpty(arguments, "Arguments must not be empty.");
        Validate.noNullElements(arguments, "Arguments cannot contain null values.");
        this.rawType = Validate.paramNotNull(rawType, "rawType");
        this.arguments = arguments;

    }

    public static ParameterizedType parameterizedType(Class<?> rawType, Type... arguments) {
        return new DefaultParameterizedType(rawType, arguments);
    }

    @Override
    public Class<?> getRawType() {
        return rawType;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return arguments.clone();
    }

    @Override
    public Type getOwnerType() {
        return null;
    }
}
