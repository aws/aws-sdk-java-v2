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

package software.amazon.awssdk.awscore.http.response;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonFactory;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.awscore.client.utils.ValidSdkObjects;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsJsonErrorUnmarshaller;
import software.amazon.awssdk.awscore.internal.protocol.json.JsonErrorCodeParser;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.protocol.json.SdkJsonErrorMessageParser;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.StringUtils;

public class AwsJsonErrorResponseHandlerTest {

    private static final String SERVICE_NAME = "someService";
    private static final String ERROR_CODE = "someErrorCode";
    private AwsJsonErrorResponseHandler responseHandler;
    private SdkHttpFullResponse.Builder httpResponseBuilder;

    @Mock
    private AwsJsonErrorUnmarshaller unmarshaller;

    @Mock
    private JsonErrorCodeParser errorCodeParser;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(errorCodeParser
                     .parseErrorCode(anyObject(), anyObject()))
                .thenReturn(ERROR_CODE);

        httpResponseBuilder =
            ValidSdkObjects.sdkHttpFullResponse().content(
                AbortableInputStream.create(new StringInputStream("{}")));

        responseHandler = new AwsJsonErrorResponseHandler(Collections.singletonList(unmarshaller), errorCodeParser,
                                                          SdkJsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER,
                                                          new JsonFactory());
    }

    @Test
    public void handle_NoUnmarshallersAdded_ReturnsGenericSdkServiceException() throws
                                                                                   Exception {
        responseHandler = new AwsJsonErrorResponseHandler(new ArrayList<>(),
                                                          new JsonErrorCodeParser(),
                                                          SdkJsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER,
                                                          new JsonFactory());

        SdkServiceException exception = responseHandler.handle(httpResponseBuilder.build(), new ExecutionAttributes());

        assertNotNull(exception);
    }

    @Test
    public void handle_NoMatchingUnmarshallers_ReturnsGenericSdkServiceException() throws
                                                                                      Exception {
        expectUnmarshallerDoesNotMatch();

        SdkServiceException exception = responseHandler.handle(httpResponseBuilder.build(), new ExecutionAttributes());

        assertNotNull(exception);
    }

    @Test
    public void handle_NullContent_ReturnsGenericSdkServiceException() throws Exception {
        httpResponseBuilder.statusCode(500);
        httpResponseBuilder.content(null);

        ExecutionAttributes attributes =
                new ExecutionAttributes().putAttribute(SdkExecutionAttribute.SERVICE_NAME, SERVICE_NAME);
        AwsServiceException exception = responseHandler.handle(httpResponseBuilder.build(), attributes);

        // We assert these common properties are set again to make sure that code path is exercised
        // for unknown SdkServiceExceptions as well
        assertEquals(ERROR_CODE, exception.awsErrorDetails().errorCode());
        assertEquals(500, exception.statusCode());
        assertEquals(SERVICE_NAME, exception.awsErrorDetails().serviceName());
    }

    @Test
    public void handle_EmptyContent_ReturnsGenericSdkServiceException() throws Exception {
        httpResponseBuilder.statusCode(500);
        httpResponseBuilder.content(AbortableInputStream.create(new StringInputStream("")));

        SdkServiceException exception = responseHandler.handle(httpResponseBuilder.build(), new ExecutionAttributes());

        assertNotNull(exception);
    }

    @Test
    public void handle_UnmarshallerReturnsNull_ReturnsGenericSdkServiceException() throws
                                                                                      Exception {
        expectUnmarshallerMatches();

        AwsServiceException exception = responseHandler.handle(httpResponseBuilder.build(), new ExecutionAttributes());

        assertNotNull(exception);
        assertEquals(ERROR_CODE, exception.awsErrorDetails().errorCode());
    }

    @Test
    public void handle_UnmarshallerThrowsException_ReturnsGenericSdkServiceException() throws
                                                                                          Exception {
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject())).thenThrow(new RuntimeException());

        AwsServiceException exception = responseHandler.handle(httpResponseBuilder.build(), new ExecutionAttributes());

        assertNotNull(exception);
        assertEquals(ERROR_CODE, exception.awsErrorDetails().errorCode());
    }

    @Test
    public void handle_UnmarshallerReturnsException_ClientErrorType() throws Exception {
        httpResponseBuilder.statusCode(400);
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject()))
                .thenReturn(AwsServiceException.builder().build());

        ExecutionAttributes attributes =
                new ExecutionAttributes().putAttribute(SdkExecutionAttribute.SERVICE_NAME, SERVICE_NAME);
        AwsServiceException exception = responseHandler.handle(httpResponseBuilder.build(), attributes);

        assertEquals(ERROR_CODE, exception.awsErrorDetails().errorCode());
        assertEquals(400, exception.statusCode());
        assertEquals(SERVICE_NAME, exception.awsErrorDetails().serviceName());
    }

    @Test
    public void handle_UnmarshallerReturnsException_WithRequestId() throws Exception {
        httpResponseBuilder.statusCode(500);
        httpResponseBuilder.putHeader(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER, "1234");
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject()))
                .thenReturn(AwsServiceException.builder().build());

        SdkServiceException exception = responseHandler.handle(httpResponseBuilder.build(), new ExecutionAttributes());

        assertEquals("1234", exception.requestId());
    }

    /**
     * Headers are case insensitive so the request id should still be parsed in this test
     */
    @Test
    public void handle_UnmarshallerReturnsException_WithCaseInsensitiveRequestId() throws
                                                                                   Exception {
        httpResponseBuilder.statusCode(500);
        httpResponseBuilder.putHeader(StringUtils.upperCase(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER),
                                      "1234");
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject()))
                .thenReturn(AwsServiceException.builder().build());

        SdkServiceException exception = responseHandler.handle(httpResponseBuilder.build(), new ExecutionAttributes());

        assertEquals("1234", exception.requestId());
    }

    /**
     * All headers (Including ones that populate other fields like request id) should be dumped into
     * the header map.
     */
    @Test
    public void handle_AllHeaders_DumpedIntoHeaderMap() throws Exception {
        httpResponseBuilder.statusCode(500);
        httpResponseBuilder.putHeader("FooHeader", "FooValue");
        httpResponseBuilder.putHeader(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER, "1234");
        expectUnmarshallerMatches();
        when(unmarshaller.unmarshall(anyObject()))
                .thenReturn(AwsServiceException.builder().build());

        AwsServiceException exception = responseHandler.handle(httpResponseBuilder.build(), new ExecutionAttributes());
        assertThat(exception.awsErrorDetails().sdkHttpResponse().headers(),
                   hasEntry("FooHeader", Collections.singletonList("FooValue")));
        assertThat(exception.awsErrorDetails().sdkHttpResponse().headers(),
                   hasEntry(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER, Collections.singletonList("1234")));
    }

    private void expectUnmarshallerMatches() throws Exception {
        when(unmarshaller.matchErrorCode(anyString())).thenReturn(true);
    }

    private void expectUnmarshallerDoesNotMatch() throws Exception {
        when(unmarshaller.matchErrorCode(anyString())).thenReturn(false);
    }
}
