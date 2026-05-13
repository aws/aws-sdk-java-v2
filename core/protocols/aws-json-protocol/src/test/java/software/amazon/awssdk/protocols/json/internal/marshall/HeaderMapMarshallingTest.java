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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;

/**
 * Tests that Map fields with {@link MarshallLocation#HEADER} (prefix headers) are marshalled
 * correctly in the JSON protocol. This covers the bug where the JSON protocol was missing a
 * headerMarshaller for {@link MarshallingType#MAP}, causing a NullPointerException in
 * {@link JsonProtocolMarshaller#marshallFieldViaRegistry}.
 */
class HeaderMapMarshallingTest {

    private static final URI ENDPOINT = URI.create("http://localhost");
    private static final String CONTENT_TYPE = "application/x-amz-json-1.1";
    private static final OperationInfo OP_INFO = OperationInfo.builder()
        .httpMethod(SdkHttpMethod.PUT)
        .hasImplicitPayloadMembers(false)
        .build();
    private static final AwsJsonProtocolMetadata METADATA =
        AwsJsonProtocolMetadata.builder()
            .protocol(AwsJsonProtocol.REST_JSON)
            .contentType(CONTENT_TYPE)
            .build();

    @Test
    void mapInHeader_withEntries_producesCorrectPrefixHeaders() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", "value2");

        SdkField<Map<String, String>> field = mapHeaderField("x-amz-meta-", obj -> metadata);
        SdkPojo pojo = new SimplePojo(field);

        SdkHttpFullRequest result = createMarshaller().marshall(pojo);

        assertThat(result.firstMatchingHeader("x-amz-meta-key1")).hasValue("value1");
        assertThat(result.firstMatchingHeader("x-amz-meta-key2")).hasValue("value2");
    }

    @Test
    void mapInHeader_withEmptyMap_producesNoHeaders() {
        Map<String, String> metadata = new HashMap<>();

        SdkField<Map<String, String>> field = mapHeaderField("x-amz-meta-", obj -> metadata);
        SdkPojo pojo = new SimplePojo(field);

        SdkHttpFullRequest result = createMarshaller().marshall(pojo);

        assertThat(result.firstMatchingHeader("x-amz-meta-")).isNotPresent();
    }

    @Test
    void mapInHeader_withNullMap_producesNoHeaders() {
        SdkField<Map<String, String>> field = mapHeaderField("x-amz-meta-", obj -> null);
        SdkPojo pojo = new SimplePojo(field);

        SdkHttpFullRequest result = createMarshaller().marshall(pojo);

        assertThat(result.firstMatchingHeader("x-amz-meta-")).isNotPresent();
    }

    @Test
    void mapInHeader_keyAlreadyHasPrefix_doesNotDoublePrefix() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("x-amz-meta-already-prefixed", "value1");

        SdkField<Map<String, String>> field = mapHeaderField("x-amz-meta-", obj -> metadata);
        SdkPojo pojo = new SimplePojo(field);

        SdkHttpFullRequest result = createMarshaller().marshall(pojo);

        assertThat(result.firstMatchingHeader("x-amz-meta-already-prefixed")).hasValue("value1");
    }

    @Test
    void mapInHeader_withEmptyStringValue_producesHeaderWithEmptyValue() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("key1", "");

        SdkField<Map<String, String>> field = mapHeaderField("x-amz-meta-", obj -> metadata);
        SdkPojo pojo = new SimplePojo(field);

        SdkHttpFullRequest result = createMarshaller().marshall(pojo);

        // The SDK's firstMatchingHeader returns Optional.empty() for empty-value headers,
        // but the header IS present in the raw headers map
        assertThat(result.headers().get("x-amz-meta-key1")).containsExactly("");
    }


    @SuppressWarnings("unchecked")
    private static SdkField<Map<String, String>> mapHeaderField(
            String prefix, java.util.function.Function<Object, Map<String, String>> getter) {

        SdkField<String> valueField = SdkField.<String>builder(MarshallingType.STRING)
            .memberName("value")
            .getter(obj -> null)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.HEADER)
                        .locationName("value")
                        .build())
            .build();

        return (SdkField<Map<String, String>>) (SdkField) SdkField.builder(MarshallingType.MAP)
            .memberName("Metadata")
            .getter((java.util.function.Function) getter)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.HEADER)
                        .locationName(prefix)
                        .build(),
                    MapTrait.builder()
                        .valueFieldInfo(valueField)
                        .build())
            .build();
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
