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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static util.exception.ExceptionTestUtils.stub404Response;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkServiceException;
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
                                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                    .region(Region.US_EAST_1)
                                    .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                    .build();
    }

    @Test
    public void unmodeledException_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>UnmodeledException</Code></Error></ErrorResponse>");
        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build()))
            .isExactlyInstanceOf(ProtocolQueryException.class);
    }

    @Test
    public void unmodeledException_ErrorCodeSetOnServiceException() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>UnmodeledException</Code></Error></ErrorResponse>");
        final AwsServiceException exception = captureServiceException(this::callAllTypes);
        assertEquals("UnmodeledException", exception.awsErrorDetails().errorCode());
    }

    @Test
    public void unmodeledExceptionWithMessage_MessageSetOnServiceException() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>UnmodeledException</Code><Message>Something happened</Message></Error></ErrorResponse>");
        final AwsServiceException exception = captureServiceException(this::callAllTypes);
        assertEquals("Something happened", exception.awsErrorDetails().errorMessage());
    }

    @Test
    public void unmodeledException_StatusCodeSetOnServiceException() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>UnmodeledException</Code></Error></ErrorResponse>");
        final SdkServiceException exception = captureServiceException(this::callAllTypes);
        assertEquals(404, exception.statusCode());
    }

    @Test
    public void modeledException_UnmarshalledIntoModeledException() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>EmptyModeledException</Code></Error></ErrorResponse>");
        try {
            callAllTypes();
        } catch (EmptyModeledException e) {
            assertThat(e, instanceOf(ProtocolQueryException.class));
            assertEquals("EmptyModeledException", e.awsErrorDetails().errorCode());
        }
    }

    @Test
    public void modeledExceptionWithMessage_MessageSetOnServiceExeption() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>EmptyModeledException</Code><Message>Something happened</Message></Error></ErrorResponse>");
        final EmptyModeledException exception = captureModeledException(this::callAllTypes);
        assertEquals("Something happened", exception.awsErrorDetails().errorMessage());
    }

    @Test
    public void modeledException_ErrorCodeSetOnServiceException() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>EmptyModeledException</Code></Error></ErrorResponse>");
        final EmptyModeledException exception = captureModeledException(this::callAllTypes);
        assertEquals("EmptyModeledException", exception.awsErrorDetails().errorCode());
    }

    @Test
    public void modeledException_StatusCodeSetOnServiceException() {
        stub404Response(PATH,
                "<ErrorResponse><Error><Code>EmptyModeledException</Code></Error></ErrorResponse>");
        final EmptyModeledException exception = captureModeledException(this::callAllTypes);
        assertEquals(404, exception.statusCode());
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
    public void emptyErrorResponse_UnmarshallsIntoUnknownErrorType() {
        stub404Response(PATH, "");
        final AwsServiceException exception = captureServiceException(this::callAllTypes);
        assertEquals("404 Not Found", exception.awsErrorDetails().errorCode());
        assertEquals(404, exception.statusCode());
    }

    @Test
    public void malformedErrorResponse_UnmarshallsIntoUnknownErrorType() {
        stub404Response(PATH, "THIS ISN'T XML");
        final AwsServiceException exception = captureServiceException(this::callAllTypes);
        assertEquals("404 Not Found", exception.awsErrorDetails().errorCode());
        assertEquals(404, exception.statusCode());
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

    private AwsServiceException captureServiceException(Runnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (AwsServiceException exception) {
            return exception;
        }
    }

    private EmptyModeledException captureModeledException(Runnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (EmptyModeledException exception) {
            return exception;
        }
    }
}
