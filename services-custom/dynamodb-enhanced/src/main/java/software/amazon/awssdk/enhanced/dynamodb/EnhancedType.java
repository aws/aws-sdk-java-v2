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

package software.amazon.awssdk.enhanced.dynamodb;

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
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.DefaultParameterizedType;
import software.amazon.awssdk.utils.Validate;

/**
 * Similar to {@link Class}, this represents a specific raw class type. Unlike {@code Class}, this allows representing type
 * parameters that would usually be erased.
 *
 * @see #EnhancedType()
 * @see #of(Class)
 * @see #listOf(Class)
 * @see #mapOf(Class, Class)
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public class EnhancedType<T> {
    private final boolean isWildcard;
    private final Class<T> rawClass;
    private final List<EnhancedType<?>> rawClassParameters;
    private final TableSchema<T> tableSchema;

    /**
     * Create a type token, capturing the generic type arguments of the token as {@link Class}es.
     *
     * <p>
     * <b>This must be called from an anonymous subclass.</b> For example,
     * {@code new EnhancedType<Iterable<String>>()&#123;&#125;} (note the extra {}) for a {@code EnhancedType<Iterable<String>>}.
     */
    protected EnhancedType() {
        this(null);
    }

    private EnhancedType(Type type) {
        if (type == null) {
            type = captureGenericTypeArguments();
        }


        if (type instanceof WildcardType) {
            this.isWildcard = true;
            this.rawClass = null;
            this.rawClassParameters = null;
            this.tableSchema = null;
        } else {
            this.isWildcard = false;
            this.rawClass = validateAndConvert(type);
            this.rawClassParameters = loadTypeParameters(type);
            this.tableSchema = null;
        }
    }

    private EnhancedType(Class<?> rawClass, List<EnhancedType<?>> rawClassParameters, TableSchema<T> tableSchema) {
        // This is only used internally, so we can make sure this cast is safe via testing.
        this.rawClass = (Class<T>) rawClass;
        this.isWildcard = false;
        this.rawClassParameters = rawClassParameters;
        this.tableSchema = tableSchema;
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
    public static <T> EnhancedType<T> of(Class<T> type) {
        return new EnhancedType<>(type);
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
    public static EnhancedType<?> of(Type type) {
        return new EnhancedType<>(type);
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
    public static <T> EnhancedType<Optional<T>> optionalOf(Class<T> valueType) {
        return new EnhancedType<>(DefaultParameterizedType.parameterizedType(Optional.class, valueType));
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
    public static <T> EnhancedType<List<T>> listOf(Class<T> valueType) {
        return new EnhancedType<>(DefaultParameterizedType.parameterizedType(List.class, valueType));
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
    public static <T> EnhancedType<List<T>> listOf(EnhancedType<T> valueType) {
        return new EnhancedType<>(List.class, Arrays.asList(valueType), null);
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
    public static <T> EnhancedType<Set<T>> setOf(Class<T> valueType) {
        return new EnhancedType<>(DefaultParameterizedType.parameterizedType(Set.class, valueType));
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
    public static <T> EnhancedType<Set<T>> setOf(EnhancedType<T> valueType) {
        return new EnhancedType<>(Set.class, Arrays.asList(valueType), null);
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
    public static <T> EnhancedType<SortedSet<T>> sortedSetOf(Class<T> valueType) {
        return new EnhancedType<>(DefaultParameterizedType.parameterizedType(SortedSet.class, valueType));
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
    public static <T> EnhancedType<SortedSet<T>> sortedSetOf(EnhancedType<T> valueType) {
        return new EnhancedType<>(SortedSet.class, Arrays.asList(valueType), null);
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
    public static <T> EnhancedType<Deque<T>> dequeOf(Class<T> valueType) {
        return new EnhancedType<>(DefaultParameterizedType.parameterizedType(Deque.class, valueType));
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
    public static <T> EnhancedType<Deque<T>> dequeOf(EnhancedType<T> valueType) {
        return new EnhancedType<>(Deque.class, Arrays.asList(valueType), null);
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
    public static <T> EnhancedType<NavigableSet<T>> navigableSetOf(Class<T> valueType) {
        return new EnhancedType<>(DefaultParameterizedType.parameterizedType(NavigableSet.class, valueType));
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
    public static <T> EnhancedType<NavigableSet<T>> navigableSetOf(EnhancedType<T> valueType) {
        return new EnhancedType<>(NavigableSet.class, Arrays.asList(valueType), null);
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
    public static <T> EnhancedType<Collection<T>> collectionOf(Class<T> valueType) {
        return new EnhancedType<>(DefaultParameterizedType.parameterizedType(Collection.class, valueType));
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
    public static <T> EnhancedType<Collection<T>> collectionOf(EnhancedType<T> valueType) {
        return new EnhancedType<>(Collection.class, Arrays.asList(valueType), null);
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
    public static <T, U> EnhancedType<Map<T, U>> mapOf(Class<T> keyType, Class<U> valueType) {
        return new EnhancedType<>(DefaultParameterizedType.parameterizedType(Map.class, keyType, valueType));
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
    public static <T, U> EnhancedType<Map<T, U>> mapOf(EnhancedType<T> keyType, EnhancedType<U> valueType) {
        return new EnhancedType<>(Map.class, Arrays.asList(keyType, valueType), null);
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
    public static <T, U> EnhancedType<SortedMap<T, U>> sortedMapOf(Class<T> keyType, Class<U> valueType) {
        return new EnhancedType<>(DefaultParameterizedType.parameterizedType(SortedMap.class, keyType, valueType));
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
    public static <T, U> EnhancedType<SortedMap<T, U>> sortedMapOf(EnhancedType<T> keyType,
                                                                   EnhancedType<U> valueType) {
        return new EnhancedType<>(SortedMap.class, Arrays.asList(keyType, valueType), null);
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
    public static <T, U> EnhancedType<ConcurrentMap<T, U>> concurrentMapOf(Class<T> keyType, Class<U> valueType) {
        return new EnhancedType<>(DefaultParameterizedType.parameterizedType(ConcurrentMap.class, keyType, valueType));
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
    public static <T, U> EnhancedType<ConcurrentMap<T, U>> concurrentMapOf(EnhancedType<T> keyType,
                                                                           EnhancedType<U> valueType) {
        return new EnhancedType<>(ConcurrentMap.class, Arrays.asList(keyType, valueType), null);
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
    public static <T, U> EnhancedType<NavigableMap<T, U>> navigableMapOf(Class<T> keyType, Class<U> valueType) {
        return new EnhancedType<>(DefaultParameterizedType.parameterizedType(NavigableMap.class, keyType, valueType));
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
    public static <T, U> EnhancedType<NavigableMap<T, U>> navigableMapOf(EnhancedType<T> keyType,
                                                                         EnhancedType<U> valueType) {
        return new EnhancedType<>(NavigableMap.class, Arrays.asList(keyType, valueType), null);
    }

    /**
     * Create a type token that represents a document that is specified by the provided {@link TableSchema}.
     *
     * @param documentClass The Class representing the modeled document.
     * @param documentTableSchema A TableSchema that describes the properties of the document.
     * @return a new {@link EnhancedType} representing the provided document.
     */
    public static <T> EnhancedType<T> documentOf(Class<T> documentClass, TableSchema<T> documentTableSchema) {
        return new EnhancedType<>(documentClass, null, documentTableSchema);
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

    /**
     * Returns whether or not the type this {@link EnhancedType} was created with is a wildcard type.
     */
    public boolean isWildcard() {
        return isWildcard;
    }

    /**
     * Retrieve the {@link Class} object that this type token represents.
     *
     * e.g. For {@code EnhancedType<String>}, this would return {@code String.class}.
     */
    public Class<T> rawClass() {
        Validate.isTrue(!isWildcard, "A wildcard type is not expected here.");
        return rawClass;
    }

    /**
     * Retrieve the {@link TableSchema} for a modeled document. This is used for
     * converting nested documents within a schema.
     */
    public Optional<TableSchema<T>> tableSchema() {
        return Optional.ofNullable(tableSchema);
    }

    /**
     * Retrieve the {@link Class} objects of any type parameters for the class that this type token represents.
     *
     * <p>
     * e.g. For {@code EnhancedType<List<String>>}, this would return {@code String.class}, and {@link #rawClass()} would
     * return {@code List.class}.
     *
     * <p>
     * If there are no type parameters, this will return an empty list.
     */
    public List<EnhancedType<?>> rawClassParameters() {
        Validate.isTrue(!isWildcard, "A wildcard type is not expected here.");
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

    private List<EnhancedType<?>> loadTypeParameters(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return Collections.emptyList();
        }

        ParameterizedType parameterizedType = (ParameterizedType) type;

        return Collections.unmodifiableList(
            Arrays.stream(parameterizedType.getActualTypeArguments())
                  .peek(t -> Validate.validState(t != null, "Invalid type argument."))
                  .map(EnhancedType::new)
                  .collect(toList()));
    }

    private StringBuilder innerToString() {
        StringBuilder result = new StringBuilder();
        result.append(rawClass.getTypeName());

        if (null != rawClassParameters && !rawClassParameters.isEmpty()) {
            result.append("<");
            result.append(rawClassParameters.stream().map(EnhancedType::innerToString).collect(Collectors.joining(", ")));
            result.append(">");
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnhancedType)) {
            return false;
        }

        EnhancedType<?> enhancedType = (EnhancedType<?>) o;

        if (isWildcard != enhancedType.isWildcard) {
            return false;
        }
        if (!rawClass.equals(enhancedType.rawClass)) {
            return false;
        }
        if (rawClassParameters != null ? !rawClassParameters.equals(enhancedType.rawClassParameters) :
            enhancedType.rawClassParameters != null) {
            return false;
        }

        return tableSchema != null ? tableSchema.equals(enhancedType.tableSchema) : enhancedType.tableSchema == null;
    }

    @Override
    public int hashCode() {
        int result = (isWildcard ? 1 : 0);
        result = 31 * result + rawClass.hashCode();
        result = 31 * result + (rawClassParameters != null ? rawClassParameters.hashCode() : 0);
        result = 31 * result + (tableSchema != null ? tableSchema.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EnhancedType(" + innerToString() + ")";
    }
}
