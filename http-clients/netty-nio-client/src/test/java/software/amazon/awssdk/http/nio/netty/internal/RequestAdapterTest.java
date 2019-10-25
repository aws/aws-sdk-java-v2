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

package software.amazon.awssdk.http.nio.netty.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import java.net.URI;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

public class RequestAdapterTest {

    private RequestAdapter instance;

    @Before
    public void setup() {
        instance = new RequestAdapter(Protocol.HTTP1_1);
    }

    @Test
    public void adaptSetsHostHeaderByDefault() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("http://localhost:12345/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        HttpRequest result = instance.adapt(sdkRequest);
        List<String> hostHeaders = result.headers()
                .getAll(HttpHeaderNames.HOST.toString());
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.size());
        assertEquals("localhost:12345", hostHeaders.get(0));
    }

    @Test
    public void defaultHttpPortsAreNotInDefaultHostHeader() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("http://localhost:80/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        HttpRequest result = instance.adapt(sdkRequest);
        List<String> hostHeaders = result.headers()
                .getAll(HttpHeaderNames.HOST.toString());
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.size());
        assertEquals("localhost", hostHeaders.get(0));

        sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("https://localhost:443/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        result = instance.adapt(sdkRequest);
        hostHeaders = result.headers()
                .getAll(HttpHeaderNames.HOST.toString());
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.size());
        assertEquals("localhost", hostHeaders.get(0));
    }
}
