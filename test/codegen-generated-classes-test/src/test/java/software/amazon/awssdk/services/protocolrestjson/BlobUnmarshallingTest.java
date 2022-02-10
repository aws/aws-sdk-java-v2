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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryAsyncClient;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryClient;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlAsyncClient;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlClient;

/**
 * This verifies that blob types are unmarshalled correctly depending on where they exist. Specifically, we currently unmarshall
 * SdkBytes fields bound to the payload as empty if the service responds with no data and SdkBytes fields bound to a field as
 * null if the service does not specify that field.
 */
@WireMockTest
public class BlobUnmarshallingTest {
    private static List<Arguments> testParameters() {
        List<Arguments> testCases = new ArrayList<>();
        for (ClientType clientType : ClientType.values()) {
            for (Protocol protocol : Protocol.values()) {
                for (SdkBytesLocation value : SdkBytesLocation.values()) {
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
        XML,
        QUERY
    }

    private enum SdkBytesLocation {
        PAYLOAD,
        FIELD
    }

    private enum ContentLength {
        ZERO,
        CHUNKED_ZERO
    }

    @ParameterizedTest
    @MethodSource("testParameters")
    public void missingSdkBytes_unmarshalledCorrectly(ClientType clientType,
                                                      Protocol protocol,
                                                      SdkBytesLocation bytesLoc,
                                                      ContentLength contentLength,
                                                      WireMockRuntimeInfo wm) {
        if (contentLength == ContentLength.ZERO) {
            stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("Content-Length", "0").withBody("")));
        } else if (contentLength == ContentLength.CHUNKED_ZERO) {
            stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("")));
        }

        SdkBytes serviceResult = callService(wm, clientType, protocol, bytesLoc);

        if (bytesLoc == SdkBytesLocation.PAYLOAD) {
            assertThat(serviceResult).isNotNull().isEqualTo(SdkBytes.fromUtf8String(""));
        } else if (bytesLoc == SdkBytesLocation.FIELD) {
            assertThat(serviceResult).isNull();
        }
    }

    @ParameterizedTest
    @MethodSource("testParameters")
    public void presentSdkBytes_unmarshalledCorrectly(ClientType clientType,
                                                      Protocol protocol,
                                                      SdkBytesLocation bytesLoc,
                                                      ContentLength contentLength,
                                                      WireMockRuntimeInfo wm) {
        String responsePayload = presentSdkBytesResponse(protocol, bytesLoc);

        if (contentLength == ContentLength.ZERO) {
            stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)
                                                         .withHeader("Content-Length", Integer.toString(responsePayload.length()))
                                                         .withBody(responsePayload)));
        } else if (contentLength == ContentLength.CHUNKED_ZERO) {
            stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody(responsePayload)));
        }

        assertThat(callService(wm, clientType, protocol, bytesLoc)).isEqualTo(SdkBytes.fromUtf8String("X"));
    }

    private String presentSdkBytesResponse(Protocol protocol, SdkBytesLocation bytesLoc) {
        switch (bytesLoc) {
            case PAYLOAD: return "X";
            case FIELD:
                switch (protocol) {
                    case JSON: return "{\"BlobArg\": \"WA==\"}";
                    case XML: return "<AllTypes><BlobArg>WA==</BlobArg></AllTypes>";
                    case QUERY: return "<AllTypesResponse><AllTypes><BlobArg>WA==</BlobArg></AllTypes></AllTypesResponse>";
                    default: throw new UnsupportedOperationException();
                }
            default: throw new UnsupportedOperationException();
        }

    }

    private SdkBytes callService(WireMockRuntimeInfo wm, ClientType clientType, Protocol protocol, SdkBytesLocation bytesLoc) {
        switch (clientType) {
            case SYNC: return syncCallService(wm, protocol, bytesLoc);
            case ASYNC: return asyncCallService(wm, protocol, bytesLoc);
            default: throw new UnsupportedOperationException();
        }
    }

    private SdkBytes syncCallService(WireMockRuntimeInfo wm, Protocol protocol, SdkBytesLocation bytesLoc) {
        switch (protocol) {
            case JSON: return syncJsonCallService(wm, bytesLoc);
            case XML: return syncXmlCallService(wm, bytesLoc);
            case QUERY: return syncQueryCallService(wm, bytesLoc);
            default: throw new UnsupportedOperationException();
        }
    }

    private SdkBytes asyncCallService(WireMockRuntimeInfo wm, Protocol protocol, SdkBytesLocation bytesLoc) {
        switch (protocol) {
            case JSON: return asyncJsonCallService(wm, bytesLoc);
            case XML: return asyncXmlCallService(wm, bytesLoc);
            case QUERY: return asyncQueryCallService(wm, bytesLoc);
            default: throw new UnsupportedOperationException();
        }
    }

    private SdkBytes asyncQueryCallService(WireMockRuntimeInfo wm, SdkBytesLocation bytesLoc) {
        ProtocolQueryAsyncClient client =
            ProtocolQueryAsyncClient.builder()
                                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                    .region(Region.US_EAST_1)
                                    .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                    .build();
        switch (bytesLoc) {
            case PAYLOAD: return client.operationWithExplicitPayloadBlob(r -> {}).join().payloadMember();
            case FIELD: return client.allTypes(r -> {}).join().blobArg();
            default: throw new UnsupportedOperationException();
        }
    }

    private SdkBytes asyncXmlCallService(WireMockRuntimeInfo wm, SdkBytesLocation bytesLoc) {
        ProtocolRestXmlAsyncClient client =
            ProtocolRestXmlAsyncClient.builder()
                                      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                      .region(Region.US_EAST_1)
                                      .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                      .build();
        switch (bytesLoc) {
            case PAYLOAD: return client.operationWithExplicitPayloadBlob(r -> {}).join().payloadMember();
            case FIELD: return client.allTypes(r -> {}).join().blobArg();
            default: throw new UnsupportedOperationException();
        }
    }

    private SdkBytes asyncJsonCallService(WireMockRuntimeInfo wm, SdkBytesLocation bytesLoc) {
        ProtocolRestJsonAsyncClient client =
            ProtocolRestJsonAsyncClient.builder()
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                       .build();
        switch (bytesLoc) {
            case PAYLOAD: return client.operationWithExplicitPayloadBlob(r -> {}).join().payloadMember();
            case FIELD: return client.allTypes(r -> {}).join().blobArg();
            default: throw new UnsupportedOperationException();
        }
    }

    private SdkBytes syncQueryCallService(WireMockRuntimeInfo wm, SdkBytesLocation bytesLoc) {
        ProtocolQueryClient client =
            ProtocolQueryClient.builder()
                               .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                               .region(Region.US_EAST_1)
                               .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                               .build();
        switch (bytesLoc) {
            case PAYLOAD: return client.operationWithExplicitPayloadBlob(r -> {}).payloadMember();
            case FIELD: return client.allTypes(r -> {}).blobArg();
            default: throw new UnsupportedOperationException();
        }
    }

    private SdkBytes syncXmlCallService(WireMockRuntimeInfo wm, SdkBytesLocation bytesLoc) {
        ProtocolRestXmlClient client =
            ProtocolRestXmlClient.builder()
                                 .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                 .region(Region.US_EAST_1)
                                 .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                 .build();
        switch (bytesLoc) {
            case PAYLOAD: return client.operationWithExplicitPayloadBlob(r -> {}).payloadMember();
            case FIELD: return client.allTypes(r -> {}).blobArg();
            default: throw new UnsupportedOperationException();
        }
    }

    private SdkBytes syncJsonCallService(WireMockRuntimeInfo wm, SdkBytesLocation bytesLoc) {
        ProtocolRestJsonClient client =
            ProtocolRestJsonClient.builder()
                                  .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                  .region(Region.US_EAST_1)
                                  .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                  .build();
        switch (bytesLoc) {
            case PAYLOAD: return client.operationWithExplicitPayloadBlob(r -> {}).payloadMember();
            case FIELD: return client.allTypes(r -> {}).blobArg();
            default: throw new UnsupportedOperationException();
        }
    }
}
