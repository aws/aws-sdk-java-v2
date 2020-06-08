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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.concurrent.CompletableFuture;

public class HttpTestUtils {
    private HttpTestUtils() {
    }

    public static WireMockServer createSelfSignedServer() {
        URL selfSignedJks = SdkHttpClientTestSuite.class.getResource("/selfSigned.jks");

        return new WireMockServer(wireMockConfig()
                                      .dynamicHttpsPort()
                                      .keystorePath(selfSignedJks.toString())
                                      .keystorePassword("changeit")
                                      .keystoreType("jks")
        );
    }

    public static KeyStore getSelfSignedKeyStore() throws Exception {
        URL selfSignedJks = SdkHttpClientTestSuite.class.getResource("/selfSigned.jks");
        KeyStore keyStore = KeyStore.getInstance("jks");
        try (InputStream stream = selfSignedJks.openStream()) {
            keyStore.load(stream, "changeit".toCharArray());
        }

        return keyStore;
    }

    public static CompletableFuture<Void> sendGetRequest(int serverPort, SdkAsyncHttpClient client) {
        AsyncExecuteRequest req = AsyncExecuteRequest.builder()
                .responseHandler(new SdkAsyncHttpResponseHandler() {
                    private SdkHttpResponse headers;

                    @Override
                    public void onHeaders(SdkHttpResponse headers) {
                        this.headers = headers;
                    }

                    @Override
                    public void onStream(Publisher<ByteBuffer> stream) {
                        Flowable.fromPublisher(stream).forEach(b -> {
                        });
                    }

                    @Override
                    public void onError(Throwable error) {
                    }
                })
                .request(SdkHttpFullRequest.builder()
                        .method(SdkHttpMethod.GET)
                        .protocol("https")
                        .host("localhost")
                        .port(serverPort)
                        .build())
                .requestContentPublisher(new EmptyPublisher())
                .build();

        return client.execute(req);
    }
}
