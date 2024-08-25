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

import static software.amazon.awssdk.protocols.core.StringToValueConverter.TO_SDK_BYTES;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.core.NumberToInstant;
import software.amazon.awssdk.protocols.core.StringToInstant;
import software.amazon.awssdk.protocols.core.StringToValueConverter;
import software.amazon.awssdk.protocols.json.internal.MarshallerUtil;
import software.amazon.awssdk.protocols.json.internal.unmarshall.document.DocumentUnmarshaller;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.utils.builder.Buildable;

/**
 * Unmarshaller implementation for both JSON RPC and REST JSON services. This class is thread-safe and it is
 * recommended to reuse a single instance for best performance.
 */
@SdkInternalApi
@ThreadSafe
public final class JsonProtocolUnmarshaller {

    public final StringToValueConverter.StringToValue<Instant> instantStringToValue;

    private final NumberToInstant numberToInstant;

    private final JsonUnmarshallerRegistry registry;

    private final JsonNodeParser parser;

    private JsonProtocolUnmarshaller(Builder builder) {
        this.parser = builder.parser;
        this.instantStringToValue = StringToInstant.create(builder.defaultTimestampFormats.isEmpty() ?
                                                           new EnumMap<>(MarshallLocation.class) :
                                                           new EnumMap<>(builder.defaultTimestampFormats));
        this.numberToInstant = NumberToInstant.create(builder.defaultTimestampFormats.isEmpty() ?
                                                      new EnumMap<>(MarshallLocation.class) :
                                                      new EnumMap<>(builder.defaultTimestampFormats));
        this.registry = createUnmarshallerRegistry(instantStringToValue, numberToInstant);
    }

    private static JsonUnmarshallerRegistry createUnmarshallerRegistry(
        StringToValueConverter.StringToValue<Instant> instantStringToValue,
        NumberToInstant numberToInstant
    ) {

        return JsonUnmarshallerRegistry
            .builder()
            .statusCodeUnmarshaller(MarshallingType.INTEGER, (context, json, f) -> context.response().statusCode())
            .headerUnmarshaller(MarshallingType.STRING, HeaderUnmarshaller.STRING)
            .headerUnmarshaller(MarshallingType.INTEGER, HeaderUnmarshaller.INTEGER)
            .headerUnmarshaller(MarshallingType.LONG, HeaderUnmarshaller.LONG)
            .headerUnmarshaller(MarshallingType.SHORT, HeaderUnmarshaller.SHORT)
            .headerUnmarshaller(MarshallingType.DOUBLE, HeaderUnmarshaller.DOUBLE)
            .headerUnmarshaller(MarshallingType.BOOLEAN, HeaderUnmarshaller.BOOLEAN)
            .headerUnmarshaller(MarshallingType.INSTANT, HeaderUnmarshaller.createInstantHeaderUnmarshaller(instantStringToValue))
            .headerUnmarshaller(MarshallingType.FLOAT, HeaderUnmarshaller.FLOAT)
            .headerUnmarshaller(MarshallingType.LIST, HeaderUnmarshaller.LIST)

            .payloadUnmarshaller(MarshallingType.STRING, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_STRING))
            .payloadUnmarshaller(MarshallingType.INTEGER, forEmbeddable(Number.class, Number::intValue,
                                                                        StringToValueConverter.TO_INTEGER))
            .payloadUnmarshaller(MarshallingType.LONG, forEmbeddable(Number.class, Number::longValue,
                                                                     StringToValueConverter.TO_LONG))
            .payloadUnmarshaller(MarshallingType.BYTE, forEmbeddable(Number.class, Number::byteValue,
                                                                     StringToValueConverter.TO_BYTE))
            .payloadUnmarshaller(MarshallingType.SHORT, forEmbeddable(Number.class, Number::shortValue,
                                                                      StringToValueConverter.TO_SHORT))
            .payloadUnmarshaller(MarshallingType.FLOAT, forEmbeddable(Number.class, Number::floatValue,
                                                                      StringToValueConverter.TO_FLOAT))
            .payloadUnmarshaller(MarshallingType.DOUBLE, forEmbeddable(Number.class, Number::doubleValue,
                                                                       StringToValueConverter.TO_DOUBLE))
            .payloadUnmarshaller(MarshallingType.BIG_DECIMAL, forEmbeddable(BigDecimal.class,
                                                                            StringToValueConverter.TO_BIG_DECIMAL))
            .payloadUnmarshaller(MarshallingType.BOOLEAN, forEmbeddable(Boolean.class, StringToValueConverter.TO_BOOLEAN))
            .payloadUnmarshaller(MarshallingType.SDK_BYTES, JsonProtocolUnmarshaller::unmarshallSdkBytes)
            .payloadUnmarshaller(MarshallingType.INSTANT, new SimpleTypeInstantJsonUnmarshaller<>(instantStringToValue,
                                                                                                  numberToInstant))
            .payloadUnmarshaller(MarshallingType.SDK_POJO, JsonProtocolUnmarshaller::unmarshallStructured)
            .payloadUnmarshaller(MarshallingType.LIST, JsonProtocolUnmarshaller::unmarshallList)
            .payloadUnmarshaller(MarshallingType.MAP, JsonProtocolUnmarshaller::unmarshallMap)
            .payloadUnmarshaller(MarshallingType.DOCUMENT, JsonProtocolUnmarshaller::unmarshallDocument)
                .build();
    }

    private static SdkBytes unmarshallSdkBytes(JsonUnmarshallerContext context,
                                               JsonNode jsonContent,
                                               SdkField<SdkBytes> field) {
        if (jsonContent == null || jsonContent.isNull()) {
            return null;
        }
        // Binary protocols like CBOR may already have the raw bytes extracted.
        if (jsonContent.isEmbeddedObject()) {
            return SdkBytes.fromByteArray((byte[]) jsonContent.asEmbeddedObject());
        } else {
            // Otherwise decode the JSON string as Base64
            return TO_SDK_BYTES.convert(jsonContent.text(), field);
        }
    }

    private static SdkPojo unmarshallStructured(JsonUnmarshallerContext context, JsonNode jsonContent, SdkField<SdkPojo> f) {
        if (jsonContent == null || jsonContent.isNull()) {
            return null;
        } else {
            return unmarshallStructured(f.constructor().get(), jsonContent, context);
        }
    }

    private static Document unmarshallDocument(JsonUnmarshallerContext context,
                                               JsonNode jsonContent,
                                               SdkField<Document> field) {
        if (jsonContent == null) {
            return null;
        }
        return jsonContent.isNull() ? Document.fromNull() : getDocumentFromJsonContent(jsonContent);
    }

    private static Document getDocumentFromJsonContent(JsonNode jsonContent) {
        return jsonContent.visit(new DocumentUnmarshaller());
    }

    private static Map<String, ?> unmarshallMap(JsonUnmarshallerContext context,
                                                JsonNode jsonContent,
                                                SdkField<Map<String, ?>> field) {
        if (jsonContent == null || jsonContent.isNull()) {
            return null;
        }
        SdkField<Object> valueInfo = field.getTrait(MapTrait.class).valueFieldInfo();
        JsonUnmarshaller<Object> unmarshaller = context.getUnmarshaller(valueInfo.location(), valueInfo.marshallingType());
        Map<String, JsonNode> asObject = jsonContent.asObject();
        Map<String, Object> map = new HashMap<>(asObject.size());
        for (Map.Entry<String, JsonNode> kvp : asObject.entrySet()) {
            map.put(kvp.getKey(), unmarshaller.unmarshall(context, kvp.getValue(), valueInfo));
        }
        return map;
    }

    private static List<?> unmarshallList(JsonUnmarshallerContext context, JsonNode jsonContent, SdkField<List<?>> field) {
        if (jsonContent == null || jsonContent.isNull()) {
            return null;
        }
        SdkField<Object> memberInfo = field.getTrait(ListTrait.class).memberFieldInfo();
        List<JsonNode> asArray = jsonContent.asArray();
        List<Object> result = new ArrayList<>(asArray.size());
        for (JsonNode node : asArray) {
            JsonUnmarshaller<Object> unmarshaller = context.getUnmarshaller(memberInfo.location(),
                                                                            memberInfo.marshallingType());
            result.add(unmarshaller.unmarshall(context, node, memberInfo));
        }
        return result;
    }

    private static class SimpleTypeJsonUnmarshaller<T> implements JsonUnmarshaller<T> {

        private final StringToValueConverter.StringToValue<T> stringToValue;

        private SimpleTypeJsonUnmarshaller(StringToValueConverter.StringToValue<T> stringToValue) {
            this.stringToValue = stringToValue;
        }

        @Override
        public T unmarshall(JsonUnmarshallerContext context,
                            JsonNode jsonContent,
                            SdkField<T> field) {
            return jsonContent != null && !jsonContent.isNull() ? stringToValue.convert(jsonContent.text(), field) : null;
        }
    }

    private static class EmbeddableTypeTransformingJsonUnmarshaller<T, V> implements JsonUnmarshaller<T> {

        private final StringToValueConverter.StringToValue<T> stringToValue;
        private final Class<V> embeddedType;
        private final Function<V, T> typeConverter;

        private EmbeddableTypeTransformingJsonUnmarshaller(
            Class<V> embeddedType,
            Function<V, T> typeConverter,
            StringToValueConverter.StringToValue<T> stringToValue
        ) {
            this.stringToValue = stringToValue;
            this.typeConverter = typeConverter;
            this.embeddedType = embeddedType;
        }

        @Override
        public T unmarshall(JsonUnmarshallerContext context,
                            JsonNode jsonContent,
                            SdkField<T> field) {
            if (jsonContent == null || jsonContent.isNull()) {
                return null;
            }
            String text = null;
            if (jsonContent.isEmbeddedObject()) {
                Object embedded = jsonContent.asEmbeddedObject();
                if (embedded == null) {
                    return null;
                }
                if (embeddedType.isAssignableFrom(embedded.getClass())) {
                    return typeConverter.apply((V) embedded);
                }
                // Fallback in case that the embedded object is not what
                // we were looking for.
                text = embedded.toString();
            }
            if (text == null) {
                text = jsonContent.text();
            }
            return stringToValue.convert(text, field);
        }
    }

    private static class SimpleTypeInstantJsonUnmarshaller<T> implements JsonUnmarshaller<T> {

        private final StringToValueConverter.StringToValue<T> stringToValue;
        private final NumberToInstant numberToInstant;

        private SimpleTypeInstantJsonUnmarshaller(
            StringToValueConverter.StringToValue<T> stringToValue,
            NumberToInstant numberToInstant
        ) {
            this.stringToValue = stringToValue;
            this.numberToInstant = numberToInstant;
        }

        @Override
        public T unmarshall(JsonUnmarshallerContext context,
                            JsonNode jsonContent,
                            SdkField<T> field) {
            if (jsonContent == null || jsonContent.isNull()) {
                return null;
            }
            String text = null;
            if (jsonContent.isEmbeddedObject()) {
                Object embedded = jsonContent.asEmbeddedObject();
                if (embedded == null) {
                    return null;
                }
                if (Number.class.isAssignableFrom(embedded.getClass())) {
                    return (T) numberToInstant.convert((Number) embedded, (SdkField<Instant>) field);
                }
                // Fallback in case that the embedded object is not what
                // we were looking for.
                text = embedded.toString();
            }
            if (text == null) {
                text = jsonContent.text();
            }
            return stringToValue.convert(text, field);
        }
    }

    private static <T, V> EmbeddableTypeTransformingJsonUnmarshaller<T, V> forEmbeddable(
        Class<V> embeddedType,
        Function<V, T> transformer,
        StringToValueConverter.StringToValue<T> stringToValue
    ) {
        return new EmbeddableTypeTransformingJsonUnmarshaller<>(embeddedType, transformer, stringToValue);
    }

    private static <T> EmbeddableTypeTransformingJsonUnmarshaller<T, T> forEmbeddable(
        Class<T> embeddedType,
        StringToValueConverter.StringToValue<T> stringToValue
    ) {
        return new EmbeddableTypeTransformingJsonUnmarshaller<>(embeddedType, Function.identity(), stringToValue);
    }

    public <TypeT extends SdkPojo> TypeT unmarshall(SdkPojo sdkPojo,
                            SdkHttpFullResponse response) throws IOException {
        JsonNode jsonNode = hasJsonPayload(sdkPojo, response) ? parser.parse(response.content().get()) : null;
        return unmarshall(sdkPojo, response, jsonNode);
    }

    private boolean hasJsonPayload(SdkPojo sdkPojo, SdkHttpFullResponse response) {
        return sdkPojo.sdkFields()
                      .stream()
                      .anyMatch(f -> isPayloadMemberOnUnmarshall(f) && !isExplicitBlobPayloadMember(f)
                                     && !isExplicitStringPayloadMember(f))
               && response.content().isPresent();
    }

    private boolean isExplicitBlobPayloadMember(SdkField<?> f) {
        return isExplicitPayloadMember(f) && f.marshallingType() == MarshallingType.SDK_BYTES;
    }

    private boolean isExplicitStringPayloadMember(SdkField<?> f) {
        return isExplicitPayloadMember(f) && f.marshallingType() == MarshallingType.STRING;
    }

    private static boolean isExplicitPayloadMember(SdkField<?> f) {
        return f.containsTrait(PayloadTrait.class);
    }

    private boolean isPayloadMemberOnUnmarshall(SdkField<?> f) {
        return f.location() == MarshallLocation.PAYLOAD || MarshallerUtil.isInUri(f.location());
    }

    public <TypeT extends SdkPojo> TypeT unmarshall(SdkPojo sdkPojo,
                            SdkHttpFullResponse response,
                            JsonNode jsonContent) {
        JsonUnmarshallerContext context = JsonUnmarshallerContext.builder()
                                                                 .unmarshallerRegistry(registry)
                                                                 .response(response)
                                                                 .build();
        return unmarshallStructured(sdkPojo, jsonContent, context);
    }

    @SuppressWarnings("unchecked")
    private static <TypeT extends SdkPojo> TypeT unmarshallStructured(SdkPojo sdkPojo,
                                                                      JsonNode jsonContent,
                                                                      JsonUnmarshallerContext context) {
        for (SdkField<?> field : sdkPojo.sdkFields()) {
            if (isExplicitPayloadMember(field) && field.marshallingType() == MarshallingType.SDK_BYTES) {
                Optional<AbortableInputStream> responseContent = context.response().content();
                if (responseContent.isPresent()) {
                    field.set(sdkPojo, SdkBytes.fromInputStream(responseContent.get()));
                } else {
                    field.set(sdkPojo, SdkBytes.fromByteArrayUnsafe(new byte[0]));
                }
            } else if (isExplicitPayloadMember(field) && field.marshallingType() == MarshallingType.STRING) {
                Optional<AbortableInputStream> responseContent = context.response().content();
                if (responseContent.isPresent()) {
                    field.set(sdkPojo, SdkBytes.fromInputStream(responseContent.get()).asUtf8String());
                } else {
                    field.set(sdkPojo, "");
                }
            } else {
                JsonNode jsonFieldContent = getJsonNode(jsonContent, field);
                JsonUnmarshaller<Object> unmarshaller = context.getUnmarshaller(field.location(), field.marshallingType());
                field.set(sdkPojo, unmarshaller.unmarshall(context, jsonFieldContent, (SdkField<Object>) field));
            }
        }
        return (TypeT) ((Buildable) sdkPojo).build();
    }

    private static JsonNode getJsonNode(JsonNode jsonContent, SdkField<?> field) {
        if (jsonContent == null) {
            return null;
        }
        return isFieldExplicitlyTransferredAsJson(field) ? jsonContent : jsonContent.field(field.locationName()).orElse(null);
    }

    private static boolean isFieldExplicitlyTransferredAsJson(SdkField<?> field) {
        return isExplicitPayloadMember(field) && !MarshallingType.DOCUMENT.equals(field.marshallingType());
    }

    /**
     * @return New instance of {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link JsonProtocolUnmarshaller}.
     */
    public static final class Builder {

        private JsonNodeParser parser;
        private Map<MarshallLocation, TimestampFormatTrait.Format> defaultTimestampFormats;

        private Builder() {
        }

        /**
         * @param parser JSON parser to use.
         * @return This builder for method chaining.
         */
        public Builder parser(JsonNodeParser parser) {
            this.parser = parser;
            return this;
        }

        /**
         * @param formats The default timestamp formats for each location in the HTTP response.
         * @return This builder for method chaining.
         */
        public Builder defaultTimestampFormats(Map<MarshallLocation, TimestampFormatTrait.Format> formats) {
            this.defaultTimestampFormats = formats;
            return this;
        }

        /**
         * @return New instance of {@link JsonProtocolUnmarshaller}.
         */
        public JsonProtocolUnmarshaller build() {
            return new JsonProtocolUnmarshaller(this);
        }
    }

}
