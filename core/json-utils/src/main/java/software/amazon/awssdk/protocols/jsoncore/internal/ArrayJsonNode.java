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

/**
 * An array {@link JsonNode}.
 */
@SdkInternalApi
public final class ArrayJsonNode implements JsonNode {
    private final List<JsonNode> value;

    public ArrayJsonNode(List<JsonNode> value) {
        this.value = value;
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public String asNumber() {
        throw new UnsupportedOperationException("A JSON array cannot be converted to a number.");
    }

    @Override
    public String asString() {
        throw new UnsupportedOperationException("A JSON array cannot be converted to a string.");
    }

    @Override
    public boolean asBoolean() {
        throw new UnsupportedOperationException("A JSON array cannot be converted to a boolean.");
    }

    @Override
    public List<JsonNode> asArray() {
        return value;
    }

    @Override
    public Map<String, JsonNode> asObject() {
        throw new UnsupportedOperationException("A JSON array cannot be converted to an object.");
    }

    @Override
    public Object asEmbeddedObject() {
        throw new UnsupportedOperationException("A JSON array cannot be converted to an embedded object.");
    }

    @Override
    public String text() {
        return null;
    }

    @Override
    public Optional<JsonNode> get(int child) {
        if (child < 0 || child >= value.size()) {
            return Optional.empty();
        }
        return Optional.of(value.get(child));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
