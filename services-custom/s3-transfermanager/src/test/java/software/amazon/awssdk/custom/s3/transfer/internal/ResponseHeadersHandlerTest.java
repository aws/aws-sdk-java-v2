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

package software.amazon.awssdk.custom.s3.transfer.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.crt.http.HttpHeader;

public class ResponseHeadersHandlerTest {
    private ResponseHeadersHandler handler;
    
    @Before
    public void setUp() {
        handler = new ResponseHeadersHandler();
    }

    @Test
    public void onResponseHeaders_shouldCreateSdkHttpResponse() {
        HttpHeader[] headers = new HttpHeader[1];
        headers[0] = new HttpHeader("foo", "bar");

        handler.onResponseHeaders(400, headers);
        assertThat(handler.sdkHttpResponseFuture()).isCompleted();
        Map<String, List<String>> actualHeaders
            = handler.sdkHttpResponseFuture().join().headers();
        assertThat(actualHeaders).hasSize(1);
        assertThat(actualHeaders.get("foo")).containsExactlyInAnyOrder("bar");
    }

    @Test
    public void responseNotReady() {
        assertThat(handler.sdkHttpResponseFuture()).isNotCompleted();
    }
}
