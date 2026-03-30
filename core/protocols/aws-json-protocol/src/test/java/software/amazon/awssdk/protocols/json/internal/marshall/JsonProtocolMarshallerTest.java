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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import software.amazon.awssdk.core.traits.RequiredTrait;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;

class JsonProtocolMarshallerTest {

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
    void nullPayloadField_notRequired_isSkipped() {
        SdkField<String> field = payloadField("OptionalField", obj -> null);
        SdkPojo pojo = new SimplePojo(field);

        SdkHttpFullRequest result = createMarshaller().marshall(pojo);

        String body = bodyAsString(result);
        assertThat(body).doesNotContain("OptionalField");
    }

    @Test
    void nullPayloadField_required_throwsIllegalArgumentException() {
        SdkField<String> field = SdkField.<String>builder(MarshallingType.STRING)
            .memberName("RequiredField")
            .getter(obj -> null)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("RequiredField")
                        .build(),
                    RequiredTrait.create())
            .build();

        SdkPojo pojo = new SimplePojo(field);

        assertThatThrownBy(() -> createMarshaller().marshall(pojo))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("RequiredField")
            .hasMessageContaining("must not be null");
    }

    @Test
    void nonNullPayloadField_isSerialized() {
        SdkField<String> field = payloadField("Name", obj -> "hello");
        SdkPojo pojo = new SimplePojo(field);

        SdkHttpFullRequest result = createMarshaller().marshall(pojo);

        String body = bodyAsString(result);
        assertThat(body).contains("\"Name\"");
        assertThat(body).contains("\"hello\"");
    }

    @Test
    void nullNonPayloadField_stillGoesToMarshallField() {
        SdkField<String> field = SdkField.<String>builder(MarshallingType.STRING)
            .memberName("HeaderField")
            .getter(obj -> null)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.HEADER)
                        .locationName("x-custom-header")
                        .build())
            .build();

        SdkPojo pojo = new SimplePojo(field);

        assertThatNoException().isThrownBy(
            () -> createMarshaller().marshall(pojo));
    }

    @Test
    void nullPathField_notRequired_stillThrows() {
        SdkField<String> field = SdkField.<String>builder(MarshallingType.STRING)
            .memberName("PathParam")
            .getter(obj -> null)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PATH)
                        .locationName("PathParam")
                        .build())
            .build();

        SdkPojo pojo = new SimplePojo(field);

        assertThatThrownBy(() -> createMarshaller().marshall(pojo))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PathParam")
            .hasMessageContaining("must not be null");
    }

    private static SdkField<String> payloadField(String name,
                                                  java.util.function.Function<Object, String> getter) {
        return SdkField.<String>builder(MarshallingType.STRING)
            .memberName(name)
            .getter(getter)
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName(name)
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
