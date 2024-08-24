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

package software.amazon.awssdk.protocols.rpcv2.internal;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonValueNodeFactory;
import software.amazon.awssdk.protocols.jsoncore.internal.EmbeddedObjectJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.NullJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.NumberJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.StringJsonNode;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParser;
import software.amazon.awssdk.thirdparty.jackson.core.JsonToken;
import software.amazon.awssdk.thirdparty.jackson.dataformat.cbor.CBORParser;

/**
 * Factory to create JsonNode that can embedded values from the CBOR parser.
 */
@SdkInternalApi
public final class SdkRpcV2CborValueNodeFactory implements JsonValueNodeFactory {
    public static final JsonValueNodeFactory INSTANCE = new SdkRpcV2CborValueNodeFactory();

    private SdkRpcV2CborValueNodeFactory() {
    }

    @Override
    public JsonNode node(JsonParser parser, JsonToken token) throws IOException {
        if (!(parser instanceof CBORParser)) {
            return DEFAULT.node(parser, token);
        }

        switch (token) {
            case VALUE_STRING:
                return new StringJsonNode(parser.getText());
            case VALUE_FALSE:
                return new EmbeddedObjectJsonNode(false);
            case VALUE_TRUE:
                return new EmbeddedObjectJsonNode(true);
            case VALUE_NULL:
                return NullJsonNode.instance();
            case VALUE_NUMBER_FLOAT:
            case VALUE_NUMBER_INT:
                return nodeForNumber(parser, token);
            case VALUE_EMBEDDED_OBJECT:
                return new EmbeddedObjectJsonNode(parser.getEmbeddedObject());
            default:
                throw new IllegalArgumentException("Unexpected JSON token - " + token);
        }
    }

    private JsonNode nodeForNumber(JsonParser parser, JsonToken token) throws IOException {
        JsonParser.NumberType numberType = parser.getNumberType();
        switch (numberType) {
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
                try {
                    Number javaNumber = parser.getNumberValue();
                    return new EmbeddedObjectJsonNode(javaNumber);
                } catch (Exception e) {
                    // ignored
                }
            case BIG_DECIMAL:
                try {
                    BigDecimal bigDecimal = parser.getDecimalValue();
                    return new EmbeddedObjectJsonNode(bigDecimal);
                } catch (Exception e) {
                    // ignored
                }
                break;
            case BIG_INTEGER:
                try {
                    BigInteger bigInteger = parser.getBigIntegerValue();
                    return new EmbeddedObjectJsonNode(bigInteger);
                } catch (Exception e) {
                    // ignored
                }
                break;
        }
        return new NumberJsonNode(parser.getText());
    }
}

