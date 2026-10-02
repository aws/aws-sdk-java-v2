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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Validates that a TLS handshake failure against an untrusted (self-signed) certificate surfaces as
 * {@link SSLHandshakeException} with a cause chain, so callers can differentiate transient
 * interruptions from persistent certificate failures.
 */
public abstract class SdkHttpClientSslHandshakeBehaviorTestSuite {
    private WireMockServer selfSignedServer;

    /**
     * Implemented by a child class to create an HTTP client to validate
     */
    protected abstract SdkHttpClient createSdkHttpClient();

    @BeforeEach
    public void setup() {
        selfSignedServer = HttpTestUtils.createSelfSignedServer();
        selfSignedServer.start();
    }

    @AfterEach
    public void teardown() {
        if (selfSignedServer != null) {
            selfSignedServer.stop();
            selfSignedServer = null;
        }
    }

    @Test
    public void sslHandshakeFailure_surfacesAsSslHandshakeException() {
        Throwable thrown = executeRequestAgainstUntrustedServer();

        assertThat(thrown).isInstanceOf(SSLHandshakeException.class);
    }

    @Test
    public void sslHandshakeFailure_exceptionCarriesDiagnosableCause() {
        Throwable thrown = executeRequestAgainstUntrustedServer();

        assertThat(thrown).isInstanceOf(SSLHandshakeException.class);
        assertThat(thrown.getCause())
            .withFailMessage("SSLHandshakeException must chain the underlying failure as its cause "
                             + "so callers can differentiate transient from persistent TLS failures. Was: %s", thrown)
            .isNotNull();
    }

    private Throwable executeRequestAgainstUntrustedServer() {
        try (SdkHttpClient client = createSdkHttpClient()) {
            SdkHttpFullRequest request = httpsRequest(selfSignedServer.httpsPort());
            return catchThrowable(client.prepareRequest(HttpExecuteRequest.builder()
                                                                          .request(request)
                                                                          .contentStreamProvider(
                                                                              request.contentStreamProvider().orElse(null))
                                                                          .build())
                                        ::call);
        }
    }

    private static SdkHttpFullRequest httpsRequest(int httpsPort) {
        URI uri = URI.create("https://localhost:" + httpsPort);
        byte[] content = "Body".getBytes(StandardCharsets.UTF_8);
        return SdkHttpFullRequest.builder()
                                 .uri(uri)
                                 .method(SdkHttpMethod.POST)
                                 .putHeader("Host", uri.getHost())
                                 .putHeader("Content-Length", Integer.toString(content.length))
                                 .contentStreamProvider(() -> new ByteArrayInputStream(content))
                                 .build();
    }
}
