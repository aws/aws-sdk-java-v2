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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.utils.Validate;

/**
 * A string {@link JsonNode}.
 */
@SdkInternalApi
public final class StringJsonNode implements JsonNode {
    private final String value;

    public StringJsonNode(String value) {
        Validate.paramNotNull(value, "value");
        this.value = value;
    }

    @Override
    public boolean isString() {
        return true;
    }

    @Override
    public String asNumber() {
        throw new UnsupportedOperationException("A JSON string cannot be converted to a number.");
    }

    @Override
    public String asString() {
        return value;
    }

    @Override
    public boolean asBoolean() {
        throw new UnsupportedOperationException("A JSON string cannot be converted to a boolean.");
    }

    @Override
    public List<JsonNode> asArray() {
        throw new UnsupportedOperationException("A JSON string cannot be converted to an array.");
    }

    @Override
    public Map<String, JsonNode> asObject() {
        throw new UnsupportedOperationException("A JSON string cannot be converted to an object.");
    }

    @Override
    public Object asEmbeddedObject() {
        throw new UnsupportedOperationException("A JSON string cannot be converted to an embedded object.");
    }

    @Override
    public String text() {
        return value;
    }

    @Override
    public String toString() {
        // Does not handle unicode control characters
        return "\"" +
               value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
               + "\"";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StringJsonNode that = (StringJsonNode) o;

        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}