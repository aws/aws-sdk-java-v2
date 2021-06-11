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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.protocols.jsoncore.internal.ObjectJsonNode;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;

/**
 * A node in a JSON document. Either a number, string, boolean, array, object or null. Also can be an embedded object,
 * which is a non-standard type used in JSON extensions, like CBOR.
 *
 * <p>Created from a JSON document via {@link #parser()} or {@link #parserBuilder()}.
 *
 * <p>The type of node can be determined using "is" methods like {@link #isNumber()} and {@link #isString()}.
 * Once the type is determined, the value of the node can be extracted via the "as" methods, like {@link #asNumber()}
 * and {@link #asString()}.
 */
@SdkProtectedApi
public interface JsonNode {
    /**
     * Create a {@link JsonNodeParser} for generating a {@link JsonNode} from a JSON document.
     */
    static JsonNodeParser parser() {
        return JsonNodeParser.create();
    }

    /**
     * Create a {@link JsonNodeParser.Builder} for generating a {@link JsonNode} from a JSON document.
     */
    static JsonNodeParser.Builder parserBuilder() {
        return JsonNodeParser.builder();
    }

    /**
     * Return an empty object node.
     */
    static JsonNode emptyObjectNode() {
        return new ObjectJsonNode(Collections.emptyMap());
    }

    /**
     * Returns true if this node represents a JSON number: https://datatracker.ietf.org/doc/html/rfc8259#section-6
     *
     * @see #asNumber()
     */
    default boolean isNumber() {
        return false;
    }

    /**
     * Returns true if this node represents a JSON string: https://datatracker.ietf.org/doc/html/rfc8259#section-7
     *
     * @see #asString()
     */
    default boolean isString() {
        return false;
    }

    /**
     * Returns true if this node represents a JSON boolean: https://datatracker.ietf.org/doc/html/rfc8259#section-3
     *
     * @see #asBoolean()
     */
    default boolean isBoolean() {
        return false;
    }

    /**
     * Returns true if this node represents a JSON null: https://datatracker.ietf.org/doc/html/rfc8259#section-3
     */
    default boolean isNull() {
        return false;
    }

    /**
     * Returns true if this node represents a JSON array: https://datatracker.ietf.org/doc/html/rfc8259#section-5
     *
     * @see #asArray()
     */
    default boolean isArray() {
        return false;
    }

    /**
     * Returns true if this node represents a JSON object: https://datatracker.ietf.org/doc/html/rfc8259#section-4
     *
     * @see #asObject()
     */
    default boolean isObject() {
        return false;
    }

    /**
     * Returns true if this node represents a JSON "embedded object". This non-standard type is associated with JSON extensions,
     * like CBOR or ION. It allows additional data types to be embedded in a JSON document, like a timestamp or a raw byte array.
     *
     * <p>Users who are only concerned with handling JSON can ignore this field. It will only be present when using a custom
     * {@link JsonFactory} via {@link JsonNodeParser.Builder#jsonFactory(JsonFactory)}.
     *
     * @see #asEmbeddedObject()
     */
    default boolean isEmbeddedObject() {
        return false;
    }

    /**
     * When {@link #isNumber()} is true, this returns the number associated with this node. This will throw an exception if
     * {@link #isNumber()} is false.
     *
     * @see #text()
     */
    String asNumber();

    /**
     * When {@link #isString()}, is true, this returns the string associated with this node. This will throw an exception if
     * {@link #isString()} ()} is false.
     */
    String asString();

    /**
     * When {@link #isBoolean()} is true, this returns the boolean associated with this node. This will throw an exception if
     * {@link #isBoolean()} is false.
     */
    boolean asBoolean();

    /**
     * When {@link #isArray()} is true, this returns the array associated with this node. This will throw an exception if
     * {@link #isArray()} is false.
     */
    List<JsonNode> asArray();

    /**
     * When {@link #isObject()} is true, this returns the object associated with this node. This will throw an exception if
     * {@link #isObject()} is false.
     */
    Map<String, JsonNode> asObject();

    /**
     * When {@link #isEmbeddedObject()} is true, this returns the embedded object associated with this node. This will throw
     * an exception if {@link #isEmbeddedObject()} is false.
     *
     * @see #isEmbeddedObject()
     */
    Object asEmbeddedObject();

    /**
     * When {@link #isString()}, {@link #isBoolean()}, or {@link #isNumber()} is true, this will return the value of this node
     * as a textual string. If this is any other type, this will return null.
     */
    String text();

    /**
     * When {@link #isObject()} is true, this will return the result of {@code Optional.ofNullable(asObject().get(child))}. If
     * this is any other type, this will return {@link Optional#empty()}.
     */
    default Optional<JsonNode> get(String child) {
        return Optional.empty();
    }

    /**
     * When {@link #isArray()} is true, this will return the result of {@code asArray().get(child)} if child is within bounds. If
     * this is any other type or the child is out of bounds, this will return {@link Optional#empty()}.
     */
    default Optional<JsonNode> get(int child) {
        return Optional.empty();
    }
}
