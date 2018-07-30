/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static java.util.stream.Collectors.toList;
import static software.amazon.awssdk.services.dynamodb.datamodeling.StandardTypeConverters.Scalar.BOOLEAN;
import static software.amazon.awssdk.services.dynamodb.datamodeling.StandardTypeConverters.Scalar.DEFAULT;
import static software.amazon.awssdk.services.dynamodb.datamodeling.StandardTypeConverters.Scalar.STRING;
import static software.amazon.awssdk.services.dynamodb.datamodeling.StandardTypeConverters.Vector.LIST;
import static software.amazon.awssdk.services.dynamodb.datamodeling.StandardTypeConverters.Vector.MAP;
import static software.amazon.awssdk.services.dynamodb.datamodeling.StandardTypeConverters.Vector.SET;
import static software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType.B;
import static software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType.N;
import static software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType.S;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.ImmutableObjectUtils;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperFieldModel.DynamoDbAttributeType;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperFieldModel.Reflect;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperModelFactory.TableFactory;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTypeConverter.AbstractConverter;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTypeConverter.DelegateConverter;
import software.amazon.awssdk.services.dynamodb.datamodeling.StandardBeanProperties.Bean;
import software.amazon.awssdk.services.dynamodb.datamodeling.StandardBeanProperties.Beans;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Pre-defined strategies for mapping between Java types and DynamoDB types.
 */
@SdkInternalApi
final class StandardModelFactories {

    private static final Logger log = LoggerFactory.getLogger(StandardModelFactories.class);

    /**
     * Creates the standard {@link DynamoDbMapperModelFactory} factory.
     */
    static DynamoDbMapperModelFactory of(S3Link.Factory s3Links) {
        return new StandardModelFactory(s3Links);
    }

    /**
     * Creates a new set of conversion rules based on the configuration.
     */
    private static <T> RuleFactory<T> rulesOf(DynamoDbMapperConfig config, S3Link.Factory s3Links,
                                              DynamoDbMapperModelFactory models) {
        final boolean ver1 = (config.getConversionSchema() == ConversionSchemas.V1);
        final boolean ver2 = (config.getConversionSchema() == ConversionSchemas.V2);
        final boolean v2Compatible = (config.getConversionSchema() == ConversionSchemas.V2_COMPATIBLE);

        final DynamoDbTypeConverterFactory.Builder scalars = config.getTypeConverterFactory().override();
        scalars.with(String.class, S3Link.class, s3Links);

        final Rules<T> factory = new Rules<T>(scalars.build());
        factory.add(factory.new NativeType(!ver1));
        factory.add(factory.new V2CompatibleBool(v2Compatible));
        factory.add(factory.new NativeBool(ver2));
        factory.add(factory.new StringScalar(true));
        factory.add(factory.new NumberScalar(true));
        factory.add(factory.new BinaryScalar(true));
        factory.add(factory.new NativeBoolSet(ver2));
        factory.add(factory.new StringScalarSet(true));
        factory.add(factory.new NumberScalarSet(true));
        factory.add(factory.new BinaryScalarSet(true));
        factory.add(factory.new ObjectSet(ver2));
        factory.add(factory.new ObjectStringSet(!ver2));
        factory.add(factory.new ObjectList(!ver1));
        factory.add(factory.new ObjectMap(!ver1));
        factory.add(factory.new ObjectDocumentMap(!ver1, models, config));
        return factory;
    }

    /**
     * Attribute value conversion.
     */
    interface Rule<T> {
        boolean isAssignableFrom(ConvertibleType<?> type);

        DynamoDbTypeConverter<AttributeValue, T> newConverter(ConvertibleType<T> type);

        DynamoDbAttributeType getAttributeType();
    }

    /**
     * Attribute value conversion factory.
     */
    interface RuleFactory<T> {
        Rule<T> getRule(ConvertibleType<T> type);
    }

    /**
     * {@link TableFactory} mapped by {@link ConversionSchema}.
     */
    private static final class StandardModelFactory implements DynamoDbMapperModelFactory {
        private final ConcurrentMap<ConversionSchema, TableFactory> cache;
        private final S3Link.Factory s3Links;

        private StandardModelFactory(S3Link.Factory s3Links) {
            this.cache = new ConcurrentHashMap<ConversionSchema, TableFactory>();
            this.s3Links = s3Links;
        }

        @Override
        public TableFactory getTableFactory(DynamoDbMapperConfig config) {
            final ConversionSchema schema = config.getConversionSchema();
            if (!cache.containsKey(schema)) {
                RuleFactory<Object> rules = rulesOf(config, s3Links, this);
                rules = new ConversionSchemas.ItemConverterRuleFactory<Object>(config, s3Links, rules);
                cache.putIfAbsent(schema, new StandardTableFactory(rules));
            }
            return cache.get(schema);
        }
    }

    /**
     * {@link DynamoDbMapperTableModel} mapped by the clazz.
     */
    private static final class StandardTableFactory implements TableFactory {
        private final ConcurrentMap<Class<?>, DynamoDbMapperTableModel<?>> cache;
        private final RuleFactory<Object> rules;

        private StandardTableFactory(RuleFactory<Object> rules) {
            this.cache = new ConcurrentHashMap<Class<?>, DynamoDbMapperTableModel<?>>();
            this.rules = rules;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> DynamoDbMapperTableModel<T> getTable(Class<T> clazz) {
            if (!this.cache.containsKey(clazz)) {
                this.cache.putIfAbsent(clazz, new TableBuilder<T>(clazz, rules).build());
            }
            return (DynamoDbMapperTableModel<T>) this.cache.get(clazz);
        }
    }

    /**
     * {@link DynamoDbMapperTableModel} builder.
     */
    private static final class TableBuilder<T> extends DynamoDbMapperTableModel.Builder<T> {
        private TableBuilder(Class<T> clazz, Beans<T> beans, RuleFactory<Object> rules) {
            super(clazz, beans.properties());
            for (final Bean<T, Object> bean : beans.map().values()) {
                try {
                    with(new FieldBuilder<T, Object>(clazz, bean, rules.getRule(bean.type())).build());
                } catch (final RuntimeException e) {
                    throw new DynamoDbMappingException(String.format(
                            "%s[%s] could not be mapped for type %s",
                            clazz.getSimpleName(), bean.properties().attributeName(), bean.type()
                                                                    ), e);
                }
            }
        }

        private TableBuilder(Class<T> clazz, RuleFactory<Object> rules) {
            this(clazz, StandardBeanProperties.<T>of(clazz), rules);
        }
    }

    /**
     * {@link DynamoDbMapperFieldModel} builder.
     */
    private static final class FieldBuilder<T, V> extends DynamoDbMapperFieldModel.Builder<T, V> {
        private FieldBuilder(Class<T> clazz, Bean<T, V> bean, Rule<V> rule) {
            super(clazz, bean.properties());
            if (bean.type().attributeType() != null) {
                with(bean.type().attributeType());
            } else {
                with(rule.getAttributeType());
            }
            with(rule.newConverter(bean.type()));
            with(bean.reflect());
        }
    }

    /**
     * Groups the conversion rules to be evaluated.
     */
    private static final class Rules<T> implements RuleFactory<T> {
        private final Set<Rule<T>> rules = new LinkedHashSet<Rule<T>>();
        private final DynamoDbTypeConverterFactory scalars;

        private Rules(DynamoDbTypeConverterFactory scalars) {
            this.scalars = scalars;
        }

        @SuppressWarnings("unchecked")
        private void add(Rule<?> rule) {
            this.rules.add((Rule<T>) rule);
        }

        @Override
        public Rule<T> getRule(ConvertibleType<T> type) {
            for (final Rule<T> rule : rules) {
                if (rule.isAssignableFrom(type)) {
                    return rule;
                }
            }
            return new NotSupported();
        }

        /**
         * Gets the scalar converter for the given source and target types.
         */
        private <S> DynamoDbTypeConverter<S, T> getConverter(Class<S> sourceType, ConvertibleType<T> type) {
            return scalars.getConverter(sourceType, type.targetType());
        }

        /**
         * Gets the nested converter for the given conversion type.
         * Also wraps the resulting converter with a nullable converter.
         */
        private DynamoDbTypeConverter<AttributeValue, T> getConverter(ConvertibleType<T> type) {
            return new DelegateConverter<AttributeValue, T>(getRule(type).newConverter(type)) {
                public AttributeValue convert(T o) {
                    return o == null ? AttributeValue.builder().nul(true).build() : super.convert(o);
                }
            };
        }

        /**
         * Native {@link AttributeValue} conversion.
         */
        private class NativeType extends AbstractRule<AttributeValue, T> {
            private NativeType(boolean supported) {
                super(DynamoDbAttributeType.NULL, supported);
            }

            @Override
            public boolean isAssignableFrom(ConvertibleType<?> type) {
                return super.supported && type.is(AttributeValue.class);
            }

            @Override
            public DynamoDbTypeConverter<AttributeValue, T> newConverter(ConvertibleType<T> type) {
                return joinAll(type.<AttributeValue>typeConverter());
            }

            @Override
            public AttributeValue get(AttributeValue o) {
                return o;
            }

            @Override
            public void set(AttributeValue value, AttributeValue o) {
                ImmutableObjectUtils.setObjectMember(value, "s", o.s());
                ImmutableObjectUtils.setObjectMember(value, "n", o.n());
                ImmutableObjectUtils.setObjectMember(value, "b", o.b());
                ImmutableObjectUtils.setObjectMember(value, "ss", o.ss());
                ImmutableObjectUtils.setObjectMember(value, "ns", o.ns());
                ImmutableObjectUtils.setObjectMember(value, "bs", o.bs());
                ImmutableObjectUtils.setObjectMember(value, "bool", o.bool());
                ImmutableObjectUtils.setObjectMember(value, "l", o.l());
                ImmutableObjectUtils.setObjectMember(value, "m", o.m());
                ImmutableObjectUtils.setObjectMember(value, "nul", o.nul());
            }
        }

        /**
         * {@code S} conversion
         */
        private class StringScalar extends AbstractRule<String, T> {
            private StringScalar(boolean supported) {
                super(DynamoDbAttributeType.S, supported);
            }

            @Override
            public boolean isAssignableFrom(ConvertibleType<?> type) {
                return super.isAssignableFrom(type) && (type.attributeType() != null || type.is(S));
            }

            @Override
            public DynamoDbTypeConverter<AttributeValue, T> newConverter(ConvertibleType<T> type) {
                return joinAll(getConverter(String.class, type), type.<String>typeConverter());
            }

            @Override
            public String get(AttributeValue value) {
                return value.s();
            }

            @Override
            public void set(AttributeValue value, String o) {
                ImmutableObjectUtils.setObjectMember(value, "s", o);
            }

            @Override
            public AttributeValue convert(String o) {
                return o.length() == 0 ? null : super.convert(o);
            }
        }

        /**
         * {@code N} conversion
         */
        private class NumberScalar extends AbstractRule<String, T> {
            private NumberScalar(boolean supported) {
                super(DynamoDbAttributeType.N, supported);
            }

            @Override
            public boolean isAssignableFrom(ConvertibleType<?> type) {
                return super.isAssignableFrom(type) && (type.attributeType() != null || type.is(N));
            }

            @Override
            public DynamoDbTypeConverter<AttributeValue, T> newConverter(ConvertibleType<T> type) {
                return joinAll(getConverter(String.class, type), type.<String>typeConverter());
            }

            @Override
            public String get(AttributeValue value) {
                return value.n();
            }

            @Override
            public void set(AttributeValue value, String o) {
                ImmutableObjectUtils.setObjectMember(value, "n", o);
                //value.setN(o);
            }
        }

        /**
         * {@code B} conversion
         */
        private class BinaryScalar extends AbstractRule<ByteBuffer, T> {
            private BinaryScalar(boolean supported) {
                super(DynamoDbAttributeType.B, supported);
            }

            @Override
            public boolean isAssignableFrom(ConvertibleType<?> type) {
                return super.isAssignableFrom(type) && (type.attributeType() != null || type.is(B));
            }

            @Override
            public DynamoDbTypeConverter<AttributeValue, T> newConverter(ConvertibleType<T> type) {
                return joinAll(getConverter(ByteBuffer.class, type), type.<ByteBuffer>typeConverter());
            }

            @Override
            public ByteBuffer get(AttributeValue value) {
                return value.b() == null ? null : value.b().asByteBuffer();
            }

            @Override
            public void set(AttributeValue value, ByteBuffer o) {
                ImmutableObjectUtils.setObjectMember(value, "b", SdkBytes.fromByteBuffer(o));
                //value.setB(o);
            }
        }

        /**
         * {@code SS} conversion
         */
        private class StringScalarSet extends AbstractRule<List<String>, Collection<T>> {
            private StringScalarSet(boolean supported) {
                super(DynamoDbAttributeType.SS, supported);
            }

            @Override
            public boolean isAssignableFrom(ConvertibleType<?> type) {
                return super.isAssignableFrom(type) && (type.attributeType() != null || type.is(S, SET));
            }

            @Override
            public DynamoDbTypeConverter<AttributeValue, Collection<T>> newConverter(ConvertibleType<Collection<T>> type) {
                return joinAll(SET.join(getConverter(String.class, type.<T>param(0))), type.<List<String>>typeConverter());
            }

            @Override
            public List<String> get(AttributeValue value) {
                return value.ss();
            }

            @Override
            public void set(AttributeValue value, List<String> o) {
                ImmutableObjectUtils.setObjectMember(value, "ss", o);
                //value.setSS(o);
            }
        }

        /**
         * {@code NS} conversion
         */
        private class NumberScalarSet extends AbstractRule<List<String>, Collection<T>> {
            private NumberScalarSet(boolean supported) {
                super(DynamoDbAttributeType.NS, supported);
            }

            @Override
            public boolean isAssignableFrom(ConvertibleType<?> type) {
                return super.isAssignableFrom(type) && (type.attributeType() != null || type.is(N, SET));
            }

            @Override
            public DynamoDbTypeConverter<AttributeValue, Collection<T>> newConverter(ConvertibleType<Collection<T>> type) {
                return joinAll(SET.join(getConverter(String.class, type.<T>param(0))), type.<List<String>>typeConverter());
            }

            @Override
            public List<String> get(AttributeValue value) {
                return value.ns();
            }

            @Override
            public void set(AttributeValue value, List<String> o) {
                ImmutableObjectUtils.setObjectMember(value, "ns", o);
                //value.setNS(o);
            }
        }

        /**
         * {@code BS} conversion
         */
        private class BinaryScalarSet extends AbstractRule<List<ByteBuffer>, Collection<T>> {
            private BinaryScalarSet(boolean supported) {
                super(DynamoDbAttributeType.BS, supported);
            }

            @Override
            public boolean isAssignableFrom(ConvertibleType<?> type) {
                return super.isAssignableFrom(type) && (type.attributeType() != null || type.is(B, SET));
            }

            @Override
            public DynamoDbTypeConverter<AttributeValue, Collection<T>> newConverter(ConvertibleType<Collection<T>> type) {
                return joinAll(SET.join(getConverter(ByteBuffer.class, type.param(0))), type.typeConverter());
            }

            @Override
            public List<ByteBuffer> get(AttributeValue value) {
                return Optional.ofNullable(value.bs())
                               .map(bs -> bs.stream()
                                            .map(SdkBytes::asByteBuffer)
                                            .collect(toList()))
                               .orElse(null);
            }

            @Override
            public void set(AttributeValue value, List<ByteBuffer> o) {
                ImmutableObjectUtils.setObjectMember(value, "bs", o.stream().map(SdkBytes::fromByteBuffer).collect(toList()));
                //value.setBS(o);
            }
        }

        /**
         * {@code SS} conversion
         */
        private class ObjectStringSet extends StringScalarSet {
            private ObjectStringSet(boolean supported) {
                super(supported);
            }

            @Override
            public boolean isAssignableFrom(ConvertibleType<?> type) {
                return type.attributeType() == null && super.supported && type.is(SET);
            }

            @Override
            public DynamoDbTypeConverter<AttributeValue, Collection<T>> newConverter(ConvertibleType<Collection<T>> type) {
                log.warn("Marshaling a set of non-String objects to a DynamoDB "
                         + "StringSet. You won't be able to read these objects back "
                         + "out of DynamoDB unless you REALLY know what you're doing: "
                         + "it's probably a bug. If you DO know what you're doing feel"
                         + "free to ignore this warning, but consider using a custom "
                         + "marshaler for this instead.");
                return joinAll(SET.join(scalars.getConverter(String.class, DEFAULT.type())), type.typeConverter());
            }
        }

        /**
         * Native boolean conversion.
         */
        private class NativeBool extends AbstractRule<Boolean, T> {
            private NativeBool(boolean supported) {
                super(DynamoDbAttributeType.BOOL, supported);
            }

            @Override
            public boolean isAssignableFrom(ConvertibleType<?> type) {
                return super.isAssignableFrom(type) && type.is(BOOLEAN);
            }

            @Override
            public DynamoDbTypeConverter<AttributeValue, T> newConverter(ConvertibleType<T> type) {
                return joinAll(getConverter(Boolean.class, type), type.typeConverter());
            }

            @Override
            public Boolean get(AttributeValue o) {
                return o.bool();
            }

            @Override
            public void set(AttributeValue o, Boolean value) {
                ImmutableObjectUtils.setObjectMember(o, "bool", value);
                //o.setBOOL(value);
            }

            @Override
            public Boolean unconvert(AttributeValue o) {
                if (o.bool() == null && o.n() != null) {
                    return BOOLEAN.<Boolean>convert(o.n());
                }
                return super.unconvert(o);
            }
        }

        /**
         * Native boolean conversion.
         */
        private class V2CompatibleBool extends AbstractRule<String, T> {
            private V2CompatibleBool(boolean supported) {
                super(DynamoDbAttributeType.N, supported);
            }

            @Override
            public boolean isAssignableFrom(ConvertibleType<?> type) {
                return super.isAssignableFrom(type) && type.is(BOOLEAN);
            }

            @Override
            public DynamoDbTypeConverter<AttributeValue, T> newConverter(ConvertibleType<T> type) {
                return joinAll(getConverter(String.class, type), type.<String>typeConverter());
            }

            /**
             * For V2 Compatible schema we support loading booleans from a numeric attribute value (0/1) or the native boolean
             * type.
             */
            @Override
            public String get(AttributeValue o) {
                if (o.bool() != null) {
                    // Handle native bools, transform to expected numeric representation.
                    return o.bool() ? "1" : "0";
                }
                return o.n();
            }

            /**
             * For the V2 compatible schema we save as a numeric attribute value unless overridden by {@link
             * DynamoDbNativeBoolean} or {@link DynamoDbTyped}.
             */
            @Override
            public void set(AttributeValue o, String value) {
                ImmutableObjectUtils.setObjectMember(o, "n", value);
                //o.setN(value);
            }
        }

        /**
         * Any {@link Set} conversions.
         */
        private class ObjectSet extends AbstractRule<List<AttributeValue>, Collection<T>> {
            private ObjectSet(boolean supported) {
                super(DynamoDbAttributeType.L, supported);
            }

            @Override
            public boolean isAssignableFrom(ConvertibleType<?> type) {
                return super.isAssignableFrom(type) && type.param(0) != null && type.is(SET);
            }

            @Override
            public DynamoDbTypeConverter<AttributeValue, Collection<T>> newConverter(ConvertibleType<Collection<T>> type) {
                return joinAll(SET.join(getConverter(type.<T>param(0))), type.<List<AttributeValue>>typeConverter());
            }

            @Override
            public List<AttributeValue> get(AttributeValue value) {
                return value.l();
            }

            @Override
            public void set(AttributeValue value, List<AttributeValue> o) {
                ImmutableObjectUtils.setObjectMember(value, "l", o);
                //value.setL(o);
            }
        }

        /**
         * Native bool {@link Set} conversions.
         */
        private class NativeBoolSet extends ObjectSet {
            private NativeBoolSet(boolean supported) {
                super(supported);
            }

            @Override
            public boolean isAssignableFrom(ConvertibleType<?> type) {
                return super.isAssignableFrom(type) && type.param(0).is(BOOLEAN);
            }

            @Override
            public List<AttributeValue> unconvert(AttributeValue o) {
                if (o.l() == null && o.ns() != null) {
                    return LIST.convert(o.ns(), new NativeBool(true).join(scalars.getConverter(Boolean.class, String.class)));
                }
                return super.unconvert(o);
            }
        }

        /**
         * Any {@link List} conversions.
         */
        private class ObjectList extends AbstractRule<List<AttributeValue>, List<T>> {
            private ObjectList(boolean supported) {
                super(DynamoDbAttributeType.L, supported);
            }

            @Override
            public boolean isAssignableFrom(ConvertibleType<?> type) {
                return super.isAssignableFrom(type) && type.param(0) != null && type.is(LIST);
            }

            @Override
            public DynamoDbTypeConverter<AttributeValue, List<T>> newConverter(ConvertibleType<List<T>> type) {
                return joinAll(LIST.join(getConverter(type.<T>param(0))), type.<List<AttributeValue>>typeConverter());
            }

            @Override
            public List<AttributeValue> get(AttributeValue value) {
                return value.l();
            }

            @Override
            public void set(AttributeValue value, List<AttributeValue> o) {
                ImmutableObjectUtils.setObjectMember(value, "l", o);
                //value.setL(o);
            }
        }

        /**
         * Any {@link Map} conversions.
         */
        private class ObjectMap extends AbstractRule<Map<String, AttributeValue>, Map<String, T>> {
            private ObjectMap(boolean supported) {
                super(DynamoDbAttributeType.M, supported);
            }

            @Override
            public boolean isAssignableFrom(ConvertibleType<?> type) {
                return super.isAssignableFrom(type) && type.param(1) != null && type.is(MAP) && type.param(0).is(STRING);
            }

            @Override
            public DynamoDbTypeConverter<AttributeValue, Map<String, T>> newConverter(ConvertibleType<Map<String, T>> type) {
                return joinAll(
                        MAP.<String, AttributeValue, T>join(getConverter(type.<T>param(1))),
                        type.<Map<String, AttributeValue>>typeConverter()
                              );
            }

            @Override
            public Map<String, AttributeValue> get(AttributeValue value) {
                return value.m();
            }

            @Override
            public void set(AttributeValue value, Map<String, AttributeValue> o) {
                ImmutableObjectUtils.setObjectMember(value, "m", o);
                //value.setM(o);
            }
        }

        /**
         * All object conversions.
         */
        private class ObjectDocumentMap extends AbstractRule<Map<String, AttributeValue>, T> {
            private final DynamoDbMapperModelFactory models;
            private final DynamoDbMapperConfig config;

            private ObjectDocumentMap(boolean supported, DynamoDbMapperModelFactory models, DynamoDbMapperConfig config) {
                super(DynamoDbAttributeType.M, supported);
                this.models = models;
                this.config = config;
            }

            @Override
            public boolean isAssignableFrom(ConvertibleType<?> type) {
                return type.attributeType() == getAttributeType() && super.supported && !type.is(MAP);
            }

            @Override
            public DynamoDbTypeConverter<AttributeValue, T> newConverter(final ConvertibleType<T> type) {
                return joinAll(new DynamoDbTypeConverter<Map<String, AttributeValue>, T>() {
                    public Map<String, AttributeValue> convert(final T o) {
                        return models.getTableFactory(config).getTable(type.targetType()).convert(o);
                    }

                    public T unconvert(final Map<String, AttributeValue> o) {
                        return models.getTableFactory(config).getTable(type.targetType()).unconvert(o);
                    }
                }, type.<Map<String, AttributeValue>>typeConverter());
            }

            @Override
            public Map<String, AttributeValue> get(AttributeValue value) {
                return value.m();
            }

            @Override
            public void set(AttributeValue value, Map<String, AttributeValue> o) {
                ImmutableObjectUtils.setObjectMember(value, "m", o);
                //value.setM(o);
            }
        }

        /**
         * Default conversion when no match could be determined.
         */
        private class NotSupported extends AbstractRule<T, T> {
            private NotSupported() {
                super(DynamoDbAttributeType.NULL, false);
            }

            @Override
            public DynamoDbTypeConverter<AttributeValue, T> newConverter(ConvertibleType<T> type) {
                return this;
            }

            @Override
            public T get(AttributeValue value) {
                throw new DynamoDbMappingException("not supported; requires @DynamoDBTyped or @DynamoDBTypeConverted");
            }

            @Override
            public void set(AttributeValue value, T o) {
                throw new DynamoDbMappingException("not supported; requires @DynamoDBTyped or @DynamoDBTypeConverted");
            }
        }
    }

    /**
     * Basic attribute value conversion functions.
     */
    private abstract static class AbstractRule<S, T> extends AbstractConverter<AttributeValue, S>
            implements Reflect<AttributeValue, S>, Rule<T> {
        protected final DynamoDbAttributeType attributeType;
        protected final boolean supported;

        protected AbstractRule(DynamoDbAttributeType attributeType, boolean supported) {
            this.attributeType = attributeType;
            this.supported = supported;
        }

        @Override
        public boolean isAssignableFrom(ConvertibleType<?> type) {
            return type.attributeType() == null ? supported : type.attributeType() == attributeType;
        }

        @Override
        public DynamoDbAttributeType getAttributeType() {
            return this.attributeType;
        }

        @Override
        public AttributeValue convert(final S o) {
            final AttributeValue value = AttributeValue.builder().build();
            set(value, o);
            return value;
        }

        @Override
        public S unconvert(final AttributeValue o) {
            final S value = get(o);
            if (value == null && o.nul() == null) {
                throw new DynamoDbMappingException("expected " + attributeType + " in value " + o);
            }
            return value;
        }
    }

}
