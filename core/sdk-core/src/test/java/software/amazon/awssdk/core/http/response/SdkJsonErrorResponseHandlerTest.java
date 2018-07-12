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

package software.amazon.awssdk.core.http.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonFactory;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.http.SdkJsonErrorResponseHandler;
import software.amazon.awssdk.core.internal.protocol.json.SdkJsonErrorUnmarshaller;
import software.amazon.awssdk.core.protocol.json.SdkJsonErrorMessageParser;
import software.amazon.awssdk.utils.StringInputStream;
import utils.ValidSdkObjects;

public class SdkJsonErrorResponseHandlerTest {

    private static final String SERVICE_NAME = "someService";
    private static final String ERROR_MESSAGE = "error";
    private SdkJsonErrorResponseHandler responseHandler;
    private HttpResponse httpResponse;

    @Mock
    private SdkJsonErrorMessageParser errorMessageParser;

    @Mock
    private SdkJsonErrorUnmarshaller unmarshaller;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(errorMessageParser.parseErrorMessage(any(), any())).thenReturn(ERROR_MESSAGE);

        httpResponse = new HttpResponse(ValidSdkObjects.sdkHttpFullRequest().build());
        httpResponse.setContent(new StringInputStream("{}"));

        responseHandler = new SdkJsonErrorResponseHandler(Collections.singletonList(unmarshaller),
                                                          new JsonFactory());
    }

    @Test
    public void handle_NoUnmarshallersAdded_ReturnsGenericSdkServiceException() throws
                                                                                   Exception {
        responseHandler = new SdkJsonErrorResponseHandler(new ArrayList<>(),
                                                          new JsonFactory());

        SdkServiceException exception = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertNotNull(exception);
    }

    @Test
    public void handle_NoMatchingUnmarshallers_ReturnsGenericSdkServiceException() throws
                                                                                      Exception {
        expectUnmarshallerDoesNotMatch();

        SdkServiceException exception = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertNotNull(exception);
    }

    @Test
    public void handle_NullContent_ReturnsGenericSdkServiceException() throws Exception {
        httpResponse.setStatusCode(500);
        httpResponse.setContent(null);

        SdkServiceException exception = responseHandler.handle(httpResponse, null);

        // We assert these common properties are set again to make sure that code path is exercised
        // for unknown SdkServiceExceptions as well
        assertEquals(500, exception.statusCode());
    }

    @Test
    public void handle_EmptyContent_ReturnsGenericSdkServiceException() throws Exception {
        httpResponse.setStatusCode(500);
        httpResponse.setContent(new StringInputStream(""));

        SdkServiceException exception = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertNotNull(exception);
    }

    @Test
    public void handle_UnmarshallerReturnsNull_ReturnsGenericSdkServiceException() throws
                                                                                      Exception {
        expectUnmarshallerMatches();

        SdkServiceException exception = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertNotNull(exception);
    }

    @Test
    public void handle_UnmarshallerThrowsException_ReturnsGenericSdkServiceException() throws
                                                                                          Exception {
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject())).thenThrow(new RuntimeException());

        SdkServiceException exception = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertNotNull(exception);
    }

    @Test
    public void handle_UnmarshallerReturnsException_ClientErrorType() throws Exception {
        httpResponse.setStatusCode(400);
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject()))
                .thenReturn(SdkServiceException.builder().build());

        ExecutionAttributes attributes =
                new ExecutionAttributes().putAttribute(SdkExecutionAttribute.SERVICE_NAME, SERVICE_NAME);
        SdkServiceException exception = responseHandler.handle(httpResponse, attributes);

        assertEquals(400, exception.statusCode());
    }

    @Test
    public void handle_UnmarshallerReturnsException_WithRequestId() throws Exception {
        httpResponse.setStatusCode(500);
        httpResponse.addHeader(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER, "1234");
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject()))
                .thenReturn(SdkServiceException.builder().build());

        SdkServiceException exception = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertEquals("1234", exception.requestId());
    }

    private void expectUnmarshallerMatches() {
        when(unmarshaller.matches(anyInt())).thenReturn(true);
    }

    private void expectUnmarshallerDoesNotMatch() {
        when(unmarshaller.matches(anyInt())).thenReturn(false);
    }
}
