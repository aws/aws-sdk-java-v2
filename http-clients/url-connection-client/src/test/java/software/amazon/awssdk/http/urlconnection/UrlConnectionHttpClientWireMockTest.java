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

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static software.amazon.awssdk.http.Header.ACCEPT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import java.io.IOException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientTestSuite;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.AttributeMap;

public final class UrlConnectionHttpClientWireMockTest extends SdkHttpClientTestSuite {

    @Override
    protected SdkHttpClient createSdkHttpClient(SdkHttpClientOptions options) {
        UrlConnectionHttpClient.Builder builder = UrlConnectionHttpClient.builder();
        AttributeMap.Builder attributeMap = AttributeMap.builder();

        if (options.tlsTrustManagersProvider() != null) {
            builder.tlsTrustManagersProvider(options.tlsTrustManagersProvider());
        }

        if (options.trustAll()) {
            attributeMap.put(TRUST_ALL_CERTIFICATES, options.trustAll());
        }

        return builder.buildWithDefaults(attributeMap.build());
    }

    @Override
    public void connectionsAreNotReusedOn5xxErrors() {
        // We cannot support this because the URL connection client doesn't allow us to disable connection reuse
    }

    // https://bugs.openjdk.org/browse/JDK-8163921
    @Test
    public void noAcceptHeader_shouldSet() throws IOException {
        SdkHttpClient client = createSdkHttpClient();

        stubForMockRequest(200);

        SdkHttpFullRequest req = mockSdkRequest("http://localhost:" + mockServer.port(), SdkHttpMethod.POST);
        HttpExecuteResponse rsp = client.prepareRequest(HttpExecuteRequest.builder()
                                                                          .request(req)
                                                                          .contentStreamProvider(req.contentStreamProvider()
                                                                                                    .orElse(null))
                                                                          .build())
                                        .call();

        mockServer.verify(postRequestedFor(urlPathEqualTo("/")).withHeader(ACCEPT, equalTo("*/*")));
    }

    @Test
    public void hasAcceptHeader_shouldNotOverride() throws IOException {
        SdkHttpClient client = createSdkHttpClient();

        stubForMockRequest(200);

        SdkHttpFullRequest req = mockSdkRequest("http://localhost:" + mockServer.port(), SdkHttpMethod.POST);
        req = req.toBuilder().putHeader(ACCEPT, "text/html").build();
        HttpExecuteResponse rsp = client.prepareRequest(HttpExecuteRequest.builder()
                                                                          .request(req)
                                                                          .contentStreamProvider(req.contentStreamProvider()
                                                                                                    .orElse(null))
                                                                          .build())
                                        .call();

        mockServer.verify(postRequestedFor(urlPathEqualTo("/")).withHeader(ACCEPT, equalTo("text/html")));
    }


    @After
    public void reset() {
        HttpsURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
    }
}
