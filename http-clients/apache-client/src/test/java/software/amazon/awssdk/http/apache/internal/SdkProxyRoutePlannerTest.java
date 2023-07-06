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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.utils.internal.SystemSettingUtilsTestBackdoor;

/**
 * Unit tests for {@link SdkProxyRoutePlanner}.
 */
public class SdkProxyRoutePlannerTest {
    private static final HttpHost INSECURE_S3_HOST = new HttpHost("s3.us-west-2.amazonaws.com", 80, "http");
    private static final HttpHost S3_HOST = new HttpHost("s3.us-west-2.amazonaws.com", 443, "https");
    private static final HttpHost EXAMPLE_HOST = new HttpHost("example.com", 443, "https");
    private static final HttpGet S3_REQUEST = new HttpGet("/my-bucket/my-object");
    private static final HttpClientContext CONTEXT = new HttpClientContext();

    @BeforeEach
    public void setup() {
        clearProxyProperties();
        SystemSettingUtilsTestBackdoor.clearEnvironmentVariableOverrides();
    }

    @AfterAll
    public static void cleanup() {
        clearProxyProperties();
        SystemSettingUtilsTestBackdoor.clearEnvironmentVariableOverrides();
    }

    @Test
    public void testSetsCorrectSchemeBasedOnProcotol_HTTPS() throws HttpException {
        SdkProxyRoutePlanner planner = new SdkProxyRoutePlanner(
            ProxyConfiguration.builder().endpoint(URI.create("https://localhost:1234")).build()
        );

        // Routing to an HTTP & HTTPS endpoint should go to the same proxy as
        // it's been explicitly configured, and by default an explicit proxy
        // goes to both by default.
        HttpHost proxyHost = planner.determineRoute(S3_HOST, S3_REQUEST, CONTEXT).getProxyHost();
        assertEquals("localhost", proxyHost.getHostName());
        assertEquals("https", proxyHost.getSchemeName());
        assertEquals(1234, proxyHost.getPort());
        HttpHost insecureProxyHost = planner.determineRoute(INSECURE_S3_HOST, S3_REQUEST, CONTEXT).getProxyHost();
        assertEquals(proxyHost, insecureProxyHost);
    }

    @Test
    public void testSetsCorrectSchemeBasedOnProcotol_HTTP() throws HttpException {
        SdkProxyRoutePlanner planner = new SdkProxyRoutePlanner(
            ProxyConfiguration.builder().endpoint(URI.create("http://localhost:1234")).build()
        );

        HttpHost proxyHost = planner.determineRoute(S3_HOST, S3_REQUEST, CONTEXT).getProxyHost();
        assertEquals("localhost", proxyHost.getHostName());
        assertEquals("http", proxyHost.getSchemeName());
        assertEquals(1234, proxyHost.getPort());
        HttpHost insecureProxyHost = planner.determineRoute(INSECURE_S3_HOST, S3_REQUEST, CONTEXT).getProxyHost();
        assertEquals(proxyHost, insecureProxyHost);
    }

    @Test
    public void testCanReturnTwoSeparateProxies() throws HttpException {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("http_proxy", "https://localhost:1234");
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("https_proxy", "http://example.com:25565");
        SdkProxyRoutePlanner envPlanner = new SdkProxyRoutePlanner(ProxyConfiguration.builder()
                                                                                     .proxyOverHttp(false)
                                                                                     .proxyOverHttps(false)
                                                                                     .useSystemPropertyValues(false)
                                                                                     .build());

        HttpHost proxyHost = envPlanner.determineRoute(S3_HOST, S3_REQUEST, CONTEXT).getProxyHost();
        assertEquals("example.com", proxyHost.getHostName());
        assertEquals("http", proxyHost.getSchemeName());
        assertEquals(25565, proxyHost.getPort());

        HttpHost insecureProxyHost = envPlanner.determineRoute(INSECURE_S3_HOST, S3_REQUEST, CONTEXT).getProxyHost();
        assertEquals("localhost", insecureProxyHost.getHostName());
        assertEquals("https", insecureProxyHost.getSchemeName());
        assertEquals(1234, insecureProxyHost.getPort());
    }

    @Test
    public void testCanIgnoreProxyHost() throws HttpException {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("http_proxy", "http://localhost:1234");
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("https_proxy", "http://example.com:1234");
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("no_proxy", "s3.us-west-2.amazonaws.com");
        SdkProxyRoutePlanner planner = new SdkProxyRoutePlanner(ProxyConfiguration.builder().build());

        assertNull(planner.determineRoute(S3_HOST, S3_REQUEST, CONTEXT).getProxyHost());
        assertNull(planner.determineRoute(INSECURE_S3_HOST, S3_REQUEST, CONTEXT).getProxyHost());
        assertNotNull(planner.determineRoute(EXAMPLE_HOST, S3_REQUEST, CONTEXT).getProxyHost());
    }

    private static void clearProxyProperties() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("http.nonProxyHosts");
        System.clearProperty("http.proxyUser");
        System.clearProperty("http.proxyPassword");

        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        System.clearProperty("https.proxyUser");
        System.clearProperty("https.proxyPassword");
    }
}
