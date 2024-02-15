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

package software.amazon.awssdk.protocol.tests.contentlength;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.OperationWithExplicitPayloadStructureResponse;
import software.amazon.awssdk.services.protocolrestjson.model.SimpleStruct;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlClient;
import software.amazon.awssdk.services.protocolrestxml.model.OperationWithExplicitPayloadStringResponse;

public class MarshallersAddContentLengthTest {
    public static final String STRING_PAYLOAD = "TEST_STRING_PAYLOAD";
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    @Test
    public void jsonMarshallers_AddContentLength_for_explicitBinaryPayload() {
        stubSuccessfulResponse();
        CaptureRequestInterceptor captureRequestInterceptor = new CaptureRequestInterceptor();
        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                              .httpClient(AwsCrtHttpClient.builder().build())
                                                              .overrideConfiguration(o -> o.addExecutionInterceptor(captureRequestInterceptor))
                                                              .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                              .build();

        client.operationWithExplicitPayloadBlob(p -> p.payloadMember(SdkBytes.fromString(STRING_PAYLOAD,
                                                                                         StandardCharsets.UTF_8)));
        verify(postRequestedFor(anyUrl()).withHeader(CONTENT_LENGTH, equalTo(String.valueOf(STRING_PAYLOAD.length()))));
        assertThat(captureRequestInterceptor.requestAfterMarshalling().firstMatchingHeader(CONTENT_LENGTH))
            .contains(String.valueOf(STRING_PAYLOAD.length()));

    }

    @Test
    public void jsonMarshallers_AddContentLength_for_explicitStringPayload() {
        stubSuccessfulResponse();
        String expectedPayload = String.format("{\"StringMember\":\"%s\"}", STRING_PAYLOAD);
        CaptureRequestInterceptor captureRequestInterceptor = new CaptureRequestInterceptor();
        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                              .httpClient(AwsCrtHttpClient.builder().build())
                                                              .overrideConfiguration(o -> o.addExecutionInterceptor(captureRequestInterceptor))
                                                              .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                              .build();

        OperationWithExplicitPayloadStructureResponse response =
            client.operationWithExplicitPayloadStructure(p -> p.payloadMember(SimpleStruct.builder().stringMember(STRING_PAYLOAD).build()));
        verify(postRequestedFor(anyUrl())
                   .withRequestBody(equalTo(expectedPayload))
                   .withHeader(CONTENT_LENGTH, equalTo(String.valueOf(expectedPayload.length()))));
        assertThat(captureRequestInterceptor.requestAfterMarshalling().firstMatchingHeader(CONTENT_LENGTH))
            .contains(String.valueOf(expectedPayload.length()));

    }

    @Test
    public void xmlMarshallers_AddContentLength_for_explicitBinaryPayload() {
        stubSuccessfulResponse();
        CaptureRequestInterceptor captureRequestInterceptor = new CaptureRequestInterceptor();
        ProtocolRestXmlClient client = ProtocolRestXmlClient.builder()
                                                            .httpClient(AwsCrtHttpClient.builder().build())
                                                            .overrideConfiguration(o -> o.addExecutionInterceptor(captureRequestInterceptor))
                                                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                            .build();
        client.operationWithExplicitPayloadBlob(r -> r.payloadMember(SdkBytes.fromString(STRING_PAYLOAD,
                                                                                         StandardCharsets.UTF_8)));

        verify(postRequestedFor(anyUrl())
                   .withRequestBody(equalTo(STRING_PAYLOAD))

                   .withHeader(CONTENT_LENGTH, equalTo(String.valueOf(STRING_PAYLOAD.length()))));
        assertThat(captureRequestInterceptor.requestAfterMarshalling().firstMatchingHeader(CONTENT_LENGTH))
            .contains(String.valueOf(STRING_PAYLOAD.length()));
    }

    @Test
    public void xmlMarshallers_AddContentLength_for_explicitStringPayload() {
        stubSuccessfulResponse();
        String expectedPayload = STRING_PAYLOAD;
        CaptureRequestInterceptor captureRequestInterceptor = new CaptureRequestInterceptor();
        ProtocolRestXmlClient client = ProtocolRestXmlClient.builder()
                                                            .httpClient(AwsCrtHttpClient.builder().build())
                                                            .overrideConfiguration(o -> o.addExecutionInterceptor(captureRequestInterceptor))
                                                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                            .build();
        OperationWithExplicitPayloadStringResponse stringResponse =
            client.operationWithExplicitPayloadString(p -> p.payloadMember(STRING_PAYLOAD));
        verify(postRequestedFor(anyUrl())
                   .withRequestBody(equalTo(expectedPayload))
                   .withHeader(CONTENT_LENGTH, equalTo(String.valueOf(expectedPayload.length()))));
        assertThat(captureRequestInterceptor.requestAfterMarshalling().firstMatchingHeader(CONTENT_LENGTH))
            .contains(String.valueOf(expectedPayload.length()));

    }

    private void stubSuccessfulResponse() {
        stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));
    }

    private static class CaptureRequestInterceptor implements ExecutionInterceptor {
        private SdkHttpRequest requestAfterMarshilling;

        public SdkHttpRequest requestAfterMarshalling() {
            return requestAfterMarshilling;
        }

        @Override
        public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
            this.requestAfterMarshilling = context.httpRequest();
        }

    }
}
