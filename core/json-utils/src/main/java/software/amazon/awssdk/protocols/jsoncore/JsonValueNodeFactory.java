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

import java.io.IOException;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.protocols.jsoncore.internal.BooleanJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.EmbeddedObjectJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.NullJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.NumberJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.StringJsonNode;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParser;
import software.amazon.awssdk.thirdparty.jackson.core.JsonToken;

/**
 * Parses JSON tokens into JsonNode's values. Used only for atomic values.
 */
@SdkProtectedApi
public interface JsonValueNodeFactory {

    /**
     * Default implementation. Takes the tokens and returns JsonNode values based on its string representation.
     */
    JsonValueNodeFactory DEFAULT = (parser, token) -> {
        switch (token) {
            case VALUE_STRING:
                return new StringJsonNode(parser.getText());
            case VALUE_FALSE:
                return new BooleanJsonNode(false);
            case VALUE_TRUE:
                return new BooleanJsonNode(true);
            case VALUE_NULL:
                return NullJsonNode.instance();
            case VALUE_NUMBER_FLOAT:
            case VALUE_NUMBER_INT:
                return new NumberJsonNode(parser.getText());
            case VALUE_EMBEDDED_OBJECT:
                return new EmbeddedObjectJsonNode(parser.getEmbeddedObject());
            default:
                throw new IllegalArgumentException("Unexpected JSON token - " + token);
        }
    };

    JsonNode node(JsonParser parser, JsonToken token) throws IOException;
}
