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
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import software.amazon.awssdk.http.SdkHttpFullResponse;
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

    private final JsonUnmarshallerRegistry registry;

    private final JsonNodeParser parser;

    private JsonProtocolUnmarshaller(Builder builder) {
        this.parser = builder.parser;
        this.instantStringToValue = StringToInstant.create(builder.defaultTimestampFormats.isEmpty() ?
                                                           new EnumMap<>(MarshallLocation.class) :
                                                           new EnumMap<>(builder.defaultTimestampFormats));
        this.registry = createUnmarshallerRegistry(instantStringToValue);
    }

    private static JsonUnmarshallerRegistry createUnmarshallerRegistry(
        StringToValueConverter.StringToValue<Instant> instantStringToValue) {

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
            .payloadUnmarshaller(MarshallingType.INTEGER, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_INTEGER))
            .payloadUnmarshaller(MarshallingType.LONG, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_LONG))
            .payloadUnmarshaller(MarshallingType.SHORT, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_SHORT))
            .payloadUnmarshaller(MarshallingType.FLOAT, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_FLOAT))
            .payloadUnmarshaller(MarshallingType.DOUBLE, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_DOUBLE))
            .payloadUnmarshaller(MarshallingType.BIG_DECIMAL, new SimpleTypeJsonUnmarshaller<>(
                StringToValueConverter.TO_BIG_DECIMAL))
            .payloadUnmarshaller(MarshallingType.BOOLEAN, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_BOOLEAN))
            .payloadUnmarshaller(MarshallingType.SDK_BYTES, JsonProtocolUnmarshaller::unmarshallSdkBytes)
            .payloadUnmarshaller(MarshallingType.INSTANT, new SimpleTypeJsonUnmarshaller<>(instantStringToValue))
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
        Map<String, Object> map = new HashMap<>();
        jsonContent.asObject().forEach((fieldName, value) -> {
            JsonUnmarshaller<Object> unmarshaller = context.getUnmarshaller(valueInfo.location(), valueInfo.marshallingType());
            map.put(fieldName, unmarshaller.unmarshall(context, value, valueInfo));
        });
        return map;
    }

    private static List<?> unmarshallList(JsonUnmarshallerContext context, JsonNode jsonContent, SdkField<List<?>> field) {
        if (jsonContent == null || jsonContent.isNull()) {
            return null;
        }
        return jsonContent.asArray()
                          .stream()
                          .map(item -> {
                              SdkField<Object> memberInfo = field.getTrait(ListTrait.class).memberFieldInfo();
                              JsonUnmarshaller<Object> unmarshaller = context.getUnmarshaller(memberInfo.location(),
                                                                                              memberInfo.marshallingType());
                              return unmarshaller.unmarshall(context, item, memberInfo);
                          })
                          .collect(Collectors.toList());
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

    public <TypeT extends SdkPojo> TypeT unmarshall(SdkPojo sdkPojo,
                            SdkHttpFullResponse response) throws IOException {
        if (hasPayloadMembersOnUnmarshall(sdkPojo) && !hasExplicitBlobPayloadMember(sdkPojo) && response.content().isPresent()) {
            JsonNode jsonNode = parser.parse(response.content().get());
            return unmarshall(sdkPojo, response, jsonNode);
        } else {
            return unmarshall(sdkPojo, response, null);
        }
    }

    private boolean hasExplicitBlobPayloadMember(SdkPojo sdkPojo) {
        return sdkPojo.sdkFields()
                      .stream()
                      .anyMatch(f -> isExplicitPayloadMember(f) && f.marshallingType() == MarshallingType.SDK_BYTES);
    }

    private static boolean isExplicitPayloadMember(SdkField<?> f) {
        return f.containsTrait(PayloadTrait.class);
    }

    private boolean hasPayloadMembersOnUnmarshall(SdkPojo sdkPojo) {
        return sdkPojo.sdkFields()
                .stream()
                .anyMatch(f -> f.location() == MarshallLocation.PAYLOAD
                        || MarshallerUtil.locationInUri(f.location()));
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
            if (isExplicitPayloadMember(field) && field.marshallingType() == MarshallingType.SDK_BYTES &&
                context.response().content().isPresent()) {
                field.set(sdkPojo, SdkBytes.fromInputStream(context.response().content().get()));
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
