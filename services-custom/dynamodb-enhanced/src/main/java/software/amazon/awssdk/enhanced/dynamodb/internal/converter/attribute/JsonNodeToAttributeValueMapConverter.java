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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeVisitor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public class JsonNodeToAttributeValueMapConverter implements JsonNodeVisitor<AttributeValue> {

    private static final JsonNodeToAttributeValueMapConverter INSTANCE = new JsonNodeToAttributeValueMapConverter();

    private JsonNodeToAttributeValueMapConverter() {
    }

    public static JsonNodeToAttributeValueMapConverter instance() {
        return INSTANCE;
    }

    @Override
    public AttributeValue visitNull() {
        return AttributeValue.fromNul(true);
    }

    @Override
    public AttributeValue visitBoolean(boolean bool) {
        return AttributeValue.builder().bool(bool).build();
    }

    @Override
    public AttributeValue visitNumber(String number) {
        return AttributeValue.builder().n(number).build();
    }

    @Override
    public AttributeValue visitString(String string) {
        return AttributeValue.builder().s(string).build();
    }

    @Override
    public AttributeValue visitArray(List<JsonNode> array) {
        return AttributeValue.builder().l(array.stream()
                                               .map(node -> node.visit(this))
                                               .collect(Collectors.toList()))
                             .build();
    }

    @Override
    public AttributeValue visitObject(Map<String, JsonNode> object) {
        return AttributeValue.builder().m(object.entrySet().stream()
                                                .collect(Collectors.toMap(
                                                    Map.Entry::getKey,
                                                    entry -> entry.getValue().visit(this),
                                                    (left, right) -> left, LinkedHashMap::new)))
                             .build();
    }

    @Override
    public AttributeValue visitEmbeddedObject(Object embeddedObject) {
        throw new UnsupportedOperationException("Embedded objects are not supported within Document types.");
    }
}