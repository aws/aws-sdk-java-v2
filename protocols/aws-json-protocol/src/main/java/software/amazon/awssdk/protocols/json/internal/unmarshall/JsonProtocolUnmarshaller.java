/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.io.ReleasableInputStream;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.core.StringToInstant;
import software.amazon.awssdk.protocols.core.StringToValueConverter;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Unmarshaller implementation for both JSON RPC and REST JSON services.
 *
 * @param <TypeT> Type to unmarshall into.
 */
@SdkInternalApi
public final class JsonProtocolUnmarshaller<TypeT extends SdkPojo> {

    public static final StringToValueConverter.StringToValue<Instant> INSTANT_STRING_TO_VALUE
        = StringToInstant.create(getDefaultTimestampFormats());

    private static final JsonUnmarshallerRegistry REGISTRY = createUnmarshallerRegistry();

    private final ObjectMapper mapper;

    public JsonProtocolUnmarshaller(ObjectMapper objectMapper) {
        mapper = objectMapper;
    }

    private static JsonUnmarshallerRegistry createUnmarshallerRegistry() {
        return JsonUnmarshallerRegistry
            .builder()
            .statusCodeUnmarshaller(MarshallingType.INTEGER, (context, json, f) -> context.response().statusCode())
            .headerUnmarshaller(MarshallingType.STRING, HeaderUnmarshaller.STRING)
            .headerUnmarshaller(MarshallingType.INTEGER, HeaderUnmarshaller.INTEGER)
            .headerUnmarshaller(MarshallingType.LONG, HeaderUnmarshaller.LONG)
            .headerUnmarshaller(MarshallingType.DOUBLE, HeaderUnmarshaller.DOUBLE)
            .headerUnmarshaller(MarshallingType.BOOLEAN, HeaderUnmarshaller.BOOLEAN)
            .headerUnmarshaller(MarshallingType.INSTANT, HeaderUnmarshaller.INSTANT)
            .headerUnmarshaller(MarshallingType.FLOAT, HeaderUnmarshaller.FLOAT)
            .payloadUnmarshaller(MarshallingType.STRING, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_STRING))
            .payloadUnmarshaller(MarshallingType.INTEGER, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_INTEGER))
            .payloadUnmarshaller(MarshallingType.LONG, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_LONG))
            .payloadUnmarshaller(MarshallingType.DOUBLE, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_DOUBLE))
            .payloadUnmarshaller(MarshallingType.BOOLEAN, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_BOOLEAN))
            .payloadUnmarshaller(MarshallingType.FLOAT, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_FLOAT))
            .payloadUnmarshaller(MarshallingType.SDK_BYTES, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_SDK_BYTES))
            .payloadUnmarshaller(MarshallingType.INSTANT, new SimpleTypeJsonUnmarshaller<>(INSTANT_STRING_TO_VALUE))
            .payloadUnmarshaller(MarshallingType.SDK_POJO, JsonProtocolUnmarshaller::unmarshallStructured)
            .payloadUnmarshaller(MarshallingType.LIST, JsonProtocolUnmarshaller::unmarshallList)
            .payloadUnmarshaller(MarshallingType.MAP, JsonProtocolUnmarshaller::unmarshallMap)
            .build();
    }

    private static Map<MarshallLocation, TimestampFormatTrait.Format> getDefaultTimestampFormats() {
        Map<MarshallLocation, TimestampFormatTrait.Format> formats = new HashMap<>();
        formats.put(MarshallLocation.HEADER, TimestampFormatTrait.Format.RFC_822);
        formats.put(MarshallLocation.PAYLOAD, TimestampFormatTrait.Format.UNIX_TIMESTAMP);
        return Collections.unmodifiableMap(formats);
    }

    private static SdkPojo unmarshallStructured(JsonUnmarshallerContext context, JsonNode jsonContent, SdkField<SdkPojo> f) {
        if (jsonContent == null || jsonContent.isNull()) {
            return null;
        } else {
            return unmarshallStructured(f.constructor().get(), jsonContent, context);
        }
    }

    private static Map<String, ?> unmarshallMap(JsonUnmarshallerContext context,
                                                JsonNode jsonContent,
                                                SdkField<Map<String, ?>> field) {
        if (jsonContent == null || jsonContent.isNull()) {
            return null;
        }
        SdkField<?> valueInfo = field.getTrait(MapTrait.class).valueFieldInfo();
        Map<String, Object> map = new HashMap<>();
        jsonContent.fieldNames().forEachRemaining(f -> {
            JsonUnmarshaller<Object> unmarshaller = context.getUnmarshaller(valueInfo.location(), valueInfo.marshallingType());
            map.put(f, unmarshaller.unmarshall(context, jsonContent.get(f), (SdkField<Object>) valueInfo));
        });
        return map;
    }

    private static List<?> unmarshallList(JsonUnmarshallerContext context, JsonNode jsonContent, SdkField<List<?>> field) {
        if (jsonContent == null || jsonContent.isNull()) {
            return null;
        }
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < jsonContent.size(); i++) {
            SdkField<?> memberInfo = field.getTrait(ListTrait.class).memberFieldInfo();
            JsonUnmarshaller<Object> unmarshaller = context.getUnmarshaller(memberInfo.location(), memberInfo.marshallingType());
            Object unmarshall = unmarshaller.unmarshall(context, jsonContent.get(i), (SdkField<Object>) memberInfo);
            list.add(unmarshall);
        }
        return list;
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
            return jsonContent != null && !jsonContent.isNull() ? stringToValue.convert(jsonContent.asText(), field) : null;
        }
    }

    public TypeT unmarshall(SdkPojo sdkPojo,
                            SdkHttpFullResponse response) throws IOException {
        if (hasPayloadMembers(sdkPojo) && !hasExplicitBlobPayloadMember(sdkPojo)) {
            JsonNode jsonNode = mapper.readTree(ReleasableInputStream.wrap(response.content().orElse(null)).disableClose());
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

    private boolean hasPayloadMembers(SdkPojo sdkPojo) {
        return sdkPojo.sdkFields()
                      .stream()
                      .anyMatch(f -> f.location() == MarshallLocation.PAYLOAD);
    }

    public TypeT unmarshall(SdkPojo sdkPojo,
                            SdkHttpFullResponse response,
                            JsonNode jsonContent) {
        JsonUnmarshallerContext context = JsonUnmarshallerContext.builder()
                                                                 .unmarshallerRegistry(REGISTRY)
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
                field.set(sdkPojo, SdkBytes.fromInputStream(context.response().content().orElse(null)));
            } else {
                JsonNode jsonFieldContent = getJsonNode(jsonContent, field);
                JsonUnmarshaller<Object> unmarshaller = context.getUnmarshaller(field.location(), field.marshallingType());
                field.set(sdkPojo, unmarshaller.unmarshall(context, jsonFieldContent, (SdkField<Object>) field));
            }
        }
        return ((SdkBuilder<?, TypeT>) sdkPojo).build();
    }

    private static JsonNode getJsonNode(JsonNode jsonContent, SdkField<?> field) {
        if (jsonContent == null) {
            return null;
        }
        return isExplicitPayloadMember(field) ? jsonContent : jsonContent.get(field.locationName());
    }

}
