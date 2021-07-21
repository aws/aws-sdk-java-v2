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

package software.amazon.awssdk.protocols.jsoncore.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeVisitor;

/**
 * An object {@link JsonNode}.
 */
@SdkInternalApi
public final class ObjectJsonNode implements JsonNode {
    private final Map<String, JsonNode> value;

    public ObjectJsonNode(Map<String, JsonNode> value) {
        this.value = value;
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public String asNumber() {
        throw new UnsupportedOperationException("A JSON object cannot be converted to a number.");
    }

    @Override
    public String asString() {
        throw new UnsupportedOperationException("A JSON object cannot be converted to a string.");
    }

    @Override
    public boolean asBoolean() {
        throw new UnsupportedOperationException("A JSON object cannot be converted to a boolean.");
    }

    @Override
    public List<JsonNode> asArray() {
        throw new UnsupportedOperationException("A JSON object cannot be converted to an array.");
    }

    @Override
    public Map<String, JsonNode> asObject() {
        return value;
    }

    @Override
    public <T> T visit(JsonNodeVisitor<T> visitor) {
        return visitor.visitObject(asObject());
    }

    @Override
    public Object asEmbeddedObject() {
        throw new UnsupportedOperationException("A JSON object cannot be converted to an embedded object.");
    }

    @Override
    public String text() {
        return null;
    }

    @Override
    public Optional<JsonNode> field(String child) {
        return Optional.ofNullable(value.get(child));
    }

    @Override
    public String toString() {
        if (value.isEmpty()) {
            return "{}";
        }

        StringBuilder output = new StringBuilder();
        output.append("{");
        value.forEach((k, v) -> output.append("\"").append(k).append("\": ")
                                      .append(v.toString()).append(","));
        output.setCharAt(output.length() - 1, '}');
        return output.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ObjectJsonNode that = (ObjectJsonNode) o;

        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
