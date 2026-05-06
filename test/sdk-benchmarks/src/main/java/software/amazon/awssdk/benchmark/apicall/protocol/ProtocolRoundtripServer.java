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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import software.amazon.awssdk.benchmark.utils.BenchmarkUtils;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Lightweight Jetty server for protocol roundtrip benchmarks.
 */
class ProtocolRoundtripServer {

    private final Server server;
    private final int port;

    ProtocolRoundtripServer(ProtocolRoundtripServlet servlet) throws IOException {
        port = BenchmarkUtils.getUnusedPort();
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[] {connector});

        ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(servlet), "/*");
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
        try (InputStream is = ProtocolRoundtripServer.class.getClassLoader()
                 .getResourceAsStream("fixtures/" + path)) {
            if (is == null) {
                throw new IOException("Fixture not found: fixtures/" + path);
            }
            return IoUtils.toByteArray(is);
        }
    }
}
