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
 * An embedded object {@link JsonNode}.
 */
@SdkInternalApi
public final class EmbeddedObjectJsonNode implements JsonNode {
    private final Object embeddedObject;

    public EmbeddedObjectJsonNode(Object embeddedObject) {
        this.embeddedObject = embeddedObject;
    }

    @Override
    public boolean isEmbeddedObject() {
        return true;
    }

    @Override
    public String asNumber() {
        throw new UnsupportedOperationException("A JSON embedded object cannot be converted to a number.");
    }

    @Override
    public String asString() {
        throw new UnsupportedOperationException("A JSON embedded object cannot be converted to a string.");
    }

    @Override
    public boolean asBoolean() {
        throw new UnsupportedOperationException("A JSON embedded object cannot be converted to a boolean.");
    }

    @Override
    public List<JsonNode> asArray() {
        throw new UnsupportedOperationException("A JSON embedded object cannot be converted to an array.");
    }

    @Override
    public Map<String, JsonNode> asObject() {
        throw new UnsupportedOperationException("A JSON embedded object cannot be converted to an object.");
    }

    @Override
    public Object asEmbeddedObject() {
        return embeddedObject;
    }

    @Override
    public String text() {
        return null;
    }

    @Override
    public String toString() {
        return "<<Embedded Object (" + embeddedObject.getClass().getSimpleName() + ")>>";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EmbeddedObjectJsonNode that = (EmbeddedObjectJsonNode) o;

        return embeddedObject.equals(that.embeddedObject);
    }

    @Override
    public int hashCode() {
        return embeddedObject.hashCode();
    }
}
