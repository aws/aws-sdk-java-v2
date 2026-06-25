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

package software.amazon.awssdk.http.urlconnection;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.core.internal.http.loader.SyncHttpClientWarmer;

/**
 * Verifies the CRaC sync warm-up sends its GET through the real {@code UrlConnectionHttpClient} discovered on the classpath.
 * The stub mirrors a real STS {@code GET} (302 redirect, empty body); the warm-up must not follow the redirect.
 */
public class UrlConnectionHttpClientWarmUpTest {

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    @Test
    public void warmAll_sendsWarmUpGetThroughUrlConnectionClient() {
        mockServer.stubFor(any(anyUrl()).willReturn(aResponse()
            .withStatus(302)
            .withHeader("Location", "https://aws.amazon.com/iam")));

        URI endpoint = URI.create("http://localhost:" + mockServer.port() + "/");
        new SyncHttpClientWarmer(() -> endpoint).warmAll();

        mockServer.verify(1, getRequestedFor(urlPathEqualTo("/")));
        mockServer.verify(1, anyRequestedFor(anyUrl()));
    }
}
