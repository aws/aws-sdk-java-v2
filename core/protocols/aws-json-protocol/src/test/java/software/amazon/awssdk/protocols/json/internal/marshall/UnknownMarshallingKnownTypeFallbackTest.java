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
import software.amazon.awssdk.core.protocol.MarshallingKnownType;
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
 * Tests that when {@code getKnownType()} returns null, the marshaller falls back to the
 * registry-based path without throwing a {@link NullPointerException} from the switch statement.
 */
class UnknownMarshallingKnownTypeFallbackTest {

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

    /**
     * A custom MarshallingType whose {@code getKnownType()} returns null.
     * This simulates a future or third-party MarshallingType that is not in the known enum set.
     */
    private static final MarshallingType<String> CUSTOM_NULL_KNOWN_TYPE = new MarshallingType<String>() {
        @Override
        public Class<? super String> getTargetClass() {
            return String.class;
        }

        @Override
        public MarshallingKnownType getKnownType() {
            return null;
        }

        @Override
        public String toString() {
            return "CUSTOM_NULL_KNOWN_TYPE";
        }
    };

    @Test
    void nullKnownType_fallsBackToRegistryPath_doesNotThrowNpeFromSwitch() {
        SdkField<String> field = SdkField.<String>builder(CUSTOM_NULL_KNOWN_TYPE)
            .memberName("customField")
            .getter(obj -> "someValue")
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("customField")
                        .build())
            .build();

        SdkPojo pojo = new SimplePojo(field);

        // The null-knownType guard in marshallPayloadField should redirect to marshallFieldViaRegistry.
        // Since CUSTOM_NULL_KNOWN_TYPE is not registered in the static MARSHALLER_REGISTRY,
        // the registry returns null and a NullPointerException occurs when invoking .marshall() on it.
        // The critical assertion: the NPE stack trace must NOT originate from the switch statement
        // in marshallPayloadField — it must come from the registry fallback path.
        assertThatThrownBy(() -> createMarshaller().marshall(pojo))
            .isInstanceOf(NullPointerException.class)
            .satisfies(thrown -> {
                // Verify the NPE comes from marshallFieldViaRegistry (the fallback),
                // not from marshallPayloadField's switch statement
                StackTraceElement[] stack = thrown.getStackTrace();
                boolean fromRegistryPath = false;
                for (StackTraceElement element : stack) {
                    if ("marshallFieldViaRegistry".equals(element.getMethodName())) {
                        fromRegistryPath = true;
                        break;
                    }
                }
                assertThat(fromRegistryPath)
                    .as("NPE should originate from marshallFieldViaRegistry (registry fallback), "
                        + "not from the switch in marshallPayloadField")
                    .isTrue();
            });
    }

    @Test
    void knownType_string_isHandledBySwitchPath() {
        SdkField<String> field = SdkField.<String>builder(MarshallingType.STRING)
            .memberName("normalField")
            .getter(obj -> "hello")
            .setter((obj, val) -> { })
            .traits(LocationTrait.builder()
                        .location(MarshallLocation.PAYLOAD)
                        .locationName("normalField")
                        .build())
            .build();

        SdkPojo pojo = new SimplePojo(field);

        SdkHttpFullRequest result = createMarshaller().marshall(pojo);
        String body = bodyAsString(result);
        assertThat(body).contains("\"normalField\":\"hello\"");
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
