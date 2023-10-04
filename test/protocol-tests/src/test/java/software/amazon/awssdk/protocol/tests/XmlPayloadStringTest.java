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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlClient;
import software.amazon.awssdk.services.protocolrestxml.model.OperationWithExplicitPayloadBlobRequest;
import software.amazon.awssdk.services.protocolrestxml.model.OperationWithExplicitPayloadBlobResponse;
import software.amazon.awssdk.services.protocolrestxml.model.OperationWithExplicitPayloadStringRequest;
import software.amazon.awssdk.services.protocolrestxml.model.OperationWithExplicitPayloadStringResponse;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

public class XmlPayloadStringTest {
    private final String PAYLOAD_STRING_XML = "<Field>StringPayload</Field>";
    private final String PAYLOAD_STRING_NON_XML = "StringPayload";
    private final SdkBytes PAYLOAD_BYTES_XML = SdkBytes.fromUtf8String(PAYLOAD_STRING_XML);
    private final SdkBytes PAYLOAD_BYTES_NON_XML = SdkBytes.fromUtf8String(PAYLOAD_STRING_NON_XML);
    private ProtocolRestXmlClient syncClient;
    private MockSyncHttpClient mockHttpClient;

    @BeforeEach
    public void setUp() {
        mockHttpClient = new MockSyncHttpClient();
        syncClient = ProtocolRestXmlClient.builder()
                                           .credentialsProvider(AnonymousCredentialsProvider.create())
                                           .region(Region.US_EAST_1)
                                           .httpClient(mockHttpClient)
                                           .build();
    }

    @AfterEach
    public void reset() {
        mockHttpClient.reset();
    }

    @Test
    public void operationWithExplicitPayloadString_xml_marshallsAndUnmarshallsCorrectly() {
        mockHttpClient.stubNextResponse(mockResponse(PAYLOAD_BYTES_XML), Duration.ofMillis(500));

        OperationWithExplicitPayloadStringRequest request = OperationWithExplicitPayloadStringRequest.builder()
                                                                                                     .payloadMember(PAYLOAD_STRING_XML)
                                                                                                     .build();
        OperationWithExplicitPayloadStringResponse response = syncClient.operationWithExplicitPayloadString(request);

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        InputStream loggedRequestStream = loggedRequest.contentStreamProvider().get().newStream();
        String loggedRequestBody = new String(SdkBytes.fromInputStream(loggedRequestStream).asByteArray());

        assertThat(loggedRequestBody).isEqualTo(PAYLOAD_STRING_XML);
        // Explicit XML String payload should be unmarshalled to raw value without tags in response POJO
        assertThat(response.payloadMember()).isEqualTo(PAYLOAD_STRING_NON_XML);
    }

    @Test
    public void operationWithExplicitPayloadString_nonXml_marshallsAndUnmarshallsCorrectly() {
        mockHttpClient.stubNextResponse(mockResponse(PAYLOAD_BYTES_NON_XML), Duration.ofMillis(500));

        OperationWithExplicitPayloadStringRequest request = OperationWithExplicitPayloadStringRequest.builder()
                                                                                                     .payloadMember(PAYLOAD_STRING_NON_XML)
                                                                                                     .build();
        OperationWithExplicitPayloadStringResponse response = syncClient.operationWithExplicitPayloadString(request);

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        InputStream loggedRequestStream = loggedRequest.contentStreamProvider().get().newStream();
        String loggedRequestBody = new String(SdkBytes.fromInputStream(loggedRequestStream).asByteArray());

        assertThat(loggedRequestBody).isEqualTo(PAYLOAD_STRING_NON_XML);
        assertThat(response.payloadMember()).isEqualTo(PAYLOAD_STRING_NON_XML);
    }

    @Test
    public void operationWithExplicitPayloadBlob_xml_marshallsAndUnmarshallsCorrectly() {
        mockHttpClient.stubNextResponse(mockResponse(PAYLOAD_BYTES_XML), Duration.ofMillis(500));

        OperationWithExplicitPayloadBlobRequest request = OperationWithExplicitPayloadBlobRequest.builder()
                                                                                                 .payloadMember(PAYLOAD_BYTES_XML)
                                                                                                 .build();
        OperationWithExplicitPayloadBlobResponse response = syncClient.operationWithExplicitPayloadBlob(request);

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        InputStream loggedRequestStream = loggedRequest.contentStreamProvider().get().newStream();
        SdkBytes loggedRequestBody = SdkBytes.fromInputStream(loggedRequestStream);

        assertThat(loggedRequestBody).isEqualTo(PAYLOAD_BYTES_XML);
        assertThat(response.payloadMember()).isEqualTo(PAYLOAD_BYTES_XML);
    }

    @Test
    public void operationWithExplicitPayloadBlob_nonXml_marshallsAndUnmarshallsCorrectly() {
        mockHttpClient.stubNextResponse(mockResponse(PAYLOAD_BYTES_NON_XML), Duration.ofMillis(500));

        OperationWithExplicitPayloadBlobRequest request = OperationWithExplicitPayloadBlobRequest.builder()
                                                                                                 .payloadMember(PAYLOAD_BYTES_NON_XML)
                                                                                                 .build();
        OperationWithExplicitPayloadBlobResponse response = syncClient.operationWithExplicitPayloadBlob(request);

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        InputStream loggedRequestStream = loggedRequest.contentStreamProvider().get().newStream();
        SdkBytes loggedRequestBody = SdkBytes.fromInputStream(loggedRequestStream);

        assertThat(loggedRequestBody).isEqualTo(PAYLOAD_BYTES_NON_XML);
        assertThat(response.payloadMember()).isEqualTo(PAYLOAD_BYTES_NON_XML);
    }

    private HttpExecuteResponse mockResponse(SdkBytes sdkBytes) {
        InputStream inputStream = new ByteArrayInputStream(sdkBytes.asByteArray());

        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(200)
                                                           .build())
                                  .responseBody(AbortableInputStream.create(inputStream))
                                  .build();
    }
}
