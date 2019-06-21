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

import static java.util.stream.Collectors.toList;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;


/**
 * A converter between a specific {@link Collection} type and {@link ItemAttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a list of attribute values. This uses a configured {@link AttributeConverter} to convert
 * the collection contents to an attribute value.
 *
 * <p>
 * This supports reading a list of attribute values. This uses a configured {@link AttributeConverter} to convert
 * the collection contents.
 *
 * <p>
 * A builder is exposed to allow defining how the collection and element types are created and converted:
 * <code>
 * AttributeConverter<List<Integer>> listConverter =
 *         CollectionAttributeConverter.builder(TypeToken.listOf(Integer.class))
 *                                           .collectionConstructor(ArrayList::new)
 *                                           .elementConverter(IntegerAttributeConverter.create())
 *                                           .build()
 * </code>
 *
 * <p>
 * For frequently-used types, static methods are exposed to reduce the amount of boilerplate involved in creation:
 * <code>
 * AttributeConverter<List<Integer>> listConverter =
 *         CollectionAttributeConverter.listConverter(IntegerAttributeConverter.create());
 *
 * AttributeConverter<Collection<Integer>> collectionConverer =
 *         CollectionAttributeConverter.collectionConverter(IntegerAttributeConverter.create());
 *
 * AttributeConverter<Set<Integer>> setConverter =
 *         CollectionAttributeConverter.setConverter(IntegerAttributeConverter.create());
 *
 * AttributeConverter<SortedSet<Integer>> sortedSetConverter =
 *         CollectionAttributeConverter.sortedSetConverter(IntegerAttributeConverter.create());
 * </code>
 *
 * @see MapAttributeConverter
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public class CollectionAttributeConverter<T extends Collection<?>> implements AttributeConverter<T> {
    private final Delegate<T, ?> delegate;

    private CollectionAttributeConverter(Delegate<T, ?> delegate) {
        this.delegate = delegate;
    }

    public static <U> CollectionAttributeConverter<Set<U>> setConverter(AttributeConverter<U> elementConverter) {
        return builder(TypeToken.setOf(elementConverter.type()))
                .collectionConstructor(LinkedHashSet::new)
                .elementConverter(elementConverter)
                .build();
    }

    public static <U> CollectionAttributeConverter<SortedSet<U>> sortedSetConverter(AttributeConverter<U> elementConverter) {
        return builder(TypeToken.sortedSetOf(elementConverter.type()))
                .collectionConstructor(TreeSet::new)
                .elementConverter(elementConverter)
                .build();
    }

    public static <U> CollectionAttributeConverter<List<U>> listConverter(AttributeConverter<U> elementConverter) {
        return builder(TypeToken.listOf(elementConverter.type()))
                .collectionConstructor(ArrayList::new)
                .elementConverter(elementConverter)
                .build();
    }

    public static <U> CollectionAttributeConverter<Collection<U>> collectionConverter(AttributeConverter<U> elementConverter) {
        return builder(TypeToken.collectionOf(elementConverter.type()))
                .collectionConstructor(ArrayList::new)
                .elementConverter(elementConverter)
                .build();
    }

    public static <U> CollectionAttributeConverter<Queue<U>> queueConverter(AttributeConverter<U> elementConverter) {
        return builder(TypeToken.queueOf(elementConverter.type()))
                .collectionConstructor(ArrayDeque::new)
                .elementConverter(elementConverter)
                .build();
    }

    public static <U> CollectionAttributeConverter<Deque<U>> dequeConverter(AttributeConverter<U> elementConverter) {
        return builder(TypeToken.dequeOf(elementConverter.type()))
                .collectionConstructor(ArrayDeque::new)
                .elementConverter(elementConverter)
                .build();
    }

    public static <U> CollectionAttributeConverter<NavigableSet<U>> navigableSetConverter(
            AttributeConverter<U> elementConverter) {
        return builder(TypeToken.navigableSetOf(elementConverter.type()))
                .collectionConstructor(TreeSet::new)
                .elementConverter(elementConverter)
                .build();
    }

    public static <T extends Collection<U>, U> CollectionAttributeConverter.Builder<T, U> builder(TypeToken<T> collectionType) {
        return new Builder<>(collectionType);
    }

    @Override
    public TypeToken<T> type() {
        return delegate.type();
    }

    @Override
    public ItemAttributeValue toAttributeValue(T input, ConversionContext context) {
        return delegate.toAttributeValue(input, context);
    }

    @Override
    public T fromAttributeValue(ItemAttributeValue input, ConversionContext context) {
        return delegate.fromAttributeValue(input, context);
    }

    private static final class Delegate<T extends Collection<U>, U> implements AttributeConverter<T> {
        private final TypeToken<T> collectionType;
        private final Supplier<? extends T> collectionConstructor;
        private final AttributeConverter<U> elementConverter;

        private Delegate(Builder<T, U> builder) {
            this.collectionType = builder.collectionType;
            this.collectionConstructor = builder.collectionConstructor;
            this.elementConverter = builder.elementConverter;
        }

        @Override
        public TypeToken<T> type() {
            return collectionType;
        }

        @Override
        public ItemAttributeValue toAttributeValue(T input, ConversionContext context) {
            return ItemAttributeValue.fromListOfAttributeValues(input.stream()
                                                                     .map(e -> elementConverter.toAttributeValue(e, context))
                                                                     .collect(toList()));
        }

        @Override
        public T fromAttributeValue(ItemAttributeValue input,
                                     ConversionContext context) {
            return input.convert(new TypeConvertingVisitor<T>(Collection.class, CollectionAttributeConverter.class) {
                @Override
                public T convertListOfAttributeValues(List<ItemAttributeValue> value) {
                    return value.stream()
                                .map(attribute -> elementConverter.fromAttributeValue(attribute, context))
                                .collect(Collectors.toCollection(collectionConstructor));
                }
            });
        }
    }

    public static final class Builder<T extends Collection<U>, U> {
        private final TypeToken<T> collectionType;
        private Supplier<? extends T> collectionConstructor;
        private AttributeConverter<U> elementConverter;

        private Builder(TypeToken<T> collectionType) {
            this.collectionType = collectionType;
        }

        public Builder<T, U> collectionConstructor(Supplier<? extends T> collectionConstructor) {
            this.collectionConstructor = collectionConstructor;
            return this;
        }

        public Builder<T, U> elementConverter(AttributeConverter<U> elementConverter) {
            this.elementConverter = elementConverter;
            return this;
        }

        public CollectionAttributeConverter<T> build() {
            return new CollectionAttributeConverter<>(new Delegate<>(this));
        }
    }
}
