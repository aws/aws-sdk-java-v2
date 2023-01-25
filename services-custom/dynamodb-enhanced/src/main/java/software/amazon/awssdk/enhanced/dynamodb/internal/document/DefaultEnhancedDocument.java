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
import static software.amazon.awssdk.enhanced.dynamodb.internal.document.DocumentUtils.convertAttributeValueToObject;
import static software.amazon.awssdk.enhanced.dynamodb.internal.document.DocumentUtils.toSimpleList;
import static software.amazon.awssdk.enhanced.dynamodb.internal.document.DocumentUtils.toSimpleMapValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.document.DocumentUtils.toSimpleValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ChainConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.JsonItemAttributeConverter;
import software.amazon.awssdk.protocols.json.internal.unmarshall.document.DocumentUnmarshaller;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Default implementation of {@link EnhancedDocument}. This class is used by SDK to create Enhanced Documents. The class
 * internally saves attributeValueMap which is saved to ddb as it is. The aattribute values are retrieved
 * by converting attributeValue from attributeValueMap at the time of get.
 */
@Immutable
@SdkInternalApi
public class DefaultEnhancedDocument implements EnhancedDocument {

    private final Map<String, AttributeValue> attributeValueMap;

    private final ChainConverterProvider attributeConverterProviders;

    public DefaultEnhancedDocument(Map<String, AttributeValue> attributeValueMap,
                                   ChainConverterProvider attributeConverterProviders) {
        this.attributeValueMap = attributeValueMap;
        this.attributeConverterProviders = attributeConverterProviders;
    }

    public DefaultEnhancedDocument(DefaultBuilder builder) {
        attributeValueMap = Collections.unmodifiableMap(builder.getAttributeValueMap());
        attributeConverterProviders = ChainConverterProvider.create(builder.attributeConverterProviders);
    }

    public static DefaultBuilder builder() {
        return new DefaultBuilder();

    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public Map<String, AttributeValue> getAttributeValueMap() {
        return attributeValueMap;
    }

    @Override
    public boolean isNull(String attributeName) {
        return isPresent(attributeName) && attributeValueMap.get(attributeName).nul();
    }

    @Override
    public boolean isPresent(String attributeName) {
        return attributeValueMap.containsKey(attributeName);
    }

    @Override
    public <T> T get(String attributeName, EnhancedType<T> type) {
        AttributeConverter<T> attributeConverter = attributeConverterProviders.converterFor(type);
        if (attributeConverter == null) {
            throw new IllegalArgumentException("type " + type + " is not found in AttributeConverterProviders");
        }
        AttributeValue attributeValue = attributeValueMap.get(attributeName);
        if (attributeValue == null) {
            return null;
        }
        return attributeConverter.transformTo(attributeValue);
    }

    @Override
    public String getString(String attributeName) {
        AttributeValue attributeValue = attributeValueMap.get(attributeName);
        return attributeValue != null
               ? attributeConverterProviders.converterFor(EnhancedType.of(String.class)).transformTo(attributeValue)
               : null;
    }

    @Override
    public SdkNumber getSdkNumber(String attributeName) {
        AttributeValue attributeValue = attributeValueMap.get(attributeName);
        return attributeValue != null
               ? SdkNumber.fromString(
            attributeConverterProviders.converterFor(EnhancedType.of(String.class)).transformTo(attributeValue))
               : null;
    }

    @Override
    public SdkBytes getSdkBytes(String attributeName) {
        AttributeValue attributeValue = attributeValueMap.get(attributeName);

        return attributeValue != null
               ? attributeConverterProviders.converterFor(EnhancedType.of(SdkBytes.class)).transformTo(attributeValue)
               : null;
    }

    @Override
    public Set<String> getStringSet(String attributeName) {
        AttributeValue attributeValue = attributeValueMap.get(attributeName);
        if (attributeValue == null || !attributeValue.hasSs()) {
            return null;
        }
        return attributeValue.ss().stream().collect(Collectors.toSet());
    }

    @Override
    public Set<SdkNumber> getNumberSet(String attributeName) {
        AttributeValue attributeValue = attributeValueMap.get(attributeName);
        if (attributeValue == null || !attributeValue.hasNs()) {
            return null;
        }
        return attributeValue.ns().stream().map(SdkNumber::fromString).collect(Collectors.toSet());
    }

    @Override
    public Set<SdkBytes> getSdkBytesSet(String attributeName) {
        AttributeValue attributeValue = attributeValueMap.get(attributeName);
        if (attributeValue == null || !attributeValue.hasBs()) {
            return null;
        }
        return attributeValue.bs().stream()
                             .map(item -> SdkBytes.fromByteArray(item.asByteArrayUnsafe()))
                             .collect(Collectors.toSet());
    }

    @Override
    public <T> List<T> getList(String attributeName, EnhancedType<T> type) {

        AttributeConverter<T> attributeConverter = attributeConverterProviders.converterFor(type);
        if (attributeConverter == null) {
            throw new IllegalArgumentException("type " + type + " is not found in AttributeConverterProviders");
        }

        AttributeValue attributeValue = attributeValueMap.get(attributeName);
        if (attributeValue == null || !attributeValue.hasL()) {
            return null;
        }
        return attributeValue.l().stream().map(
            value -> attributeConverterProviders.converterFor(type).transformTo(value)).collect(Collectors.toList());
    }

    @Override
    public List<?> getList(String attributeName) {
        AttributeValue attributeValue = attributeValueMap.get(attributeName);
        if (attributeValue == null || !attributeValue.hasL()) {
            return null;
        }
        return toSimpleList(attributeValue.l());
    }

    @Override
    public <T> Map<String, T> getMap(String attributeName, EnhancedType<T> type) {
        validateConverter(type);
        AttributeValue attributeValue = attributeValueMap.get(attributeName);
        if (attributeValue == null || !attributeValue.hasM()) {
            return null;
        }
        Map<String, T> result = new LinkedHashMap<>();
        attributeValue.m().forEach((key, value) ->
                                       result.put(key, attributeConverterProviders.converterFor(type).transformTo(value)));
        return result;
    }

    private <T> void validateConverter(EnhancedType<T> type) {
        AttributeConverter<T> attributeConverter = attributeConverterProviders.converterFor(type);
        if (attributeConverter == null) {
            throw new IllegalArgumentException("type " + type + " is not found in AttributeConverterProviders");
        }
    }

    @Override
    public <T extends Number> Map<String, T> getMapOfNumbers(String attributeName, Class<T> valueType) {
        AttributeValue attributeValue = attributeValueMap.get(attributeName);
        if (attributeValue == null || !attributeValue.hasM()) {
            return null;
        }
        Map<String, T> result = new LinkedHashMap<>();
        attributeValue.m().entrySet().forEach(
            entry -> result.put(entry.getKey(),
                                attributeConverterProviders.converterFor(
                                    EnhancedType.of(valueType)).transformTo(entry.getValue())));
        return result;
    }

    @Override
    public Map<String, Object> getRawMap(String attributeName) {
        AttributeValue attributeValue = attributeValueMap.get(attributeName);
        if (attributeValue == null || !attributeValue.hasM()) {
            return null;
        }
        return toSimpleMapValue(attributeValue.m());
    }

    @Override
    public EnhancedDocument getMapAsDocument(String attributeName) {

        AttributeValue attributeValue = attributeValueMap.get(attributeName);
        if (attributeValue == null) {
            return null;
        }
        if (!attributeValue.hasM()) {
            throw new RuntimeException("Cannot get " + attributeName + " attribute as map since its of type "
                                               + attributeValue.type());
        }
        return new DefaultEnhancedDocument(attributeValue.m(), this.attributeConverterProviders);
    }

    @Override
    public String getJson(String attributeName) {

        if (attributeValueMap.get(attributeName) == null) {
            return null;
        }
        JsonItemAttributeConverter jsonItemAttributeConverter = JsonItemAttributeConverter.create();
        JsonNode jsonNode = jsonItemAttributeConverter.transformTo(attributeValueMap.get(attributeName));
        Document document = jsonNode.visit(new DocumentUnmarshaller());
        return document.toString();
    }

    @Override
    public String getJsonPretty(String attributeName) {
        //TODO : Implementation in next revision or next PR.
        throw new UnsupportedOperationException("Currently unsupported");
    }

    @Override
    public Boolean getBoolean(String attributeName) {
        return getBool(attributeName);
    }

    /**
     * Keeping the backward compatibility with older version of sdk where 0 and 1 are treated a true and false respectively.
     */
    private Boolean getBool(String attributeName) {
        Object object = get(attributeName);
        if (object instanceof Boolean) {
            return (Boolean) object;
        }
        if (object instanceof String || object instanceof SdkNumber) {
            if ("1".equals(object.toString())) {
                return true;
            }
            if ("0".equals(object.toString())) {
                return false;
            }
            return Boolean.valueOf((String) object);
        }
        throw new IllegalStateException("Value of attribute " + attributeName + " of type " + getTypeOf(attributeName)
                                        + " cannot be converted into a Boolean value.");
    }

    @Override
    public Object get(String attributeName) {
        AttributeValue attributeValue = attributeValueMap.get(attributeName);
        if (attributeValue == null) {
            return null;
        }
        return convertAttributeValueToObject(attributeValue);
    }

    @Override
    public EnhancedType<?> getTypeOf(String attributeName) {
        Object attributeValue = get(attributeName);
        return EnhancedType.of(attributeValue.getClass());
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        attributeValueMap.forEach((s, attributeValue) -> result.put(s, toSimpleValue(attributeValue)));
        return result;
    }

    @Override
    public String toJson() {
        AttributeValue jsonMap = AttributeValue.fromM(attributeValueMap);
        JsonItemAttributeConverter jsonItemAttributeConverter = JsonItemAttributeConverter.create();
        JsonNode jsonNode = jsonItemAttributeConverter.transformTo(jsonMap);
        Document document = jsonNode.visit(new DocumentUnmarshaller());
        return document.toString();
    }

    @Override
    public String toJsonPretty() {
        return null;
    }

    public static class DefaultBuilder implements EnhancedDocument.Builder {


        Map<String, AttributeValue> attributeValueMap = new LinkedHashMap<>();
        ChainConverterProvider converterProvider = ChainConverterProvider.create(DefaultAttributeConverterProvider.create());
        List<AttributeConverterProvider> attributeConverterProviders;

        public DefaultBuilder(DefaultEnhancedDocument enhancedDocument) {
            attributeValueMap = enhancedDocument.attributeValueMap;
            attributeConverterProviders = enhancedDocument.attributeConverterProviders != null ?
                                          enhancedDocument.attributeConverterProviders.chainedProviders() : null;
        }

        public DefaultBuilder() {
        }

        public Map<String, AttributeValue> getAttributeValueMap() {
            return attributeValueMap;
        }

        @Override
        public Builder add(String attributeName, Object value) {

            attributeValueMap.put(attributeName, convert(value, converterProvider));
            return this;
        }

        @Override
        public Builder addString(String attributeName, String value) {
            attributeValueMap.put(attributeName, AttributeValue.fromS(value));
            return this;
        }

        @Override
        public Builder addNumber(String attributeName, Number value) {
            attributeValueMap.put(attributeName, AttributeValue.fromN(value != null ? String.valueOf(value) : null));
            return this;
        }

        @Override
        public Builder addSdkBytes(String attributeName, SdkBytes value) {
            attributeValueMap.put(attributeName, AttributeValue.fromB(value));
            return this;
        }

        @Override
        public Builder addBoolean(String attributeName, boolean value) {
            attributeValueMap.put(attributeName, AttributeValue.fromBool(value));
            return this;
        }

        @Override
        public Builder addNull(String attributeName) {
            attributeValueMap.put(attributeName, NULL_ATTRIBUTE_VALUE);
            return this;
        }

        @Override
        public Builder addStringSet(String attributeName, Set<String> values) {
            attributeValueMap.put(attributeName, AttributeValue.fromSs(values.stream().collect(Collectors.toList())));
            return this;
        }

        @Override
        public Builder addNumberSet(String attributeName, Set<Number> values) {


            List<String> collect = values.stream().map(value -> value.toString()).collect(Collectors.toList());

            attributeValueMap.put(attributeName, AttributeValue.fromNs(collect));
            return this;
        }

        @Override
        public Builder addSdkBytesSet(String attributeName, Set<SdkBytes> values) {
            attributeValueMap.put(attributeName, AttributeValue.fromBs(values.stream().collect(Collectors.toList())));
            return this;
        }

        @Override
        public Builder addList(String attributeName, List<?> value) {
            attributeValueMap.put(attributeName, convert(value, converterProvider));
            return this;
        }

        @Override
        public Builder addMap(String attributeName, Map<String, ?> value) {
            attributeValueMap.put(attributeName, convert(value, converterProvider));
            return this;
        }

        @Override
        public Builder addJson(String attributeName, String json) {
            JsonItemAttributeConverter jsonItemAttributeConverter = JsonItemAttributeConverter.create();
            JsonNodeParser build = JsonNodeParser.builder().build();
            JsonNode jsonNode = build.parse(json);
            AttributeValue attributeValue = jsonItemAttributeConverter.transformFrom(jsonNode);
            attributeValueMap.put(attributeName, attributeValue);
            return this;
        }

        @Override
        public Builder addEnhancedDocument(String attributeName, EnhancedDocument enhancedDocument) {
            if (enhancedDocument == null) {
                attributeValueMap.put(attributeName, NULL_ATTRIBUTE_VALUE);
                return this;
            }
            DefaultEnhancedDocument defaultEnhancedDocument =
                enhancedDocument instanceof DefaultEnhancedDocument
                ? (DefaultEnhancedDocument) enhancedDocument
                : (DefaultEnhancedDocument) enhancedDocument.toBuilder().json(enhancedDocument.toJson()).build();
            attributeValueMap.put(attributeName, AttributeValue.fromM(defaultEnhancedDocument.attributeValueMap));
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

            this.attributeConverterProviders = attributeConverterProviders != null ? new ArrayList<>(attributeConverterProviders)
                                                                                   : null;
            return null;
        }

        // TODO : Will add test for attributeConverterProvider in future revision on next PR.
        @Override
        public Builder attributeConverterProviders(AttributeConverterProvider... attributeConverterProvider) {

            this.attributeConverterProviders = attributeConverterProvider != null
                                               ? Arrays.asList(attributeConverterProvider)

                                               : null;
            return this;
        }

        @Override
        public Builder json(String json) {
            JsonNodeParser build = JsonNodeParser.builder().build();
            JsonNode jsonNode = build.parse(json);
            if (jsonNode == null) {
                throw new IllegalArgumentException("Could not parse argument json " + json);
            }

            JsonItemAttributeConverter jsonItemAttributeConverter = JsonItemAttributeConverter.create();
            AttributeValue attributeValue = jsonItemAttributeConverter.transformFrom(jsonNode);
            this.attributeValueMap = attributeValue.m();
            return this;
        }

        @Override
        public EnhancedDocument build() {
            return new DefaultEnhancedDocument(this);
        }
    }
}
