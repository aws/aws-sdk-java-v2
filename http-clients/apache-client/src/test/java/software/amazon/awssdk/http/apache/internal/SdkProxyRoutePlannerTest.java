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

package software.amazon.awssdk.http.apache.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Unit tests for {@link SdkProxyRoutePlanner}.
 */
public class SdkProxyRoutePlannerTest {
    private static final HttpHost S3_HOST = new HttpHost("s3.us-west-2.amazonaws.com", 443, "https");
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

    @Test
    public void testWildcardInNonProxyHosts() throws HttpException {
        Set<String> nonProxyHosts = new HashSet<>();
        nonProxyHosts.add("*.s3.us-west-2.amazonaws.com");
        
        SdkProxyRoutePlanner planner = new SdkProxyRoutePlanner("localhost", 1234, "http", nonProxyHosts);
        
        HttpHost bucketHost = new HttpHost("my-bucket.s3.us-west-2.amazonaws.com", 443, "https");
        HttpHost proxyHost = planner.determineRoute(bucketHost, S3_REQUEST, CONTEXT).getProxyHost();
        
        assertNull(proxyHost, "Wildcard pattern should bypass proxy for matching subdomains");
    }

    @Test
    public void testNonProxyHostsWithManualRegex() throws HttpException {
        Set<String> nonProxyHosts = new HashSet<>();
        nonProxyHosts.add(".*\\.s3\\.us-west-2\\.amazonaws\\.com");
        
        SdkProxyRoutePlanner planner = new SdkProxyRoutePlanner("localhost", 1234, "http", nonProxyHosts);
        
        HttpHost bucketHost = new HttpHost("my-bucket.s3.us-west-2.amazonaws.com", 443, "https");
        HttpHost proxyHost = planner.determineRoute(bucketHost, S3_REQUEST, CONTEXT).getProxyHost();
        
        assertNull(proxyHost, "Manual regex should still work after wildcard fix");
    }

    @Test
    public void testNonProxyHostsExactMatchFails() throws HttpException {
        Set<String> nonProxyHosts = new HashSet<>();
        nonProxyHosts.add("s3.us-west-2.amazonaws.com");
        
        SdkProxyRoutePlanner planner = new SdkProxyRoutePlanner("localhost", 1234, "http", nonProxyHosts);
        
        HttpHost bucketHost = new HttpHost("my-bucket.s3.us-west-2.amazonaws.com", 443, "https");
        HttpHost proxyHost = planner.determineRoute(bucketHost, S3_REQUEST, CONTEXT).getProxyHost();
        
        assertEquals("localhost", proxyHost.getHostName(), "Bucket subdomain should NOT bypass proxy without wildcard");
    }
}
