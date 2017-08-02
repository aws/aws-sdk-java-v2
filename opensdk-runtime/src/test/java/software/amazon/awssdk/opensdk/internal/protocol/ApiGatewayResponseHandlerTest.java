/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.opensdk.internal.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.opensdk.BaseResult;
import software.amazon.awssdk.opensdk.SdkResponseMetadata;
import utils.http.HttpResponseBuilder;

@RunWith(MockitoJUnitRunner.class)
public class ApiGatewayResponseHandlerTest {

    @Mock
    private HttpResponseHandler<MockResult> mockResponseHandler;

    private ApiGatewayResponseHandler<MockResult> responseHandler;

    @Before
    public void setup() throws Exception {
        responseHandler = new ApiGatewayResponseHandler<>(mockResponseHandler);
        when(mockResponseHandler.handle(any(), any())).thenReturn(new MockResult());
    }

    @Test
    public void httpResponseContainsRequestId_UnmarshalledIntoMetadata() throws
                                                                         Exception {
        HttpResponse httpResponse = new HttpResponseBuilder()
                .withHeader(SdkResponseMetadata.HEADER_REQUEST_ID, "1234")
                .build();

        MockResult unmarshalled = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertEquals("1234", unmarshalled.sdkResponseMetadata().requestId());
    }

    @Test
    public void httpResponseDoesNotContainRequestId_RequestIdIsNullInMetadata() throws
                                                                                Exception {
        HttpResponse httpResponse = new HttpResponseBuilder()
                .build();

        MockResult unmarshalled = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertNull(unmarshalled.sdkResponseMetadata().requestId());
    }

    @Test
    public void httpHeadersInResponse_AreAccessibleFromSdkResponseMetadata() throws Exception {
        HttpResponse httpResponse = new HttpResponseBuilder()
                .withHeader("foo", "a")
                .withHeader("bar", "b")
                .withHeader("baz", "c")
                .build();

        final SdkResponseMetadata sdkResponseMetadata = responseHandler.handle(httpResponse, new ExecutionAttributes())
                .sdkResponseMetadata();


        assertContainsHeader(sdkResponseMetadata, "foo", "a");
        assertContainsHeader(sdkResponseMetadata, "bar", "b");
        assertContainsHeader(sdkResponseMetadata, "baz", "c");
    }

    @Test
    public void httpResponseStatusCode_IsSetOnHttpMetadata() throws Exception {
        HttpResponse httpResponse = new HttpResponseBuilder()
                .withStatusCode(201)
                .build();

        MockResult unmarshalled = responseHandler.handle(httpResponse, new ExecutionAttributes());

        assertEquals(201, unmarshalled.sdkResponseMetadata().httpStatusCode());
    }

    @Test
    public void responseHandlerDelegatesToWrappedHandlerForNeedsConnectionLeftOpen() {
        responseHandler.needsConnectionLeftOpen();

        verify(mockResponseHandler).needsConnectionLeftOpen();
    }

    private static void assertContainsHeader(SdkResponseMetadata responseMetadata,
                                             String headerName, String headerValue) {
        String actualHeaderValue = responseMetadata.header(headerName)
                .orElseThrow(
                        () -> new AssertionError(headerName + " was not present in the metadata"));
        assertEquals(actualHeaderValue, headerValue);
    }

    public static class MockResult extends BaseResult {

    }
}
