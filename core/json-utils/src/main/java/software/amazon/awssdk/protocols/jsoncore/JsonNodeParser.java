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

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.protocols.jsoncore.internal.ArrayJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.BooleanJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.EmbeddedObjectJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.NullJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.NumberJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.ObjectJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.StringJsonNode;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParseException;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParser;
import software.amazon.awssdk.thirdparty.jackson.core.JsonToken;
import software.amazon.awssdk.thirdparty.jackson.core.json.JsonReadFeature;

/**
 * Parses an JSON document into a simple DOM-like structure, {@link JsonNode}.
 *
 * <p>This is created using {@link #create()} or {@link #builder()}.
 */
@SdkProtectedApi
public final class JsonNodeParser {
    /**
     * The default {@link JsonFactory} used for {@link #create()} or if a factory is not configured via
     * {@link Builder#jsonFactory(JsonFactory)}.
     */
    public static final JsonFactory DEFAULT_JSON_FACTORY =
        JsonFactory.builder()
                   .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
                   .build();

    private final boolean removeErrorLocations;
    private final JsonFactory jsonFactory;

    private JsonNodeParser(Builder builder) {
        this.removeErrorLocations = builder.removeErrorLocations;
        this.jsonFactory = builder.jsonFactory;
    }

    /**
     * Create a parser using the default configuration.
     */
    public static JsonNodeParser create() {
        return builder().build();
    }

    /**
     * Create a parser using custom configuration.
     */
    public static JsonNodeParser.Builder builder() {
        return new Builder();
    }

    /**
     * Parse the provided {@link InputStream} into a {@link JsonNode}.
     */
    public JsonNode parse(InputStream content) {
        return invokeSafely(() -> {
            try (JsonParser parser = jsonFactory.createParser(content)
                                                .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)) {
                return parse(parser);
            }
        });
    }

    /**
     * Parse the provided {@code byte[]} into a {@link JsonNode}.
     */
    public JsonNode parse(byte[] content) {
        return invokeSafely(() -> {
            try (JsonParser parser = jsonFactory.createParser(content)
                                                .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)) {
                return parse(parser);
            }
        });
    }

    /**
     * Parse the provided {@link String} into a {@link JsonNode}.
     */
    public JsonNode parse(String content) {
        return invokeSafely(() -> {
            try (JsonParser parser = jsonFactory.createParser(content)
                                                .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)) {
                return parse(parser);
            }
        });
    }

    private JsonNode parse(JsonParser parser) throws IOException {
        try {
            return parseToken(parser, parser.nextToken());
        } catch (Exception e) {
            removeErrorLocationsIfRequired(e);
            throw e;
        }
    }

    private void removeErrorLocationsIfRequired(Throwable exception) {
        if (removeErrorLocations) {
            removeErrorLocations(exception);
        }
    }

    private void removeErrorLocations(Throwable exception) {
        if (exception == null) {
            return;
        }

        if (exception instanceof JsonParseException) {
            ((JsonParseException) exception).clearLocation();
        }

        removeErrorLocations(exception.getCause());
    }

    private JsonNode parseToken(JsonParser parser, JsonToken token) throws IOException {
        if (token == null) {
            return null;
        }
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
            case START_OBJECT:
                return parseObject(parser);
            case START_ARRAY:
                return parseArray(parser);
            case VALUE_EMBEDDED_OBJECT:
                return new EmbeddedObjectJsonNode(parser.getEmbeddedObject());
            default:
                throw new IllegalArgumentException("Unexpected JSON token - " + token);
        }
    }

    private JsonNode parseObject(JsonParser parser) throws IOException {
        JsonToken currentToken = parser.nextToken();
        Map<String, JsonNode> object = new LinkedHashMap<>();
        while (currentToken != JsonToken.END_OBJECT) {
            String fieldName = parser.getText();
            object.put(fieldName, parseToken(parser, parser.nextToken()));
            currentToken = parser.nextToken();
        }
        return new ObjectJsonNode(object);
    }

    private JsonNode parseArray(JsonParser parser) throws IOException {
        JsonToken currentToken = parser.nextToken();
        List<JsonNode> array = new ArrayList<>();
        while (currentToken != JsonToken.END_ARRAY) {
            array.add(parseToken(parser, currentToken));
            currentToken = parser.nextToken();
        }
        return new ArrayJsonNode(array);
    }

    /**
     * A builder for configuring and creating {@link JsonNodeParser}. Created via {@link #builder()}.
     */
    public static final class Builder {
        private JsonFactory jsonFactory = DEFAULT_JSON_FACTORY;
        private boolean removeErrorLocations = false;

        private Builder() {
        }

        /**
         * Whether error locations should be removed if parsing fails. This prevents the content of the JSON from appearing in
         * error messages. This is useful when the content of the JSON may be sensitive and not want to be logged.
         *
         * <p>By default, this is false.
         */
        public Builder removeErrorLocations(boolean removeErrorLocations) {
            this.removeErrorLocations = removeErrorLocations;
            return this;
        }

        /**
         * The {@link JsonFactory} implementation to be used when parsing the input. This allows JSON extensions like CBOR or
         * Ion to be supported.
         *
         * <p>It's highly recommended us use a shared {@code JsonFactory} where possible, so they should be stored statically:
         * http://wiki.fasterxml.com/JacksonBestPracticesPerformance
         *
         * <p>By default, this is {@link #DEFAULT_JSON_FACTORY}.
         */
        public Builder jsonFactory(JsonFactory jsonFactory) {
            this.jsonFactory = jsonFactory;
            return this;
        }

        /**
         * Build a {@link JsonNodeParser} based on the current configuration of this builder.
         */
        public JsonNodeParser build() {
            return new JsonNodeParser(this);
        }
    }
}
