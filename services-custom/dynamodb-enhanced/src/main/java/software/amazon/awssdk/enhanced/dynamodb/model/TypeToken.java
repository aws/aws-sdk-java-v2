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

package software.amazon.awssdk.enhanced.dynamodb.model;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.internal.model.DefaultParameterizedType;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Similar to {@link Class}, this represents a specific raw class type. Unlike {@code Class}, this allows representing type
 * parameters that would usually be erased.
 *
 * @see #TypeToken()
 * @see #from(Class)
 * @see #listOf(Class)
 * @see #mapOf(Class, Class)
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public class TypeToken<T> {
    private final Class<T> rawClass;
    private final List<TypeToken<?>> rawClassParameters;

    /**
     * Create a type token, capturing the generic type arguments of the token as {@link Class}es.
     *
     * <p>
     * <b>This must be called from an anonymous subclass.</b> For example, </b>
     * {@code new TypeToken<Iterable<String>>()&#123;&#125;} (note the extra {}) for a {@code TypeToken<Iterable<String>>}.
     */
    protected TypeToken() {
        this(null);
    }

    private TypeToken(Type type) {
        if (type == null) {
            type = captureGenericTypeArguments();
        }

        this.rawClass = validateAndConvert(type);
        this.rawClassParameters = loadTypeParameters(type);
    }

    private static TypeToken<?> from(Type type) {
        return new TypeToken<>(validateIsSupportedType(type));
    }

    /**
     * Create a type token for the provided non-parameterized class.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<T> from(Class<T> type) {
        return new TypeToken<>(validateIsSupportedType(type));
    }

    /**
     * Create a type token for a list, with the provided value type class.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<List<T>> listOf(Class<T> valueType) {
        return new TypeToken<>(DefaultParameterizedType.parameterizedType(List.class, valueType));
    }

    /**
     * Create a type token for a map, with the provided key and value type classes.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided types are null.</li>
     * </ol>
     */
    public static <T, U> TypeToken<Map<T, U>> mapOf(Class<T> keyType, Class<U> valueType) {
        return new TypeToken<>(DefaultParameterizedType.parameterizedType(Map.class, keyType, valueType));
    }

    private static Type validateIsSupportedType(Type type) {
        Validate.validState(type != null, "Type must not be null.");
        Validate.validState(!(type instanceof GenericArrayType),
                            "Array type %s is not supported. Use java.util.List instead of arrays.", type);
        Validate.validState(!(type instanceof TypeVariable), "Type variable type %s is not supported.", type);
        Validate.validState(!(type instanceof WildcardType), "Wildcard type %s is not supported.", type);
        return type;
    }

    /**
     * Retrieve the {@link Class} object that this type token represents.
     *
     * e.g. For {@code TypeToken<String>}, this would return {@code String.class}.
     */
    public Class<T> rawClass() {
        return rawClass;
    }

    /**
     * Retrieve the {@link Class} objects of any type parameters for the class that this type token represents.
     *
     * <p>
     * e.g. For {@code TypeToken<List<String>>}, this would return {@code String.class}, and {@link #rawClass()} would
     * return {@code List.class}.
     *
     * <p>
     * If there are no type parameters, this will return an empty list.
     */
    public List<TypeToken<?>> rawClassParameters() {
        return rawClassParameters;
    }

    private Type captureGenericTypeArguments() {
        Type superclass = getClass().getGenericSuperclass();

        ParameterizedType parameterizedSuperclass =
                Validate.isInstanceOf(ParameterizedType.class, superclass, "%s isn't parameterized", superclass);

        return parameterizedSuperclass.getActualTypeArguments()[0];
    }

    private Class<T> validateAndConvert(Type type) {
        validateIsSupportedType(type);

        if (type instanceof Class) {
            return (Class<T>) type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return validateAndConvert(parameterizedType.getRawType());
        } else {
            throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    private List<TypeToken<?>> loadTypeParameters(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return Collections.emptyList();
        }

        ParameterizedType parameterizedType = (ParameterizedType) type;

        return Collections.unmodifiableList(
                Arrays.stream(parameterizedType.getActualTypeArguments())
                      .peek(t -> Validate.validState(t != null, "Invalid type argument."))
                      .map(TypeToken::from)
                      .collect(Collectors.toList()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TypeToken)) {
            return false;
        }
        TypeToken<?> typeToken = (TypeToken<?>) o;
        return rawClass.equals(typeToken.rawClass) &&
               rawClassParameters.equals(typeToken.rawClassParameters);
    }

    @Override
    public int hashCode() {
        int result = rawClass.hashCode();
        result = 31 * result + rawClassParameters.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("TypeToken")
                       .add("rawClass", rawClass)
                       .add("rawClassParameters", rawClassParameters)
                       .build();
    }
}
