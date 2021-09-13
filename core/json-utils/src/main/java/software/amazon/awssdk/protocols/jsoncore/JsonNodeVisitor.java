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

package software.amazon.awssdk.protocols.jsoncore;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Converter from a {@link JsonNode} to a new type. This is usually invoked via {@link JsonNode#visit(JsonNodeVisitor)}.
 */
@SdkProtectedApi
public interface JsonNodeVisitor<T> {
    /**
     * Invoked if {@link JsonNode#visit(JsonNodeVisitor)} is invoked on a null JSON node.
     */
    T visitNull();

    /**
     * Invoked if {@link JsonNode#visit(JsonNodeVisitor)} is invoked on a boolean JSON node.
     */
    T visitBoolean(boolean bool);

    /**
     * Invoked if {@link JsonNode#visit(JsonNodeVisitor)} is invoked on a number JSON node.
     */
    T visitNumber(String number);

    /**
     * Invoked if {@link JsonNode#visit(JsonNodeVisitor)} is invoked on a string JSON node.
     */
    T visitString(String string);

    /**
     * Invoked if {@link JsonNode#visit(JsonNodeVisitor)} is invoked on an array JSON node.
     */
    T visitArray(List<JsonNode> array);

    /**
     * Invoked if {@link JsonNode#visit(JsonNodeVisitor)} is invoked on an object JSON node.
     */
    T visitObject(Map<String, JsonNode> object);

    /**
     * Invoked if {@link JsonNode#visit(JsonNodeVisitor)} is invoked on an embedded object JSON node.
     */
    T visitEmbeddedObject(Object embeddedObject);
}
