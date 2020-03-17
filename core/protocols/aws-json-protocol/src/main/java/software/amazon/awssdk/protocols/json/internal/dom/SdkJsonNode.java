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

package software.amazon.awssdk.protocols.json.internal.dom;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * DOM interface for reading a JSON document.
 */
@SdkInternalApi
public interface SdkJsonNode {

    /**
     * @return True if the node represents an explicit JSON null, false otherwise.
     */
    default boolean isNull() {
        return false;
    }

    /**
     * @return The value of the node as text. Returns null for most nodes except for {@link SdkScalarNode}.
     */
    default String asText() {
        return null;
    }

    /**
     * @return The embedded object value of the node. See {@link SdkEmbeddedObject}.
     */
    default Object embeddedObject() {
        return null;
    }

    /**
     * @param fieldName Field to get value for.
     * @return Value of field in the JSON object if this node represents an object, otherwise returns null.
     */
    default SdkJsonNode get(String fieldName) {
        return null;
    }

    /**
     * @return If this node represents a JSON array, then this returns the list of items in that array. Otherwise returns null.
     */
    default List<SdkJsonNode> items() {
        return null;
    }

    /**
     * @return If this node represents a JSON object, then this returns the map of field names to field values in that
     * object. Otherwise returns null.
     */
    default Map<String, SdkJsonNode> fields() {
        return null;
    }
}
