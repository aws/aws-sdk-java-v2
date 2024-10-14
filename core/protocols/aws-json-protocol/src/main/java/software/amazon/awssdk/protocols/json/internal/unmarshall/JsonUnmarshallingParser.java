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
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingKnownType;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.core.traits.TraitType;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.protocols.jsoncore.JsonValueNodeFactory;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParseException;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParser;
import software.amazon.awssdk.thirdparty.jackson.core.JsonToken;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.builder.Buildable;

/**
 * Parses and unmarshalls an JSON document.
 */
@SdkInternalApi
@ThreadSafe
@SuppressWarnings("unchecked")
final class JsonUnmarshallingParser {
    private final JsonFactory jsonFactory;
    private final JsonValueNodeFactory jsonValueNodeFactory;
    private final JsonUnmarshallerRegistry unmarshallerRegistry;
    private final TimestampFormatTrait.Format defaultFormat;

    private JsonUnmarshallingParser(Builder builder) {
        this.jsonFactory = builder.jsonFactory;
        this.jsonValueNodeFactory = builder.jsonValueNodeFactory;
        this.unmarshallerRegistry = builder.unmarshallerRegistry;
        this.defaultFormat = builder.defaultFormat;
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
                    throw new JsonParseException("expecting start object, got instead: " + token);
                }
                return parseSdkPojo(c, pojo, parser);
            }
        });
    }

    /**
     * Parses an sdk pojo and fills its fields. The given SdkPojo instance is expected to be a {@link Buildable} instance. This
     * method expects that the START_OBJECT token has been already consumed, so the next token should be either a field name or an
     * END_OBJECT.
     */
    private SdkPojo parseSdkPojo(JsonUnmarshallerContext c, SdkPojo pojo, JsonParser parser) throws IOException {
        Map<String, SdkField<?>> pojoFields = pojo.sdkFieldNameToField();
        JsonToken currentToken = parser.nextToken();
        while (currentToken != JsonToken.END_OBJECT) {
            String fieldName = parser.getText();
            SdkField<?> pojoField = pojoFields.get(fieldName);
            // if the name of the field is unknown or the field is expected in a non-payload location (e.g., header), we ignore
            // its value here.
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

    /**
     * Returns true if the given location is considered as in the payload for unmarshalling. Those include
     * <ul>
     *     <li>{@link MarshallLocation#PAYLOAD}</li>
     *     <li>{@link MarshallLocation#PATH}</li>
     *     <li>{@link MarshallLocation#QUERY_PARAM}</li>
     *     <li>{@link MarshallLocation#GREEDY_PATH}</li>
     * </ul>
     */
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

    /**
     * Parses a list of the field member field info. This method expects that the BEGIN_ARRAY token has been already consumed.
     */
    private List<Object> parseList(JsonUnmarshallerContext c, SdkField<?> field, JsonParser parser) throws IOException {
        SdkField<Object> memberInfo = (SdkField<Object>) field.getTrait(ListTrait.class).memberFieldInfo();
        MarshallingType<?> marshallingType = memberInfo.marshallingType();
        List<Object> result = new ArrayList<>();
        JsonToken currentToken = parser.nextToken();

        // For lists of scalar types we use directly the unmarshaller here to reduce the work done, instead of calling the
        // valueFor method.

        if (isScalarType(marshallingType)) {
            MarshallingKnownType marshallingKnownType = marshallingType.getKnownType();
            while (currentToken != JsonToken.END_ARRAY) {
                result.add(simpleValueFor(field, marshallingKnownType, c, parser, currentToken));
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

    /**
     * Parses a map of the field member value field info. This method expects that the BEGIN_OBJECT token has been already
     * consumed.
     */
    private Map<String, Object> parseMap(JsonUnmarshallerContext c, SdkField<?> field, JsonParser parser) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        SdkField<Object> valueInfo = field.getTrait(MapTrait.class, TraitType.MAP_TRAIT).valueFieldInfo();
        MarshallingType<?> valueMarshallingType = valueInfo.marshallingType();

        // For maps of string to scalar types we use directly the unmarshaller here to reduce the work done, instead of
        // calling the valueFor method.
        JsonToken currentToken = parser.nextToken();
        if (isScalarType(valueMarshallingType)) {
            MarshallingKnownType valueMarshallingKnownType = valueMarshallingType.getKnownType();
            while (currentToken != JsonToken.END_OBJECT) {
                String fieldName = parser.getText();
                currentToken = parser.nextToken();
                Object valueFor = simpleValueFor(field, valueMarshallingKnownType, c, parser, currentToken);
                result.put(fieldName, valueFor);
                currentToken = parser.nextToken();
            }
            return result;
        }

        while (currentToken != JsonToken.END_OBJECT) {
            String fieldName = parser.getText();
            currentToken = parser.nextToken();
            Object valueFor = valueFor(valueInfo, c, valueMarshallingType, parser, currentToken);
            result.put(fieldName, valueFor);
            currentToken = parser.nextToken();
        }
        return result;
    }

    /**
     * Parses and returns the value for the given field. This can be a scalar value (e.g., number, string, boolean), or a
     * composite one (e.g., list, map, pojo). This method is expected to be called with a valid lookAhead token.
     */
    @SuppressWarnings("unchecked")
    private Object valueFor(
        SdkField<?> field,
        JsonUnmarshallerContext context,
        MarshallingType<?> type,
        JsonParser parser,
        JsonToken lookAhead
    ) throws IOException {
        MarshallingKnownType marshallingKnownType = type.getKnownType();
        // We check first if we are unmarshalling a document, if so we
        // delegate to a different method, this is needed since documents
        // have their own class to represent null values: NullDocument.
        if (marshallingKnownType == MarshallingKnownType.DOCUMENT) {
            return parseDocumentValue(context, parser, lookAhead);
        }
        if (lookAhead == JsonToken.VALUE_NULL) {
            if (marshallingKnownType == MarshallingKnownType.DOCUMENT) {
                return Document.fromNull();
            }
            return null;
        }
        switch (marshallingKnownType) {
            case DOCUMENT:
                return parseDocumentValue(context, parser, lookAhead);
            case SDK_POJO:
                expect(lookAhead, JsonToken.START_OBJECT);
                return parseSdkPojo(context, field.constructor().get(), parser);
            case LIST:
                expect(lookAhead, JsonToken.START_ARRAY);
                return parseList(context, field, parser);
            case MAP:
                expect(lookAhead, JsonToken.START_OBJECT);
                return parseMap(context, field, parser);
            case INSTANT:
                return instantValueFor(field, parser, context, lookAhead);
            default:
                if (lookAhead == JsonToken.VALUE_STRING
                    && marshallingKnownType != MarshallingKnownType.STRING
                    && marshallingKnownType != MarshallingKnownType.SDK_BYTES
                ) {
                    JsonUnmarshaller<Object> unmarshaller = unmarshallerRegistry.getUnmarshaller(MarshallLocation.PAYLOAD, type);
                    return unmarshaller.unmarshall(context, jsonValueNodeFactory.node(parser, lookAhead),
                                                   (SdkField<Object>) field);
                }
                return simpleValueFor(field, marshallingKnownType, context, parser, lookAhead);
        }
    }

    /**
     * Returns a parsed simple value for the given SdkField.
     */
    private Object simpleValueFor(
        SdkField<?> field,
        MarshallingKnownType knownType,
        JsonUnmarshallerContext context,
        JsonParser parser,
        JsonToken lookAhead
    ) throws IOException {
        if (lookAhead == JsonToken.VALUE_NULL) {
            return null;
        }
        switch (knownType) {
            case INTEGER:
                expect(lookAhead, JsonToken.VALUE_NUMBER_INT);
                return parser.getIntValue();
            case LONG:
                expect(lookAhead, JsonToken.VALUE_NUMBER_INT);
                return parser.getLongValue();
            case SHORT:
                expect(lookAhead, JsonToken.VALUE_NUMBER_INT);
                return parser.getShortValue();
            case BYTE:
                expect(lookAhead, JsonToken.VALUE_NUMBER_INT);
                return parser.getByteValue();
            case FLOAT:
                expect(lookAhead, JsonToken.VALUE_NUMBER_INT, JsonToken.VALUE_NUMBER_FLOAT);
                return parser.getFloatValue();
            case DOUBLE:
                expect(lookAhead, JsonToken.VALUE_NUMBER_INT, JsonToken.VALUE_NUMBER_FLOAT);
                return parser.getDoubleValue();
            case BIG_DECIMAL:
                expect(lookAhead, JsonToken.VALUE_NUMBER_INT, JsonToken.VALUE_NUMBER_FLOAT);
                return parser.getDecimalValue();
            case BOOLEAN:
                expect(lookAhead, JsonToken.VALUE_FALSE, JsonToken.VALUE_TRUE);
                return parser.getBooleanValue();
            case INSTANT:
                return instantValueFor(field, parser, context, lookAhead);
            case STRING:
                // At least one protocol tests expects a floating number
                // to be parsed as string, so we can't assert that:
                // expect(lookAhead, JsonToken.VALUE_STRING);
                return parser.getText();
            case SDK_BYTES:
                if (lookAhead == JsonToken.VALUE_EMBEDDED_OBJECT) {
                    return SdkBytes.fromByteArray((byte[]) parser.getEmbeddedObject());
                }
                expect(lookAhead, JsonToken.VALUE_STRING);
                return SdkBytes.fromByteArray(BinaryUtils.fromBase64(parser.getText()));
            default:
                throw new JsonParseException("unexpected token, expecting token for: " + knownType + ", got: " + lookAhead);
        }
    }

    /**
     * Consumes all the needed tokens that represent a single value, the value can be scalar or composite. If lookAhead is null a
     * new token is consumed.
     */
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
                throw new JsonParseException("unexpected JSON token - " + current);
        }
    }

    /**
     * Validates that the lookAhead token is of the given type, throws a JsonParseException otherwise.
     */
    private void expect(JsonToken lookAhead, JsonToken expected) throws IOException {
        if (lookAhead != expected) {
            throw new JsonParseException("unexpected token, expecting token: " + expected + ", got: " + lookAhead);
        }
    }

    /**
     * Validates that the lookAhead token is of either of the given type, throws a JsonParseException otherwise.
     */
    private void expect(JsonToken lookAhead, JsonToken expected0, JsonToken expected1) throws IOException {
        if (lookAhead != expected0 && lookAhead != expected1) {
            throw new JsonParseException("unexpected token, expecting token: "
                                         + expected0 + ", or " + expected1 + ", got: " + lookAhead);
        }
    }

    /**
     * Parses and returns an {@link Instant} value for a timestamp field.
     */
    private Instant instantValueFor(
        SdkField<?> field,
        JsonParser parser,
        JsonUnmarshallerContext context,
        JsonToken lookAhead
    ) throws IOException {
        TimestampFormatTrait.Format format = resolveTimestampFormat(field);
        switch (format) {
            case UNIX_TIMESTAMP:
                return Instant.ofEpochMilli((long) (parser.getDoubleValue() * 1_000d));
            case UNIX_TIMESTAMP_MILLIS:
                return Instant.ofEpochMilli(parser.getLongValue());
            default:
                JsonUnmarshaller<Object> unmarshaller = unmarshallerRegistry.getUnmarshaller(MarshallLocation.PAYLOAD,
                                                                                             field.marshallingType());
                return (Instant) unmarshaller.unmarshall(context, jsonValueNodeFactory.node(parser, lookAhead),
                                                         (SdkField<Object>) field);
        }
    }

    /**
     * Returns the timestamp format for the give field.
     */
    private TimestampFormatTrait.Format resolveTimestampFormat(SdkField<?> field) {
        TimestampFormatTrait trait = field.getTrait(TimestampFormatTrait.class, TraitType.TIMESTAMP_FORMAT_TRAIT);
        if (trait == null) {
            return defaultFormat;
        } else {
            return trait.format();
        }
    }

    /**
     * Returns true if the marshallingType is composite, i.e., non-scalar.
     */
    private boolean isCompositeType(MarshallingType<?> marshallingType) {
        return marshallingType == MarshallingType.LIST
               || marshallingType == MarshallingType.MAP
               || marshallingType == MarshallingType.SDK_POJO
               || marshallingType == MarshallingType.DOCUMENT;
    }

    /**
     * Returns true if the marshallingType is scalar, i.e., non-composite.
     */
    private boolean isScalarType(MarshallingType<?> marshallingType) {
        return !isCompositeType(marshallingType);
    }

    /**
     * Parses a {@link Document} value, either composite or scalar.
     */
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
                return parseDocumentList(c, parser);
            case START_OBJECT:
                return parseDocumentMap(c, parser);
            default:
                throw new JsonParseException("unexpected JSON token - " + token);
        }
    }

    /**
     * Parses a document list. This method expects that the BEING_ARRAY token has been already consumed.
     */
    private Document parseDocumentList(JsonUnmarshallerContext c, JsonParser parser) throws IOException {
        Document.ListBuilder builder = Document.listBuilder();
        JsonToken currentToken = parser.nextToken();
        while (currentToken != JsonToken.END_ARRAY) {
            builder.addDocument(parseDocumentValue(c, parser, currentToken));
            currentToken = parser.nextToken();
        }
        return builder.build();
    }

    /**
     * Parses a document map. This method expects that the BEING_OBJECT token has been already consumed.
     */
    private Document parseDocumentMap(JsonUnmarshallerContext c, JsonParser parser) throws IOException {
        Document.MapBuilder builder = Document.mapBuilder();
        JsonToken currentToken = parser.nextToken();
        while (currentToken != JsonToken.END_OBJECT) {
            String key = parser.getText();
            Document value = parseDocumentValue(c, parser, null);
            builder.putDocument(key, value);
            currentToken = parser.nextToken();
        }
        return builder.build();
    }

    /**
     * A builder for configuring and creating {@link JsonUnmarshallingParser}. Created via {@link #builder()}.
     */
    public static final class Builder {
        private JsonFactory jsonFactory;
        private JsonValueNodeFactory jsonValueNodeFactory = JsonValueNodeFactory.DEFAULT;
        private JsonUnmarshallerRegistry unmarshallerRegistry;
        private TimestampFormatTrait.Format defaultFormat;

        private Builder() {
        }

        /**
         * The {@link JsonFactory} implementation to be used when parsing the input. This allows JSON extensions like CBOR or Ion
         * to be supported.
         *
         * <p>It's highly recommended us use a shared {@code JsonFactory} where possible, so they should be stored statically:
         * http://wiki.fasterxml.com/JacksonBestPracticesPerformance
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
         * Default timestamp format for payload location.
         */
        public Builder defaultTimestampFormat(TimestampFormatTrait.Format defaultFormat) {
            this.defaultFormat = defaultFormat;
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
