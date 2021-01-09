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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Parses an JSON document into a simple DOM like structure, {@link SdkJsonNode}.
 */
@SdkInternalApi
public final class JsonDomParser {

    private final JsonFactory jsonFactory;

    private JsonDomParser(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
    }

    public SdkJsonNode parse(InputStream content) throws IOException {
        try (JsonParser parser = jsonFactory.createParser(content)
                                            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)) {
            return parseToken(parser, parser.nextToken());
        }
    }

    private SdkJsonNode parseToken(JsonParser parser, JsonToken token) throws IOException {
        if (token == null) {
            return null;
        }
        switch (token) {
            case VALUE_EMBEDDED_OBJECT:
                return SdkEmbeddedObject.create(parser.getEmbeddedObject());
            case VALUE_STRING:
                return SdkScalarNode.create(parser.getText());
            case VALUE_FALSE:
                return SdkScalarNode.create("false");
            case VALUE_TRUE:
                return SdkScalarNode.create("true");
            case VALUE_NULL:
                return SdkNullNode.instance();
            case VALUE_NUMBER_FLOAT:
            case VALUE_NUMBER_INT:
                return SdkScalarNode.create(parser.getNumberValue().toString());
            case START_OBJECT:
                return parseObject(parser);
            case START_ARRAY:
                return parseArray(parser);
            default:
                throw SdkClientException.create("Unexpected JSON token - " + token);
        }
    }

    private SdkJsonNode parseObject(JsonParser parser) throws IOException {
        JsonToken currentToken = parser.nextToken();
        SdkObjectNode.Builder builder = SdkObjectNode.builder();
        while (currentToken != JsonToken.END_OBJECT) {
            String fieldName = parser.getText();
            builder.putField(fieldName, parseToken(parser, parser.nextToken()));
            currentToken = parser.nextToken();
        }
        return builder.build();
    }

    private SdkJsonNode parseArray(JsonParser parser) throws IOException {
        JsonToken currentToken = parser.nextToken();
        SdkArrayNode.Builder builder = SdkArrayNode.builder();
        while (currentToken != JsonToken.END_ARRAY) {
            builder.addItem(parseToken(parser, currentToken));
            currentToken = parser.nextToken();
        }
        return builder.build();
    }

    public static JsonDomParser create(JsonFactory jsonFactory) {
        return new JsonDomParser(jsonFactory);
    }
}
