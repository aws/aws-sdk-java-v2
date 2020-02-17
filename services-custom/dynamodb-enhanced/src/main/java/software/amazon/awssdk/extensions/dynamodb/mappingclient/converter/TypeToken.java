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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.converter;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.core.DefaultParameterizedType;
import software.amazon.awssdk.utils.Validate;

/**
 * Similar to {@link Class}, this represents a specific raw class type. Unlike {@code Class}, this allows representing type
 * parameters that would usually be erased.
 *
 * @see #TypeToken()
 * @see #of(Class)
 * @see #listOf(Class)
 * @see #mapOf(Class, Class)
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public class TypeToken<T> {
    private final boolean isWildcard;
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


        if (type instanceof WildcardType) {
            this.isWildcard = true;
            this.rawClass = null;
            this.rawClassParameters = null;
        } else {
            this.isWildcard = false;
            this.rawClass = validateAndConvert(type);
            this.rawClassParameters = loadTypeParameters(type);
        }
    }

    private TypeToken(Class<?> rawClass, List<TypeToken<?>> rawClassParameters) {
        // This is only used internally, so we can make sure this cast is safe via testing.
        this.rawClass = (Class<T>) rawClass;
        this.rawClassParameters = rawClassParameters;
        this.isWildcard = false;
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
    public static <T> TypeToken<T> of(Class<T> type) {
        return new TypeToken<>(type);
    }

    public static TypeToken<?> of(Type type) {
        return new TypeToken<>(type);
    }

    /**
     * Create a type token for a optional, with the provided value type class.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<Optional<T>> optionalOf(Class<T> valueType) {
        return new TypeToken<>(DefaultParameterizedType.parameterizedType(Optional.class, valueType));
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
     * Create a type token for a list, with the provided value type class.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<List<T>> listOf(TypeToken<T> valueType) {
        return new TypeToken<>(List.class, Arrays.asList(valueType));
    }

    /**
     * Create a type token for a set, with the provided value type class.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<Set<T>> setOf(Class<T> valueType) {
        return new TypeToken<>(DefaultParameterizedType.parameterizedType(Set.class, valueType));
    }

    /**
     * Create a type token for a set, with the provided value type class.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<Set<T>> setOf(TypeToken<T> valueType) {
        return new TypeToken<>(Set.class, Arrays.asList(valueType));
    }

    /**
     * Create a type token for a sorted set, with the provided value type class.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<SortedSet<T>> sortedSetOf(Class<T> valueType) {
        return new TypeToken<>(DefaultParameterizedType.parameterizedType(SortedSet.class, valueType));
    }

    /**
     * Create a type token for a sorted set, with the provided value type class.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<SortedSet<T>> sortedSetOf(TypeToken<T> valueType) {
        return new TypeToken<>(SortedSet.class, Arrays.asList(valueType));
    }

    /**
     * Create a type token for a queue, with the provided value type class.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<Queue<T>> queueOf(Class<T> valueType) {
        return new TypeToken<>(DefaultParameterizedType.parameterizedType(Queue.class, valueType));
    }

    /**
     * Create a type token for a queue, with the provided value type token.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<Queue<T>> queueOf(TypeToken<T> valueType) {
        return new TypeToken<>(Queue.class, Arrays.asList(valueType));
    }

    /**
     * Create a type token for a deque, with the provided value type class.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<Deque<T>> dequeOf(Class<T> valueType) {
        return new TypeToken<>(DefaultParameterizedType.parameterizedType(Deque.class, valueType));
    }

    /**
     * Create a type token for a deque, with the provided value type token.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<Deque<T>> dequeOf(TypeToken<T> valueType) {
        return new TypeToken<>(Deque.class, Arrays.asList(valueType));
    }

    /**
     * Create a type token for a navigable set, with the provided value type class.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<NavigableSet<T>> navigableSetOf(Class<T> valueType) {
        return new TypeToken<>(DefaultParameterizedType.parameterizedType(NavigableSet.class, valueType));
    }

    /**
     * Create a type token for a navigable set, with the provided value type token.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<NavigableSet<T>> navigableSetOf(TypeToken<T> valueType) {
        return new TypeToken<>(NavigableSet.class, Arrays.asList(valueType));
    }

    /**
     * Create a type token for a collection, with the provided value type class.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<Collection<T>> collectionOf(Class<T> valueType) {
        return new TypeToken<>(DefaultParameterizedType.parameterizedType(Collection.class, valueType));
    }

    /**
     * Create a type token for a collection, with the provided value type token.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     */
    public static <T> TypeToken<Collection<T>> collectionOf(TypeToken<T> valueType) {
        return new TypeToken<>(Collection.class, Arrays.asList(valueType));
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

    /**
     * Create a type token for a map, with the provided key and value type classes.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided types are null.</li>
     * </ol>
     */
    public static <T, U> TypeToken<Map<T, U>> mapOf(TypeToken<T> keyType, TypeToken<U> valueType) {
        return new TypeToken<>(Map.class, Arrays.asList(keyType, valueType));
    }

    /**
     * Create a type token for a sorted map, with the provided key and value type classes.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided types are null.</li>
     * </ol>
     */
    public static <T, U> TypeToken<SortedMap<T, U>> sortedMapOf(Class<T> keyType, Class<U> valueType) {
        return new TypeToken<>(DefaultParameterizedType.parameterizedType(SortedMap.class, keyType, valueType));
    }

    /**
     * Create a type token for a sorted map, with the provided key and value type classes.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided types are null.</li>
     * </ol>
     */
    public static <T, U> TypeToken<SortedMap<T, U>> sortedMapOf(TypeToken<T> keyType, TypeToken<U> valueType) {
        return new TypeToken<>(SortedMap.class, Arrays.asList(keyType, valueType));
    }

    /**
     * Create a type token for a concurrent map, with the provided key and value type classes.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided types are null.</li>
     * </ol>
     */
    public static <T, U> TypeToken<ConcurrentMap<T, U>> concurrentMapOf(Class<T> keyType, Class<U> valueType) {
        return new TypeToken<>(DefaultParameterizedType.parameterizedType(ConcurrentMap.class, keyType, valueType));
    }

    /**
     * Create a type token for a concurrent map, with the provided key and value type classes.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided types are null.</li>
     * </ol>
     */
    public static <T, U> TypeToken<ConcurrentMap<T, U>> concurrentMapOf(TypeToken<T> keyType, TypeToken<U> valueType) {
        return new TypeToken<>(ConcurrentMap.class, Arrays.asList(keyType, valueType));
    }

    /**
     * Create a type token for a navigable map, with the provided key and value type classes.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided types are null.</li>
     * </ol>
     */
    public static <T, U> TypeToken<NavigableMap<T, U>> navigableMapOf(Class<T> keyType, Class<U> valueType) {
        return new TypeToken<>(DefaultParameterizedType.parameterizedType(NavigableMap.class, keyType, valueType));
    }

    /**
     * Create a type token for a navigable map, with the provided key and value type classes.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided types are null.</li>
     * </ol>
     */
    public static <T, U> TypeToken<NavigableMap<T, U>> navigableMapOf(TypeToken<T> keyType, TypeToken<U> valueType) {
        return new TypeToken<>(NavigableMap.class, Arrays.asList(keyType, valueType));
    }

    private static Type validateIsSupportedType(Type type) {
        Validate.validState(type != null, "Type must not be null.");
        Validate.validState(!(type instanceof GenericArrayType),
                            "Array type %s is not supported. Use java.util.List instead of arrays.", type);
        Validate.validState(!(type instanceof TypeVariable), "Type variable type %s is not supported.", type);

        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            Validate.validState(wildcardType.getUpperBounds().length == 1 && wildcardType.getUpperBounds()[0] == Object.class,
                                "Non-Object wildcard type upper bounds are not supported.");
            Validate.validState(wildcardType.getLowerBounds().length == 0,
                                "Wildcard type lower bounds are not supported.");
        }

        return type;
    }

    public boolean isWildcard() {
        return isWildcard;
    }

    /**
     * Retrieve the {@link Class} object that this type token represents.
     *
     * e.g. For {@code TypeToken<String>}, this would return {@code String.class}.
     */
    public Class<T> rawClass() {
        Validate.isTrue(!isWildcard, "A wildcard type is not expected here.");
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
        Validate.isTrue(!isWildcard, "A wildcard type is not expected here.");
        return rawClassParameters;
    }

    public boolean isSuperTypeOf(TypeToken<?> rhs) {
        // Covariant or contravariant wildcard types aren't supported, so if we're a wildcard then we're a supertype.
        if (isWildcard) {
            return true;
        }

        // If they aren't assignable to us, then we're obviously not a subtype.
        if (!rawClass.isAssignableFrom(rhs.rawClass)) {
            return false;
        }

        // Now things are tricky - if they are definitely a subtype of us,
        if (rawClass.equals(rhs.rawClass)) {
            if (rawClassParameters.size() != rhs.rawClassParameters.size()) {
                return false;
            }

            for (int i = 0; i < rawClassParameters.size(); i++) {
                if (!rawClassParameters.get(i).isSuperTypeOf(rhs.rawClassParameters.get(i))) {
                    return false;
                }
            }
        }

        return true;
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
                      .map(TypeToken::new)
                      .collect(toList()));
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
        return "TypeToken(" + innerToString() + ")";
    }

    private StringBuilder innerToString() {
        StringBuilder result = new StringBuilder();
        result.append(rawClass.getTypeName());

        if (!rawClassParameters.isEmpty()) {
            result.append("<");
            result.append(rawClassParameters.stream().map(TypeToken::innerToString).collect(Collectors.joining(", ")));
            result.append(">");
        }

        return result;
    }
}
