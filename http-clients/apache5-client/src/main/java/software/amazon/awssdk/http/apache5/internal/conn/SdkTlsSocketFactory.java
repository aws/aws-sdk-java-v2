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
import java.util.Arrays;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.core5.http.protocol.HttpContext;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.apache5.internal.net.SdkSslSocket;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public class SdkTlsSocketFactory extends DefaultClientTlsStrategy {

    private static final Logger log = Logger.loggerFor(SdkTlsSocketFactory.class);

    public SdkTlsSocketFactory(SSLContext sslContext, HostnameVerifier hostnameVerifier) {
        super(sslContext, HostnameVerificationPolicy.CLIENT, hostnameVerifier);
        if (sslContext == null) {
            throw new IllegalArgumentException(
                "sslContext must not be null. Use SSLContext.getDefault() if you are unsure.");
        }
    }

    @Override
    protected void initializeSocket(SSLSocket socket) {
        super.initializeSocket(socket);
        log.debug(() -> String.format("socket.getSupportedProtocols(): %s, socket.getEnabledProtocols(): %s",
                                      Arrays.toString(socket.getSupportedProtocols()),
                                      Arrays.toString(socket.getEnabledProtocols())));
    }

    @Override
    public SSLSocket upgrade(Socket socket,
                             String target,
                             int port,
                             Object attachment,
                             HttpContext context) throws IOException {
        log.trace(() -> String.format("Upgrading socket to TLS for %s:%s", target, port));

        SSLSocket upgradedSocket = super.upgrade(socket, target, port, attachment, context);

        // Wrap the upgraded SSLSocket in SdkSSLSocket for logging
        return new SdkSslSocket(upgradedSocket);
    }

}
