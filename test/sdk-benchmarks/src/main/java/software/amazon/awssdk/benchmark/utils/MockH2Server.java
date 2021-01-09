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

package software.amazon.awssdk.benchmark.utils;

import java.io.IOException;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Local h2 server used to stub fixed response.
 *
 * See:
 * https://git.eclipse.org/c/jetty/org.eclipse.jetty.project
 * .git/tree/examples/embedded/src/main/java/org/eclipse/jetty/embedded/Http2Server.java
 */
public class MockH2Server extends BaseMockServer {
    private final Server server;

    public MockH2Server(boolean usingAlpn) throws IOException {
        super();
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(httpPort);

        // HTTP Configuration
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setSecureScheme("https");
        httpConfiguration.setSecurePort(httpsPort);
        httpConfiguration.setSendXPoweredBy(true);
        httpConfiguration.setSendServerVersion(true);

        // HTTP Connector
        ServerConnector http = new ServerConnector(server,
                                                   new HttpConnectionFactory(httpConfiguration),
                                                   new HTTP2CServerConnectionFactory(httpConfiguration));
        http.setPort(httpPort);
        server.addConnector(http);


        // HTTPS Configuration
        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        // SSL Context Factory for HTTPS and HTTP/2
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setTrustAll(true);
        sslContextFactory.setValidateCerts(false);
        sslContextFactory.setNeedClientAuth(false);
        sslContextFactory.setWantClientAuth(false);
        sslContextFactory.setValidatePeerCerts(false);
        sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
        sslContextFactory.setKeyStorePassword("password");
        sslContextFactory.setKeyStorePath(MockServer.class.getResource("mock-keystore.jks").toExternalForm());


        // HTTP/2 Connection Factory
        HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(https);

        // SSL Connection Factory
        SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, "h2");
        ServerConnector http2Connector;

        if (usingAlpn) {
            ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
            alpn.setDefaultProtocol("h2");
            // HTTP/2 Connector
            http2Connector = new ServerConnector(server, ssl, alpn, h2, new HttpConnectionFactory(https));
        } else {
            http2Connector = new ServerConnector(server, ssl, h2, new HttpConnectionFactory(https));
        }

        http2Connector.setPort(httpsPort);
        server.addConnector(http2Connector);

        ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(new AlwaysSuccessServlet()), "/*");
        server.setHandler(context);
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }
}