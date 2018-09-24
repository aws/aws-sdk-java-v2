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

package software.amazon.awssdk.core.internal.protocol.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.io.ReleasableInputStream;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.protocol.SdkField;
import software.amazon.awssdk.core.protocol.SdkPojo;
import software.amazon.awssdk.core.protocol.traits.JsonValueTrait;
import software.amazon.awssdk.core.protocol.traits.ListTrait;
import software.amazon.awssdk.core.protocol.traits.MapTrait;
import software.amazon.awssdk.core.protocol.traits.PayloadTrait;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.builder.SdkBuilder;

public class JsonProtocolUnmarshaller<TypeT extends SdkPojo> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final UnmarshallerRegistry REGISTRY = UnmarshallerRegistry
        .builder()
        .headerUnmarshaller(MarshallingType.STRING, new JsonUnmarshaller<String>() {
            @Override
            public String unmarshall(JsonUnmarshallerContext context, JsonNode jsonContent, SdkField<String> field) {
                if (field.containsTrait(JsonValueTrait.class)) {
                    return context.response().firstMatchingHeader(field.locationName())
                                  .map(s -> new String(BinaryUtils.fromBase64(s), StandardCharsets.UTF_8))
                                  .orElse(null);
                } else {
                    return context.response().firstMatchingHeader(field.locationName())
                                  .orElse(null);
                }
            }
        })
        .headerUnmarshaller(MarshallingType.INTEGER, new HeaderUnmarshaller<>(StringToValueConverter.TO_INTEGER))
        .headerUnmarshaller(MarshallingType.LONG, new HeaderUnmarshaller<>(StringToValueConverter.TO_LONG))
        .headerUnmarshaller(MarshallingType.DOUBLE, new HeaderUnmarshaller<>(StringToValueConverter.TO_DOUBLE))
        .headerUnmarshaller(MarshallingType.BOOLEAN, new HeaderUnmarshaller<>(StringToValueConverter.TO_BOOLEAN))
        .headerUnmarshaller(MarshallingType.INSTANT, new HeaderUnmarshaller<>(StringToValueConverter.TO_INSTANT_ISO))
        .headerUnmarshaller(MarshallingType.FLOAT, new HeaderUnmarshaller<>(StringToValueConverter.TO_FLOAT))
        .payloadUnmarshaller(MarshallingType.STRING, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_STRING))
        .payloadUnmarshaller(MarshallingType.INTEGER, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_INTEGER))
        .payloadUnmarshaller(MarshallingType.LONG, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_LONG))
        .payloadUnmarshaller(MarshallingType.DOUBLE, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_DOUBLE))
        .payloadUnmarshaller(MarshallingType.BOOLEAN, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_BOOLEAN))
        .payloadUnmarshaller(MarshallingType.FLOAT, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_FLOAT))
        .payloadUnmarshaller(MarshallingType.SDK_BYTES, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_SDK_BYTES))
        .payloadUnmarshaller(MarshallingType.INSTANT, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_INSTANT))
        .payloadUnmarshaller(MarshallingType.SDK_POJO, JsonProtocolUnmarshaller::unmarshallStructured)
        .payloadUnmarshaller(MarshallingType.LIST, JsonProtocolUnmarshaller::unmarshallList)
        .payloadUnmarshaller(MarshallingType.MAP, JsonProtocolUnmarshaller::unmarshallMap)
        .build();

    private static SdkPojo unmarshallStructured(JsonUnmarshallerContext context, JsonNode jsonContent, SdkField<SdkPojo> f) {
        if (jsonContent == null || jsonContent.isNull()) {
            return null;
        } else {
            return unmarshallStructured(f.constructor(), jsonContent, context);
        }
    }

    private static Map unmarshallMap(JsonUnmarshallerContext context, JsonNode jsonContent, SdkField<Map> field) {
        if (jsonContent == null || jsonContent.isNull()) {
            return null;
        }
        SdkField<?> valueInfo = field.getTrait(MapTrait.class).valueFieldInfo();
        Map<String, Object> map = new HashMap<>();
        jsonContent.fieldNames().forEachRemaining(f -> {
            map.put(f, context.unmarshallerRegistry().getUnmarshaller(valueInfo.location(), valueInfo.marshallingType())
                              .unmarshall(context, jsonContent.get(f), (SdkField<Object>) valueInfo));
        });
        return map;
    }

    private static List unmarshallList(JsonUnmarshallerContext context, JsonNode jsonContent, SdkField<List> field) {
        if (jsonContent == null || jsonContent.isNull()) {
            return null;
        }
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < jsonContent.size(); i++) {
            SdkField<?> memberInfo = field.getTrait(ListTrait.class).memberFieldInfo();
            Object unmarshall = context.unmarshallerRegistry().getUnmarshaller(memberInfo.location(), memberInfo.marshallingType())
                                       .unmarshall(context, jsonContent.get(i), (SdkField<Object>) memberInfo);
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
            return jsonContent != null && !jsonContent.isNull() ? stringToValue.apply(jsonContent.asText()) : null;
        }
    }

    private static class HeaderUnmarshaller<T> implements JsonUnmarshaller<T> {

        private final StringToValueConverter.StringToValue<T> stringToValue;

        private HeaderUnmarshaller(StringToValueConverter.StringToValue<T> stringToValue) {
            this.stringToValue = stringToValue;
        }

        @Override
        public T unmarshall(JsonUnmarshallerContext context,
                            JsonNode jsonContent,
                            SdkField<T> field) {
            return context.response().firstMatchingHeader(field.locationName())
                          .map(stringToValue::apply)
                          .orElse(null);
        }
    }

    public TypeT unmarshall(Supplier<SdkPojo> pojoSupplier,
                            SdkHttpFullResponse response) throws IOException {
        SdkPojo sdkPojo = pojoSupplier.get();
        if (hasPayloadMembers(sdkPojo) && !hasExplicitBlobPayloadMember(sdkPojo)) {
            JsonNode jsonNode = MAPPER.readTree(ReleasableInputStream.wrap(response.content().orElse(null)).disableClose());
            return unmarshall(pojoSupplier, response, jsonNode);
        } else {
            return unmarshall(pojoSupplier, response, null);
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

    public TypeT unmarshall(Supplier<SdkPojo> pojoSupplier,
                            SdkHttpFullResponse response,
                            JsonNode jsonContent) {
        JsonUnmarshallerContext context = JsonUnmarshallerContext.builder()
                                                                 .unmarshallerRegistry(REGISTRY)
                                                                 .response(response)
                                                                 .build();
        return unmarshallStructured(pojoSupplier, jsonContent, context);
    }

    @SuppressWarnings("unchecked")
    private static <TypeT extends SdkPojo> TypeT unmarshallStructured(Supplier<SdkPojo> pojoSupplier,
                                                                      JsonNode jsonContent,
                                                                      JsonUnmarshallerContext context) {
        SdkPojo structuredPojo = pojoSupplier.get();
        for (SdkField<?> field : structuredPojo.sdkFields()) {
            if (isExplicitPayloadMember(field) && field.marshallingType() == MarshallingType.SDK_BYTES) {
                field.set(structuredPojo, SdkBytes.fromInputStream(context.response().content().orElse(null)));
            } else {
                JsonNode jsonFieldContent = getJsonNode(jsonContent, field);
                // TODO handle no unmarshaller found
                field.set(structuredPojo,
                          context.unmarshallerRegistry().getUnmarshaller(field.location(), field.marshallingType())
                                 .unmarshall(context, jsonFieldContent, (SdkField<Object>) field));
            }
        }
        return ((SdkBuilder<?, TypeT>) structuredPojo).build();
    }

    private static JsonNode getJsonNode(JsonNode jsonContent, SdkField<?> field) {
        if (jsonContent == null) {
            return null;
        }
        return isExplicitPayloadMember(field) ? jsonContent : jsonContent.get(field.locationName());
    }

}
