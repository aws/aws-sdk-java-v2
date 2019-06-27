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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.MappedTable;
import software.amazon.awssdk.enhanced.dynamodb.Table;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ElementTypeAwareCollection;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.GenericConvertibleCollection;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.GenericConvertibleMap;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.SubtypeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ConvertibleItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.utils.Validate;

/**
 * A converter between {@link Collection} subtypes and {@link ItemAttributeValue}.
 *
 * <p>
 * This stores and reads values in DynamoDB as lists of attribute values.
 *
 * <p>
 * <b>Element Conversion:</b> This uses the {@link ConversionContext#attributeConverter()} to convert the collection
 * contents to and from attribute values. Because of this, the client or item must be configured with a converter for
 * the element type, or an exception will be raised. If you wish to statically configure the element type converter, use
 * {@link CollectionAttributeConverter}.
 *
 * <p>
 * <b>Element Type:</b> To delegate element conversion to the {@link ConversionContext#attributeConverter()}, the type of the
 * element must be determined. For converting from Java collections to DynamoDB collections, the element's {@link Class}
 * is used. For converting from DynamoDB collections to Java collections, the output type is selected by the requested attribute
 * type. For {@link MappedTable}s, this is the type associated with the attribute. For {@link Table}s, this is the type passed
 * to the {@link ConvertibleItemAttributeValue#as(TypeToken)} method. If the type has a single type parameter (e.g.
 * {@code List<String>}) the element type is assumed to be equivalent to the type parameter. If the type has zero or multiple
 * type parameters, the {@code Collection} implementation must either implement the {@link ElementTypeAwareCollection} or
 * {@link GenericConvertibleMap} interfaces.
 *
 * <p>
 * <b>Collection Type:</b> When converting DynamoDB collections to Java collections, this converter must automatically create
 * an instance of the collection. This is done via the following process:
 * <ol>
 *     <li>Check for a constructor that was specified when the converter was created via
 *     {@link Builder#putCollectionConstructor(Class, Supplier)}.</li>
 *     <li>Check for a built-in constructor. A constructor is currently built-in for the following types: {@link Collection},
 *     {@link List}, {@link Set}, {@link SortedSet}, {@link Queue}, {@link Deque}, {@link NavigableSet}, {@link BlockingQueue},
 *     {@link BlockingDeque}, and {@link TransferQueue}.</li>
 *     <li>Check for a zero-argument method on the requested collection type.</li>
 * </ol>
 *
 * <p>
 * This can be created via {@link #create()}.
 *
 * @see MapSubtypeAttributeConverter
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class CollectionSubtypeAttributeConverter implements SubtypeAttributeConverter<Collection<?>> {
    private static final TypeToken<Collection<?>> TYPE = new TypeToken<Collection<?>>(){};
    private static final Map<Class<?>, Supplier<Collection<Object>>> TYPE_CONSTRUCTORS;
    private final Map<Class<?>, Supplier<Collection<Object>>> additionalTypeConstructors;

    static {
        Map<Class<?>, Supplier<Collection<Object>>> constructors = new HashMap<>();
        constructors.put(Collection.class, ArrayList::new);
        constructors.put(List.class, ArrayList::new);
        constructors.put(Set.class, LinkedHashSet::new);
        constructors.put(SortedSet.class, TreeSet::new);
        constructors.put(Queue.class, ArrayDeque::new);
        constructors.put(Deque.class, ArrayDeque::new);
        constructors.put(NavigableSet.class, TreeSet::new);
        constructors.put(BlockingQueue.class, LinkedBlockingQueue::new);
        constructors.put(BlockingDeque.class, LinkedBlockingDeque::new);
        constructors.put(TransferQueue.class, LinkedTransferQueue::new);
        TYPE_CONSTRUCTORS = Collections.unmodifiableMap(constructors);
    }

    private CollectionSubtypeAttributeConverter(Builder builder) {
        this.additionalTypeConstructors = Collections.unmodifiableMap(new HashMap<>(builder.additionalCollectionConstructors));
    }

    /**
     * Create a {@link CollectionSubtypeAttributeConverter} with default options set. {@link #builder()} can be used to configure
     * the way in which collection types are created.
     */
    public static CollectionSubtypeAttributeConverter create() {
        return builder().build();
    }

    /**
     * Create a {@link Builder} for defining a {@link CollectionSubtypeAttributeConverter} with custom configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public TypeToken<Collection<?>> type() {
        return TYPE;
    }

    @Override
    public ItemAttributeValue toAttributeValue(Collection<?> input, ConversionContext context) {
        List<ItemAttributeValue> attributeValues = new ArrayList<>();
        for (Object object : input) {
            attributeValues.add(context.attributeConverter().toAttributeValue(object, context));
        }
        return ItemAttributeValue.fromListOfAttributeValues(attributeValues);
    }

    @Override
    public <U extends Collection<?>> U fromAttributeValue(ItemAttributeValue input,
                                                          TypeToken<U> collectionType,
                                                          ConversionContext context) {
        return input.convert(new TypeConvertingVisitor<U>(collectionType.rawClass(), CollectionSubtypeAttributeConverter.class) {
            @Override
            public U convertSetOfStrings(List<String> value) {
                return convertCollection(value, ItemAttributeValue::fromString);
            }

            @Override
            public U convertSetOfNumbers(List<String> value) {
                return convertCollection(value, ItemAttributeValue::fromNumber);
            }

            @Override
            public U convertSetOfBytes(List<SdkBytes> value) {
                return convertCollection(value, ItemAttributeValue::fromBytes);
            }

            @Override
            public U convertListOfAttributeValues(List<ItemAttributeValue> value) {
                return convertCollection(value, Function.identity());
            }

            private <V> U convertCollection(Collection<V> collection,
                                            Function<V, ItemAttributeValue> toAttributeValueFunction) {
                Collection<Object> result = create(collectionType.rawClass());
                TypeToken<?> parameterType = getElementType(result, collectionType);

                collection.stream()
                          .map(toAttributeValueFunction)
                          .map(v -> context.attributeConverter().fromAttributeValue(v, parameterType, context))
                          .forEach(result::add);

                // This is a safe cast - We know the values we added to the list match the type that the customer requested.
                return (U) result;
            }
        });
    }

    private <U extends Collection<?>> TypeToken<?> getElementType(Collection<Object> result, TypeToken<U> collectionType) {
        if (result instanceof ElementTypeAwareCollection) {
            TypeToken<?> elementType = ((ElementTypeAwareCollection) result).elementConversionType();
            Validate.notNull(elementType, "Invalid element conversion type (null) for collection (%s).", collectionType);
            return elementType;
        }

        List<TypeToken<?>> listTypeParameters = collectionType.rawClassParameters();

        int typeIndex;
        if (result instanceof GenericConvertibleCollection) {
            typeIndex = ((GenericConvertibleCollection) result).elementConversionTypeIndex();
            Validate.isTrue(0 <= typeIndex && typeIndex < listTypeParameters.size(),
                            "Invalid element conversion type index (%s) for collection (%s). Must be between 0 and %s.",
                            typeIndex, collectionType, listTypeParameters.size());
        } else {
            typeIndex = 0;
            Validate.isTrue(listTypeParameters.size() == 1,
                            "Cannot determine element type for the requested collection type: %s. If you're using bean-based " +
                            "transformation, be sure you're annotating your collection with the @AttributeElementType. If you're " +
                            "specifying a type token, make sure you are specifying an element type (e.g. " +
                            "TypeToken.listOf(String.class) instead of TypeToken.of(List.class). If you're using a custom " +
                            "collection type without the element type as a parameter, it will need to implement " +
                            "ElementTypeAwareCollection to specify the element type. If you're using a custom collection " +
                            "type with the element type as a parameter, but there is more than one parameter, it will " +
                            "need to implement GenericConvertibleCollection to specify which type parameter is " +
                            "encoding the element type.", collectionType);
        }

        return listTypeParameters.get(typeIndex);
    }

    private Collection<Object> create(Class<? extends Collection<?>> listType) {
        Supplier<Collection<Object>> constructor = additionalTypeConstructors.get(listType);
        if (constructor != null) {
            return constructor.get();
        }

        constructor = TYPE_CONSTRUCTORS.get(listType);
        if (constructor != null) {
            return constructor.get();
        }

        try {
            //  This cast is safe, because we'll only add the requested types to it.
            return (Collection<Object>) listType.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | SecurityException e) {
            throw new IllegalStateException("Failed to instantiate the requested type " + listType.getTypeName() + ".", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Requested type " + listType.getTypeName() + " is not supported, because it " +
                                            "does not have a public, callable zero-arg constructor. Either add such a " +
                                            "constructor to the object or configure the CollectionSubtypeAttributeConverter " +
                                            "with a construction method to use instead.", e);
        }
    }

    /**
     * A builder for configuring and creating {@link CollectionSubtypeAttributeConverter}s.
     */
    public static final class Builder {
        private Map<Class<?>, Supplier<Collection<Object>>> additionalCollectionConstructors = new HashMap<>();

        private Builder() {}

        /**
         * Add an additional constructor to be used for creating an instance of the specified type. When the specified type is
         * requested in the {@link #fromAttributeValue} methods, this constructor will be used instead of the built-in behavior.
         */
        public <T extends Collection<?>> Builder putCollectionConstructor(Class<T> type, Supplier<? extends T> constructor) {
            this.additionalCollectionConstructors.put(type, (Supplier<Collection<Object>>) constructor);
            return this;
        }

        public CollectionSubtypeAttributeConverter build() {
            return new CollectionSubtypeAttributeConverter(this);
        }
    }
}
