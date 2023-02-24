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

import static software.amazon.awssdk.enhanced.dynamodb.internal.document.DocumentUtils.NULL_ATTRIBUTE_VALUE;
import static software.amazon.awssdk.enhanced.dynamodb.internal.document.DocumentUtils.convert;
import static software.amazon.awssdk.enhanced.dynamodb.internal.document.DocumentUtils.getAttributeConverterOrError;
import static software.amazon.awssdk.enhanced.dynamodb.internal.document.DocumentUtils.toSimpleList;
import static software.amazon.awssdk.enhanced.dynamodb.internal.document.DocumentUtils.toSimpleMapValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.document.DocumentUtils.toSimpleValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.JsonItemAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MapAttributeConverter;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Default implementation of {@link EnhancedDocument}. This class is used by SDK to create Enhanced Documents.
 * Internally saves attributes in an attributeValueMap which can be written to DynamoDB without further conversion.
 * The attribute values are retrieved by converting attributeValue from attributeValueMap at the time of get.
 */
@Immutable
@SdkInternalApi
public class DefaultEnhancedDocument implements EnhancedDocument {

    private static final JsonItemAttributeConverter JSON_ITEM_ATTRIBUTE_CONVERTER = JsonItemAttributeConverter.create();

    private final Map<String, Object> attributeValueObjectMap;

    private final List<AttributeConverterProvider> attributeConverterProviders;
    
    public DefaultEnhancedDocument(DefaultBuilder builder) {
        Validate.notEmpty(builder.attributeConverterProviders,
                          "The attributeConverterProviders must not be empty", builder.attributeConverterProviders);
        this.attributeConverterProviders = builder.attributeConverterProviders;
        attributeValueObjectMap = Collections.unmodifiableMap(builder.attributeValueObjectMap);
    }


    public static DefaultBuilder builder() {
        return new DefaultBuilder();

    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this.attributeValueObjectMap, this.attributeConverterProviders);
    }

    @Override
    public Map<String, AttributeValue> toAttributeValueMap() {
        return DocumentUtils.objectMapToAttributeMap(attributeValueObjectMap, attributeConverterProviders);
    }

    @Override
    public List<AttributeConverterProvider> attributeConverterProviders() {
        return attributeConverterProviders;
    }

    @Override
    public boolean isNull(String attributeName) {
        return isPresent(attributeName) && NULL_ATTRIBUTE_VALUE.equals(attributeValueObjectMap.get(attributeName));
    }

    @Override
    public boolean isPresent(String attributeName) {
        return attributeValueObjectMap.containsKey(attributeName);
    }

    @Override
    public <T> T get(String attributeName, EnhancedType<T> type) {
        Object objectValue = attributeValueObjectMap.get(attributeName);
        if (objectValue == null) {
            return null;
        }
        AttributeConverter<T> attributeConverter = getAttributeConverterOrError(type, attributeConverterProviders);
        return attributeConverter.transformTo(convert(objectValue, attributeConverterProviders));
    }

    @Override
    public String getString(String attributeName) {
        return  get(attributeName, String.class);
    }

    @Override
    public SdkNumber getNumber(String attributeName) {
        return get(attributeName, SdkNumber.class);
    }

    private <T> T get(String attributeName, Class<T> clazz) {
        Object objectValue = attributeValueObjectMap.get(attributeName);
        if (objectValue == null) {
            return null;
        }
        return getAttributeConverterOrError(EnhancedType.of(clazz), attributeConverterProviders)
                                                        .transformTo(convert(objectValue, attributeConverterProviders));
    }

    @Override
    public SdkBytes getBytes(String attributeName) {
        return get(attributeName, SdkBytes.class);
    }

    @Override
    public Set<String> getStringSet(String attributeName) {
        Object objectValue = attributeValueObjectMap.get(attributeName);
        AttributeValue attributeValue = convert(objectValue, attributeConverterProviders);

        if(attributeValue.hasSs()){
            return attributeValue.ss().stream().collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public Set<SdkNumber> getNumberSet(String attributeName) {
        Object objectValue = attributeValueObjectMap.get(attributeName);
        AttributeValue attributeValue = convert(objectValue, attributeConverterProviders);
        if(attributeValue.hasNs()){
            return attributeValue.ns().stream().map(number -> SdkNumber.fromString(number)).collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public Set<SdkBytes> getBytesSet(String attributeName) {
        Object objectValue = attributeValueObjectMap.get(attributeName);
        AttributeValue attributeValue = convert(objectValue, attributeConverterProviders);
        if(attributeValue.hasBs()){
            return attributeValue.bs().stream().collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public <T> List<T> getList(String attributeName, EnhancedType<T> type) {

        AttributeValue attributeValue = convert(attributeValueObjectMap.get(attributeName), attributeConverterProviders);
        if (attributeValue == null || !attributeValue.hasL()) {
            return null;
        }
        return attributeValue.l().stream().map(
            value -> getAttributeConverterOrError(type, attributeConverterProviders).transformTo(value)).collect(Collectors.toList());
    }

    @Override
    public List<?> getList(String attributeName) {
        Object objectValue = attributeValueObjectMap.get(attributeName);
        if(objectValue == null){
            return null;
        }
        AttributeValue attributeValue = convert(objectValue, attributeConverterProviders);
        if (objectValue == null || !attributeValue.hasL()) {
            return null;
        }
        return toSimpleList(attributeValue.l());
    }

    @Override
    public <K, V> Map<K, V> getMapType(String attributeName, EnhancedType<K> keyType, EnhancedType<V> valueType) {

        StringConverter<K> keyConverter = StringConverterProvider.defaultProvider().converterFor(keyType);
        if (keyConverter == null) {
            throw new IllegalStateException("Key Converter not found for " + keyType);
        }
        AttributeConverter<V> valueConverter = getAttributeConverterOrError(valueType, attributeConverterProviders);
        if (valueConverter == null) {
            throw new IllegalStateException("Converter not found for " + valueType);
        }
        Object objectValue = attributeValueObjectMap.get(attributeName);
        AttributeValue attributeValue = convert(objectValue, attributeConverterProviders);
        return MapAttributeConverter.mapConverter(keyConverter, valueConverter).transformTo(attributeValue);
    }

    @Override
    public Map<String, Object> getRawMap(String attributeName) {
        Object objectValue = attributeValueObjectMap.get(attributeName);
        AttributeValue attributeValue = convert(objectValue, attributeConverterProviders);
        if (attributeValue == null || !attributeValue.hasM()) {
            return null;
        }
        return toSimpleMapValue(attributeValue.m());
    }

    @Override
    public EnhancedDocument getEnhancedDocument(String attributeName) {
        Object objectValue = attributeValueObjectMap.get(attributeName);
        AttributeValue attributeValue = convert(objectValue, attributeConverterProviders);
        if (attributeValue == null) {
            return null;
        }
        if (!attributeValue.hasM()) {
            throw new RuntimeException("Cannot get "
                                       + attributeName
                                       + " attribute as map since its of type "
                                       + attributeValue.type());
        }
        return new DefaultBuilder().attributeValueMap(attributeValue.m())
                                   .attributeConverterProviders(attributeConverterProviders)
                                   .build();
    }

    @Override
    public String getJson(String attributeName) {

        Object objectValue = attributeValueObjectMap.get(attributeName);
        if (objectValue == null) {
            return null;
        }
        JsonNode jsonNode = JSON_ITEM_ATTRIBUTE_CONVERTER.transformTo(convert(objectValue, attributeConverterProviders));
        return jsonNode != null ? jsonNode.toString() : null;
    }


    @Override
    public Boolean getBoolean(String attributeName) {
        return get(attributeName, Boolean.class);
    }

    @Override
    public Object get(String attributeName) {
        Object objectValue = attributeValueObjectMap.get(attributeName);
        if (objectValue == null) {
            return null;
        }
        AttributeValue attributeValue = convert(objectValue, attributeConverterProviders);
        return toSimpleValue(attributeValue);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        attributeValueObjectMap.forEach((s, objectValue) -> {
            result.put(s, toSimpleValue(convert(objectValue, attributeConverterProviders)));
        });
        return result;
    }

    @Override
    public String toJson() {
        AttributeValue jsonMap = AttributeValue
            .fromM(DocumentUtils.objectMapToAttributeMap(this.attributeValueObjectMap, attributeConverterProviders));
        JsonItemAttributeConverter jsonItemAttributeConverter = JsonItemAttributeConverter.create();
        JsonNode jsonNode = jsonItemAttributeConverter.transformTo(jsonMap);
        return jsonNode != null ? jsonNode.toString() : null;
    }

    public static class DefaultBuilder implements EnhancedDocument.Builder {

        Map<String, Object> attributeValueObjectMap = new LinkedHashMap<>();

        List<AttributeConverterProvider> attributeConverterProviders = new ArrayList<>();

        private DefaultBuilder() {
        }

        private DefaultBuilder(Map<String, Object> attributeValueObjectMap,  
                               List<AttributeConverterProvider> attributeConverterProviders) {
            this.attributeValueObjectMap = new LinkedHashMap<>(attributeValueObjectMap);
            this.attributeConverterProviders = new ArrayList<>(attributeConverterProviders);
        }

        @Override
        public Builder putObject(String attributeName, Object value) {
            this.attributeValueObjectMap.put(attributeName, value);
            return this;
        }

        @Override
        public Builder putString(String attributeName, String value) {
            Validate.isTrue(!StringUtils.isEmpty(attributeName), "attributeName cannot empty or null");
            attributeValueObjectMap.put(attributeName, value);
            return this;
        }

        @Override
        public Builder putNumber(String attributeName, Number value) {
            Validate.isTrue(!StringUtils.isEmpty(attributeName), "attributeName cannot empty or null");
            attributeValueObjectMap.put(attributeName, value);
            return this;
        }

        @Override
        public Builder putBytes(String attributeName, SdkBytes value) {
            Validate.isTrue(!StringUtils.isEmpty(attributeName), "attributeName cannot empty or null");
            attributeValueObjectMap.put(attributeName, value);
            return this;
        }

        @Override
        public Builder putBoolean(String attributeName, boolean value) {
            Validate.isTrue(!StringUtils.isEmpty(attributeName), "attributeName cannot empty or null");
            attributeValueObjectMap.put(attributeName, value);
            return this;
        }

        @Override
        public Builder putNull(String attributeName) {
            Validate.isTrue(!StringUtils.isEmpty(attributeName), "attributeName cannot empty or null");
            attributeValueObjectMap.put(attributeName, NULL_ATTRIBUTE_VALUE);
            return this;
        }

        // Single Set API
        @Override
        public Builder putStringSet(String attributeName, Set<String> values) {
            Validate.isTrue(!StringUtils.isEmpty(attributeName), "attributeName cannot empty or null");
            attributeValueObjectMap.put(attributeName, values);
            return this;
        }

        @Override
        public Builder putNumberSet(String attributeName, Set<Number> values) {
            Validate.isTrue(!StringUtils.isEmpty(attributeName), "attributeName cannot empty or null");
            attributeValueObjectMap.put(attributeName, values);
            return this;
        }

        @Override
        public Builder putBytesSet(String attributeName, Set<SdkBytes> values) {
            Validate.isTrue(!StringUtils.isEmpty(attributeName), "attributeName cannot empty or null");
            attributeValueObjectMap.put(attributeName, values);
            return this;
        }

        @Override
        public Builder putObjectList(String attributeName, List<?> value) {
            Validate.isTrue(!StringUtils.isEmpty(attributeName), "attributeName cannot empty or null");
            attributeValueObjectMap.put(attributeName, value);
            return this;
        }

        @Override
        public <T> Builder putMap(String attributeName, Map<T, ?> value, Class<T> keyType) {
            Validate.isTrue(!StringUtils.isEmpty(attributeName), "attributeName cannot empty or null");
            if (!isNullValueAdded(attributeName, value)) {
                StringConverter<T> converter =
                    StringConverterProvider.defaultProvider().converterFor(EnhancedType.of(keyType));
                if(converter == null){
                    throw new IllegalArgumentException("The Key cannot be converted to String" );

                }
                Map<String, Object> result = new LinkedHashMap<>(value.size());
                value.forEach((k, v) -> result.put(converter.toString(k), v));
                attributeValueObjectMap.put(attributeName, result);
            }
            return this;
        }

        @Override
        public Builder putMap(String attributeName, Map<String, ?> value) {
            putMap(attributeName, value, String.class);
            return this;
        }

        @Override
        public Builder putJson(String attributeName, String json) {
            Validate.isTrue(!StringUtils.isEmpty(attributeName), "attributeName cannot empty or null");
            if (!isNullValueAdded(attributeName, json)) {
                JsonItemAttributeConverter jsonItemAttributeConverter = JsonItemAttributeConverter.create();
                JsonNodeParser build = JsonNodeParser.builder().build();
                JsonNode jsonNode = build.parse(json);
                AttributeValue attributeValue = jsonItemAttributeConverter.transformFrom(jsonNode);
                attributeValueObjectMap.put(attributeName, attributeValue);
            }
            return this;
        }

        @Override
        public Builder putEnhancedDocument(String attributeName, EnhancedDocument enhancedDocument) {
            Validate.isTrue(!StringUtils.isEmpty(attributeName), "attributeName cannot empty or null");
            if (!isNullValueAdded(attributeName, enhancedDocument)) {
                attributeValueObjectMap.put(attributeName, AttributeValue.fromM(enhancedDocument.toAttributeValueMap()));
            }
            return this;
        }

        @Override
        public Builder addAttributeConverterProvider(AttributeConverterProvider attributeConverterProvider) {
            if (attributeConverterProviders == null) {
                attributeConverterProviders = new ArrayList<>();
            }
            attributeConverterProviders.add(attributeConverterProvider);
            return this;
        }

        @Override
        public Builder attributeConverterProviders(List<AttributeConverterProvider> attributeConverterProviders) {
            this.attributeConverterProviders = attributeConverterProviders;
            return this;
        }

        @Override
        public Builder attributeConverterProviders(AttributeConverterProvider... attributeConverterProvider) {
            this.attributeConverterProviders = attributeConverterProvider != null
                                               ? Arrays.asList(attributeConverterProvider)
                                               : null;
            return this;
        }

        @Override
        public Builder json(String json) {
            Validate.paramNotNull(json, "json");
            JsonNodeParser build = JsonNodeParser.builder().build();
            JsonNode jsonNode = build.parse(json);
            if (jsonNode == null) {
                throw new IllegalArgumentException("Could not parse argument json " + json);
            }
            AttributeValue attributeValue = JSON_ITEM_ATTRIBUTE_CONVERTER.transformFrom(jsonNode);
            if (attributeValue != null && attributeValue.hasM()) {
                attributeValueObjectMap = new LinkedHashMap<>(attributeValue.m());
            }
            return this;
        }

        @Override
        public EnhancedDocument build() {
            return new DefaultEnhancedDocument(this);
        }

        public DefaultBuilder attributeValueMap(Map<String, AttributeValue> attributeValueMap) {
            this.attributeValueObjectMap = attributeValueMap != null ? new LinkedHashMap<>(attributeValueMap) : null;
            return this;
        }

        private boolean isNullValueAdded(String attributeName, Object value) {
            if (value == null) {
                putNull(attributeName);
                return true;
            }
            return false;
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

        return Objects.equals(attributeValueObjectMap, that.attributeValueObjectMap) && Objects.equals(attributeConverterProviders,
                                                                                                       that.attributeConverterProviders);
    }

    @Override
    public int hashCode() {
        int result = attributeValueObjectMap != null ? attributeValueObjectMap.hashCode() : 0;
        result = 31 * result + (attributeConverterProviders != null ? attributeConverterProviders.hashCode() : 0);
        return result;
    }

}