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

package software.amazon.awssdk.http.apache5.internal;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SdkProxyRoutePlanner}.
 */
public class SdkProxyRoutePlannerTest {
    private static final HttpHost S3_HOST = new HttpHost("https", "s3.us-west-2.amazonaws.com", 443);
    private static final HttpGet S3_REQUEST = new HttpGet("/my-bucket/my-object");
    private static final HttpClientContext CONTEXT = new HttpClientContext();

    @Test
    public void testSetsCorrectSchemeBasedOnProcotol_HTTPS() throws HttpException {
        SdkProxyRoutePlanner planner = new SdkProxyRoutePlanner("localhost", 1234, "https", Collections.emptySet());

        HttpHost proxyHost = planner.determineRoute(S3_HOST, S3_REQUEST, CONTEXT).getProxyHost();
        assertEquals("localhost", proxyHost.getHostName());
        assertEquals("https", proxyHost.getSchemeName());
    }

    @Test
    public void testSetsCorrectSchemeBasedOnProcotol_HTTP() throws HttpException {
        SdkProxyRoutePlanner planner = new SdkProxyRoutePlanner("localhost", 1234, "http", Collections.emptySet());

        HttpHost proxyHost = planner.determineRoute(S3_HOST, S3_REQUEST, CONTEXT).getProxyHost();
        assertEquals("localhost", proxyHost.getHostName());
        assertEquals("http", proxyHost.getSchemeName());
    }
}
