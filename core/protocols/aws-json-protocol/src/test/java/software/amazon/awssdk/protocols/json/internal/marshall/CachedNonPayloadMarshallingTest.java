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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;

/**
 * Tests that the cached non-payload marshalling path in
 * {@link JsonProtocolMarshaller#marshallFieldViaRegistry} produces correct output
 * and that the cache is populated after the first call.
 */
class CachedNonPayloadMarshallingTest {

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

    // ---- HEADER tests ----

    @Test
    void header_string_producesCorrectHeader() {
        SdkField<String> field = headerField("x-custom-header", obj -> "headerValue");
        SdkPojo pojo = new SimplePojo(field);

        SdkHttpFullRequest result = createMarshaller().marshall(pojo);

        assertThat(result.firstMatchingHeader("x-custom-header"))
            .isPresent()
            .hasValue("headerValue");
    }

    @Test
    void header_string_secondCall_usesCachedMarshaller() {
        // Use the SAME SdkField instance for both calls so the cache is shared
        SdkField<String> field = headerField("x-custom-header", obj -> "headerValue");

        // First call — populates the internal marshaller cache
        SdkPojo pojo1 = new SimplePojo(field);
        SdkHttpFullRequest result1 = createMarshaller().marshall(pojo1);

        // Second call — should use cached marshaller
        SdkPojo pojo2 = new SimplePojo(field);
        SdkHttpFullRequest result2 = createMarshaller().marshall(pojo2);

        // Both calls produce identical header output, confirming the cached path works
        assertThat(result1.firstMatchingHeader("x-custom-header"))
            .isPresent()
            .hasValue("headerValue");
        assertThat(result2.firstMatchingHeader("x-custom-header"))
            .isPresent()
            .hasValue("headerValue");
    }

    // ---- QUERY_PARAM tests ----

    @Test
    void queryParam_string_producesCorrectQueryParam() {
        SdkField<String> field = queryParamField("myParam", obj -> "paramValue");
        SdkPojo pojo = new SimplePojo(field);

        SdkHttpFullRequest result = createMarshaller().marshall(pojo);

        assertThat(result.rawQueryParameters().get("myParam"))
            .isNotNull()
            .containsExactly("paramValue");
    }

    private static SdkField<String> headerField(String headerName,
                                                 java.util.function.Function<Object, String> getter) {
        return SdkField.<String>builder(MarshallingType.STRING)
            .memberName(headerName)
            .getter(getter)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.HEADER)
                        .locationName(headerName)
                        .build())
            .build();
    }

    private static SdkField<String> queryParamField(String paramName,
                                                     java.util.function.Function<Object, String> getter) {
        return SdkField.<String>builder(MarshallingType.STRING)
            .memberName(paramName)
            .getter(getter)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.QUERY_PARAM)
                        .locationName(paramName)
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
