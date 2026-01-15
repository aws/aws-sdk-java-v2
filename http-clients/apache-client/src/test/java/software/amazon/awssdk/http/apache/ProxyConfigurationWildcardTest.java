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

package software.amazon.awssdk.http.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.apache.internal.SdkProxyRoutePlanner;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Integration tests for NO_PROXY wildcard support with ProxyConfiguration.
 */
public class ProxyConfigurationWildcardTest {

    private static final EnvironmentVariableHelper ENV_HELPER = new EnvironmentVariableHelper();
    private static final HttpHost S3_BUCKET_HOST = new HttpHost("my-bucket.s3.us-east-1.amazonaws.com", 443, "https");
    private static final HttpGet REQUEST = new HttpGet("/test");
    private static final HttpClientContext CONTEXT = new HttpClientContext();

    @BeforeEach
    public void setup() {
        ENV_HELPER.reset();
    }

    @AfterEach
    public void cleanup() {
        ENV_HELPER.reset();
    }

    @Test
    public void testWildcardInEnvironmentVariable() throws HttpException {
        // Set NO_PROXY environment variable with wildcard
        ENV_HELPER.set("NO_PROXY", "*.s3.us-east-1.amazonaws.com");

        // Build ProxyConfiguration that reads from environment
        ProxyConfiguration proxyConfig = ProxyConfiguration.builder()
                .endpoint(URI.create("http://proxy.example.com:8080"))
                .useEnvironmentVariableValues(true)
                .build();

        // Create SdkProxyRoutePlanner with the nonProxyHosts from ProxyConfiguration
        SdkProxyRoutePlanner planner = new SdkProxyRoutePlanner(
                proxyConfig.host(),
                proxyConfig.port(),
                proxyConfig.scheme(),
                proxyConfig.nonProxyHosts()
        );

        // Verify that bucket subdomain bypasses proxy
        HttpHost proxyHost = planner.determineRoute(S3_BUCKET_HOST, REQUEST, CONTEXT).getProxyHost();
        assertNull(proxyHost, "Wildcard from NO_PROXY should bypass proxy for bucket subdomains");
    }

    @Test
    public void testWildcardInProgrammaticConfig() throws HttpException {
        Set<String> nonProxyHosts = new HashSet<>();
        nonProxyHosts.add("*.s3.us-east-1.amazonaws.com");

        // Build ProxyConfiguration with programmatic nonProxyHosts
        ProxyConfiguration proxyConfig = ProxyConfiguration.builder()
                .endpoint(URI.create("http://proxy.example.com:8080"))
                .nonProxyHosts(nonProxyHosts)
                .build();

        // Create SdkProxyRoutePlanner with the nonProxyHosts from ProxyConfiguration
        SdkProxyRoutePlanner planner = new SdkProxyRoutePlanner(
                proxyConfig.host(),
                proxyConfig.port(),
                proxyConfig.scheme(),
                proxyConfig.nonProxyHosts()
        );

        // Verify that bucket subdomain bypasses proxy
        HttpHost proxyHost = planner.determineRoute(S3_BUCKET_HOST, REQUEST, CONTEXT).getProxyHost();
        assertNull(proxyHost, "Wildcard from programmatic config should bypass proxy for bucket subdomains");
    }

    @Test
    public void testProgrammaticOverridesEnvironmentVariable() throws HttpException {
        // Set NO_PROXY environment variable
        ENV_HELPER.set("NO_PROXY", "*.dynamodb.amazonaws.com");

        Set<String> nonProxyHosts = new HashSet<>();
        nonProxyHosts.add("*.s3.us-east-1.amazonaws.com");

        // Build ProxyConfiguration - programmatic should take precedence
        ProxyConfiguration proxyConfig = ProxyConfiguration.builder()
                .endpoint(URI.create("http://proxy.example.com:8080"))
                .nonProxyHosts(nonProxyHosts)
                .useEnvironmentVariableValues(true)
                .build();

        // Create SdkProxyRoutePlanner
        SdkProxyRoutePlanner planner = new SdkProxyRoutePlanner(
                proxyConfig.host(),
                proxyConfig.port(),
                proxyConfig.scheme(),
                proxyConfig.nonProxyHosts()
        );

        // Verify S3 bypasses (from programmatic), DynamoDB doesn't (env var ignored)
        HttpHost s3ProxyHost = planner.determineRoute(S3_BUCKET_HOST, REQUEST, CONTEXT).getProxyHost();
        assertNull(s3ProxyHost, "S3 should bypass proxy (programmatic config)");

        HttpHost dynamoHost = new HttpHost("my-table.dynamodb.amazonaws.com", 443, "https");
        HttpHost dynamoProxyHost = planner.determineRoute(dynamoHost, REQUEST, CONTEXT).getProxyHost();
        assertEquals("proxy.example.com", dynamoProxyHost.getHostName(), "DynamoDB should use proxy (env var ignored when programmatic set)");
    }
}
