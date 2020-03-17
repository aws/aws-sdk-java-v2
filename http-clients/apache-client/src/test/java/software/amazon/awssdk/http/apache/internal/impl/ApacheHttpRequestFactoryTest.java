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

package software.amazon.awssdk.http.apache.internal.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.apache.internal.ApacheHttpRequestConfig;

public class ApacheHttpRequestFactoryTest {

    private ApacheHttpRequestConfig requestConfig;
    private ApacheHttpRequestFactory instance;

    @Before
    public void setup() {
        instance = new ApacheHttpRequestFactory();
        requestConfig = ApacheHttpRequestConfig.builder()
                .connectionAcquireTimeout(Duration.ZERO)
                .connectionTimeout(Duration.ZERO)
                .localAddress(InetAddress.getLoopbackAddress())
                .socketTimeout(Duration.ZERO)
                .build();
    }

    @Test
    public void ceateSetsHostHeaderByDefault() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("http://localhost:12345/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                .request(sdkRequest)
                .build();
        HttpRequestBase result = instance.create(request, requestConfig);
        Header[] hostHeaders = result.getHeaders(HttpHeaders.HOST);
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.length);
        assertEquals("localhost:12345", hostHeaders[0].getValue());
    }

    @Test
    public void defaultHttpPortsAreNotInDefaultHostHeader() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("http://localhost:80/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                .request(sdkRequest)
                .build();
        HttpRequestBase result = instance.create(request, requestConfig);
        Header[] hostHeaders = result.getHeaders(HttpHeaders.HOST);
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.length);
        assertEquals("localhost", hostHeaders[0].getValue());

        sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("https://localhost:443/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        request = HttpExecuteRequest.builder()
                .request(sdkRequest)
                .build();
        result = instance.create(request, requestConfig);
        hostHeaders = result.getHeaders(HttpHeaders.HOST);
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.length);
        assertEquals("localhost", hostHeaders[0].getValue());
    }
}
