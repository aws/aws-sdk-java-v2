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

package software.amazon.awssdk.internal.http.response;

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
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonServiceException.ErrorType;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.handlers.AwsExecutionAttributes;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullRequestAdapter;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.internal.http.JsonErrorCodeParser;
import software.amazon.awssdk.runtime.http.JsonErrorMessageParser;
import software.amazon.awssdk.runtime.transform.JsonErrorUnmarshaller;
import software.amazon.awssdk.util.StringInputStream;
import software.amazon.awssdk.util.StringUtils;

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

        httpResponse = new HttpResponse(SdkHttpFullRequestAdapter.toHttpFullRequest(new DefaultRequest<>(SERVICE_NAME)));
        httpResponse.setContent(new StringInputStream("{}"));

        responseHandler = new JsonErrorResponseHandler(Collections.singletonList(unmarshaller), errorCodeParser,
                                                       JsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER,
                                                       new JsonFactory());
    }

    @Test
    public void handle_NoUnmarshallersAdded_ReturnsGenericAmazonServiceException() throws
                                                                                   Exception {
        responseHandler = new JsonErrorResponseHandler(new ArrayList<>(),
                                                       new JsonErrorCodeParser(),
                                                       JsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER,
                                                       new JsonFactory());

        AmazonServiceException ase = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertNotNull(ase);
    }

    @Test
    public void handle_NoMatchingUnmarshallers_ReturnsGenericAmazonServiceException() throws
                                                                                      Exception {
        expectUnmarshallerDoesNotMatch();

        AmazonServiceException ase = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertNotNull(ase);
    }

    @Test
    public void handle_NullContent_ReturnsGenericAmazonServiceException() throws Exception {
        httpResponse.setStatusCode(500);
        httpResponse.setContent(null);

        ExecutionAttributes attributes =
                new ExecutionAttributes().putAttribute(AwsExecutionAttributes.SERVICE_NAME, SERVICE_NAME);
        AmazonServiceException ase = responseHandler.handle(httpResponse, attributes);

        // We assert these common properties are set again to make sure that code path is exercised
        // for unknown AmazonServiceExceptions as well
        assertEquals(ERROR_CODE, ase.getErrorCode());
        assertEquals(500, ase.getStatusCode());
        assertEquals(SERVICE_NAME, ase.getServiceName());
        assertEquals(ErrorType.Service, ase.getErrorType());
    }

    @Test
    public void handle_EmptyContent_ReturnsGenericAmazonServiceException() throws Exception {
        httpResponse.setStatusCode(500);
        httpResponse.setContent(new StringInputStream(""));

        AmazonServiceException ase = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertNotNull(ase);
    }

    @Test
    public void handle_UnmarshallerReturnsNull_ReturnsGenericAmazonServiceException() throws
                                                                                      Exception {
        expectUnmarshallerMatches();

        AmazonServiceException ase = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertNotNull(ase);
        assertEquals(ERROR_CODE, ase.getErrorCode());
    }

    @Test
    public void handle_UnmarshallerThrowsException_ReturnsGenericAmazonServiceException() throws
                                                                                          Exception {
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject())).thenThrow(new RuntimeException());

        AmazonServiceException ase = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertNotNull(ase);
        assertEquals(ERROR_CODE, ase.getErrorCode());
    }

    @Test
    public void handle_UnmarshallerReturnsException_ClientErrorType() throws Exception {
        httpResponse.setStatusCode(400);
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject()))
                .thenReturn(new CustomException("error"));

        ExecutionAttributes attributes =
                new ExecutionAttributes().putAttribute(AwsExecutionAttributes.SERVICE_NAME, SERVICE_NAME);
        AmazonServiceException ase = responseHandler.handle(httpResponse, attributes);

        assertEquals(ERROR_CODE, ase.getErrorCode());
        assertEquals(400, ase.getStatusCode());
        assertEquals(SERVICE_NAME, ase.getServiceName());
        assertEquals(ErrorType.Client, ase.getErrorType());
    }

    @Test
    public void handle_UnmarshallerReturnsException_ServiceErrorType() throws Exception {
        httpResponse.setStatusCode(500);
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject()))
                .thenReturn(new CustomException("error"));

        AmazonServiceException ase = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertEquals(ErrorType.Service, ase.getErrorType());
    }

    @Test
    public void handle_UnmarshallerReturnsException_WithRequestId() throws Exception {
        httpResponse.setStatusCode(500);
        httpResponse.addHeader(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER, "1234");
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject()))
                .thenReturn(new CustomException("error"));

        AmazonServiceException ase = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertEquals("1234", ase.getRequestId());
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

        AmazonServiceException ase = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertEquals("1234", ase.getRequestId());
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

        AmazonServiceException ase = responseHandler.handle(httpResponse, new ExecutionAttributes());
        assertThat(ase.getHttpHeaders(), hasEntry("FooHeader", "FooValue"));
        assertThat(ase.getHttpHeaders(),
                   hasEntry(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER, "1234"));
    }

    private void expectUnmarshallerMatches() throws Exception {
        when(unmarshaller.matchErrorCode(anyString())).thenReturn(true);
    }

    private void expectUnmarshallerDoesNotMatch() throws Exception {
        when(unmarshaller.matchErrorCode(anyString())).thenReturn(false);
    }

    private static class CustomException extends AmazonServiceException {

        private static final long serialVersionUID = 1305027296023640779L;

        public CustomException(String errorMessage) {
            super(errorMessage);
        }
    }
}
