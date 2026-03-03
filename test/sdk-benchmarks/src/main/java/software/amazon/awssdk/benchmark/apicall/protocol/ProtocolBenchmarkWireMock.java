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

package software.amazon.awssdk.benchmark.apicall.protocol;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * WireMock server for protocol benchmarking.
 */
class ProtocolBenchmarkWireMock {
    
    private final WireMockServer wireMock;
    
    ProtocolBenchmarkWireMock() {
        wireMock = new WireMockServer(options()
                .dynamicPort()
                .jettyStopTimeout(1000L));
    }
    
    void start() {
        wireMock.start();
    }
    
    void stop() {
        if (wireMock != null && wireMock.isRunning()) {
            wireMock.stop();
        }
    }
    
    String baseUrl() {
        return wireMock.baseUrl();
    }
    
    void stubDynamoDbResponses(String putItemResponse, String queryResponse) {

        wireMock.stubFor(post(urlEqualTo("/"))
                .withHeader("X-Amz-Target", com.github.tomakehurst.wiremock.client.WireMock.equalTo("DynamoDB_20120810.PutItem"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-amz-json-1.0")
                        .withBody(putItemResponse)));
        

        wireMock.stubFor(post(urlEqualTo("/"))
                .withHeader("X-Amz-Target", com.github.tomakehurst.wiremock.client.WireMock.equalTo("DynamoDB_20120810.Query"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-amz-json-1.0")
                        .withBody(queryResponse)));
    }
    
    static String loadFixture(String path) throws IOException {
        try (InputStream is = ProtocolBenchmarkWireMock.class.getClassLoader()
                .getResourceAsStream("fixtures/" + path)) {
            if (is == null) {
                throw new IOException("Fixture not found: " + path);
            }
            byte[] buffer = new byte[8192];
            int bytesRead;
            StringBuilder sb = new StringBuilder();
            while ((bytesRead = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            }
            return sb.toString();
        }
    }
}
