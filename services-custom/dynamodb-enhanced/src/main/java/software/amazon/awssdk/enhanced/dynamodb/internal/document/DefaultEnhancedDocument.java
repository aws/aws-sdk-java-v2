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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ChainConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.JsonItemAttributeConverter;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Lazy;
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

    private static final JsonNodeParser JSON_PARSER = JsonNodeParser.create();
    private static final JsonItemAttributeConverter JSON_ATTRIBUTE_CONVERTER = JsonItemAttributeConverter.create();

    private final Map<String, Object> nonAttributeValueMap;
    private final Lazy<Map<String, AttributeValue>> attributeValueMap = new Lazy<>(this::initializeAttributeValueMap);

    private final List<AttributeConverterProvider> attributeConverterProviders;

    private final ChainConverterProvider attributeConverterChain;

    public DefaultEnhancedDocument(DefaultBuilder builder) {
        this.nonAttributeValueMap = unmodifiableMap(new LinkedHashMap<>(builder.nonAttributeValueMap));
        this.attributeConverterProviders = unmodifiableList(new ArrayList<>(builder.attributeConverterProviders));
        this.attributeConverterChain = ChainConverterProvider.create(attributeConverterProviders);
    }

    public static DefaultBuilder builder() {
        return new DefaultBuilder();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this.nonAttributeValueMap, this.attributeConverterProviders);
    }

    @Override
    public Map<String, AttributeValue> toAttributeValueMap() {
        Map<String, AttributeValue> result = new HashMap<>();
        nonAttributeValueMap.forEach((k, v) -> {
            result.put(k, toAttributeValue(v));
        });
        return result;
    }

    @Override
    public List<AttributeConverterProvider> attributeConverterProviders() {
        return attributeConverterProviders;
    }

    @Override
    public boolean isNull(String attributeName) {
        return isPresent(attributeName) && nonAttributeValueMap.get(attributeName) == null;
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
    public <T> Set<T> getSet(String attributeName, EnhancedType<T> type) {
        return get(attributeName, EnhancedType.setOf(type));
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
        return JSON_ATTRIBUTE_CONVERTER.transformTo(attributeValue).toString(); // TODO: Does toString return valid JSON?
    }


    @Override
    public Boolean getBoolean(String attributeName) {
        return get(attributeName, Boolean.class);
    }

    @Override
    public String toJson() {
        // TODO: Maybe there is a better way, but I'm in a hurry!

        StringBuilder output = new StringBuilder();
        output.append('{');
        int startLength = output.length();
        attributeValueMap.getValue().forEach((k, v) -> {
            output.append('"')
                  .append(StringUtils.replace(k, "\"", "\\"))
                  .append("\": ")
                  .append(JSON_ATTRIBUTE_CONVERTER.transformTo(v).toString())
                  .append(", ");
        });

        if (output.length() != startLength) {
            output.setLength(output.length() - ", ".length());
        }

        output.append('}');
        return output.toString();
    }

    @Override
    public Map<String, Object> toMap() {
        return this.nonAttributeValueMap;
    }

    private Map<String, AttributeValue> initializeAttributeValueMap() {
        Map<String, AttributeValue> result = new LinkedHashMap<>(this.nonAttributeValueMap.size());
        this.nonAttributeValueMap.forEach((k, v) -> {
            result.put(k, toAttributeValue(v));
        });
        return result;
    }

    private <T> AttributeValue toAttributeValue(T value) {
        if (value instanceof AttributeValue) {
            return (AttributeValue) value;
        }

        AttributeConverter<T> converter =
            (AttributeConverter<T>) attributeConverterChain.converterFor(EnhancedType.of(value.getClass()));
        return converter.transformFrom(value);
    }

    private <T> T fromAttributeValue(AttributeValue attributeValue, EnhancedType<T> type) {
        if (type.rawClass().equals(AttributeValue.class)) {
            return (T) attributeValue;
        }

        return attributeConverterChain.converterFor(type).transformTo(attributeValue);
    }

    public static class DefaultBuilder implements EnhancedDocument.Builder {

        Map<String, Object> nonAttributeValueMap = new LinkedHashMap<>();

        List<AttributeConverterProvider> attributeConverterProviders = new ArrayList<>();

        private DefaultBuilder() {
        }

        private DefaultBuilder(Map<String, Object> nonAttributeValueMap,
                               List<AttributeConverterProvider> attributeConverterProviders) {
            this.nonAttributeValueMap = new LinkedHashMap<>(nonAttributeValueMap);
            this.attributeConverterProviders = new ArrayList<>(attributeConverterProviders);
        }

        @Override
        public Builder putObject(String attributeName, Object value) {
            Validate.notNull(attributeName, "attributeName cannot be null.");
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
        public Builder putSet(String attributeName, Set<?> values) {
            return putObject(attributeName, values);
        }

        @Override
        public Builder putList(String attributeName, List<?> value) {
            return putObject(attributeName, value);
        }

        @Override
        public Builder putMap(String attributeName, Map<?, ?> value) {
            return putObject(attributeName, value);
        }

        @Override
        public Builder putJson(String attributeName, String json) {
            JsonNode jsonNode = JSON_PARSER.parse(json);
            Validate.isTrue(jsonNode != null, "Provided JSON was not valid JSON.");
            return putObject(attributeName, jsonNode.visit(new IdentityJsonNodeVisitor()));
        }

        @Override
        public Builder putEnhancedDocument(String attributeName, EnhancedDocument enhancedDocument) {
            putObject(attributeName, enhancedDocument.toMap());
            return this;
        }

        @Override
        public Builder addAttributeConverterProvider(AttributeConverterProvider attributeConverterProvider) {
            attributeConverterProviders.add(attributeConverterProvider);
            return this;
        }

        @Override
        public Builder attributeConverterProviders(List<AttributeConverterProvider> attributeConverterProviders) {
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
            JsonNode jsonNode = JSON_PARSER.parse(json);
            Validate.isTrue(jsonNode != null && jsonNode.isObject(), "Provided JSON was not an object (did not start with '{')");
            Map<String, JsonNode> object = jsonNode.asObject();

            this.nonAttributeValueMap.clear();
            object.forEach((k, v) -> putObject(k, v.visit(new IdentityJsonNodeVisitor())));
            return this;
        }

        @Override
        public EnhancedDocument build() {
            return new DefaultEnhancedDocument(this);
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

        return Objects.equals(nonAttributeValueMap, that.nonAttributeValueMap) && Objects.equals(attributeConverterProviders,
                                                                                                 that.attributeConverterProviders);
    }

    @Override
    public int hashCode() {
        int result = nonAttributeValueMap != null ? nonAttributeValueMap.hashCode() : 0;
        result = 31 * result + (attributeConverterProviders != null ? attributeConverterProviders.hashCode() : 0);
        return result;
    }

}