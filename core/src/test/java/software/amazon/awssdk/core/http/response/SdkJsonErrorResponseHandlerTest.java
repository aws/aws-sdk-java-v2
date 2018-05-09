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

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
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
import software.amazon.awssdk.core.interceptor.SdkExecutionAttributes;
import software.amazon.awssdk.core.protocol.json.SdkJsonErrorUnmarshaller;
import software.amazon.awssdk.core.protocol.json.SdkJsonErrorMessageParser;
import software.amazon.awssdk.core.runtime.http.response.SdkJsonErrorResponseHandler;
import software.amazon.awssdk.core.util.StringInputStream;
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
                                                          SdkJsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER,
                                                          new JsonFactory());
    }

    @Test
    public void handle_NoUnmarshallersAdded_ReturnsGenericSdkServiceException() throws
                                                                                   Exception {
        responseHandler = new SdkJsonErrorResponseHandler(new ArrayList<>(),
                                                          SdkJsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER,
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

        ExecutionAttributes attributes =
                new ExecutionAttributes().putAttribute(SdkExecutionAttributes.SERVICE_NAME, SERVICE_NAME);
        SdkServiceException exception = responseHandler.handle(httpResponse, attributes);

        // We assert these common properties are set again to make sure that code path is exercised
        // for unknown SdkServiceExceptions as well
        assertEquals(500, exception.statusCode());
        assertEquals(SERVICE_NAME, exception.serviceName());
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
                .thenReturn(new CustomException(ERROR_MESSAGE));

        ExecutionAttributes attributes =
                new ExecutionAttributes().putAttribute(SdkExecutionAttributes.SERVICE_NAME, SERVICE_NAME);
        SdkServiceException exception = responseHandler.handle(httpResponse, attributes);

        assertEquals(ERROR_MESSAGE, exception.errorMessage());
        assertEquals(400, exception.statusCode());
        assertEquals(SERVICE_NAME, exception.serviceName());
    }

    @Test
    public void handle_UnmarshallerReturnsException_WithRequestId() throws Exception {
        httpResponse.setStatusCode(500);
        httpResponse.addHeader(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER, "1234");
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject()))
                .thenReturn(new CustomException(ERROR_MESSAGE));

        SdkServiceException exception = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertEquals("1234", exception.requestId());
    }

    /**
     * All headers (Including ones that populate other fields like request id) should be dumped into
     * the header map.
     */
    @Test
    public void handle_AllHeaders_DumpedIntoHeaderMap() throws Exception {
        httpResponse.setStatusCode(500);
        httpResponse.addHeader("FooHeader", "FooValue");
        httpResponse.addHeader(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER, "1234");
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject()))
                .thenReturn(new CustomException(ERROR_MESSAGE));

        SdkServiceException exception = responseHandler.handle(httpResponse, new ExecutionAttributes());
        assertThat(exception.headers(), hasEntry("FooHeader", "FooValue"));
        assertThat(exception.headers(),
                   hasEntry(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER, "1234"));
    }

    private void expectUnmarshallerMatches() {
        when(unmarshaller.matches(anyInt())).thenReturn(true);
    }

    private void expectUnmarshallerDoesNotMatch() {
        when(unmarshaller.matches(anyInt())).thenReturn(false);
    }

    private static class CustomException extends SdkServiceException {

        private static final long serialVersionUID = 1305027296023640779L;

        public CustomException(String errorMessage) {
            super(errorMessage);
        }
    }
}
