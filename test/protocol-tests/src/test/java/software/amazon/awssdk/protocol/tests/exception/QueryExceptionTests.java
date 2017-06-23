/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryClient;
import software.amazon.awssdk.services.protocolquery.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolquery.model.EmptyModeledException;
import software.amazon.awssdk.services.protocolquery.model.ProtocolQueryException;

public class QueryExceptionTests {

    private static final String PATH = "/";

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolQueryClient client;

    @Before
    public void setupClient() {
        client = ProtocolQueryClient.builder()
                                    .credentialsProvider(new StaticCredentialsProvider(new AwsCredentials("akid", "skid")))
                                    .region(Region.US_EAST_1)
                                    .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                    .build();
    }

    @Test
    public void unmodeledException_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>UnmodeledException</Code></Error></ErrorResponse>");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void unmodeledException_ErrorCodeSetOnAse() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>UnmodeledException</Code></Error></ErrorResponse>");
        final AmazonServiceException ase = captureAse(this::callAllTypes);
        assertEquals("UnmodeledException", ase.getErrorCode());
    }

    @Test
    public void unmodeledExceptionWithMessage_MessageSetOnAse() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>UnmodeledException</Code><Message>Something happened</Message></Error></ErrorResponse>");
        final AmazonServiceException ase = captureAse(this::callAllTypes);
        assertEquals("Something happened", ase.getErrorMessage());
    }

    @Test
    public void unmodeledException_StatusCodeSetOnAse() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>UnmodeledException</Code></Error></ErrorResponse>");
        final AmazonServiceException ase = captureAse(this::callAllTypes);
        assertEquals(404, ase.getStatusCode());
    }

    @Test
    public void modeledException_UnmarshalledIntoModeledException() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>EmptyModeledException</Code></Error></ErrorResponse>");
        try {
            callAllTypes();
        } catch (EmptyModeledException e) {
            assertThat(e, instanceOf(ProtocolQueryException.class));
            assertEquals("EmptyModeledException", e.getErrorCode());
        }
    }

    @Test
    public void modeledExceptionWithMessage_MessageSetOnAse() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>EmptyModeledException</Code><Message>Something happened</Message></Error></ErrorResponse>");
        final EmptyModeledException ase = captureModeledException(this::callAllTypes);
        assertEquals("Something happened", ase.getErrorMessage());
    }

    @Test
    public void modeledException_ErrorCodeSetOnAse() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>EmptyModeledException</Code></Error></ErrorResponse>");
        final EmptyModeledException ase = captureModeledException(this::callAllTypes);
        assertEquals("EmptyModeledException", ase.getErrorCode());
    }

    @Test
    public void modeledException_StatusCodeSetOnAse() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>EmptyModeledException</Code></Error></ErrorResponse>");
        final EmptyModeledException ase = captureModeledException(this::callAllTypes);
        assertEquals(404, ase.getStatusCode());
    }

    @Test
    public void emptyErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH, "");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void malformedErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH, "THIS ISN'T XML");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void errorTypeSender_UnmarshallsIntoClientErrorType() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>UnmodeledException</Code><Type>Sender</Type></Error></ErrorResponse>");
        final AmazonServiceException ase = captureAse(this::callAllTypes);
        assertEquals(AmazonServiceException.ErrorType.Client, ase.getErrorType());
    }

    @Test
    public void errorTypeReceiver_UnmarshallsIntoServiceErrorType() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>UnmodeledException</Code><Type>Receiver</Type></Error></ErrorResponse>");
        final AmazonServiceException ase = captureAse(this::callAllTypes);
        assertEquals(AmazonServiceException.ErrorType.Service, ase.getErrorType());
    }

    @Test
    public void noErrorTypeInXml_UnmarshallsIntoUnknownErrorType() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>UnmodeledException</Code></Error></ErrorResponse>");
        final AmazonServiceException ase = captureAse(this::callAllTypes);
        assertEquals(AmazonServiceException.ErrorType.Unknown, ase.getErrorType());
    }

    @Test
    public void emptyErrorResponse_UnmarshallsIntoUnknownErrorType() {
        stub404Response(PATH, "");
        final AmazonServiceException ase = captureAse(this::callAllTypes);
        assertEquals(AmazonServiceException.ErrorType.Unknown, ase.getErrorType());
        assertEquals("404 Not Found", ase.getErrorCode());
        assertEquals(404, ase.getStatusCode());
    }

    @Test
    public void malformedErrorResponse_UnmarshallsIntoUnknownErrorType() {
        stub404Response(PATH, "THIS ISN'T XML");
        final AmazonServiceException ase = captureAse(this::callAllTypes);
        assertEquals(AmazonServiceException.ErrorType.Unknown, ase.getErrorType());
        assertEquals("404 Not Found", ase.getErrorCode());
        assertEquals(404, ase.getStatusCode());
    }

    private void callAllTypes() {
        client.allTypes(AllTypesRequest.builder().build());
    }

    private void assertThrowsServiceBaseException(Runnable runnable) {
        try {
            runnable.run();
        } catch (ProtocolQueryException e) {
            assertEquals(ProtocolQueryException.class, e.getClass());
        }
    }

    private AmazonServiceException captureAse(Runnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (AmazonServiceException ase) {
            return ase;
        }
    }

    private EmptyModeledException captureModeledException(Runnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (EmptyModeledException ase) {
            return ase;
        }
    }
}
