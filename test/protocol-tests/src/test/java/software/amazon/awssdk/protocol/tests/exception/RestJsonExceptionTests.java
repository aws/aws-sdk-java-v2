/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.protocol.tests.exception;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static util.exception.ExceptionTestUtils.stub404Response;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.EmptyModeledException;
import software.amazon.awssdk.services.protocolrestjson.model.HeadOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.MultiLocationOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;

/**
 * Exception related tests for AWS REST JSON.
 */
public class RestJsonExceptionTests {

    private static final String ALL_TYPES_PATH = "/2016-03-11/allTypes";

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolRestJsonClient client;

    @Before
    public void setupClient() {
        client = ProtocolRestJsonClient.builder()
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsCredentials.create("akid", "skid")))
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                       .build();
    }

    @Test
    public void unmodeledException_UnmarshalledIntoBaseServiceException() {
        stub404Response(ALL_TYPES_PATH, "{\"__type\": \"SomeUnknownType\"}");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void modeledException_UnmarshalledIntoModeledException() {
        stub404Response(ALL_TYPES_PATH, "{\"__type\": \"EmptyModeledException\"}");
        assertThrowsException(this::callAllTypes, EmptyModeledException.class);
    }

    @Test
    public void emptyErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(ALL_TYPES_PATH, "");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void malformedErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(ALL_TYPES_PATH, "THIS ISN'T JSON");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void modeledExceptionInHeadRequest_UnmarshalledIntoModeledException() {
        stubFor(head(urlEqualTo("/2016-03-11/headOperation"))
                        .willReturn(aResponse()
                                            .withStatus(404)
                                            .withHeader("x-amzn-ErrorType", "EmptyModeledException")));
        assertThrowsException(() -> client.headOperation(HeadOperationRequest.builder().build()), EmptyModeledException.class);
    }

    @Test
    public void unmodeledExceptionInHeadRequest_UnmarshalledIntoModeledException() {
        stubFor(head(urlEqualTo("/2016-03-11/headOperation"))
                        .willReturn(aResponse()
                                            .withStatus(404)
                                            .withHeader("x-amzn-ErrorType", "SomeUnknownType")));
        assertThrowsServiceBaseException(() -> client.headOperation(HeadOperationRequest.builder().build()));
    }

    @Test
    public void nullPathParam_ThrowsSdkClientException() {
        assertThrowsSdkClientException(() -> client.multiLocationOperation(MultiLocationOperationRequest.builder().build()));
    }

    @Test
    public void emptyPathParam_ThrowsSdkClientException() {
        assertThrowsSdkClientException(() -> client.multiLocationOperation(MultiLocationOperationRequest.builder().pathParam("").build()));
    }


    private void callAllTypes() {
        client.allTypes(AllTypesRequest.builder().build());
    }

    private void assertThrowsServiceBaseException(Runnable runnable) {
        assertThrowsException(runnable, ProtocolRestJsonException.class);
    }

    private void assertThrowsSdkClientException(Runnable runnable) {
        assertThrowsException(runnable, SdkClientException.class);
    }

    private void assertThrowsException(Runnable runnable, Class<? extends Exception> expectedException) {
        try {
            runnable.run();
        } catch (Exception e) {
            assertEquals(expectedException, e.getClass());
        }
    }
}
