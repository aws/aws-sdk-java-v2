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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Level;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.StringInputStream;

public class Expect100ContinueTest {
    @Test
    public void expect100ContinueWorksWithZeroContentLength200() throws Exception {
        Handler handler = new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException {
                response.setStatus(200);
                response.setContentLength(0);
                response.addHeader("x-amz-test-header", "foo");
                response.flushBuffer();
            }
        };

        try (SdkHttpClient client = UrlConnectionHttpClient.create();
             EmbeddedServer server = new EmbeddedServer(handler)) {
            HttpExecuteResponse response = sendRequest(client, server);
            assertThat(response.httpResponse().statusCode()).isEqualTo(200);
            assertThat(response.httpResponse().firstMatchingHeader("x-amz-test-header")).hasValue("foo");
        }
    }

    @Test
    public void expect100ContinueWorksWith204() throws Exception {
        Handler handler = new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException {
                response.setStatus(204);
                response.addHeader("x-amz-test-header", "foo");
                response.flushBuffer();
            }
        };

        try (SdkHttpClient client = UrlConnectionHttpClient.create();
             EmbeddedServer server = new EmbeddedServer(handler)) {
            HttpExecuteResponse response = sendRequest(client, server);
            assertThat(response.httpResponse().statusCode()).isEqualTo(204);
            assertThat(response.httpResponse().firstMatchingHeader("x-amz-test-header")).hasValue("foo");
        }
    }

    @Test
    public void expect100ContinueWorksWith304() throws Exception {
        Handler handler = new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException {
                response.setStatus(304);
                response.addHeader("x-amz-test-header", "foo");
                response.flushBuffer();
            }
        };

        try (SdkHttpClient client = UrlConnectionHttpClient.create();
             EmbeddedServer server = new EmbeddedServer(handler)) {
            HttpExecuteResponse response = sendRequest(client, server);
            assertThat(response.httpResponse().statusCode()).isEqualTo(304);
            assertThat(response.httpResponse().firstMatchingHeader("x-amz-test-header")).hasValue("foo");
        }
    }

    @Test
    public void expect100ContinueWorksWith417() throws Exception {
        Handler handler = new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException {
                response.setStatus(417);
                response.addHeader("x-amz-test-header", "foo");
                response.flushBuffer();
            }
        };

        try (SdkHttpClient client = UrlConnectionHttpClient.create();
             EmbeddedServer server = new EmbeddedServer(handler)) {
            HttpExecuteResponse response = sendRequest(client, server);
            assertThat(response.httpResponse().statusCode()).isEqualTo(417);
            assertThat(response.httpResponse().firstMatchingHeader("x-amz-test-header")).hasValue("foo");
        }
    }

    @Test
    public void expect100ContinueWorksWithZeroContentLength500() throws Exception {
        Handler handler = new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException {
                response.setStatus(500);
                response.setContentLength(0);
                response.addHeader("x-amz-test-header", "foo");
                response.flushBuffer();
            }
        };

        try (SdkHttpClient client = UrlConnectionHttpClient.create();
             EmbeddedServer server = new EmbeddedServer(handler)) {
            HttpExecuteResponse response = sendRequest(client, server);
            assertThat(response.httpResponse().statusCode()).isEqualTo(500);
            assertThat(response.httpResponse().firstMatchingHeader("x-amz-test-header")).hasValue("foo");
        }
    }

    @Test
    public void expect100ContinueWorksWithPositiveContentLength500() throws Exception {
        Handler handler = new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException {
                response.setStatus(500);
                response.setContentLength(5);
                response.addHeader("x-amz-test-header", "foo");
                response.flushBuffer();
            }
        };

        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG);
             SdkHttpClient client = UrlConnectionHttpClient.create();
             EmbeddedServer server = new EmbeddedServer(handler)) {
            HttpExecuteResponse response = sendRequest(client, server);
            assertThat(response.httpResponse().statusCode()).isEqualTo(500);
            assertThat(response.httpResponse().firstMatchingHeader("x-amz-test-header")).hasValue("foo");
            assertThat(logCaptor.loggedEvents()).anySatisfy(logEvent -> {
                assertThat(logEvent.getLevel()).isEqualTo(Level.DEBUG);
                assertThat(logEvent.getMessage().getFormattedMessage()).contains("response payload has been dropped");
            });
        }
    }

    @Test
    public void expect100ContinueWorksWithPositiveContentLength400() throws Exception {
        Handler handler = new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException {
                response.setStatus(400);
                response.setContentLength(5);
                response.addHeader("x-amz-test-header", "foo");
                response.flushBuffer();
            }
        };

        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG);
             SdkHttpClient client = UrlConnectionHttpClient.create();
             EmbeddedServer server = new EmbeddedServer(handler)) {
            HttpExecuteResponse response = sendRequest(client, server);
            assertThat(response.httpResponse().statusCode()).isEqualTo(400);
            assertThat(response.httpResponse().firstMatchingHeader("x-amz-test-header")).hasValue("foo");
            assertThat(logCaptor.loggedEvents()).anySatisfy(logEvent -> {
                assertThat(logEvent.getLevel()).isEqualTo(Level.DEBUG);
                assertThat(logEvent.getMessage().getFormattedMessage()).contains("response payload has been dropped");
            });
        }
    }

    @Test
    public void expect100ContinueFailsWithPositiveContentLength200() throws Exception {
        Handler handler = new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException {
                response.setStatus(200);
                response.setContentLength(1);
                response.flushBuffer();
            }
        };

        try (SdkHttpClient client = UrlConnectionHttpClient.create();
             EmbeddedServer server = new EmbeddedServer(handler)) {
            assertThatThrownBy(() -> sendRequest(client, server)).isInstanceOf(UncheckedIOException.class);
        }
    }

    @Test
    public void expect100ContinueFailsWithChunkedEncoded200() throws Exception {
        Handler handler = new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException {
                response.setStatus(200);
                response.addHeader("Transfer-Encoding", "chunked");
                response.flushBuffer();
            }
        };

        try (SdkHttpClient client = UrlConnectionHttpClient.create();
             EmbeddedServer server = new EmbeddedServer(handler)) {
            assertThatThrownBy(() -> sendRequest(client, server)).isInstanceOf(UncheckedIOException.class);
        }
    }

    private HttpExecuteResponse sendRequest(SdkHttpClient client, EmbeddedServer server) throws IOException {
        return client.prepareRequest(HttpExecuteRequest.builder()
                                                       .request(SdkHttpRequest.builder()
                                                                       .uri(server.uri())
                                                                       .putHeader("Expect", "100-continue")
                                                                       .putHeader("Content-Length", "0")
                                                                       .method(SdkHttpMethod.PUT)
                                                                       .build())
                                                       .contentStreamProvider(() -> new StringInputStream(""))
                                                       .build())
                     .call();
    }

    private static class EmbeddedServer implements SdkAutoCloseable {
        private final Server server;

        public EmbeddedServer(Handler handler) throws Exception {
            server = new Server(0);
            server.setHandler(handler);
            server.start();
        }

        public URI uri() {
            return server.getURI();
        }

        @Override
        public void close() {
            try {
                server.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
