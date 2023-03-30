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

package software.amazon.awssdk.enhanced.dynamodb.converters.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformFrom;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformTo;
import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue.fromNumber;
import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue.fromString;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.CharacterArrayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.JsonItemAttributeConverter;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.ArrayJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.BooleanJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.NumberJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.ObjectJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.StringJsonNode;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class JsonItemAttributeConverterTest {

    @Test
    void jsonAttributeConverterWithString() {
        JsonItemAttributeConverter converter = JsonItemAttributeConverter.create();
        StringJsonNode stringJsonNode = new StringJsonNode("testString");
        assertThat(transformFrom(converter, stringJsonNode).s()).isEqualTo("testString");
        assertThat(transformTo(converter, AttributeValue.fromS("testString"))).isEqualTo(stringJsonNode);
    }

    @Test
    void jsonAttributeConverterWithBoolean() {
        JsonItemAttributeConverter converter = JsonItemAttributeConverter.create();
        BooleanJsonNode booleanJsonNode = new BooleanJsonNode(true);
        assertThat(transformFrom(converter, booleanJsonNode).bool()).isTrue();
        assertThat(transformFrom(converter, booleanJsonNode).s()).isNull();
        assertThat(transformTo(converter, AttributeValue.fromBool(true))).isEqualTo(booleanJsonNode);
    }

    @Test
    void jsonAttributeConverterWithNumber() {
        JsonItemAttributeConverter converter = JsonItemAttributeConverter.create();
        NumberJsonNode numberJsonNode = new NumberJsonNode("20");
        assertThat(transformFrom(converter, numberJsonNode).n()).isEqualTo("20");
        assertThat(transformFrom(converter, numberJsonNode).s()).isNull();
        assertThat(transformTo(converter, AttributeValue.fromN("20"))).isEqualTo(numberJsonNode);
    }

    @Test
    void jsonAttributeConverterWithSdkBytes() {
        JsonItemAttributeConverter converter = JsonItemAttributeConverter.create();
        StringJsonNode sdkByteJsonNode = new StringJsonNode(SdkBytes.fromUtf8String("a").asUtf8String());

        assertThat(transformFrom(converter, sdkByteJsonNode).s()).isEqualTo(SdkBytes.fromUtf8String("a").asUtf8String());
        assertThat(transformFrom(converter, sdkByteJsonNode).b()).isNull();
        assertThat(transformTo(converter, AttributeValue.fromB(SdkBytes.fromUtf8String("a")))).isEqualTo(new StringJsonNode("YQ=="));
    }

    @Test
    void jsonAttributeConverterWithSet() {
        JsonItemAttributeConverter converter = JsonItemAttributeConverter.create();
        ArrayJsonNode arrayJsonNode =
            new ArrayJsonNode(Stream.of(new NumberJsonNode("10"), new NumberJsonNode("20")).collect(Collectors.toList()));

        assertThat(transformFrom(converter, arrayJsonNode).l())
            .isEqualTo(Stream.of(AttributeValue.fromN("10"), AttributeValue.fromN("20"))
                             .collect(Collectors.toList()));
    }

    @Test
    void jsonAttributeWithMap(){

        Map<String, JsonNode> jsonNodeMap  = new LinkedHashMap<>();
        jsonNodeMap.put("key", new StringJsonNode("value"));
        ObjectJsonNode objectJsonNode = new ObjectJsonNode(jsonNodeMap);
        JsonItemAttributeConverter converter = JsonItemAttributeConverter.create();
        AttributeValue convertedMap = converter.transformFrom(objectJsonNode);

        assertThat(convertedMap.hasM()).isTrue();
        Map<String, AttributeValue> expectedMap = new LinkedHashMap<>();
        expectedMap.put("key", AttributeValue.fromS("value"));
        assertThat(convertedMap).isEqualTo(AttributeValue.fromM(expectedMap));
    }
}
