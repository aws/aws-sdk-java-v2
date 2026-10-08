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

package software.amazon.awssdk.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

/**
 * Base test suite that verifies an HTTP client can construct and execute requests
 * under a SecurityManager with the appropriate permissions granted via a policy file.
 *
 * <p>Subclasses provide the HTTP client implementation and a policy file path.
 * The policy file for Apache 4.x does not need jdk.net.NetworkPermission entries,
 * while Apache 5.x requires them for TCP_KEEPIDLE/KEEPINTERVAL/KEEPCOUNT.</p>
 */
@EnabledForJreRange(max = JRE.JAVA_17)
public abstract class SdkHttpClientSecurityManagerTestSuite {

    private WireMockServer server;

    @BeforeEach
    void setUpServer() {
        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        server.stubFor(get(urlPathEqualTo("/"))
                           .willReturn(aResponse().withStatus(200).withBody("ok")));
    }

    @AfterEach
    void tearDownServer() {
        System.setSecurityManager(null);
        System.clearProperty("java.security.policy");
        java.security.Policy.getPolicy().refresh();
        server.stop();
    }

    /**
     * Creates the HTTP client to test.
     */
    protected abstract SdkHttpClient createHttpClient();

    /**
     * Returns the policy file URL to use. Subclasses load from their own resource path.
     */
    protected abstract String getPolicyFileUrl();

    @Test
    void httpCallSucceedsWhenSecurityManagerActiveWithCorrectPermissions() throws Exception {
        System.setProperty("java.security.policy", "=" + getPolicyFileUrl());
        java.security.Policy.getPolicy().refresh();
        System.setSecurityManager(new SecurityManager());

        SdkHttpClient client = createHttpClient();
        try {
            SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                           .uri(URI.create("http://localhost:" + server.port() + "/"))
                                                           .method(SdkHttpMethod.GET)
                                                           .build();
            HttpExecuteResponse response = client.prepareRequest(
                HttpExecuteRequest.builder().request(request).build()).call();

            assertThat(response.httpResponse().statusCode()).isEqualTo(200);
        } finally {
            client.close();
        }
    }

    protected int serverPort() {
        return server.port();
    }
}
