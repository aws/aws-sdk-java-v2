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

package software.amazon.awssdk.enhanced.dynamodb.internal.document;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static software.amazon.awssdk.enhanced.dynamodb.internal.document.JsonStringFormatHelper.addEscapeCharacters;
import static software.amazon.awssdk.enhanced.dynamodb.internal.document.JsonStringFormatHelper.stringValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ChainConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.JsonItemAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MapAttributeConverter;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Validate;

/**
 * Default implementation of {@link EnhancedDocument} used by the SDK to create Enhanced Documents. Attributes are initially saved
 * as a String-Object Map when documents are created using the builder. Conversion to an AttributeValueMap is done lazily when
 * values are accessed. When the document is retrieved from DynamoDB, the AttributeValueMap is internally saved as the attribute
 * value map. Custom objects or collections are saved in the enhancedTypeMap to preserve the generic class information. Note that
 * no default ConverterProviders are assigned, so ConverterProviders must be passed in the builder when creating enhanced
 * documents.
 */
@Immutable
@SdkInternalApi
public class DefaultEnhancedDocument implements EnhancedDocument {

    public static final IllegalStateException NULL_SET_ERROR = new IllegalStateException("Set must not have null values.");
    private static final JsonItemAttributeConverter JSON_ATTRIBUTE_CONVERTER = JsonItemAttributeConverter.create();
    private final Map<String, Object> nonAttributeValueMap;
    private final Map<String, EnhancedType> enhancedTypeMap;
    private final List<AttributeConverterProvider> attributeConverterProviders;
    private final ChainConverterProvider attributeConverterChain;
    private final Lazy<Map<String, AttributeValue>> attributeValueMap = new Lazy<>(this::initializeAttributeValueMap);

    public DefaultEnhancedDocument(DefaultBuilder builder) {
        this.nonAttributeValueMap = unmodifiableMap(new LinkedHashMap<>(builder.nonAttributeValueMap));
        this.attributeConverterProviders = unmodifiableList(new ArrayList<>(builder.attributeConverterProviders));
        this.attributeConverterChain = ChainConverterProvider.create(attributeConverterProviders);
        this.enhancedTypeMap = unmodifiableMap(builder.enhancedTypeMap);
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    public static <T> AttributeConverter<T> converterForClass(EnhancedType<T> type,
                                                              ChainConverterProvider chainConverterProvider) {

        if (type.rawClass().isAssignableFrom(List.class)) {
            return (AttributeConverter<T>) ListAttributeConverter
                .create(converterForClass(type.rawClassParameters().get(0), chainConverterProvider));
        }
        if (type.rawClass().isAssignableFrom(Map.class)) {
            return (AttributeConverter<T>) MapAttributeConverter.mapConverter(
                StringConverterProvider.defaultProvider().converterFor(type.rawClassParameters().get(0)),
                converterForClass(type.rawClassParameters().get(1), chainConverterProvider));
        }
        return Optional.ofNullable(chainConverterProvider.converterFor(type))
                       .orElseThrow(() -> new IllegalStateException(
                           "AttributeConverter not found for class " + type
                           + ". Please add an AttributeConverterProvider for this type. If it is a default type, add the "
                           + "DefaultAttributeConverterProvider to the builder."));
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    @Override
    public List<AttributeConverterProvider> attributeConverterProviders() {
        return attributeConverterProviders;
    }

    @Override
    public boolean isNull(String attributeName) {
        return isPresent(attributeName) && (nonAttributeValueMap.get(attributeName) == null
               || AttributeValue.fromNul(true).equals(nonAttributeValueMap.get(attributeName)));
    }

    @Override
    public boolean isPresent(String attributeName) {
        return nonAttributeValueMap.containsKey(attributeName);
    }

    @Override
    public <T> T get(String attributeName, EnhancedType<T> type) {
        AttributeValue attributeValue = attributeValueMap.getValue().get(attributeName);
        if (attributeValue == null) {
            return null;
        }
        return fromAttributeValue(attributeValue, type);
    }

    @Override
    public String getString(String attributeName) {
        return get(attributeName, String.class);
    }

    @Override
    public SdkNumber getNumber(String attributeName) {
        return get(attributeName, SdkNumber.class);
    }

    private <T> T get(String attributeName, Class<T> clazz) {
        return get(attributeName, EnhancedType.of(clazz));
    }

    @Override
    public SdkBytes getBytes(String attributeName) {
        return get(attributeName, SdkBytes.class);
    }

    @Override
    public Set<String> getStringSet(String attributeName) {
        return get(attributeName, EnhancedType.setOf(String.class));

    }

    @Override
    public Set<SdkNumber> getNumberSet(String attributeName) {
        return get(attributeName, EnhancedType.setOf(SdkNumber.class));
    }

    @Override
    public Set<SdkBytes> getBytesSet(String attributeName) {
        return get(attributeName, EnhancedType.setOf(SdkBytes.class));
    }

    @Override
    public <T> List<T> getList(String attributeName, EnhancedType<T> type) {
        return get(attributeName, EnhancedType.listOf(type));
    }

    @Override
    public <K, V> Map<K, V> getMap(String attributeName, EnhancedType<K> keyType, EnhancedType<V> valueType) {
        return get(attributeName, EnhancedType.mapOf(keyType, valueType));
    }

    @Override
    public String getJson(String attributeName) {
        AttributeValue attributeValue = attributeValueMap.getValue().get(attributeName);
        if (attributeValue == null) {
            return null;
        }
        return JSON_ATTRIBUTE_CONVERTER.transformTo(attributeValue).toString();
    }

    @Override
    public Boolean getBoolean(String attributeName) {
        return get(attributeName, Boolean.class);
    }

    @Override
    public List<AttributeValue> getUnknownTypeList(String attributeName) {
        AttributeValue attributeValue = attributeValueMap.getValue().get(attributeName);
        if (attributeValue == null) {
            return null;
        }
        if (!attributeValue.hasL()) {
            throw new IllegalStateException("Cannot get a List from attribute value of Type " + attributeValue.type());
        }
        return attributeValue.l();
    }

    @Override
    public Map<String, AttributeValue> getUnknownTypeMap(String attributeName) {
        AttributeValue attributeValue = attributeValueMap.getValue().get(attributeName);
        if (attributeValue == null) {
            return null;
        }
        if (!attributeValue.hasM()) {
            throw new IllegalStateException("Cannot get a Map from attribute value of Type " + attributeValue.type());
        }
        return attributeValue.m();
    }

    @Override
    public String toJson() {
        if (nonAttributeValueMap.isEmpty()) {
            return "{}";
        }
        return attributeValueMap.getValue().entrySet().stream()
                                .map(entry -> "\""
                                              + addEscapeCharacters(entry.getKey())
                                              + "\":"
                                              + stringValue(JSON_ATTRIBUTE_CONVERTER.transformTo(entry.getValue())))
                                .collect(Collectors.joining(",", "{", "}"));
    }

    @Override
    public Map<String, AttributeValue> toMap() {
        return attributeValueMap.getValue();
    }

    private Map<String, AttributeValue> initializeAttributeValueMap() {
        Map<String, AttributeValue> result = new LinkedHashMap<>(this.nonAttributeValueMap.size());
        Validate.notEmpty(this.attributeConverterProviders, "attributeConverterProviders");
        this.nonAttributeValueMap.forEach((k, v) -> {
            if (v == null) {
                result.put(k, AttributeValue.fromNul(true));
            } else {

                result.put(k, toAttributeValue(v, enhancedTypeMap.getOrDefault(k, EnhancedType.of(v.getClass()))));
            }

        });
        return result;
    }

    private <T> AttributeValue toAttributeValue(T value, EnhancedType<T> enhancedType) {
        if (value instanceof AttributeValue) {
            return (AttributeValue) value;
        }
        return converterForClass(enhancedType, attributeConverterChain).transformFrom(value);
    }

    private <T> T fromAttributeValue(AttributeValue attributeValue, EnhancedType<T> type) {
        if (type.rawClass().equals(AttributeValue.class)) {
            return (T) attributeValue;
        }
        return (T) converterForClass(type, attributeConverterChain).transformTo(attributeValue);
    }

    public static class DefaultBuilder implements EnhancedDocument.Builder {

        Map<String, Object> nonAttributeValueMap = new LinkedHashMap<>();
        Map<String, EnhancedType> enhancedTypeMap = new HashMap<>();

        List<AttributeConverterProvider> attributeConverterProviders = new ArrayList<>();

        private DefaultBuilder() {
        }


        public DefaultBuilder(DefaultEnhancedDocument enhancedDocument) {
            this.nonAttributeValueMap = new LinkedHashMap<>(enhancedDocument.nonAttributeValueMap);
            this.attributeConverterProviders = new ArrayList<>(enhancedDocument.attributeConverterProviders);
            this.enhancedTypeMap = new HashMap<>(enhancedDocument.enhancedTypeMap);
        }

        public Builder putObject(String attributeName, Object value) {
            Validate.paramNotNull(attributeName, "attributeName");
            Validate.paramNotBlank(attributeName.trim(), "attributeName");
            enhancedTypeMap.remove(attributeName);
            nonAttributeValueMap.remove(attributeName);
            nonAttributeValueMap.put(attributeName, value);
            return this;
        }

        @Override
        public Builder putString(String attributeName, String value) {
            return putObject(attributeName, value);
        }

        @Override
        public Builder putNumber(String attributeName, Number value) {
            return putObject(attributeName, value);
        }

        @Override
        public Builder putBytes(String attributeName, SdkBytes value) {
            return putObject(attributeName, value);
        }

        @Override
        public Builder putBoolean(String attributeName, Boolean value) {
            return putObject(attributeName, value);
        }

        @Override
        public Builder putNull(String attributeName) {
            return putObject(attributeName, null);
        }

        @Override
        public Builder putStringSet(String attributeName, Set<String> values) {
            checkInvalidAttribute(attributeName, values);
            if (values.stream().anyMatch(Objects::isNull)) {
                throw NULL_SET_ERROR;
            }
            return putWithType(attributeName, values, EnhancedType.setOf(String.class));
        }

        @Override
        public Builder putNumberSet(String attributeName, Set<Number> values) {
            checkInvalidAttribute(attributeName, values);
            Set<SdkNumber> sdkNumberSet =
                values.stream().map(number -> {
                    if (number == null) {
                        throw NULL_SET_ERROR;
                    }
                    return SdkNumber.fromString(number.toString());
                }).collect(Collectors.toCollection(LinkedHashSet::new));
            return putWithType(attributeName, sdkNumberSet, EnhancedType.setOf(SdkNumber.class));
        }

        @Override
        public Builder putBytesSet(String attributeName, Set<SdkBytes> values) {
            checkInvalidAttribute(attributeName, values);
            if (values.stream().anyMatch(Objects::isNull)) {
                throw NULL_SET_ERROR;
            }
            return putWithType(attributeName, values, EnhancedType.setOf(SdkBytes.class));
        }

        @Override
        public <T> Builder putList(String attributeName, List<T> value, EnhancedType<T> type) {
            checkInvalidAttribute(attributeName, value);
            Validate.paramNotNull(type, "type");
            return putWithType(attributeName, value, EnhancedType.listOf(type));
        }

        @Override
        public <T> Builder putWithType(String attributeName, T value, EnhancedType<T> type) {
            checkInvalidAttribute(attributeName, value);
            Validate.notNull(attributeName, "attributeName cannot be null.");
            enhancedTypeMap.put(attributeName, type);
            nonAttributeValueMap.remove(attributeName);
            nonAttributeValueMap.put(attributeName, value);
            return this;
        }

        @Override
        public <K, V> Builder putMapOfType(String attributeName, Map<K, V> value, EnhancedType<K> keyType,
                                           EnhancedType<V> valueType) {
            checkInvalidAttribute(attributeName, value);
            Validate.notNull(attributeName, "attributeName cannot be null.");
            Validate.paramNotNull(keyType, "keyType");
            Validate.paramNotNull(valueType, "valueType");
            return putWithType(attributeName, value, EnhancedType.mapOf(keyType, valueType));
        }

        @Override
        public Builder putJson(String attributeName, String json) {
            checkInvalidAttribute(attributeName, json);
            return putObject(attributeName, getAttributeValueFromJson(json));
        }

        @Override
        public Builder addAttributeConverterProvider(AttributeConverterProvider attributeConverterProvider) {
            Validate.paramNotNull(attributeConverterProvider, "attributeConverterProvider");
            attributeConverterProviders.add(attributeConverterProvider);
            return this;
        }

        @Override
        public Builder attributeConverterProviders(List<AttributeConverterProvider> attributeConverterProviders) {
            Validate.paramNotNull(attributeConverterProviders, "attributeConverterProviders");
            this.attributeConverterProviders.clear();
            this.attributeConverterProviders.addAll(attributeConverterProviders);
            return this;
        }

        @Override
        public Builder attributeConverterProviders(AttributeConverterProvider... attributeConverterProviders) {
            Validate.paramNotNull(attributeConverterProviders, "attributeConverterProviders");
            return attributeConverterProviders(Arrays.asList(attributeConverterProviders));
        }

        @Override
        public Builder json(String json) {
            Validate.paramNotNull(json, "json");
            AttributeValue attributeValue = getAttributeValueFromJson(json);
            if (attributeValue != null && attributeValue.hasM()) {
                nonAttributeValueMap = new LinkedHashMap<>(attributeValue.m());
            }
            return this;
        }

        public Builder attributeValueMap(Map<String, AttributeValue> attributeValueMap) {
            Validate.paramNotNull(attributeConverterProviders, "attributeValueMap");
            nonAttributeValueMap.clear();
            attributeValueMap.forEach(this::putObject);
            return this;
        }

        @Override
        public EnhancedDocument build() {
            return new DefaultEnhancedDocument(this);
        }

        private static AttributeValue getAttributeValueFromJson(String json) {
            JsonNodeParser build = JsonNodeParser.builder().build();
            JsonNode jsonNode = build.parse(json);
            if (jsonNode == null) {
                throw new IllegalArgumentException("Could not parse argument json " + json);
            }
            return JSON_ATTRIBUTE_CONVERTER.transformFrom(jsonNode);
        }

        private static void checkInvalidAttribute(String attributeName, Object value) {
            Validate.paramNotNull(attributeName, "attributeName");
            Validate.paramNotBlank(attributeName.trim(), "attributeName");
            Validate.notNull(value, "%s must not be null. Use putNull API to insert a Null value", value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultEnhancedDocument that = (DefaultEnhancedDocument) o;
        return nonAttributeValueMap.equals(that.nonAttributeValueMap) && Objects.equals(enhancedTypeMap, that.enhancedTypeMap)
               && Objects.equals(attributeValueMap, that.attributeValueMap) && Objects.equals(attributeConverterProviders,
                                                                                              that.attributeConverterProviders)
               && attributeConverterChain.equals(that.attributeConverterChain);
    }

    @Override
    public int hashCode() {
        int result = nonAttributeValueMap != null ? nonAttributeValueMap.hashCode() : 0;
        result = 31 * result + (attributeConverterProviders != null ? attributeConverterProviders.hashCode() : 0);
        return result;
    }


}