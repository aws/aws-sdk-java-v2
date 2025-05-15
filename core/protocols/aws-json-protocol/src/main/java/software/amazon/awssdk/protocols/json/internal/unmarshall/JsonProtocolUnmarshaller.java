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
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import software.amazon.awssdk.core.traits.TraitType;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.core.StringToInstant;
import software.amazon.awssdk.protocols.core.StringToValueConverter;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;
import software.amazon.awssdk.protocols.json.internal.MarshallerUtil;
import software.amazon.awssdk.protocols.json.internal.unmarshall.document.DocumentUnmarshaller;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.protocols.jsoncore.JsonValueNodeFactory;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.builder.Buildable;

/**
 * Unmarshaller implementation for both JSON RPC and REST JSON services. This class is thread-safe and it is recommended to reuse
 * a single instance for best performance.
 */
@SdkInternalApi
@ThreadSafe
public class JsonProtocolUnmarshaller {
    private static final Lazy<DefaultProtocolUnmarshallDependencies> DEFAULT_DEPENDENCIES =
        new Lazy<>(JsonProtocolUnmarshaller::newProtocolUnmarshallDependencies);

    private final JsonUnmarshallerRegistry registry;
    private final JsonUnmarshallingParser unmarshallingParser;
    private final JsonNodeParser parser;

    private JsonProtocolUnmarshaller(Builder builder) {
        ProtocolUnmarshallDependencies dependencies = builder.protocolUnmarshallDependencies;
        this.registry = dependencies.jsonUnmarshallerRegistry();
        if (builder.enableFastUnmarshalling) {
            this.unmarshallingParser = JsonUnmarshallingParser.builder()
                                                              .jsonValueNodeFactory(dependencies.nodeValueFactory())
                                                              .jsonFactory(dependencies.jsonFactory())
                                                              .unmarshallerRegistry(dependencies.jsonUnmarshallerRegistry())
                                                              .defaultTimestampFormat(dependencies.timestampFormats()
                                                                                                  .get(MarshallLocation.PAYLOAD))

                                                              .build();
            this.parser = null;
        } else {
            this.unmarshallingParser = null;
            this.parser = createParser(builder, dependencies);
        }
    }

    private JsonNodeParser createParser(Builder builder, ProtocolUnmarshallDependencies dependencies) {
        if (builder.parser != null) {
            return builder.parser;
        }
        return JsonNodeParser
            .builder()
            .jsonFactory(dependencies.jsonFactory())
            .jsonValueNodeFactory(dependencies.nodeValueFactory())
            .build();
    }

    public static DefaultProtocolUnmarshallDependencies defaultProtocolUnmarshallDependencies() {
        return DEFAULT_DEPENDENCIES.getValue();
    }

    public static DefaultProtocolUnmarshallDependencies newProtocolUnmarshallDependencies() {
        return DefaultProtocolUnmarshallDependencies.builder()
                                                    .jsonUnmarshallerRegistry(defaultJsonUnmarshallerRegistry())
                                                    .nodeValueFactory(JsonValueNodeFactory.DEFAULT)
                                                    .timestampFormats(defaultFormats())
                                                    .jsonFactory(AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY.getJsonFactory())
                                                    .build();
    }

    private static Map<MarshallLocation, TimestampFormatTrait.Format> defaultFormats() {
        Map<MarshallLocation, TimestampFormatTrait.Format> formats = new EnumMap<>(MarshallLocation.class);
        formats.put(MarshallLocation.HEADER, TimestampFormatTrait.Format.RFC_822);
        formats.put(MarshallLocation.PAYLOAD, TimestampFormatTrait.Format.UNIX_TIMESTAMP);
        return Collections.unmodifiableMap(formats);
    }

    private static JsonUnmarshallerRegistry defaultJsonUnmarshallerRegistry() {
        return timestampFormatRegistryFactory(defaultFormats());
    }

    public static DefaultJsonUnmarshallerRegistry createSharedRegistry() {
        return DefaultJsonUnmarshallerRegistry
            .builder()
            .statusCodeUnmarshaller(MarshallingType.INTEGER, (context, json, f) -> context.response().statusCode())
            .headerUnmarshaller(MarshallingType.STRING, HeaderUnmarshaller.STRING)
            .headerUnmarshaller(MarshallingType.INTEGER, HeaderUnmarshaller.INTEGER)
            .headerUnmarshaller(MarshallingType.LONG, HeaderUnmarshaller.LONG)
            .headerUnmarshaller(MarshallingType.SHORT, HeaderUnmarshaller.SHORT)
            .headerUnmarshaller(MarshallingType.DOUBLE, HeaderUnmarshaller.DOUBLE)
            .headerUnmarshaller(MarshallingType.BOOLEAN, HeaderUnmarshaller.BOOLEAN)
            .headerUnmarshaller(MarshallingType.FLOAT, HeaderUnmarshaller.FLOAT)
            .headerUnmarshaller(MarshallingType.LIST, HeaderUnmarshaller.LIST)

            .payloadUnmarshaller(MarshallingType.STRING, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_STRING))
            .payloadUnmarshaller(MarshallingType.INTEGER, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_INTEGER))
            .payloadUnmarshaller(MarshallingType.LONG, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_LONG))
            .payloadUnmarshaller(MarshallingType.BYTE, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_BYTE))
            .payloadUnmarshaller(MarshallingType.SHORT, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_SHORT))
            .payloadUnmarshaller(MarshallingType.FLOAT, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_FLOAT))
            .payloadUnmarshaller(MarshallingType.DOUBLE, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_DOUBLE))
            .payloadUnmarshaller(MarshallingType.BIG_DECIMAL, new SimpleTypeJsonUnmarshaller<>(
                StringToValueConverter.TO_BIG_DECIMAL))
            .payloadUnmarshaller(MarshallingType.BOOLEAN, new SimpleTypeJsonUnmarshaller<>(StringToValueConverter.TO_BOOLEAN))
            .payloadUnmarshaller(MarshallingType.SDK_BYTES, JsonProtocolUnmarshaller::unmarshallSdkBytes)
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

        SdkField<Object> valueInfo = field.getTrait(MapTrait.class, TraitType.MAP_TRAIT).valueFieldInfo();
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

        SdkField<Object> memberInfo = field.getTrait(ListTrait.class, TraitType.LIST_TRAIT).memberFieldInfo();
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

    public <TypeT extends SdkPojo> TypeT unmarshall(SdkPojo sdkPojo,
                                                    SdkHttpFullResponse response) throws IOException {
        if (this.unmarshallingParser != null) {
            return fastUnmarshall(sdkPojo, response);
        }
        JsonNode jsonNode = hasJsonPayload(sdkPojo, response) ? parser.parse(response.content().get()) : null;
        return unmarshall(sdkPojo, response, jsonNode);
    }

    private <TypeT extends SdkPojo> TypeT fastUnmarshall(SdkPojo sdkPojo,
                                                    SdkHttpFullResponse response) throws IOException {
        if (!hasJsonPayload(sdkPojo, response)) {
            return unmarshallResponse(sdkPojo, response);
        }
        if (hasExplicitJsonPayloadMember(sdkPojo)) {
            return unmarshallResponse(sdkPojo, response);
        }
        if (hasMixedLocations(sdkPojo)) {
            unmarshallFromJson(sdkPojo, response.content().get());
            return unmarshallResponse(sdkPojo, response);
        }
        return unmarshallFromJson(sdkPojo, response.content().get());
    }

    @SuppressWarnings("unchecked")
    private <T extends SdkPojo> T unmarshallFromJson(SdkPojo sdkPojo, InputStream inputStream) {
        return (T) unmarshallingParser.parse(sdkPojo, inputStream);
    }

    private <TypeT extends SdkPojo> TypeT unmarshallResponse(SdkPojo sdkPojo,
                                                             SdkHttpFullResponse response) throws IOException {
        JsonUnmarshallerContext context = JsonUnmarshallerContext.builder()
                                                                 .unmarshallerRegistry(registry)
                                                                 .response(response)
                                                                 .build();
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
            } else if (isExplicitPayloadMember(field) && field.marshallingType() == MarshallingType.SDK_POJO) {
                Optional<AbortableInputStream> responseContent = context.response().content();
                if (responseContent.isPresent()) {
                    field.set(sdkPojo, unmarshallFromJson(field.constructor().get(), responseContent.get()));
                } else {
                    field.set(sdkPojo, null);
                }
            } else if (!isPayloadUnmarshalling(field.location())) {
                JsonUnmarshaller<Object> unmarshaller = context.getUnmarshaller(field.location(), field.marshallingType());
                field.set(sdkPojo, unmarshaller.unmarshall(context, null, (SdkField<Object>) field));
            }
        }
        return (TypeT) ((Buildable) sdkPojo).build();
    }

    private boolean hasJsonPayload(SdkPojo sdkPojo, SdkHttpFullResponse response) {
        if (!response.content().isPresent()) {
            return false;
        }
        for (SdkField<?> field : sdkPojo.sdkFields()) {
            if (isPayloadMemberOnUnmarshall(field)
                && !(isExplicitBlobPayloadMember(field) || isExplicitStringPayloadMember(field))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExplicitJsonPayloadMember(SdkPojo sdkPojo) {
        for (SdkField<?> field : sdkPojo.sdkFields()) {
            if (isExplicitSdkPojoPayloadMember(field)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMixedLocations(SdkPojo sdkPojo) {
        int payload = 0;
        int header = 0;
        int statusCode = 0;
        for (SdkField<?> field : sdkPojo.sdkFields()) {
            MarshallLocation location = field.location();
            if (isPayloadUnmarshalling(location)) {
                payload = 1;
            } else if (location == MarshallLocation.HEADER) {
                header = 1;
            } else if (location == MarshallLocation.STATUS_CODE) {
                statusCode = 1;
            }
        }
        return (payload + header + statusCode) > 1;
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

    private boolean isExplicitBlobPayloadMember(SdkField<?> f) {
        return isExplicitPayloadMember(f) && f.marshallingType() == MarshallingType.SDK_BYTES;
    }

    private boolean isExplicitStringPayloadMember(SdkField<?> f) {
        return isExplicitPayloadMember(f) && f.marshallingType() == MarshallingType.STRING;
    }

    private boolean isExplicitSdkPojoPayloadMember(SdkField<?> f) {
        return isExplicitPayloadMember(f) && f.marshallingType() == MarshallingType.SDK_POJO;
    }

    private static boolean isExplicitPayloadMember(SdkField<?> f) {
        return f.containsTrait(PayloadTrait.class, TraitType.PAYLOAD_TRAIT);
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
     * Creates the default {@link JsonProtocolUnmarshaller}, which parses {@link Instant} using the default formats passed in.
     */
    public static JsonUnmarshallerRegistry timestampFormatRegistryFactory(
        Map<MarshallLocation, TimestampFormatTrait.Format> formats
    ) {
        StringToValueConverter.StringToValue<Instant> instantStringToValue = StringToInstant
            .create(formats.isEmpty() ?
                    new EnumMap<>(MarshallLocation.class) :
                    new EnumMap<>(formats));

        return createSharedRegistry()
            .toBuilder()
            .headerUnmarshaller(MarshallingType.INSTANT,
                                HeaderUnmarshaller.createInstantHeaderUnmarshaller(instantStringToValue))
            .payloadUnmarshaller(MarshallingType.INSTANT,
                                 new SimpleTypeJsonUnmarshaller<>(instantStringToValue))
            .build();
    }

    /**
     * Builder for {@link JsonProtocolUnmarshaller}.
     */
    public static final class Builder {

        private JsonNodeParser parser;
        private ProtocolUnmarshallDependencies protocolUnmarshallDependencies;
        private boolean enableFastUnmarshalling = false;

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
         * @deprecated Use instead {@link #protocolUnmarshallDependencies}
         */
        @Deprecated
        public Builder defaultTimestampFormats(Map<MarshallLocation, TimestampFormatTrait.Format> formats) {
            return this;
        }

        /**
         * @param protocolUnmarshallDependencies The default instant registry unmarshaller factory.
         * @return This builder for method chaining.
         */
        public Builder protocolUnmarshallDependencies(
            ProtocolUnmarshallDependencies protocolUnmarshallDependencies
        ) {
            this.protocolUnmarshallDependencies = protocolUnmarshallDependencies;
            return this;
        }

        /**
         * @param enableFastUnmarshalling Whether to enable the fast unmarshalling codepath. Default to {@code false}.
         * @return This builder for method chaining.
         */
        public Builder enableFastUnmarshalling(boolean enableFastUnmarshalling) {
            this.enableFastUnmarshalling = enableFastUnmarshalling;
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
