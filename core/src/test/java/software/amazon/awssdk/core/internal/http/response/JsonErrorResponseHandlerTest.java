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

package software.amazon.awssdk.core.internal.http.response;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonFactory;
import java.io.UnsupportedEncodingException;
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
import software.amazon.awssdk.core.internal.http.JsonErrorCodeParser;
import software.amazon.awssdk.core.runtime.http.JsonErrorMessageParser;
import software.amazon.awssdk.core.runtime.transform.JsonErrorUnmarshaller;
import software.amazon.awssdk.core.util.StringInputStream;
import software.amazon.awssdk.utils.StringUtils;
import utils.ValidSdkObjects;

public class JsonErrorResponseHandlerTest {

    private static final String SERVICE_NAME = "someService";
    private static final String ERROR_CODE = "someErrorCode";
    private JsonErrorResponseHandler responseHandler;
    private HttpResponse httpResponse;

    @Mock
    private JsonErrorUnmarshaller unmarshaller;

    @Mock
    private JsonErrorCodeParser errorCodeParser;

    @Before
    public void setup() throws UnsupportedEncodingException {
        MockitoAnnotations.initMocks(this);
        when(errorCodeParser
                     .parseErrorCode(anyObject(), anyObject()))
                .thenReturn(ERROR_CODE);

        httpResponse = new HttpResponse(ValidSdkObjects.sdkHttpFullRequest().build());
        httpResponse.setContent(new StringInputStream("{}"));

        responseHandler = new JsonErrorResponseHandler(Collections.singletonList(unmarshaller), errorCodeParser,
                                                       JsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER,
                                                       new JsonFactory());
    }

    @Test
    public void handle_NoUnmarshallersAdded_ReturnsGenericSdkServiceException() throws
                                                                                   Exception {
        responseHandler = new JsonErrorResponseHandler(new ArrayList<>(),
                                                       new JsonErrorCodeParser(),
                                                       JsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER,
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
        assertEquals(ERROR_CODE, exception.errorCode());
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
        assertEquals(ERROR_CODE, exception.errorCode());
    }

    @Test
    public void handle_UnmarshallerThrowsException_ReturnsGenericSdkServiceException() throws
                                                                                          Exception {
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject())).thenThrow(new RuntimeException());

        SdkServiceException exception = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertNotNull(exception);
        assertEquals(ERROR_CODE, exception.errorCode());
    }

    @Test
    public void handle_UnmarshallerReturnsException_ClientErrorType() throws Exception {
        httpResponse.setStatusCode(400);
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject()))
                .thenReturn(new CustomException("error"));

        ExecutionAttributes attributes =
                new ExecutionAttributes().putAttribute(SdkExecutionAttributes.SERVICE_NAME, SERVICE_NAME);
        SdkServiceException exception = responseHandler.handle(httpResponse, attributes);

        assertEquals(ERROR_CODE, exception.errorCode());
        assertEquals(400, exception.statusCode());
        assertEquals(SERVICE_NAME, exception.serviceName());
    }

    @Test
    public void handle_UnmarshallerReturnsException_WithRequestId() throws Exception {
        httpResponse.setStatusCode(500);
        httpResponse.addHeader(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER, "1234");
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject()))
                .thenReturn(new CustomException("error"));

        SdkServiceException exception = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertEquals("1234", exception.requestId());
    }

    /**
     * Headers are case insensitive so the request id should still be parsed in this test
     */
    @Test
    public void handle_UnmarshallerReturnsException_WithCaseInsensitiveRequestId() throws
                                                                                   Exception {
        httpResponse.setStatusCode(500);
        httpResponse.addHeader(StringUtils.upperCase(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER),
                               "1234");
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject()))
                .thenReturn(new CustomException("error"));

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
                .thenReturn(new CustomException("error"));

        SdkServiceException exception = responseHandler.handle(httpResponse, new ExecutionAttributes());
        assertThat(exception.headers(), hasEntry("FooHeader", "FooValue"));
        assertThat(exception.headers(),
                   hasEntry(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER, "1234"));
    }

    private void expectUnmarshallerMatches() throws Exception {
        when(unmarshaller.matchErrorCode(anyString())).thenReturn(true);
    }

    private void expectUnmarshallerDoesNotMatch() throws Exception {
        when(unmarshaller.matchErrorCode(anyString())).thenReturn(false);
    }

    private static class CustomException extends SdkServiceException {

        private static final long serialVersionUID = 1305027296023640779L;

        public CustomException(String errorMessage) {
            super(errorMessage);
        }
    }
}
