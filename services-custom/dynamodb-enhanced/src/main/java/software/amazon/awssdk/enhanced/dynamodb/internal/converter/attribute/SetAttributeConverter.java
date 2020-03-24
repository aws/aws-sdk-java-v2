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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;


/**
 * A converter between a specific {@link Collection} type and {@link EnhancedAttributeValue}.
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
 * CollectionAttributeConverter.builder(EnhancedType.listOf(Integer.class))
 * .collectionConstructor(ArrayList::new)
 * .elementConverter(IntegerAttributeConverter.create())
 * .build()
 * </code>
 *
 * <p>
 * For frequently-used types, static methods are exposed to reduce the amount of boilerplate involved in creation:
 * <code>
 * AttributeConverter<List<Integer>> listConverter =
 * CollectionAttributeConverter.listConverter(IntegerAttributeConverter.create());
 * <p>
 * AttributeConverter<Collection<Integer>> collectionConverer =
 * CollectionAttributeConverter.collectionConverter(IntegerAttributeConverter.create());
 * <p>
 * AttributeConverter<Set<Integer>> setConverter =
 * CollectionAttributeConverter.setConverter(IntegerAttributeConverter.create());
 * <p>
 * AttributeConverter<SortedSet<Integer>> sortedSetConverter =
 * CollectionAttributeConverter.sortedSetConverter(IntegerAttributeConverter.create());
 * </code>
 *
 * @see MapAttributeConverter
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public class SetAttributeConverter<T extends Collection<?>> implements AttributeConverter<T> {
    private final Delegate<T, ?> delegate;

    private SetAttributeConverter(Delegate<T, ?> delegate) {
        this.delegate = delegate;
    }

    public static <U> SetAttributeConverter<Set<U>> setConverter(AttributeConverter<U> elementConverter) {
        return builder(EnhancedType.setOf(elementConverter.type()))
            .collectionConstructor(LinkedHashSet::new)
            .elementConverter(elementConverter)
            .build();
    }

    public static <T extends Collection<U>, U> SetAttributeConverter.Builder<T, U> builder(EnhancedType<T> collectionType) {
        return new Builder<>(collectionType);
    }

    @Override
    public EnhancedType<T> type() {
        return delegate.type();
    }

    @Override
    public AttributeValueType attributeValueType() {
        return delegate.attributeValueType();
    }

    @Override
    public AttributeValue transformFrom(T input) {
        return delegate.transformFrom(input);
    }

    @Override
    public T transformTo(AttributeValue input) {
        return delegate.transformTo(input);
    }

    private static final class Delegate<T extends Collection<U>, U> implements AttributeConverter<T> {
        private final EnhancedType<T> type;
        private final Supplier<? extends T> collectionConstructor;
        private final AttributeConverter<U> elementConverter;
        private final AttributeValueType attributeValueType;

        private Delegate(Builder<T, U> builder) {
            this.type = builder.collectionType;
            this.collectionConstructor = builder.collectionConstructor;
            this.elementConverter = builder.elementConverter;
            this.attributeValueType = attributeValueTypeForSet(this.elementConverter);
        }

        @Override
        public EnhancedType<T> type() {
            return type;
        }

        @Override
        public AttributeValueType attributeValueType() {
            return attributeValueType;
        }

        @Override
        public AttributeValue transformFrom(T input) {
            return EnhancedAttributeValue.flatten(input.stream()
                                                       .map(e -> EnhancedAttributeValue.fromAttributeValue(
                                                           elementConverter.transformFrom(e)))
                                                       .collect(toList()))
                                         .toAttributeValue();
        }

        @Override
        public T transformTo(AttributeValue input) {
            return EnhancedAttributeValue.fromAttributeValue(input)
                                         .convert(new TypeConvertingVisitor<T>(type.rawClass(), SetAttributeConverter.class) {
                                             @Override
                                             public T convertSetOfStrings(List<String> value) {
                                                 return convertCollection(value, EnhancedAttributeValue::fromString);
                                             }

                                             @Override
                                             public T convertSetOfNumbers(List<String> value) {
                                                 return convertCollection(value, EnhancedAttributeValue::fromNumber);
                                             }

                                             @Override
                                             public T convertSetOfBytes(List<SdkBytes> value) {
                                                 return convertCollection(value, EnhancedAttributeValue::fromBytes);
                                             }

                                             @Override
                                             public T convertListOfAttributeValues(List<EnhancedAttributeValue> value) {
                                                 return convertCollection(value, Function.identity());
                                             }

                                             private <V> T convertCollection(Collection<V> collection,
                                                                             Function<V, EnhancedAttributeValue> transformFrom) {
                                                 Collection<Object> result = (Collection<Object>) collectionConstructor.get();

                                                 collection.stream()
                                                           .map(transformFrom)
                                                           .map(v -> elementConverter.transformTo(v.toAttributeValue()))
                                                           .forEach(result::add);

                                                 // This is a safe cast - We know the values we added to the
                                                 // set match the type that the customer requested.
                                                 return (T) result;
                                             }
                                         });
        }

        private AttributeValueType attributeValueTypeForSet(AttributeConverter<U> innerType) {
            switch (innerType.attributeValueType()) {
                case N:
                    return AttributeValueType.NS;
                case S:
                    return AttributeValueType.SS;
                case B:
                    return AttributeValueType.BS;
                default:
                    throw new IllegalArgumentException(
                        String.format("SetAttributeConverter cannot be created with a parameterized type of '%s'. " +
                                          "Supported parameterized types must convert to B, S or N DynamoDB " +
                                          "AttributeValues.", innerType.type().rawClass()));
            }
        }
    }

    public static final class Builder<T extends Collection<U>, U> {
        private final EnhancedType<T> collectionType;
        private Supplier<? extends T> collectionConstructor;
        private AttributeConverter<U> elementConverter;

        private Builder(EnhancedType<T> collectionType) {
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

        public SetAttributeConverter<T> build() {
            return new SetAttributeConverter<>(new Delegate<>(this));
        }
    }
}
