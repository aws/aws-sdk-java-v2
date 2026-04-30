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

package software.amazon.awssdk.protocols.json.internal.marshall;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;

/**
 * Tests that the switch-based payload dispatch in {@link JsonProtocolMarshaller#marshallPayloadField}
 * produces correct output for all 16 {@code MarshallingKnownType} values.
 */
class PayloadMarshallingEquivalenceTest {

    private static final URI ENDPOINT = URI.create("http://localhost");
    private static final String CONTENT_TYPE = "application/x-amz-json-1.0";
    private static final OperationInfo OP_INFO = OperationInfo.builder()
        .httpMethod(SdkHttpMethod.POST)
        .hasImplicitPayloadMembers(true)
        .build();
    private static final AwsJsonProtocolMetadata METADATA =
        AwsJsonProtocolMetadata.builder()
            .protocol(AwsJsonProtocol.AWS_JSON)
            .contentType(CONTENT_TYPE)
            .build();

    @Test
    void marshallPayloadField_withStringValue_producesCorrectJson() {
        SdkField<String> field = payloadField("fieldName", MarshallingType.STRING, obj -> "hello world");
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":\"hello world\"");
    }

    @Test
    void marshallPayloadField_withIntegerValue_producesCorrectJson() {
        SdkField<Integer> field = payloadField("fieldName", MarshallingType.INTEGER, obj -> 42);
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":42");
    }

    @Test
    void marshallPayloadField_withLongValue_producesCorrectJson() {
        SdkField<Long> field = payloadField("fieldName", MarshallingType.LONG, obj -> 123456789L);
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":123456789");
    }

    @Test
    void marshallPayloadField_withShortValue_producesCorrectJson() {
        SdkField<Short> field = payloadField("fieldName", MarshallingType.SHORT, obj -> (short) 7);
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":7");
    }

    @Test
    void marshallPayloadField_withByteValue_producesCorrectJson() {
        SdkField<Byte> field = payloadField("fieldName", MarshallingType.BYTE, obj -> (byte) 3);
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":3");
    }

    @Test
    void marshallPayloadField_withFloatValue_producesCorrectJson() {
        SdkField<Float> field = payloadField("fieldName", MarshallingType.FLOAT, obj -> 1.5f);
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":1.5");
    }

    @Test
    void marshallPayloadField_withDoubleValue_producesCorrectJson() {
        SdkField<Double> field = payloadField("fieldName", MarshallingType.DOUBLE, obj -> 3.14);
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":3.14");
    }

    @Test
    void marshallPayloadField_withBigDecimalValue_producesCorrectJson() {
        SdkField<BigDecimal> field = payloadField("fieldName", MarshallingType.BIG_DECIMAL,
                                                   obj -> new BigDecimal("99.99"));
        String body = marshallAndGetBody(field);
        // BigDecimal is serialized as a quoted string by the JSON generator
        assertThat(body).contains("\"fieldName\":\"99.99\"");
    }

    @Test
    void marshallPayloadField_withBooleanValue_producesCorrectJson() {
        SdkField<Boolean> field = payloadField("fieldName", MarshallingType.BOOLEAN, obj -> true);
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":true");
    }

    @Test
    void marshallPayloadField_withInstantDefaultFormat_producesUnixTimestamp() {
        SdkField<Instant> field = payloadField("fieldName", MarshallingType.INSTANT,
                                               obj -> Instant.ofEpochSecond(1000));
        String body = marshallAndGetBody(field);
        // Default PAYLOAD format is UNIX_TIMESTAMP — written via jsonGenerator.writeValue(Instant)
        // which for plain JSON writes epoch seconds (e.g. 1000.0 or 1000)
        assertThat(body).contains("\"fieldName\":");
        assertThat(body).contains("1000");
    }

    @Test
    void marshallPayloadField_withInstantUnixTimestampTrait_producesUnixTimestamp() {
        SdkField<Instant> field = SdkField.<Instant>builder(MarshallingType.INSTANT)
            .memberName("fieldName")
            .getter(obj -> Instant.ofEpochSecond(1000))
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("fieldName")
                        .build(),
                    TimestampFormatTrait.create(TimestampFormatTrait.Format.UNIX_TIMESTAMP))
            .build();
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":");
        assertThat(body).contains("1000");
    }

    @Test
    void marshallPayloadField_withInstantRfc822Trait_producesRfc822String() {
        SdkField<Instant> field = SdkField.<Instant>builder(MarshallingType.INSTANT)
            .memberName("fieldName")
            .getter(obj -> Instant.ofEpochSecond(1000))
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("fieldName")
                        .build(),
                    TimestampFormatTrait.create(TimestampFormatTrait.Format.RFC_822))
            .build();
        String body = marshallAndGetBody(field);
        // RFC 822 format: e.g. "Thu, 01 Jan 1970 00:16:40 GMT"
        assertThat(body).contains("\"fieldName\":\"");
        assertThat(body).contains("1970");
    }

    @Test
    void marshallPayloadField_withInstantIso8601Trait_producesIso8601String() {
        SdkField<Instant> field = SdkField.<Instant>builder(MarshallingType.INSTANT)
            .memberName("fieldName")
            .getter(obj -> Instant.ofEpochSecond(1000))
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("fieldName")
                        .build(),
                    TimestampFormatTrait.create(TimestampFormatTrait.Format.ISO_8601))
            .build();
        String body = marshallAndGetBody(field);
        // ISO 8601 format: e.g. "1970-01-01T00:16:40Z"
        assertThat(body).contains("\"fieldName\":\"");
        assertThat(body).contains("1970-01-01T");
    }

    @Test
    void marshallPayloadField_withSdkBytesValue_producesBase64EncodedJson() {
        SdkField<SdkBytes> field = payloadField("fieldName", MarshallingType.SDK_BYTES,
                                                 obj -> SdkBytes.fromUtf8String("data"));
        String body = marshallAndGetBody(field);
        // "data" base64 encoded is "ZGF0YQ=="
        assertThat(body).contains("\"fieldName\":\"ZGF0YQ==\"");
    }

    @Test
    void marshallPayloadField_withSdkPojoValue_producesNestedObjectJson() {
        // Inner pojo with a single string field
        SdkField<String> innerField = payloadField("innerField", MarshallingType.STRING, obj -> "innerValue");
        SimplePojo innerPojo = new SimplePojo(innerField);

        SdkField<SdkPojo> outerField = SdkField.<SdkPojo>builder(MarshallingType.SDK_POJO)
            .memberName("fieldName")
            .getter(obj -> innerPojo)
            .setter((obj, val) -> { })
            .constructor(() -> innerPojo)
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("fieldName")
                        .build())
            .build();

        String body = marshallAndGetBody(outerField);
        assertThat(body).contains("\"fieldName\":{\"innerField\":\"innerValue\"}");
    }

    @Test
    void marshallPayloadField_withNonEmptyList_producesArrayJson() {
        List<String> listValue = Arrays.asList("a", "b", "c");

        SdkField<String> memberField = SdkField.<String>builder(MarshallingType.STRING)
            .memberName("member")
            .getter(obj -> null)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("member")
                        .build())
            .build();

        SdkField<List<String>> field = SdkField.<List<String>>builder(MarshallingType.LIST)
            .memberName("fieldName")
            .getter(obj -> listValue)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("fieldName")
                        .build(),
                    ListTrait.builder()
                        .memberFieldInfo(memberField)
                        .build())
            .build();

        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":[\"a\",\"b\",\"c\"]");
    }

    @Test
    void marshallPayloadField_withEmptySdkAutoConstructList_isSkipped() {
        List<String> autoList = DefaultSdkAutoConstructList.getInstance();

        SdkField<String> memberField = SdkField.<String>builder(MarshallingType.STRING)
            .memberName("member")
            .getter(obj -> null)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("member")
                        .build())
            .build();

        SdkField<List<String>> field = SdkField.<List<String>>builder(MarshallingType.LIST)
            .memberName("fieldName")
            .getter(obj -> autoList)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("fieldName")
                        .build(),
                    ListTrait.builder()
                        .memberFieldInfo(memberField)
                        .build())
            .build();

        String body = marshallAndGetBody(field);
        assertThat(body).doesNotContain("fieldName");
    }

    @Test
    void marshallPayloadField_withEmptyRegularList_producesEmptyArray() {
        List<String> emptyList = new ArrayList<>();

        SdkField<String> memberField = SdkField.<String>builder(MarshallingType.STRING)
            .memberName("member")
            .getter(obj -> null)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("member")
                        .build())
            .build();

        SdkField<List<String>> field = SdkField.<List<String>>builder(MarshallingType.LIST)
            .memberName("fieldName")
            .getter(obj -> emptyList)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("fieldName")
                        .build(),
                    ListTrait.builder()
                        .memberFieldInfo(memberField)
                        .build())
            .build();

        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":[]");
    }

    @Test
    void marshallPayloadField_withNonEmptyMap_producesObjectJson() {
        // Use LinkedHashMap for deterministic ordering
        Map<String, String> mapValue = new LinkedHashMap<>();
        mapValue.put("key1", "val1");
        mapValue.put("key2", "val2");

        SdkField<String> valueField = SdkField.<String>builder(MarshallingType.STRING)
            .memberName("value")
            .getter(obj -> null)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("value")
                        .build())
            .build();

        SdkField<Map<String, String>> field = SdkField.<Map<String, String>>builder(MarshallingType.MAP)
            .memberName("fieldName")
            .getter(obj -> mapValue)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("fieldName")
                        .build(),
                    MapTrait.builder()
                        .valueFieldInfo(valueField)
                        .build())
            .build();

        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":{\"key1\":\"val1\",\"key2\":\"val2\"}");
    }

    @Test
    void marshallPayloadField_withEmptySdkAutoConstructMap_isSkipped() {
        Map<String, String> autoMap = DefaultSdkAutoConstructMap.getInstance();

        SdkField<String> valueField = SdkField.<String>builder(MarshallingType.STRING)
            .memberName("value")
            .getter(obj -> null)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("value")
                        .build())
            .build();

        SdkField<Map<String, String>> field = SdkField.<Map<String, String>>builder(MarshallingType.MAP)
            .memberName("fieldName")
            .getter(obj -> autoMap)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("fieldName")
                        .build(),
                    MapTrait.builder()
                        .valueFieldInfo(valueField)
                        .build())
            .build();

        String body = marshallAndGetBody(field);
        assertThat(body).doesNotContain("fieldName");
    }

    @Test
    void marshallPayloadField_withEmptyRegularMap_producesEmptyObject() {
        Map<String, String> emptyMap = new HashMap<>();

        SdkField<String> valueField = SdkField.<String>builder(MarshallingType.STRING)
            .memberName("value")
            .getter(obj -> null)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("value")
                        .build())
            .build();

        SdkField<Map<String, String>> field = SdkField.<Map<String, String>>builder(MarshallingType.MAP)
            .memberName("fieldName")
            .getter(obj -> emptyMap)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("fieldName")
                        .build(),
                    MapTrait.builder()
                        .valueFieldInfo(valueField)
                        .build())
            .build();

        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":{}");
    }

    @Test
    void marshallPayloadField_withMapNullValueEntry_isSkipped() {
        Map<String, String> mapValue = new LinkedHashMap<>();
        mapValue.put("key1", "val1");
        mapValue.put("key2", null);
        mapValue.put("key3", "val3");

        SdkField<String> valueField = SdkField.<String>builder(MarshallingType.STRING)
            .memberName("value")
            .getter(obj -> null)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("value")
                        .build())
            .build();

        SdkField<Map<String, String>> field = SdkField.<Map<String, String>>builder(MarshallingType.MAP)
            .memberName("fieldName")
            .getter(obj -> mapValue)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("fieldName")
                        .build(),
                    MapTrait.builder()
                        .valueFieldInfo(valueField)
                        .build())
            .build();

        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"key1\":\"val1\"");
        assertThat(body).doesNotContain("key2");
        assertThat(body).contains("\"key3\":\"val3\"");
    }

    // ---- DOCUMENT ----

    @Test
    void marshallPayloadField_withDocumentValue_producesCorrectJson() {
        SdkField<Document> field = payloadField("fieldName", MarshallingType.DOCUMENT,
                                                obj -> Document.fromString("test"));
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":\"test\"");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> SdkField<T> payloadField(String name,
                                                 MarshallingType marshallingType,
                                                 Function<Object, T> getter) {
        return (SdkField<T>) SdkField.builder(marshallingType)
            .memberName(name)
            .getter((Function) getter)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName(name)
                        .build())
            .build();
    }

    private String marshallAndGetBody(SdkField<?>... fields) {
        SdkPojo pojo = new SimplePojo(fields);
        SdkHttpFullRequest result = createMarshaller().marshall(pojo);
        return bodyAsString(result);
    }

    private static ProtocolMarshaller<SdkHttpFullRequest> createMarshaller() {
        return JsonProtocolMarshallerBuilder.create()
            .endpoint(ENDPOINT)
            .jsonGenerator(AwsStructuredPlainJsonFactory
                .SDK_JSON_FACTORY.createWriter(CONTENT_TYPE))
            .contentType(CONTENT_TYPE)
            .operationInfo(OP_INFO)
            .sendExplicitNullForPayload(false)
            .protocolMetadata(METADATA)
            .build();
    }

    private static String bodyAsString(SdkHttpFullRequest request) {
        return request.contentStreamProvider()
            .map(p -> {
                try {
                    return software.amazon.awssdk.utils.IoUtils.toUtf8String(p.newStream());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .orElse("");
    }

    private static final class SimplePojo implements SdkPojo {
        private final List<SdkField<?>> fields;

        SimplePojo(SdkField<?>... fields) {
            this.fields = Arrays.asList(fields);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return fields;
        }

        @Override
        public boolean equalsBySdkFields(Object other) {
            return other instanceof SimplePojo;
        }

        @Override
        public Map<String, SdkField<?>> sdkFieldNameToField() {
            return Collections.emptyMap();
        }
    }
}
