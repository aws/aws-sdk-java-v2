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

package software.amazon.awssdk.services.protocolrestjson;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlAsyncClient;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlClient;

@WireMockTest
public class StringPayloadUnmarshallingTest {
    private static final String TEST_PAYLOAD = "X";

    private static List<Arguments> testParameters() {
        List<Arguments> testCases = new ArrayList<>();
        for (ClientType clientType : ClientType.values()) {
            for (Protocol protocol : Protocol.values()) {
                for (StringLocation value : StringLocation.values()) {
                    for (ContentLength contentLength : ContentLength.values()) {
                        testCases.add(Arguments.arguments(clientType, protocol, value, contentLength));
                    }
                }
            }
        }
        return testCases;
    }

    private enum ClientType {
        SYNC,
        ASYNC
    }

    private enum Protocol {
        JSON,
        XML
    }

    private enum StringLocation {
        PAYLOAD,
        FIELD
    }

    private enum ContentLength {
        ZERO,
        NOT_PRESENT
    }

    @ParameterizedTest
    @MethodSource("testParameters")
    public void missingStringPayload_unmarshalledCorrectly(ClientType clientType,
                                                           Protocol protocol,
                                                           StringLocation stringLoc,
                                                           ContentLength contentLength,
                                                           WireMockRuntimeInfo wm) {
        if (contentLength == ContentLength.ZERO) {
            stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("Content-Length", "0").withBody("")));
        } else if (contentLength == ContentLength.NOT_PRESENT) {
            stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("")));
        }

        String serviceResult = callService(wm, clientType, protocol, stringLoc);

        if (stringLoc == StringLocation.PAYLOAD) {
            assertThat(serviceResult).isNotNull().isEqualTo("");
        } else if (stringLoc == StringLocation.FIELD) {
            assertThat(serviceResult).isNull();
        }
    }

    @ParameterizedTest
    @MethodSource("testParameters")
    public void presentStringPayload_unmarshalledCorrectly(ClientType clientType,
                                                           Protocol protocol,
                                                           StringLocation stringLoc,
                                                           ContentLength contentLength,
                                                           WireMockRuntimeInfo wm) {
        String responsePayload = presentStringResponse(protocol, stringLoc);

        if (contentLength == ContentLength.ZERO) {
            stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)
                                                         .withHeader("Content-Length", Integer.toString(responsePayload.length()))
                                                         .withBody(responsePayload)));
        } else if (contentLength == ContentLength.NOT_PRESENT) {
            stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody(responsePayload)));
        }

        assertThat(callService(wm, clientType, protocol, stringLoc)).isEqualTo(TEST_PAYLOAD);
    }

    private String presentStringResponse(Protocol protocol, StringLocation stringLoc) {
        switch (stringLoc) {
            case PAYLOAD: return TEST_PAYLOAD;
            case FIELD:
                switch (protocol) {
                    case JSON: return "{\"StringMember\": \"X\"}";
                    case XML: return "<AllTypes><StringMember>X</StringMember></AllTypes>";
                    default: throw new UnsupportedOperationException();
                }
            default: throw new UnsupportedOperationException();
        }

    }

    private String callService(WireMockRuntimeInfo wm, ClientType clientType, Protocol protocol, StringLocation stringLoc) {
        switch (clientType) {
            case SYNC: return syncCallService(wm, protocol, stringLoc);
            case ASYNC: return asyncCallService(wm, protocol, stringLoc);
            default: throw new UnsupportedOperationException();
        }
    }

    private String syncCallService(WireMockRuntimeInfo wm, Protocol protocol, StringLocation stringLoc) {
        switch (protocol) {
            case JSON: return syncJsonCallService(wm, stringLoc);
            case XML: return syncXmlCallService(wm, stringLoc);
            default: throw new UnsupportedOperationException();
        }
    }

    private String asyncCallService(WireMockRuntimeInfo wm, Protocol protocol, StringLocation stringLoc) {
        switch (protocol) {
            case JSON: return asyncJsonCallService(wm, stringLoc);
            case XML: return asyncXmlCallService(wm, stringLoc);
            default: throw new UnsupportedOperationException();
        }
    }

    private String syncJsonCallService(WireMockRuntimeInfo wm, StringLocation stringLoc) {
        ProtocolRestJsonClient client =
            ProtocolRestJsonClient.builder()
                                  .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                  .region(Region.US_EAST_1)
                                  .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                  .build();
        switch (stringLoc) {
            case PAYLOAD: return client.operationWithExplicitPayloadString(r -> {}).payloadMember();
            case FIELD: return client.allTypes(r -> {}).stringMember();
            default: throw new UnsupportedOperationException();
        }
    }

    private String asyncJsonCallService(WireMockRuntimeInfo wm, StringLocation stringLoc) {
        ProtocolRestJsonAsyncClient client =
            ProtocolRestJsonAsyncClient.builder()
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                       .build();

        switch (stringLoc) {
            case PAYLOAD: return client.operationWithExplicitPayloadString(r -> {}).join().payloadMember();
            case FIELD: return client.allTypes(r -> {}).join().stringMember();
            default: throw new UnsupportedOperationException();
        }
    }

    private String syncXmlCallService(WireMockRuntimeInfo wm, StringLocation stringLoc) {
        ProtocolRestXmlClient client =
            ProtocolRestXmlClient.builder()
                                 .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                 .region(Region.US_EAST_1)
                                 .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                 .build();
        switch (stringLoc) {
            case PAYLOAD: return client.operationWithExplicitPayloadString(r -> {}).payloadMember();
            case FIELD: return client.allTypes(r -> {}).stringMember();
            default: throw new UnsupportedOperationException();
        }
    }

    private String asyncXmlCallService(WireMockRuntimeInfo wm, StringLocation stringLoc) {
        ProtocolRestXmlAsyncClient client =
            ProtocolRestXmlAsyncClient.builder()
                                      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                      .region(Region.US_EAST_1)
                                      .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                      .build();

        switch (stringLoc) {
            case PAYLOAD: return client.operationWithExplicitPayloadString(r -> {}).join().payloadMember();
            case FIELD: return client.allTypes(r -> {}).join().stringMember();
            default: throw new UnsupportedOperationException();
        }
    }
}
