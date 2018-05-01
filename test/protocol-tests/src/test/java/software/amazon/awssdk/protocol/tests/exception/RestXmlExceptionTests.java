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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static util.exception.ExceptionTestUtils.stub404Response;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlClient;
import software.amazon.awssdk.services.protocolrestxml.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestxml.model.EmptyModeledException;
import software.amazon.awssdk.services.protocolrestxml.model.MultiLocationOperationRequest;
import software.amazon.awssdk.services.protocolrestxml.model.ProtocolRestXmlException;

public class RestXmlExceptionTests {

    private static final String ALL_TYPES_PATH = "/2016-03-11/allTypes";

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolRestXmlClient client;

    @Before
    public void setupClient() {
        client = ProtocolRestXmlClient.builder()
                                  .credentialsProvider(StaticCredentialsProvider.create(AwsCredentials.create("akid", "skid")))
                                  .region(Region.US_EAST_1)
                                  .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                  .build();
    }

    @Test
    public void unmodeledException_UnmarshalledIntoBaseServiceException() {
        stub404Response(ALL_TYPES_PATH,
                        "<ErrorResponse><Error><Code>UnmodeledException</Code></Error></ErrorResponse>");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void modeledException_UnmarshalledIntoModeledException() {
        stub404Response(ALL_TYPES_PATH,
                        "<ErrorResponse><Error><Code>EmptyModeledException</Code></Error></ErrorResponse>");
        try {
            callAllTypes();
        } catch (EmptyModeledException e) {
            assertThat(e, instanceOf(ProtocolRestXmlException.class));
        }
    }

    @Test
    public void emptyErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(ALL_TYPES_PATH, "");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void malformedErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(ALL_TYPES_PATH, "THIS ISN'T XML");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void illegalArgumentException_nullPathParam() {
        assertThrowsIllegalArgumentException(() -> client.multiLocationOperation(MultiLocationOperationRequest.builder().build()));
    }

    @Test
    public void illegalArgumentException_emptyPathParam() {
        assertThrowsIllegalArgumentException(() -> client.multiLocationOperation(
                MultiLocationOperationRequest.builder().pathParam("").build()));
    }

    private void callAllTypes() {
        client.allTypes(AllTypesRequest.builder().build());
    }

    private void assertThrowsServiceBaseException(Runnable runnable) {
        assertThrowsException(runnable, ProtocolRestXmlException.class);
    }

    private void assertThrowsIllegalArgumentException(Runnable runnable) {
        assertThrowsException(runnable, IllegalArgumentException.class);
    }

    private void assertThrowsException(Runnable runnable, Class<? extends Exception> expectedException) {
        try {
            runnable.run();
        } catch (Exception e) {
            assertEquals(expectedException, e.getClass());
        }
    }
}
