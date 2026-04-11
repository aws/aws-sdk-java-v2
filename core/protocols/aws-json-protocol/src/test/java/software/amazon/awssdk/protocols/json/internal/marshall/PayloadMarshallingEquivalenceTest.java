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
 * produces correct JSON output for all 16 {@code MarshallingKnownType} values.
 *
 * <p><b>Validates: Property 1 — Payload marshalling behavioral equivalence</b></p>
 * <p><b>Validates: Requirements 2.1–2.12, 3.1–3.5, 4.1, 5.1–5.3, 6.1–6.4</b></p>
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

    // ---- STRING ----

    @Test
    void string_producesCorrectJson() {
        SdkField<String> field = payloadField("fieldName", MarshallingType.STRING, obj -> "hello world");
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":\"hello world\"");
    }

    // ---- INTEGER ----

    @Test
    void integer_producesCorrectJson() {
        SdkField<Integer> field = payloadField("fieldName", MarshallingType.INTEGER, obj -> 42);
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":42");
    }

    // ---- LONG ----

    @Test
    void long_producesCorrectJson() {
        SdkField<Long> field = payloadField("fieldName", MarshallingType.LONG, obj -> 123456789L);
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":123456789");
    }

    // ---- SHORT ----

    @Test
    void short_producesCorrectJson() {
        SdkField<Short> field = payloadField("fieldName", MarshallingType.SHORT, obj -> (short) 7);
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":7");
    }

    // ---- BYTE ----

    @Test
    void byte_producesCorrectJson() {
        SdkField<Byte> field = payloadField("fieldName", MarshallingType.BYTE, obj -> (byte) 3);
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":3");
    }

    // ---- FLOAT ----

    @Test
    void float_producesCorrectJson() {
        SdkField<Float> field = payloadField("fieldName", MarshallingType.FLOAT, obj -> 1.5f);
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":1.5");
    }

    // ---- DOUBLE ----

    @Test
    void double_producesCorrectJson() {
        SdkField<Double> field = payloadField("fieldName", MarshallingType.DOUBLE, obj -> 3.14);
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":3.14");
    }

    // ---- BIG_DECIMAL ----

    @Test
    void bigDecimal_producesCorrectJson() {
        SdkField<BigDecimal> field = payloadField("fieldName", MarshallingType.BIG_DECIMAL,
                                                   obj -> new BigDecimal("99.99"));
        String body = marshallAndGetBody(field);
        // BigDecimal is serialized as a quoted string by the JSON generator
        assertThat(body).contains("\"fieldName\":\"99.99\"");
    }

    // ---- BOOLEAN ----

    @Test
    void boolean_producesCorrectJson() {
        SdkField<Boolean> field = payloadField("fieldName", MarshallingType.BOOLEAN, obj -> true);
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":true");
    }

    // ---- INSTANT (default format — UNIX_TIMESTAMP for PAYLOAD) ----

    @Test
    void instant_defaultFormat_producesUnixTimestamp() {
        SdkField<Instant> field = payloadField("fieldName", MarshallingType.INSTANT,
                                               obj -> Instant.ofEpochSecond(1000));
        String body = marshallAndGetBody(field);
        // Default PAYLOAD format is UNIX_TIMESTAMP — written via jsonGenerator.writeValue(Instant)
        // which for plain JSON writes epoch seconds (e.g. 1000.0 or 1000)
        assertThat(body).contains("\"fieldName\":");
        assertThat(body).contains("1000");
    }

    // ---- INSTANT with UNIX_TIMESTAMP trait ----

    @Test
    void instant_unixTimestampTrait_producesUnixTimestamp() {
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

    // ---- INSTANT with RFC_822 trait ----

    @Test
    void instant_rfc822Trait_producesRfc822String() {
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

    // ---- INSTANT with ISO_8601 trait ----

    @Test
    void instant_iso8601Trait_producesIso8601String() {
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

    // ---- SDK_BYTES ----

    @Test
    void sdkBytes_producesBase64EncodedJson() {
        SdkField<SdkBytes> field = payloadField("fieldName", MarshallingType.SDK_BYTES,
                                                 obj -> SdkBytes.fromUtf8String("data"));
        String body = marshallAndGetBody(field);
        // "data" base64 encoded is "ZGF0YQ=="
        assertThat(body).contains("\"fieldName\":\"ZGF0YQ==\"");
    }

    // ---- SDK_POJO (nested) ----

    @Test
    void sdkPojo_producesNestedObjectJson() {
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

    // ---- LIST (non-empty) ----

    @Test
    void list_nonEmpty_producesArrayJson() {
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

    // ---- LIST (empty SdkAutoConstructList — should be skipped) ----

    @Test
    void list_emptySdkAutoConstructList_isSkipped() {
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

    // ---- LIST (empty regular list — should emit empty array) ----

    @Test
    void list_emptyRegularList_producesEmptyArray() {
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

    // ---- MAP (non-empty) ----

    @Test
    void map_nonEmpty_producesObjectJson() {
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

    // ---- MAP (empty SdkAutoConstructMap — should be skipped) ----

    @Test
    void map_emptySdkAutoConstructMap_isSkipped() {
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

    // ---- MAP (empty regular map — should emit empty object) ----

    @Test
    void map_emptyRegularMap_producesEmptyObject() {
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

    // ---- MAP with null value entry — entry is skipped ----

    @Test
    void map_nullValueEntry_isSkipped() {
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
    void document_producesCorrectJson() {
        SdkField<Document> field = payloadField("fieldName", MarshallingType.DOCUMENT,
                                                obj -> Document.fromString("test"));
        String body = marshallAndGetBody(field);
        assertThat(body).contains("\"fieldName\":\"test\"");
    }

    // ---- Helper methods ----

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
