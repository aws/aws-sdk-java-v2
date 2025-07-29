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

package software.amazon.awssdk.http.apache5.internal.conn;

import java.io.IOException;
import java.net.Socket;
import javax.net.ssl.SSLSocket;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.LayeredConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.protocol.HttpContext;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Adapter to wrap ConnectionSocketFactory as TlsSocketStrategy.
 * Supports both plain and layered (SSL/TLS) socket factories.
 */
@SdkInternalApi
public class ConnectionSocketFactoryToTlsStrategyAdapter implements TlsSocketStrategy {

    private final ConnectionSocketFactory socketFactory;

    public ConnectionSocketFactoryToTlsStrategyAdapter(ConnectionSocketFactory socketFactory) {
        this.socketFactory = Validate.paramNotNull(socketFactory, "socketFactory");
    }

    @Override
    public SSLSocket upgrade(Socket socket,
                             String target,
                             int port,
                             Object attachment,
                             HttpContext context) throws IOException {

        // Only LayeredConnectionSocketFactory can upgrade to SSL
        if (socketFactory instanceof LayeredConnectionSocketFactory) {
            LayeredConnectionSocketFactory layeredFactory = (LayeredConnectionSocketFactory) socketFactory;
            Socket upgradedSocket = layeredFactory.createLayeredSocket(socket, target, port, context);

            if (upgradedSocket == null) {
                throw new IOException("LayeredConnectionSocketFactory.createLayeredSocket returned null");
            }
            if (!(upgradedSocket instanceof SSLSocket)) {
                throw new IOException("LayeredConnectionSocketFactory.createLayeredSocket did not return an SSLSocket. " +
                                      "Returned type: " + upgradedSocket.getClass().getName());
            }

            return (SSLSocket) upgradedSocket;
        }

        // For plain socket factories (like PlainConnectionSocketFactory),
        // we can't upgrade to TLS, but we shouldn't throw an exception
        // Return null to indicate no TLS upgrade is possible/needed
        return null;
    }


}
