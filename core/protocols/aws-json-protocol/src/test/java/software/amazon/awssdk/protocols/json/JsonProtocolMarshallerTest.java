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

package software.amazon.awssdk.protocols.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.json.internal.marshall.DirectJsonProtocolMarshaller;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshallerBuilder;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;
import software.amazon.awssdk.utils.IoUtils;

class JsonProtocolMarshallerTest {

    private static final URI ENDPOINT = URI.create("https://test.amazonaws.com");
    
    private OperationInfo operationInfo;
    private AwsJsonProtocolMetadata protocolMetadata;

    @BeforeEach
    void setUp() {
        operationInfo = OperationInfo.builder()
                .requestUri("/")
                .httpMethod(software.amazon.awssdk.http.SdkHttpMethod.POST)
                .hasImplicitPayloadMembers(true)
                .build();
        
        protocolMetadata = AwsJsonProtocolMetadata.builder()
                .protocol(AwsJsonProtocol.AWS_JSON)
                .protocolVersion("1.1")
                .build();
    }

    @Test
    void testMarshallPrimitiveTypes() {
        SimplePojo pojo = new SimplePojo("testString", 42, 123L, true, 3.14f, 2.71828);
        
        String jsonJackson = marshallToStringWithJackson(pojo);
        String jsonDirect = marshallToStringWithDirect(pojo);
        
        // Parse both and verify they produce the same structure
        JsonNode nodeJackson = JsonNodeParser.create().parse(jsonJackson);
        JsonNode nodeDirect = JsonNodeParser.create().parse(jsonDirect);
        
        // Verify Jackson output
        assertThat(nodeJackson.asObject().get("stringField").text()).isEqualTo("testString");
        assertThat(Integer.parseInt(nodeJackson.asObject().get("intField").asNumber())).isEqualTo(42);
        assertThat(Long.parseLong(nodeJackson.asObject().get("longField").asNumber())).isEqualTo(123L);
        assertThat(nodeJackson.asObject().get("boolField").asBoolean()).isTrue();
        assertThat(Float.parseFloat(nodeJackson.asObject().get("floatField").asNumber())).isEqualTo(3.14f);
        assertThat(Double.parseDouble(nodeJackson.asObject().get("doubleField").asNumber())).isEqualTo(2.71828);
        
        // Verify Direct output matches
        assertThat(nodeDirect.asObject().get("stringField").text()).isEqualTo("testString");
        assertThat(Integer.parseInt(nodeDirect.asObject().get("intField").asNumber())).isEqualTo(42);
        assertThat(Long.parseLong(nodeDirect.asObject().get("longField").asNumber())).isEqualTo(123L);
        assertThat(nodeDirect.asObject().get("boolField").asBoolean()).isTrue();
        assertThat(Float.parseFloat(nodeDirect.asObject().get("floatField").asNumber())).isEqualTo(3.14f);
        assertThat(Double.parseDouble(nodeDirect.asObject().get("doubleField").asNumber())).isEqualTo(2.71828);
    }

    @Test
    void testMarshallList() {
        ListPojo pojo = new ListPojo(Arrays.asList("one", "two", "three"));
        
        String jsonJackson = marshallToStringWithJackson(pojo);
        String jsonDirect = marshallToStringWithDirect(pojo);
        
        JsonNode nodeJackson = JsonNodeParser.create().parse(jsonJackson);
        JsonNode nodeDirect = JsonNodeParser.create().parse(jsonDirect);
        
        // Verify both produce the same list structure
        JsonNode listNodeJackson = nodeJackson.asObject().get("items");
        JsonNode listNodeDirect = nodeDirect.asObject().get("items");
        
        assertThat(listNodeJackson.isArray()).isTrue();
        assertThat(listNodeJackson.asArray().size()).isEqualTo(3);
        assertThat(listNodeJackson.asArray().get(0).text()).isEqualTo("one");
        assertThat(listNodeJackson.asArray().get(1).text()).isEqualTo("two");
        assertThat(listNodeJackson.asArray().get(2).text()).isEqualTo("three");
        
        assertThat(listNodeDirect.isArray()).isTrue();
        assertThat(listNodeDirect.asArray().size()).isEqualTo(3);
        assertThat(listNodeDirect.asArray().get(0).text()).isEqualTo("one");
        assertThat(listNodeDirect.asArray().get(1).text()).isEqualTo("two");
        assertThat(listNodeDirect.asArray().get(2).text()).isEqualTo("three");
    }

    @Test
    void testMarshallMap() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        MapPojo pojo = new MapPojo(map);
        
        String jsonJackson = marshallToStringWithJackson(pojo);
        String jsonDirect = marshallToStringWithDirect(pojo);
        
        JsonNode nodeJackson = JsonNodeParser.create().parse(jsonJackson);
        JsonNode nodeDirect = JsonNodeParser.create().parse(jsonDirect);
        
        JsonNode mapNodeJackson = nodeJackson.asObject().get("attributes");
        JsonNode mapNodeDirect = nodeDirect.asObject().get("attributes");
        
        assertThat(mapNodeJackson.isObject()).isTrue();
        assertThat(mapNodeJackson.asObject().get("key1").text()).isEqualTo("value1");
        assertThat(mapNodeJackson.asObject().get("key2").text()).isEqualTo("value2");
        
        assertThat(mapNodeDirect.isObject()).isTrue();
        assertThat(mapNodeDirect.asObject().get("key1").text()).isEqualTo("value1");
        assertThat(mapNodeDirect.asObject().get("key2").text()).isEqualTo("value2");
    }

    @Test
    void testMarshallNestedObject() {
        NestedPojo nested = new NestedPojo("nestedValue");
        ParentPojo pojo = new ParentPojo("parentValue", nested);
        
        String jsonJackson = marshallToStringWithJackson(pojo);
        String jsonDirect = marshallToStringWithDirect(pojo);
        
        JsonNode nodeJackson = JsonNodeParser.create().parse(jsonJackson);
        JsonNode nodeDirect = JsonNodeParser.create().parse(jsonDirect);
        
        assertThat(nodeJackson.asObject().get("name").text()).isEqualTo("parentValue");
        JsonNode nestedNodeJackson = nodeJackson.asObject().get("nested");
        assertThat(nestedNodeJackson.isObject()).isTrue();
        assertThat(nestedNodeJackson.asObject().get("value").text()).isEqualTo("nestedValue");
        
        assertThat(nodeDirect.asObject().get("name").text()).isEqualTo("parentValue");
        JsonNode nestedNodeDirect = nodeDirect.asObject().get("nested");
        assertThat(nestedNodeDirect.isObject()).isTrue();
        assertThat(nestedNodeDirect.asObject().get("value").text()).isEqualTo("nestedValue");
    }

    @Test
    void testMarshallNullValues() {
        SimplePojo pojo = new SimplePojo(null, null, null, null, null, null);
        
        String jsonJackson = marshallToStringWithJackson(pojo);
        String jsonDirect = marshallToStringWithDirect(pojo);
        
        JsonNode nodeJackson = JsonNodeParser.create().parse(jsonJackson);
        JsonNode nodeDirect = JsonNodeParser.create().parse(jsonDirect);
        
        // Null values should not be present in JSON for both implementations
        assertThat(nodeJackson.asObject().get("stringField")).isNull();
        assertThat(nodeJackson.asObject().get("intField")).isNull();
        
        assertThat(nodeDirect.asObject().get("stringField")).isNull();
        assertThat(nodeDirect.asObject().get("intField")).isNull();
    }

    @Test
    void testMarshallEmptyCollections() {
        ListPojo listPojo = new ListPojo(Collections.emptyList());
        MapPojo mapPojo = new MapPojo(Collections.emptyMap());
        
        String jsonJacksonList = marshallToStringWithJackson(listPojo);
        String jsonDirectList = marshallToStringWithDirect(listPojo);
        
        String jsonJacksonMap = marshallToStringWithJackson(mapPojo);
        String jsonDirectMap = marshallToStringWithDirect(mapPojo);
        
        // Both implementations should produce the same output for empty collections
        System.out.println("Jackson list: " + jsonJacksonList);
        System.out.println("Direct list: " + jsonDirectList);
        System.out.println("Jackson map: " + jsonJacksonMap);
        System.out.println("Direct map: " + jsonDirectMap);
        
        JsonNode nodeJacksonList = JsonNodeParser.create().parse(jsonJacksonList);
        JsonNode nodeDirectList = JsonNodeParser.create().parse(jsonDirectList);
        
        JsonNode nodeJacksonMap = JsonNodeParser.create().parse(jsonJacksonMap);
        JsonNode nodeDirectMap = JsonNodeParser.create().parse(jsonDirectMap);
        
        // Verify both produce the same structure
        assertThat(nodeJacksonList.asObject().get("items")).isEqualTo(nodeDirectList.asObject().get("items"));
        assertThat(nodeJacksonMap.asObject().get("attributes")).isEqualTo(nodeDirectMap.asObject().get("attributes"));
    }

    @Test
    void testMarshallBinaryData() {
        // Test with binary data that includes null bytes and non-printable characters
        byte[] binaryData = new byte[] {0x6c, 0x66, 0x6b, 0x6e, 0x33, 0x00, 0x6c, 0x6b, 0x66, 0x6e, 0x33, 0x66, 0x68, 0x30, 0x33, 0x39, 0x66, 0x68, 0x69, 0x69, 0x6c};
        BinaryPojo pojo = new BinaryPojo(SdkBytes.fromByteArray(binaryData));
        
        String jsonJackson = marshallToStringWithJackson(pojo);
        String jsonDirect = marshallToStringWithDirect(pojo);
        
        System.out.println("Jackson binary: " + jsonJackson);
        System.out.println("Direct binary: " + jsonDirect);
        
        JsonNode nodeJackson = JsonNodeParser.create().parse(jsonJackson);
        JsonNode nodeDirect = JsonNodeParser.create().parse(jsonDirect);
        
        // Both should produce the same base64-encoded string
        assertThat(nodeJackson.asObject().get("data").text()).isEqualTo(nodeDirect.asObject().get("data").text());
        
        // Verify it's actually base64 encoded (should be "bGZrbjMAbGtmbjNmaDAzOWZoaWls")
        assertThat(nodeJackson.asObject().get("data").text()).isEqualTo("bGZrbjMAbGtmbjNmaDAzOWZoaWls");
    }

    private String marshallToStringWithJackson(SdkPojo pojo) {
        SdkHttpFullRequest request = JsonProtocolMarshallerBuilder.create()
                .endpoint(ENDPOINT)
                .jsonGenerator(new SdkJsonGenerator(new JsonFactory(), "application/json"))
                .contentType("application/json")
                .operationInfo(operationInfo)
                .protocolMetadata(protocolMetadata)
                .build()
                .marshall(pojo);
        
        try {
            return IoUtils.toUtf8String(request.contentStreamProvider().get().newStream());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read marshalled content", e);
        }
    }

    private String marshallToStringWithDirect(SdkPojo pojo) {
        DirectJsonProtocolMarshaller marshaller = new DirectJsonProtocolMarshaller(
                ENDPOINT,
                "application/json",
                operationInfo,
                protocolMetadata,
                false
        );
        
        SdkHttpFullRequest request = marshaller.marshall(pojo);
        
        try {
            return IoUtils.toUtf8String(request.contentStreamProvider().get().newStream());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read marshalled content", e);
        }
    }

    // Test POJO classes
    private static class SimplePojo implements SdkPojo {
        private final String stringField;
        private final Integer intField;
        private final Long longField;
        private final Boolean boolField;
        private final Float floatField;
        private final Double doubleField;

        SimplePojo(String stringField, Integer intField, Long longField, Boolean boolField,
                   Float floatField, Double doubleField) {
            this.stringField = stringField;
            this.intField = intField;
            this.longField = longField;
            this.boolField = boolField;
            this.floatField = floatField;
            this.doubleField = doubleField;
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Arrays.asList(
                    SdkField.<String>builder(MarshallingType.STRING)
                            .memberName("stringField").getter(p -> ((SimplePojo) p).stringField).setter((p, v) -> {})
                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("stringField").build()).build(),
                    SdkField.<Integer>builder(MarshallingType.INTEGER)
                            .memberName("intField").getter(p -> ((SimplePojo) p).intField).setter((p, v) -> {})
                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("intField").build()).build(),
                    SdkField.<Long>builder(MarshallingType.LONG)
                            .memberName("longField").getter(p -> ((SimplePojo) p).longField).setter((p, v) -> {})
                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("longField").build()).build(),
                    SdkField.<Boolean>builder(MarshallingType.BOOLEAN)
                            .memberName("boolField").getter(p -> ((SimplePojo) p).boolField).setter((p, v) -> {})
                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("boolField").build()).build(),
                    SdkField.<Float>builder(MarshallingType.FLOAT)
                            .memberName("floatField").getter(p -> ((SimplePojo) p).floatField).setter((p, v) -> {})
                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("floatField").build()).build(),
                    SdkField.<Double>builder(MarshallingType.DOUBLE)
                            .memberName("doubleField").getter(p -> ((SimplePojo) p).doubleField).setter((p, v) -> {})
                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("doubleField").build()).build()
            );
        }
    }

    private static class ListPojo implements SdkPojo {
        private final List<String> items;

        ListPojo(List<String> items) {
            this.items = items;
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.singletonList(
                    SdkField.<List<String>>builder(MarshallingType.LIST)
                            .memberName("items").getter(p -> ((ListPojo) p).items).setter((p, v) -> {})
                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("items").build()).build()
            );
        }
    }

    private static class MapPojo implements SdkPojo {
        private final Map<String, String> attributes;

        MapPojo(Map<String, String> attributes) {
            this.attributes = attributes;
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.singletonList(
                    SdkField.<Map<String, String>>builder(MarshallingType.MAP)
                            .memberName("attributes").getter(p -> ((MapPojo) p).attributes).setter((p, v) -> {})
                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("attributes").build()).build()
            );
        }
    }

    private static class NestedPojo implements SdkPojo {
        private final String value;

        NestedPojo(String value) {
            this.value = value;
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.singletonList(
                    SdkField.<String>builder(MarshallingType.STRING)
                            .memberName("value").getter(p -> ((NestedPojo) p).value).setter((p, v) -> {})
                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("value").build()).build()
            );
        }
    }

    private static class ParentPojo implements SdkPojo {
        private final String name;
        private final NestedPojo nested;

        ParentPojo(String name, NestedPojo nested) {
            this.name = name;
            this.nested = nested;
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Arrays.asList(
                    SdkField.<String>builder(MarshallingType.STRING)
                            .memberName("name").getter(p -> ((ParentPojo) p).name).setter((p, v) -> {})
                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("name").build()).build(),
                    SdkField.<NestedPojo>builder(MarshallingType.SDK_POJO)
                            .memberName("nested").getter(p -> ((ParentPojo) p).nested).setter((p, v) -> {})
                            .constructor(() -> new NestedPojo(null))
                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("nested").build()).build()
            );
        }
    }

    private static class BinaryPojo implements SdkPojo {
        private final SdkBytes data;

        BinaryPojo(SdkBytes data) {
            this.data = data;
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.singletonList(
                    SdkField.<SdkBytes>builder(MarshallingType.SDK_BYTES)
                            .memberName("data").getter(p -> ((BinaryPojo) p).data).setter((p, v) -> {})
                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("data").build()).build()
            );
        }
    }
}
