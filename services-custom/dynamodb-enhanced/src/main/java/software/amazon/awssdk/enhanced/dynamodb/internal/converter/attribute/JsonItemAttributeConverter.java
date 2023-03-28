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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.ArrayJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.BooleanJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.NullJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.NumberJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.ObjectJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.StringJsonNode;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * An Internal converter between JsonNode and {@link AttributeValue}.
 *
 * <p>
 * This converts the Attribute Value read from the DDB to JsonNode.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class JsonItemAttributeConverter implements AttributeConverter<JsonNode> {
    private static final Visitor VISITOR = new Visitor();

    private JsonItemAttributeConverter() {
    }

    public static JsonItemAttributeConverter create() {
        return new JsonItemAttributeConverter();
    }

    @Override
    public EnhancedType<JsonNode> type() {
        return EnhancedType.of(JsonNode.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.M;
    }

    @Override
    public AttributeValue transformFrom(JsonNode input) {
        JsonNodeToAttributeValueMapConverter attributeValueMapConverter = JsonNodeToAttributeValueMapConverter.instance();
        return input.visit(attributeValueMapConverter);
    }

    @Override
    public JsonNode transformTo(AttributeValue input) {
        if (AttributeValue.fromNul(true).equals(input)) {
            return NullJsonNode.instance();
        }
        return EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
    }

    private static final class Visitor extends TypeConvertingVisitor<JsonNode> {
        private Visitor() {
            super(JsonNode.class, JsonItemAttributeConverter.class);
        }

        @Override
        public JsonNode convertMap(Map<String, AttributeValue> value) {
            if (value == null) {
                return null;
            }
            Map<String, JsonNode> jsonNodeMap = new LinkedHashMap<>();
            value.entrySet().forEach(
                k -> {
                    JsonNode jsonNode = this.convert(EnhancedAttributeValue.fromAttributeValue(k.getValue()));
                    jsonNodeMap.put(k.getKey(), jsonNode == null ? NullJsonNode.instance() : jsonNode);
                });
            return new ObjectJsonNode(jsonNodeMap);
        }

        @Override
        public JsonNode convertString(String value) {
            if (value == null) {
                return null;
            }
            return new StringJsonNode(value);
        }

        @Override
        public JsonNode convertNumber(String value) {
            if (value == null) {
                return null;
            }
            return new NumberJsonNode(value);
        }

        @Override
        public JsonNode convertBytes(SdkBytes value) {
            if (value == null) {
                return null;
            }
            return new StringJsonNode(BinaryUtils.toBase64(value.asByteArray()));
        }

        @Override
        public JsonNode convertBoolean(Boolean value) {
            if (value == null) {
                return null;
            }
            return new BooleanJsonNode(value);
        }

        @Override
        public JsonNode convertSetOfStrings(List<String> value) {
            if (value == null) {
                return null;
            }
            return new ArrayJsonNode(value.stream().map(StringJsonNode::new).collect(Collectors.toList()));
        }

        @Override
        public JsonNode convertSetOfNumbers(List<String> value) {
            if (value == null) {
                return null;
            }
            return new ArrayJsonNode(value.stream().map(NumberJsonNode::new).collect(Collectors.toList()));
        }

        @Override
        public JsonNode convertSetOfBytes(List<SdkBytes> value) {
            if (value == null) {
                return null;
            }
            return new ArrayJsonNode(value.stream().map(
                sdkByte -> new StringJsonNode(BinaryUtils.toBase64(sdkByte.asByteArray()))
            ).collect(Collectors.toList()));
        }

        @Override
        public JsonNode convertListOfAttributeValues(List<AttributeValue> value) {
            if (value == null) {
                return null;
            }
            return new ArrayJsonNode(value.stream().map(
                attributeValue -> {
                    EnhancedAttributeValue enhancedAttributeValue = EnhancedAttributeValue.fromAttributeValue(attributeValue);
                    return enhancedAttributeValue.isNull() ? NullJsonNode.instance() : enhancedAttributeValue.convert(VISITOR);
                }).collect(Collectors.toList()));
        }
    }
}