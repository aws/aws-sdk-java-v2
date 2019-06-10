/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.java.internal;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.*;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

public class JavaHttpResponseBodyHandlerTest {

    @Test
    public void BodyHandlerCreatedSuccessfullyTest() {
        SdkAsyncHttpResponseHandler mockSdkHttpResponseHandler = mock(SdkAsyncHttpResponseHandler.class);
        HttpResponse.ResponseInfo responseInfo = mock(HttpResponse.ResponseInfo.class);

        ListToByteBufferProcessor listToByteBufferProcessor = new ListToByteBufferProcessor();

        JavaHttpResponseBodyHandler javaBodyHandler = new JavaHttpResponseBodyHandler(mockSdkHttpResponseHandler, listToByteBufferProcessor);

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("foo", Collections.singletonList("bar"));

        HttpHeaders httpHeaders = HttpHeaders.of(headers, (s, s2) -> false);

        when(responseInfo.headers()).thenReturn(httpHeaders);
        when(responseInfo.statusCode()).thenReturn(200);


        javaBodyHandler.apply(responseInfo);

        ArgumentCaptor<SdkHttpResponse> capturedResponse = ArgumentCaptor.forClass(SdkHttpResponse.class);
        verify(mockSdkHttpResponseHandler).onHeaders(capturedResponse.capture());
        verify(mockSdkHttpResponseHandler).onStream(listToByteBufferProcessor.getPublisherToSdk());

        assertEquals(responseInfo.statusCode(), capturedResponse.getValue().statusCode());
        assertEquals(httpHeaders.map(), capturedResponse.getValue().headers());
    }

}