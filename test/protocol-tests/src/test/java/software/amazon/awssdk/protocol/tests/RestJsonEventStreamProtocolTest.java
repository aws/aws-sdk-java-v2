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
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.services.protocolrestjsoncontenttype.model.BlobAndHeadersEvent;
import software.amazon.awssdk.services.protocolrestjsoncontenttype.model.HeadersOnlyEvent;
import software.amazon.awssdk.services.protocolrestjsoncontenttype.model.ImplicitPayloadAndHeadersEvent;
import software.amazon.awssdk.services.protocolrestjsoncontenttype.model.InputEventStream;
import software.amazon.awssdk.services.protocolrestjsoncontenttype.model.ProtocolRestJsonContentTypeException;
import software.amazon.awssdk.services.protocolrestjsoncontenttype.transform.BlobAndHeadersEventMarshaller;
import software.amazon.awssdk.services.protocolrestjsoncontenttype.transform.HeadersOnlyEventMarshaller;
import software.amazon.awssdk.services.protocolrestjsoncontenttype.transform.ImplicitPayloadAndHeadersEventMarshaller;

public class RestJsonEventStreamProtocolTest {
    private static final String EVENT_CONTENT_TYPE_HEADER = ":content-type";

    @Test
    public void implicitPayloadAndHeaders_payloadMemberPresent() {
        ImplicitPayloadAndHeadersEventMarshaller marshaller = new ImplicitPayloadAndHeadersEventMarshaller(protocolFactory());

        ImplicitPayloadAndHeadersEvent event = InputEventStream.implicitPayloadAndHeadersEventBuilder()
                                                               .stringMember("hello rest-json")
                                                               .headerMember("hello rest-json")
                                                               .build();

        SdkHttpFullRequest marshalledEvent = marshaller.marshall(event);

        assertThat(marshalledEvent.headers().get(EVENT_CONTENT_TYPE_HEADER)).containsExactly("application/json");

        String content = contentAsString(marshalledEvent);
        assertThat(content).isEqualTo("{\"StringMember\":\"hello rest-json\"}");
    }

    @Test
    public void implicitPayloadAndHeaders_payloadMemberNotPresent() {
        ImplicitPayloadAndHeadersEventMarshaller marshaller = new ImplicitPayloadAndHeadersEventMarshaller(protocolFactory());

        ImplicitPayloadAndHeadersEvent event = InputEventStream.implicitPayloadAndHeadersEventBuilder()
                                                               .headerMember("hello rest-json")
                                                               .build();

        SdkHttpFullRequest marshalledEvent = marshaller.marshall(event);

        assertThat(marshalledEvent.headers().get(EVENT_CONTENT_TYPE_HEADER)).containsExactly("application/json");

        String content = contentAsString(marshalledEvent);
        assertThat(content).isEqualTo("{}");
    }

    @Test
    public void blobAndHeadersEvent() {
        BlobAndHeadersEventMarshaller marshaller = new BlobAndHeadersEventMarshaller(protocolFactory());

        BlobAndHeadersEvent event = InputEventStream.blobAndHeadersEventBuilder()
                                                    .headerMember("hello rest-json")
                                                    .blobPayloadMember(SdkBytes.fromUtf8String("hello rest-json"))
                                                    .build();

        SdkHttpFullRequest marshalledEvent = marshaller.marshall(event);

        assertThat(marshalledEvent.headers().get(EVENT_CONTENT_TYPE_HEADER)).containsExactly("application/octet-stream");

        String content = contentAsString(marshalledEvent);
        assertThat(content).isEqualTo("hello rest-json");
    }

    @Test
    public void headersOnly() {
        HeadersOnlyEventMarshaller marshaller = new HeadersOnlyEventMarshaller(protocolFactory());

        HeadersOnlyEvent event = InputEventStream.headersOnlyEventBuilder()
                                                               .headerMember("hello rest-json")
                                                               .build();

        SdkHttpFullRequest marshalledEvent = marshaller.marshall(event);

        assertThat(marshalledEvent.headers().keySet()).doesNotContain(EVENT_CONTENT_TYPE_HEADER);

        String content = contentAsString(marshalledEvent);
        assertThat(content).isEqualTo("");
    }

    private static AwsJsonProtocolFactory protocolFactory() {
        return AwsJsonProtocolFactory.builder()
                                     .clientConfiguration(
                                         SdkClientConfiguration.builder()
                                                               .option(SdkClientOption.ENDPOINT,
                                                                       URI.create("https://test.aws.com"))
                                                               .build())
                                     .defaultServiceExceptionSupplier(ProtocolRestJsonContentTypeException::builder)
                                     .protocol(AwsJsonProtocol.REST_JSON)
                                     .protocolVersion("1.1")
                                     .build();
    }

    private static String contentAsString(SdkHttpFullRequest request) {
        return request.contentStreamProvider()
                      .map(ContentStreamProvider::newStream)
                      .map(s -> SdkBytes.fromInputStream(s).asString(StandardCharsets.UTF_8))
                      .orElse(null);
    }
}
