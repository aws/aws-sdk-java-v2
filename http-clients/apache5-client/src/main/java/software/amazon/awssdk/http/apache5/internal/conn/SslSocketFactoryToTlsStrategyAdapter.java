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
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.protocol.HttpContext;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Adapter to wrap legacy SSLConnectionSocketFactory as TlsSocketStrategy
 */
@SdkInternalApi
public class SslSocketFactoryToTlsStrategyAdapter implements TlsSocketStrategy {

    private final SSLConnectionSocketFactory legacySocketFactory;

    public SslSocketFactoryToTlsStrategyAdapter(SSLConnectionSocketFactory legacySocketFactory) {
        this.legacySocketFactory = legacySocketFactory;
    }

    @Override
    public SSLSocket upgrade(Socket socket,
                             String target,
                             int port,
                             Object attachment,
                             HttpContext context) throws IOException {
        Socket layeredSocket = legacySocketFactory.createLayeredSocket(socket, target, port, context);

        if (!(layeredSocket instanceof SSLSocket)) {
            throw new IOException("SSLConnectionSocketFactory.createLayeredSocket did not return an SSLSocket. " +
                                  "Returned type: " + layeredSocket.getClass().getName());
        }

        return (SSLSocket) layeredSocket;
    }
}
