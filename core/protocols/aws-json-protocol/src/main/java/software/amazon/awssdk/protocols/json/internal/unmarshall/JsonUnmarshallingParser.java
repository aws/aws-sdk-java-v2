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

package software.amazon.awssdk.protocols.json.internal.unmarshall;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingKnownType;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.core.traits.TraitType;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.protocols.jsoncore.JsonValueNodeFactory;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParseException;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParser;
import software.amazon.awssdk.thirdparty.jackson.core.JsonToken;
import software.amazon.awssdk.thirdparty.jackson.core.json.JsonReadFeature;
import software.amazon.awssdk.utils.builder.Buildable;

/**
 * Parses and unmarshalls an JSON document.
 */
@SdkInternalApi
@ThreadSafe
@SuppressWarnings("unchecked")
final class JsonUnmarshallingParser {
    /**
     * The default {@link JsonFactory} used for or if a factory is not configured via {@link Builder#jsonFactory(JsonFactory)}.
     */
    public static final JsonFactory DEFAULT_JSON_FACTORY =
        JsonFactory.builder()
                   .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
                   .build();

    private final boolean removeErrorLocations;
    private final JsonFactory jsonFactory;
    private final JsonValueNodeFactory jsonValueNodeFactory;
    private final JsonUnmarshallerRegistry unmarshallerRegistry;

    private JsonUnmarshallingParser(Builder builder) {
        this.removeErrorLocations = builder.removeErrorLocations;
        this.jsonFactory = builder.jsonFactory;
        this.jsonValueNodeFactory = builder.jsonValueNodeFactory;
        this.unmarshallerRegistry = builder.unmarshallerRegistry;
    }

    /**
     * Create a parser using custom configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parse the provided {@link InputStream} and return the deserialized {@link SdkPojo}.
     */
    public SdkPojo parse(SdkPojo pojo, InputStream content) {
        return invokeSafely(() -> {
            try (JsonParser parser = jsonFactory.createParser(content)
                                                .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)) {

                JsonUnmarshallerContext c = JsonUnmarshallerContext.builder().build();
                JsonToken token = parser.nextToken();
                if (token == null) {
                    return (SdkPojo) ((Buildable) pojo).build();
                }
                if (token == JsonToken.VALUE_NULL) {
                    return null;
                }
                if (token != JsonToken.START_OBJECT) {
                    throw new JsonParseException("expecting start object");
                }
                return parseSdkPojo(c, pojo, parser);
            } catch (RuntimeException e) {
                removeErrorLocationsIfRequired(e);
                throw e;
            }
        });
    }

    private SdkPojo parseSdkPojo(JsonUnmarshallerContext c, SdkPojo pojo, JsonParser parser) throws IOException {
        Map<String, SdkField<?>> pojoFields = pojo.sdkFieldNameToField();
        JsonToken currentToken = parser.nextToken();
        while (currentToken != JsonToken.END_OBJECT) {
            String fieldName = parser.getText();
            SdkField<?> pojoField = pojoFields.get(fieldName);
            if (pojoField == null || !isPayloadUnmarshalling(pojoField.location())) {
                skipValue(parser, null);
                currentToken = parser.nextToken();
                continue;
            }
            currentToken = parser.nextToken();
            Object valueFor = valueFor(pojoField, c, pojoField.marshallingType(), parser, currentToken);
            pojoField.set(pojo, valueFor);
            currentToken = parser.nextToken();
        }

        return (SdkPojo) ((Buildable) pojo).build();
    }

    private boolean isPayloadUnmarshalling(MarshallLocation location) {
        switch (location) {
            case PAYLOAD:
            case PATH:
            case QUERY_PARAM:
            case GREEDY_PATH:
                return true;
            default:
                return false;
        }
    }

    private List<Object> parseList(JsonUnmarshallerContext c, SdkField<?> field, JsonParser parser) throws IOException {
        SdkField<Object> memberInfo = (SdkField<Object>) field.getTrait(ListTrait.class).memberFieldInfo();
        MarshallingType<?> marshallingType = memberInfo.marshallingType();
        List<Object> result = new ArrayList<>();
        JsonToken currentToken = parser.nextToken();

        if (!isAggregateType(marshallingType)) {
            JsonUnmarshaller<Object> unmarshaller = unmarshallerRegistry.getUnmarshaller(MarshallLocation.PAYLOAD,
                                                                                         marshallingType);
            while (currentToken != JsonToken.END_ARRAY) {
                result.add(unmarshaller.unmarshall(c, jsonValueNodeFactory.node(parser, currentToken), (SdkField<Object>) field));
                currentToken = parser.nextToken();
            }
            return result;
        }

        while (currentToken != JsonToken.END_ARRAY) {
            result.add(valueFor(memberInfo, c, marshallingType, parser, currentToken));
            currentToken = parser.nextToken();
        }
        return result;
    }

    private Map<String, Object> parseMap(JsonUnmarshallerContext c, SdkField<?> field, JsonParser parser) throws IOException {
        JsonToken currentToken = parser.nextToken();
        Map<String, Object> result = new LinkedHashMap<>();
        SdkField<Object> valueInfo = field.getTrait(MapTrait.class, TraitType.MAP_TRAIT).valueFieldInfo();
        MarshallingType<?> marshallingType = valueInfo.marshallingType();

        if (!isAggregateType(marshallingType)) {
            JsonUnmarshaller<Object> unmarshaller = unmarshallerRegistry.getUnmarshaller(MarshallLocation.PAYLOAD,
                                                                                         marshallingType);
            while (currentToken != JsonToken.END_OBJECT) {
                String fieldName = parser.getText();
                currentToken = parser.nextToken();
                Object valueFor = unmarshaller.unmarshall(c, jsonValueNodeFactory.node(parser, currentToken),
                                                          (SdkField<Object>) field);
                result.put(fieldName, valueFor);
                currentToken = parser.nextToken();
            }
            return result;
        }

        while (currentToken != JsonToken.END_OBJECT) {
            String fieldName = parser.getText();
            currentToken = parser.nextToken();
            Object valueFor = valueFor(valueInfo, c, marshallingType, parser, currentToken);
            result.put(fieldName, valueFor);
            currentToken = parser.nextToken();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object valueFor(
        SdkField<?> field,
        JsonUnmarshallerContext context,
        MarshallingType<?> type,
        JsonParser parser,
        JsonToken lookAhead
    ) throws IOException {
        MarshallingKnownType knownType = type.getKnownType();
        if (knownType == MarshallingKnownType.DOCUMENT) {
            return parseDocumentValue(context, parser, lookAhead);
        }
        if (lookAhead == JsonToken.VALUE_NULL) {
            return null;
        }
        if (knownType == MarshallingKnownType.SDK_POJO) {
            if (lookAhead != JsonToken.START_OBJECT) {
                throw new JsonParseException("expecting start object, got instead: " + lookAhead);
            }
            return parseSdkPojo(context, field.constructor().get(), parser);
        }
        if (knownType == MarshallingKnownType.LIST) {
            if (lookAhead != JsonToken.START_ARRAY) {
                throw new JsonParseException("expecting start array, got instead: " + lookAhead);
            }
            return parseList(context, field, parser);
        }
        if (knownType == MarshallingKnownType.MAP) {
            if (lookAhead != JsonToken.START_OBJECT) {
                throw new JsonParseException("expecting start object, got instead: " + lookAhead);
            }
            return parseMap(context, field, parser);
        }
        JsonUnmarshaller<Object> unmarshaller = unmarshallerRegistry.getUnmarshaller(MarshallLocation.PAYLOAD, type);
        return unmarshaller.unmarshall(context, jsonValueNodeFactory.node(parser, lookAhead), (SdkField<Object>) field);
    }

    private void skipValue(JsonParser parser, JsonToken lookAhead) throws IOException {
        JsonToken current = lookAhead != null ? lookAhead : parser.nextToken();
        switch (current) {
            case VALUE_STRING:
            case VALUE_FALSE:
            case VALUE_TRUE:
            case VALUE_NULL:
            case VALUE_NUMBER_FLOAT:
            case VALUE_NUMBER_INT:
            case VALUE_EMBEDDED_OBJECT:
                return;
            case START_OBJECT:
                do {
                    // skip field name
                    current = parser.nextToken();
                    if (current == JsonToken.END_OBJECT) {
                        break;
                    }
                    skipValue(parser, null);
                } while (true);
                return;
            case START_ARRAY:
                do {
                    current = parser.nextToken();
                    if (current == JsonToken.END_ARRAY) {
                        break;
                    }
                    skipValue(parser, current);
                } while (true);
                return;
            default:
                throw new IllegalArgumentException("Unexpected JSON token - " + current);
        }
    }

    private boolean isAggregateType(MarshallingType<?> marshallingType) {
        return marshallingType == MarshallingType.LIST
               || marshallingType == MarshallingType.MAP
               || marshallingType == MarshallingType.SDK_POJO
               || marshallingType == MarshallingType.DOCUMENT;
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

    private Document parseDocumentValue(JsonUnmarshallerContext c, JsonParser parser, JsonToken lookAhead) throws IOException {
        JsonToken token = lookAhead != null ? lookAhead : parser.nextToken();
        switch (token) {
            case VALUE_STRING:
                return Document.fromString(parser.getText());
            case VALUE_NUMBER_FLOAT:
            case VALUE_NUMBER_INT:
                return Document.fromNumber(parser.getText());
            case VALUE_FALSE:
                return Document.fromBoolean(false);
            case VALUE_TRUE:
                return Document.fromBoolean(true);
            case VALUE_NULL:
                return Document.fromNull();
            case START_ARRAY:
                return parseDocumentArray(c, parser);
            case START_OBJECT:
                return parseDocumentObject(c, parser);
            default:
                throw new RuntimeException("unknown token found: " + token);
        }
    }

    private Document parseDocumentArray(JsonUnmarshallerContext c, JsonParser parser) throws IOException {
        List<Document> result = new ArrayList<>();
        JsonToken token = parser.nextToken();
        while (token != JsonToken.END_ARRAY) {
            result.add(parseDocumentValue(c, parser, token));
            token = parser.nextToken();
        }
        return Document.fromList(result);
    }

    private Document parseDocumentObject(JsonUnmarshallerContext c, JsonParser parser) throws IOException {
        Map<String, Document> result = new LinkedHashMap<>();
        JsonToken token = parser.nextToken();
        while (token != JsonToken.END_OBJECT) {
            if (token != JsonToken.FIELD_NAME) {
                throw new IllegalArgumentException("Unexpected JSON token - " + token);
            }
            String key = parser.getText();
            Document value = parseDocumentValue(c, parser, null);
            result.put(key, value);
            token = parser.nextToken();
        }
        return Document.fromMap(result);
    }

    /**
     * A builder for configuring and creating {@link JsonUnmarshallingParser}. Created via {@link #builder()}.
     */
    public static final class Builder {
        private JsonFactory jsonFactory = DEFAULT_JSON_FACTORY;
        private JsonValueNodeFactory jsonValueNodeFactory = JsonValueNodeFactory.DEFAULT;
        private boolean removeErrorLocations = false;
        private JsonUnmarshallerRegistry unmarshallerRegistry;

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
         * The {@link JsonFactory} implementation to be used when parsing the input. This allows JSON extensions like CBOR or Ion
         * to be supported.
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
         * Factory to create JsonNode out of JSON tokens. This allows JSON variants, such as CBOR, to produce actual values
         * instead of having to parse them out of strings.
         *
         * <p>By default, this is {@link JsonValueNodeFactory#DEFAULT}.
         */
        public Builder jsonValueNodeFactory(JsonValueNodeFactory jsonValueNodeFactory) {
            this.jsonValueNodeFactory = jsonValueNodeFactory;
            return this;
        }

        /**
         * Unmarshaller registry used to convert from JSON to Java values.
         */
        public Builder unmarshallerRegistry(JsonUnmarshallerRegistry unmarshallerRegistry) {
            this.unmarshallerRegistry = unmarshallerRegistry;
            return this;
        }

        /**
         * Build a {@link JsonNodeParser} based on the current configuration of this builder.
         */
        public JsonUnmarshallingParser build() {
            return new JsonUnmarshallingParser(this);
        }
    }
}
