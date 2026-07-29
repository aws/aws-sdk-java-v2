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

package software.amazon.awssdk.benchmark.coldstart;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import software.amazon.awssdk.benchmark.utils.BenchmarkUtils;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Lightweight plain-HTTP Jetty server that answers every request with a fixed status, body and content type, so a real
 * service client can complete an operation without leaving the machine.
 *
 * <p>This mirrors {@code apicall.protocol.ProtocolRoundtripServer}, which is package-private to its own package. It is
 * duplicated here rather than shared so the cold-start benchmarks do not force a visibility change on the throughput
 * benchmarks that use the original.
 *
 * <p>Only the plain HTTP connector is exposed. That is deliberate: the cold-start benchmarks take a single measurement per
 * JVM, and TLS handshake plus trust-all keystore setup would add variance to the one sample that matters.
 */
final class ColdStartMockServer {

    private final Server server;
    private final int port;

    ColdStartMockServer(byte[] responseBody, String contentType) throws IOException {
        port = BenchmarkUtils.getUnusedPort();
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[] {connector});

        ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(new FixedResponseServlet(responseBody, contentType)), "/*");
        server.setHandler(context);
    }

    void start() throws Exception {
        server.start();
    }

    void stop() throws Exception {
        server.stop();
    }

    URI getHttpUri() {
        return URI.create("http://localhost:" + port);
    }

    static byte[] loadFixture(String path) throws IOException {
        try (InputStream is = ColdStartMockServer.class.getClassLoader().getResourceAsStream("fixtures/" + path)) {
            if (is == null) {
                throw new IOException("Fixture not found: fixtures/" + path);
            }
            return IoUtils.toByteArray(is);
        }
    }

    /**
     * Stateless: returns the same response for every method, path and body, and does not validate the request.
     */
    private static final class FixedResponseServlet extends HttpServlet {
        private final byte[] body;
        private final String contentType;

        private FixedResponseServlet(byte[] body, String contentType) {
            this.body = body;
            this.contentType = contentType;
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setStatus(200);
            resp.setContentLength(body.length);
            resp.setContentType(contentType);
            resp.getOutputStream().write(body);
        }
    }
}
