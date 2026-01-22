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

package software.amazon.awssdk.services.s3;

import javax.servlet.http.HttpServlet;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
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
 * Simple test server that routes all requests to the given servlet.
 */
public class TestServer {
    private final int httpPort;
    private final int httpsPort;
    private final HttpServlet servlet;

    private Server server;
    private ServerConnector connector;
    private ServerConnector sslConnector;

    public TestServer(int httpPort, int httpsPort, HttpServlet servlet) {
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.servlet = servlet;
    }

    public TestServer(HttpServlet servlet) {
        this(0, 0, servlet);
    }

    public void start() throws Exception {
        server = new Server();
        connector = new ServerConnector(server);
        connector.setReuseAddress(true);
        connector.setPort(httpPort);

        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setTrustAll(true);
        sslContextFactory.setValidateCerts(false);
        sslContextFactory.setNeedClientAuth(false);
        sslContextFactory.setWantClientAuth(false);
        sslContextFactory.setValidatePeerCerts(false);
        sslContextFactory.setKeyStorePassword("password");
        sslContextFactory.setKeyStorePath(software.amazon.awssdk.services.s3.TestServer.class.getResource("mock-keystore.jks").toExternalForm());

        sslConnector = new ServerConnector(server,
                                           new SslConnectionFactory(sslContextFactory,
                                                                    HttpVersion.HTTP_1_1.asString()),
                                           new HttpConnectionFactory(https));
        sslConnector.setReuseAddress(true);
        sslConnector.setPort(httpsPort);

        server.setConnectors(new Connector[] {connector, sslConnector});

        ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(servlet), "/*");
        server.setHandler(context);

        server.start();
    }

    public void stop() throws Exception {
        server.stop();
        sslConnector.stop();
        connector.stop();
    }

    public int getPort() {
        return connector.getLocalPort();
    }

    public int getHttpsPort() {
        return sslConnector.getLocalPort();
    }
}