/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.crt.internal;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpHeaderBlock;
import software.amazon.awssdk.crt.http.HttpStreamBase;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.crt.internal.response.ResponseHandlerHelper;

@ExtendWith(MockitoExtension.class)
class ResponseHandlerHelperTest {

    @Mock
    private HttpStreamBase stream;

    private ResponseHandlerHelper helper;

    @BeforeEach
    void setUp() {
        helper = new ResponseHandlerHelper(SdkHttpResponse.builder());
        // Register the stream via onResponseHeaders
        HttpHeader[] headers = { new HttpHeader("Content-Length", "1") };
        helper.onResponseHeaders(stream, 200, HttpHeaderBlock.MAIN.getValue(), headers);
    }

    @Test
    void releaseConnection_shouldOnlyCallClose() {
        helper.releaseConnection();

        verify(stream, never()).cancel();
        verify(stream).close();
    }

    @Test
    void closeConnection_shouldCallCancelThenClose() {
        helper.closeConnection();

        InOrder inOrder = Mockito.inOrder(stream);
        inOrder.verify(stream).cancel();
        inOrder.verify(stream).close();
    }

    @Test
    void releaseConnection_calledTwice_shouldOnlyCloseOnce() {
        helper.releaseConnection();
        helper.releaseConnection();

        verify(stream, Mockito.times(1)).close();
    }

    @Test
    void closeConnection_calledTwice_shouldOnlyCloseOnce() {
        helper.closeConnection();
        helper.closeConnection();

        verify(stream, Mockito.times(1)).cancel();
        verify(stream, Mockito.times(1)).close();
    }

    @Test
    void releaseConnection_afterCloseConnection_shouldBeNoOp() {
        helper.closeConnection();
        helper.releaseConnection();

        verify(stream, Mockito.times(1)).cancel();
        verify(stream, Mockito.times(1)).close();
    }

    @Test
    void closeConnection_afterReleaseConnection_shouldBeNoOp() {
        helper.releaseConnection();
        helper.closeConnection();

        verify(stream, never()).cancel();
        verify(stream, Mockito.times(1)).close();
    }

    @Test
    void incrementWindow_afterReleaseConnection_shouldBeNoOp() {
        helper.releaseConnection();
        helper.incrementWindow(1024);

        verify(stream, never()).incrementWindow(1024);
    }

    @Test
    void incrementWindow_afterCloseConnection_shouldBeNoOp() {
        helper.closeConnection();
        helper.incrementWindow(1024);

        verify(stream, never()).incrementWindow(1024);
    }

    @Test
    void incrementWindow_beforeClose_shouldWork() {
        helper.incrementWindow(1024);

        verify(stream).incrementWindow(1024);
    }
}
