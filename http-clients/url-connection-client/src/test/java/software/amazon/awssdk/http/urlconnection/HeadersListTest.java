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
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

public class HeadersListTest {
    private static final WireMockServer WIRE_MOCK = new WireMockServer(0);
    private static SdkHttpClient client;

    @BeforeAll
    public static void setup() {
        WIRE_MOCK.start();
        client = UrlConnectionHttpClient.create();
    }

    @AfterAll
    public static void teardown() {
        client.close();
        WIRE_MOCK.stop();
    }

    @Test
    public void execute_requestHeaderHasMultipleValues_allValuesSent() throws IOException {
        ContentStreamProvider provider = () -> new ByteArrayInputStream(new byte[0]);
        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.PUT)
                                                           .host("localhost")
                                                           .port(WIRE_MOCK.port())
                                                           .protocol("http")
                                                           .putHeader("my-header", Arrays.asList("value1", "value2"))
                                                           .encodedPath("/test")
                                                           .build();

        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        client.prepareRequest(request).call();

        WIRE_MOCK.verify(putRequestedFor(urlEqualTo("/test"))
                             .withHeader("my-header", equalTo("value1,value2")));
    }
}
