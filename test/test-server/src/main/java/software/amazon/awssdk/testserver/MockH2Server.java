/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.testserver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jetty.http2.HTTP2Session;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.api.server.ServerSessionListener;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.GoAwayFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http2.frames.ResetFrame;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.Callback;

/**
 * Local h2 server used to stub fixed response.
 *
 * See:
 * https://git.eclipse.org/c/jetty/org.eclipse.jetty.project
 * .git/tree/examples/embedded/src/main/java/org/eclipse/jetty/embedded/Http2Server.java
 */
public class MockH2Server extends BaseMockServer {
    private final Server server = new Server();

    public MockH2Server(boolean usingAlpn) throws IOException {
        ServletContextHandler context = new ServletContextHandler(server, "/");
        context.addServlet(new ServletHolder(new AlwaysSuccessServlet()), "/*");
        server.setHandler(context);

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(httpPort);

        // HTTP Configuration
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setSendXPoweredBy(true);
        httpConfiguration.setSendServerVersion(true);

        // HTTP Connector
        ServerConnector http = new ServerConnector(server,
                                                   new HttpConnectionFactory(httpConfiguration),
                                                   new HTTP2CServerConnectionFactory(httpConfiguration) {
           private AtomicBoolean goAwaySent = new AtomicBoolean(false);
           private AtomicInteger goAwayStreamId = new AtomicInteger(-1);

           @Override
           protected ServerSessionListener newSessionListener(Connector connector, EndPoint endPoint) {
               return new MySessionListener(connector, endPoint);
           }

           class MySessionListener extends HTTP2ServerConnectionFactory.HTTPServerSessionListener {
               public MySessionListener(Connector connector, EndPoint endPoint) {
                   super(connector, endPoint);
               }

               @Override
               public Stream.Listener onNewStream(Stream stream, HeadersFrame frame) {
                   return super.onNewStream(stream, frame);
               }

               @Override
               public void onData(Stream stream, DataFrame frame, Callback callback) {
                    if (goAwayStreamId.compareAndSet(-1, frame.getStreamId())) {
                        // This the first stream we've gotten. Tell the client that this one is okay, but later ones are not.
                        System.out.println("GO AWAY BEFORE " + frame.getStreamId());
                        Session session = stream.getSession();
                        if (session instanceof HTTP2Session) {
                            HTTP2Session http2Session = (HTTP2Session) session;
                            http2Session.frames(null,
                                                callback,
                                                new GoAwayFrame(frame.getStreamId(), 0, "Yikes".getBytes(StandardCharsets.UTF_8)));
                        }
                    } else if (frame.getStreamId() > goAwayStreamId.get()) {
                        // This stream is newer than the go-away stream ID, so RESET it.
                        System.out.println("RESET " + frame.getStreamId());
                        stream.reset(new ResetFrame(frame.getStreamId(), 0), callback);
                        callback.failed(new RuntimeException("Reset!"));
                    }

                    // This stream is allowed, handle it.
                   super.onData(stream, frame, callback);
               }

               class DoNothingCallback implements Callback {}
           }
        });
        http.setPort(httpPort);
        server.addConnector(http);
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }
}