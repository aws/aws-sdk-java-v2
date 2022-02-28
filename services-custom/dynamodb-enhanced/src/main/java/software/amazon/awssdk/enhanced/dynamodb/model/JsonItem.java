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

package software.amazon.awssdk.enhanced.dynamodb.model;


import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.JsonItemAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.JsonNodeToAttributeValueMapConvertor;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.ObjectJsonNode;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.StringUtils;


@SdkPublicApi
public class JsonItem {
    private final JsonNode delegateJsonNode;

    public JsonItem(String jsonString) {
        this.delegateJsonNode = JsonNode.parser().parse(jsonString);
    }

    private JsonItem(JsonNode objectJsonNode) {
        this.delegateJsonNode = objectJsonNode;

    }

    public static JsonItem fromJson(String jsonString) {
        StringUtils.isNotBlank(jsonString);
        return new JsonItem(jsonString);
    }

    public static JsonItem fromAttributeValueMap(Map<String, AttributeValue> attributeMap) {
        JsonItemAttributeConverter jsonItemAttributeConverter = JsonItemAttributeConverter.create();
        Map<String, JsonNode> jsonNodeMap = new HashMap<>();
        attributeMap.entrySet().forEach(entrt -> jsonNodeMap.put(entrt.getKey(),
                                                                 jsonItemAttributeConverter.transformTo(entrt.getValue())));

        return new JsonItem(new ObjectJsonNode(jsonNodeMap));
    }

    @Override
    public String toString() {
        return delegateJsonNode.toString();

    }

    public boolean contains(String key) {
        return delegateJsonNode.field(key).isPresent();
    }

    public Optional<JsonItem> get(String key) {
        return delegateJsonNode.isObject() ? Optional.of(new JsonItem(delegateJsonNode.field(key).get()))
                                           : Optional.empty();
    }


    public String toJson() {
        return delegateJsonNode != null ? delegateJsonNode.toString() : null;
    }

    public Map<String, AttributeValue> itemToMap() {
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();

        if (!this.delegateJsonNode.isObject()) {
            throw new IllegalStateException("Cannot convert item " + this.delegateJsonNode + " to map.");

        }
        JsonNodeToAttributeValueMapConvertor jsonNodeToAttributeValueMapConvertor = new JsonNodeToAttributeValueMapConvertor();
        this.delegateJsonNode.asObject().forEach(
            (key, value) -> attributeValueMap.put(key, value.visit(jsonNodeToAttributeValueMapConvertor)));
        return unmodifiableMap(attributeValueMap);
    }

}
