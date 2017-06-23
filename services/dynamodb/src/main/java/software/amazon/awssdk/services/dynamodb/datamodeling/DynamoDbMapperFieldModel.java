/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbAutoGenerateStrategy.ALWAYS;
import static software.amazon.awssdk.services.dynamodb.datamodeling.StandardTypeConverters.Vector.LIST;
import static software.amazon.awssdk.services.dynamodb.model.ComparisonOperator.BEGINS_WITH;
import static software.amazon.awssdk.services.dynamodb.model.ComparisonOperator.BETWEEN;
import static software.amazon.awssdk.services.dynamodb.model.ComparisonOperator.CONTAINS;
import static software.amazon.awssdk.services.dynamodb.model.ComparisonOperator.EQ;
import static software.amazon.awssdk.services.dynamodb.model.ComparisonOperator.GE;
import static software.amazon.awssdk.services.dynamodb.model.ComparisonOperator.GT;
import static software.amazon.awssdk.services.dynamodb.model.ComparisonOperator.IN;
import static software.amazon.awssdk.services.dynamodb.model.ComparisonOperator.LE;
import static software.amazon.awssdk.services.dynamodb.model.ComparisonOperator.LT;
import static software.amazon.awssdk.services.dynamodb.model.ComparisonOperator.NE;
import static software.amazon.awssdk.services.dynamodb.model.ComparisonOperator.NOT_CONTAINS;
import static software.amazon.awssdk.services.dynamodb.model.ComparisonOperator.NOT_NULL;
import static software.amazon.awssdk.services.dynamodb.model.ComparisonOperator.NULL;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.KeyType;

/**
 * Field model.
 *
 * @param <T> The object type.
 * @param <V> The field model type.
 */
public class DynamoDbMapperFieldModel<T, V> implements DynamoDbAutoGenerator<V>, DynamoDbTypeConverter<AttributeValue, V> {

    private final DynamoDbMapperFieldModel.Properties<V> properties;


    private final DynamoDbTypeConverter<AttributeValue, V> converter;
    private final DynamoDbAttributeType attributeType;
    private final DynamoDbMapperFieldModel.Reflect<T, V> reflect;

    /**
     * Creates a new field model instance.
     * @param builder The builder.
     */
    private DynamoDbMapperFieldModel(final DynamoDbMapperFieldModel.Builder<T, V> builder) {
        this.properties = builder.properties;
        this.converter = builder.converter;
        this.attributeType = builder.attributeType;
        this.reflect = builder.reflect;
    }

    /**
     * @deprecated replaced by {@link DynamoDbMapperFieldModel#name}
     */
    @Deprecated
    public String getDynamoDbAttributeName() {
        return properties.attributeName();
    }

    /**
     * @deprecated replaced by {@link DynamoDbMapperFieldModel#attributeType}
     */
    @Deprecated
    public DynamoDbAttributeType getDynamoDbAttributeType() {
        return attributeType;
    }

    /**
     * Gets the attribute name.
     * @return The attribute name.
     */
    public final String name() {
        return properties.attributeName();
    }

    /**
     * Gets the value from the object instance.
     * @param object The object instance.
     * @return The value.
     */
    public final V get(final T object) {
        return reflect.get(object);
    }

    /**
     * Sets the value on the object instance.
     * @param object The object instance.
     * @param value The value.
     */
    public final void set(final T object, final V value) {
        reflect.set(object, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final DynamoDbAutoGenerateStrategy getGenerateStrategy() {
        if (properties.autoGenerator() != null) {
            return properties.autoGenerator().getGenerateStrategy();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final V generate(final V currentValue) {
        return properties.autoGenerator().generate(currentValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final AttributeValue convert(final V object) {
        AttributeValue v = converter.convert(object);
        return v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final V unconvert(final AttributeValue object) {
        return converter.unconvert(object);
    }

    /**
     * Get the current value from the object and convert it.
     * @param object The object instance.
     * @return The converted value.
     */
    public final AttributeValue getAndConvert(final T object) {
        return convert(get(object));
    }

    /**
     * Unconverts the value and sets it on the object.
     * @param object The object instance.
     * @param value The attribute value.
     */
    public final void unconvertAndSet(final T object, final AttributeValue value) {
        set(object, unconvert(value));
    }

    /**
     * Gets the DynamoDB attribute type.
     * @return The DynamoDB attribute type.
     */
    public final DynamoDbAttributeType attributeType() {
        return attributeType;
    }

    /**
     * Gets the key type.
     * @return The key type if a key field, null otherwise.
     */
    public final KeyType keyType() {
        return properties.keyType();
    }

    /**
     * Indicates if this attribute is a version attribute.
     * @return True if it is, false otherwise.
     */
    public final boolean versioned() {
        return properties.versioned();
    }

    /**
     * Gets the global secondary indexes.
     * @param keyType The key type.
     * @return The list of global secondary indexes.
     */
    public final List<String> globalSecondaryIndexNames(final KeyType keyType) {
        if (properties.globalSecondaryIndexNames().containsKey(keyType)) {
            return properties.globalSecondaryIndexNames().get(keyType);
        }
        return Collections.emptyList();
    }

    /**
     * Gets the local secondary indexes.
     * @return The list of local secondary indexes.
     */
    public final List<String> localSecondaryIndexNames() {
        return properties.localSecondaryIndexNames();
    }

    /**
     * Returns true if the field has any indexes.
     * @return True if the propery matches.
     */
    public final boolean indexed() {
        return !properties.globalSecondaryIndexNames().isEmpty() || !properties.localSecondaryIndexNames().isEmpty();
    }

    /**
     * Creates a condition which filters on the specified value.
     * @param value The value.
     * @return The condition.
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#BEGINS_WITH
     * @see software.amazon.awssdk.services.dynamodb.model.Condition
     */
    public final Condition beginsWith(final V value) {
        return Condition.builder().comparisonOperator(BEGINS_WITH).attributeValueList(convert(value)).build();
    }

    /**
     * Creates a condition which filters on the specified values.
     * @param lo The start of the range (inclusive).
     * @param hi The end of the range (inclusive).
     * @return The condition.
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#BETWEEN
     * @see software.amazon.awssdk.services.dynamodb.model.Condition
     */
    public final Condition between(final V lo, final V hi) {
        return Condition.builder().comparisonOperator(BETWEEN).attributeValueList(convert(lo), convert(hi)).build();
    }

    /**
     * Creates a condition which filters on the specified value.
     * @param value The value.
     * @return The condition.
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#CONTAINS
     * @see software.amazon.awssdk.services.dynamodb.model.Condition
     */
    public final Condition contains(final V value) {
        return Condition.builder().comparisonOperator(CONTAINS).attributeValueList(convert(value)).build();
    }

    /**
     * Creates a condition which filters on the specified value.
     * @param value The value.
     * @return The condition.
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#EQ
     * @see software.amazon.awssdk.services.dynamodb.model.Condition
     */
    public final Condition eq(final V value) {
        return Condition.builder().comparisonOperator(EQ).attributeValueList(convert(value)).build();
    }

    /**
     * Creates a condition which filters on the specified value.
     * @param value The value.
     * @return The condition.
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#GE
     * @see software.amazon.awssdk.services.dynamodb.model.Condition
     */
    public final Condition ge(final V value) {
        return Condition.builder().comparisonOperator(GE).attributeValueList(convert(value)).build();
    }

    /**
     * Creates a condition which filters on the specified value.
     * @param value The value.
     * @return The condition.
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#GT
     * @see software.amazon.awssdk.services.dynamodb.model.Condition
     */
    public final Condition gt(final V value) {
        return Condition.builder().comparisonOperator(GT).attributeValueList(convert(value)).build();
    }

    /**
     * Creates a condition which filters on the specified values.
     * @param values The values.
     * @return The condition.
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#IN
     * @see software.amazon.awssdk.services.dynamodb.model.Condition
     */
    public final Condition in(final Collection<V> values) {
        return Condition.builder().comparisonOperator(IN).attributeValueList(LIST.convert(values, this)).build();
    }

    /**
     * Creates a condition which filters on the specified values.
     * @param values The values.
     * @return The condition.
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#IN
     * @see software.amazon.awssdk.services.dynamodb.model.Condition
     */
    public final Condition in(final V... values) {
        return in(Arrays.asList(values));
    }

    /**
     * Creates a condition which filters on the specified value.
     * @return The condition.
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#NULL
     * @see software.amazon.awssdk.services.dynamodb.model.Condition
     */
    public final Condition isNull() {
        return Condition.builder().comparisonOperator(NULL).build();
    }

    /**
     * Creates a condition which filters on the specified value.
     * @param value The value.
     * @return The condition.
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#LE
     * @see software.amazon.awssdk.services.dynamodb.model.Condition
     */
    public final Condition le(final V value) {
        return Condition.builder().comparisonOperator(LE).attributeValueList(convert(value)).build();
    }

    /**
     * Creates a condition which filters on the specified value.
     * @param value The value.
     * @return The condition.
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#LT
     * @see software.amazon.awssdk.services.dynamodb.model.Condition
     */
    public final Condition lt(final V value) {
        return Condition.builder().comparisonOperator(LT).attributeValueList(convert(value)).build();
    }

    /**
     * Creates a condition which filters on the specified value.
     * @param value The value.
     * @return The condition.
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#NE
     * @see software.amazon.awssdk.services.dynamodb.model.Condition
     */
    public final Condition ne(final V value) {
        return Condition.builder().comparisonOperator(NE).attributeValueList(convert(value)).build();
    }

    /**
     * Creates a condition which filters on the specified value.
     * @param value The value.
     * @return The condition.
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#NOT_CONTAINS
     * @see software.amazon.awssdk.services.dynamodb.model.Condition
     */
    public final Condition notContains(final V value) {
        return Condition.builder().comparisonOperator(NOT_CONTAINS).attributeValueList(convert(value)).build();
    }

    /**
     * Creates a condition which filters on the specified value.
     * @return The condition.
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#NOT_NULL
     * @see software.amazon.awssdk.services.dynamodb.model.Condition
     */
    public final Condition notNull() {
        return Condition.builder().comparisonOperator(NOT_NULL).build();
    }

    /**
     * Creates a condition which filters on any non-null argument; if {@code lo}
     * is null a {@code LE} condition is applied on {@code hi}, if {@code hi}
     * is null a {@code GE} condition is applied on {@code lo}.
     * @param lo The start of the range (inclusive).
     * @param hi The end of the range (inclusive).
     * @return The condition or null if both arguments are null.
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#BETWEEN
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#EQ
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#GE
     * @see software.amazon.awssdk.services.dynamodb.model.ComparisonOperator#LE
     * @see software.amazon.awssdk.services.dynamodb.model.Condition
     */
    public final Condition betweenAny(final V lo, final V hi) {
        return lo == null ? (hi == null ? null : le(hi)) : (hi == null ? ge(lo) : (lo.equals(hi) ? eq(lo) : between(lo, hi)));
    }

    public static enum DynamoDbAttributeType {
        B, N, S, BS, NS, SS, BOOL, NULL, L, M
    }

    /**
     * The field model properties.
     */
    static interface Properties<V> {
        public String attributeName();

        public KeyType keyType();

        public boolean versioned();

        public Map<KeyType, List<String>> globalSecondaryIndexNames();

        public List<String> localSecondaryIndexNames();

        public DynamoDbAutoGenerator<V> autoGenerator();

        static final class Immutable<V> implements Properties<V> {
            private final String attributeName;
            private final KeyType keyType;
            private final boolean versioned;
            private final Map<KeyType, List<String>> globalSecondaryIndexNames;
            private final List<String> localSecondaryIndexNames;
            private final DynamoDbAutoGenerator<V> autoGenerator;

            public Immutable(final Properties<V> properties) {
                this.attributeName = properties.attributeName();
                this.keyType = properties.keyType();
                this.versioned = properties.versioned();
                this.globalSecondaryIndexNames = properties.globalSecondaryIndexNames();
                this.localSecondaryIndexNames = properties.localSecondaryIndexNames();
                this.autoGenerator = properties.autoGenerator();
            }

            @Override
            public final String attributeName() {
                return this.attributeName;
            }

            @Override
            public final KeyType keyType() {
                return this.keyType;
            }

            @Override
            public final boolean versioned() {
                return this.versioned;
            }

            @Override
            public final Map<KeyType, List<String>> globalSecondaryIndexNames() {
                return this.globalSecondaryIndexNames;
            }

            @Override
            public final List<String> localSecondaryIndexNames() {
                return this.localSecondaryIndexNames;
            }

            @Override
            public final DynamoDbAutoGenerator<V> autoGenerator() {
                return this.autoGenerator;
            }
        }
    }

    /**
     * Get/set reflection operations.
     * @param <T> The object type.
     * @param <V> The value type.
     */
    static interface Reflect<T, V> {
        public V get(T object);

        public void set(T object, V value);
    }

    /**
     * {@link DynamoDbMapperFieldModel} builder.
     */
    static class Builder<T, V> {
        private final DynamoDbMapperFieldModel.Properties<V> properties;
        private DynamoDbTypeConverter<AttributeValue, V> converter;
        private DynamoDbMapperFieldModel.Reflect<T, V> reflect;
        private DynamoDbAttributeType attributeType;
        private Class<T> targetType;

        public Builder(Class<T> targetType, DynamoDbMapperFieldModel.Properties<V> properties) {
            this.properties = properties;
            this.targetType = targetType;
        }

        public final Builder<T, V> with(DynamoDbTypeConverter<AttributeValue, V> converter) {
            this.converter = converter;
            return this;
        }

        public final Builder<T, V> with(DynamoDbAttributeType attributeType) {
            this.attributeType = attributeType;
            return this;
        }

        public final Builder<T, V> with(DynamoDbMapperFieldModel.Reflect<T, V> reflect) {
            this.reflect = reflect;
            return this;
        }

        public final DynamoDbMapperFieldModel<T, V> build() {
            final DynamoDbMapperFieldModel<T, V> result = new DynamoDbMapperFieldModel<T, V>(this);
            if ((result.keyType() != null || result.indexed()) && !result.attributeType().name().matches("[BNS]")) {
                throw new DynamoDbMappingException(String.format(
                        "%s[%s]; only scalar (B, N, or S) type allowed for key",
                        targetType.getSimpleName(), result.name()
                                                                ));
            } else if (result.keyType() != null && result.getGenerateStrategy() == ALWAYS) {
                throw new DynamoDbMappingException(String.format(
                        "%s[%s]; auto-generated key and ALWAYS not allowed",
                        targetType.getSimpleName(), result.name()
                                                                ));
            }
            return result;
        }
    }

}
