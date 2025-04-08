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

package software.amazon.awssdk.protocol.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.services.protocolrestjson.model.InputEvent;
import software.amazon.awssdk.services.protocolrestjson.model.InputEventStream;
import software.amazon.awssdk.services.protocolrestjson.model.InputEventStreamStringPayload;
import software.amazon.awssdk.services.protocolrestjson.model.InputEventStringPayload;
import software.amazon.awssdk.services.protocolrestjson.transform.InputEventMarshaller;
import software.amazon.awssdk.services.protocolrestjson.transform.InputEventStringPayloadMarshaller;

/**
 * Marshalling and Unmarshalling tests for events.
 */
public class EventTransformTest {
    private static final String EXPLICIT_PAYLOAD_JSON = "{\"ExplicitPayloadMember\": \"bar\"}";
    private static final String EXPLICIT_PAYLOAD_NON_JSON = "bar";
    private static final String HEADER_MEMBER_NAME = "HeaderMember";
    private static final String HEADER_MEMBER = "foo";
    private static AwsJsonProtocolFactory protocolFactory;

    @BeforeAll
    public static void setup() {
        protocolFactory = AwsJsonProtocolFactory.builder()
                                                .clientConfiguration(
                                                    SdkClientConfiguration.builder()
                                                                          .option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER,
                                                                                  ClientEndpointProvider.forEndpointOverride(URI.create("http://foo.amazonaws.com")))
                                                                          .build())
                                                .protocol(AwsJsonProtocol.AWS_JSON)
                                                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {EXPLICIT_PAYLOAD_JSON, EXPLICIT_PAYLOAD_NON_JSON})
    public void testUnmarshalling_BlobPayload(String payload) throws Exception {
        HttpResponseHandler<SdkPojo> responseHandler = protocolFactory
            .createResponseHandler(JsonOperationMetadata.builder().build(), InputEvent::builder);

        InputEvent unmarshalled = (InputEvent) responseHandler.handle(SdkHttpFullResponse.builder()
                                                                                         .content(AbortableInputStream.create(SdkBytes.fromUtf8String(payload).asInputStream()))
                                                                                         .putHeader(HEADER_MEMBER_NAME, HEADER_MEMBER)
                                                                                         .build(),
                                                                      new ExecutionAttributes());

        assertThat(unmarshalled.headerMember()).isEqualTo(HEADER_MEMBER);
        assertThat(unmarshalled.explicitPayloadMember().asUtf8String()).isEqualTo(payload);
    }

    @ParameterizedTest
    @ValueSource(strings = {EXPLICIT_PAYLOAD_JSON, EXPLICIT_PAYLOAD_NON_JSON})
    public void testUnmarshalling_StringPayload(String payload) throws Exception {
        HttpResponseHandler<SdkPojo> responseHandler = protocolFactory
            .createResponseHandler(JsonOperationMetadata.builder().build(), InputEventStringPayload::builder);

        InputEventStringPayload unmarshalled = (InputEventStringPayload) responseHandler.handle(SdkHttpFullResponse.builder()
                                                                                         .content(AbortableInputStream.create(SdkBytes.fromUtf8String(payload).asInputStream()))
                                                                                         .putHeader(HEADER_MEMBER_NAME, HEADER_MEMBER)
                                                                                         .build(),
                                                                      new ExecutionAttributes());

        assertThat(unmarshalled.headerMember()).isEqualTo(HEADER_MEMBER);
        assertThat(unmarshalled.explicitPayloadStringMember()).isEqualTo(payload);
    }

    @ParameterizedTest
    @ValueSource(strings = {EXPLICIT_PAYLOAD_JSON, EXPLICIT_PAYLOAD_NON_JSON})
    public void testMarshalling_BlobPayload(String payload) {
        InputEventMarshaller marshaller = new InputEventMarshaller(protocolFactory);

        InputEvent e = InputEventStream
            .inputEventBuilder()
            .headerMember(HEADER_MEMBER)
            .explicitPayloadMember(SdkBytes.fromUtf8String(payload))
            .build();

        SdkHttpFullRequest marshalled = marshaller.marshall(e);

        assertThat(marshalled.headers().get(HEADER_MEMBER_NAME)).containsExactly(HEADER_MEMBER);
        assertThat(marshalled.contentStreamProvider().get().newStream())
                .hasSameContentAs(SdkBytes.fromUtf8String(payload).asInputStream());
    }

    @ParameterizedTest
    @ValueSource(strings = {EXPLICIT_PAYLOAD_JSON, EXPLICIT_PAYLOAD_NON_JSON})
    public void testMarshalling_StringPayload(String payload) {
        InputEventStringPayloadMarshaller marshaller = new InputEventStringPayloadMarshaller(protocolFactory);

        InputEventStringPayload e = InputEventStreamStringPayload
            .inputEventBuilder()
            .headerMember(HEADER_MEMBER)
            .explicitPayloadStringMember(payload).build();

        SdkHttpFullRequest marshalled = marshaller.marshall(e);

        assertThat(marshalled.headers().get(HEADER_MEMBER_NAME)).containsExactly(HEADER_MEMBER);
        assertThat(marshalled.contentStreamProvider().get().newStream())
            .hasSameContentAs(SdkBytes.fromUtf8String(payload).asInputStream());
    }
}
