/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.apache.internal.conn;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.apache.internal.net.SdkSocket;
import software.amazon.awssdk.http.apache.internal.net.SdkSslSocket;

/**
 * Used to enforce the preferred TLS protocol during SSL handshake.
 */
public class SdkTlsSocketFactory extends SSLConnectionSocketFactory {

    private static final Logger log = LoggerFactory.getLogger(SdkTlsSocketFactory.class);
    private final SSLContext sslContext;

    public SdkTlsSocketFactory(final SSLContext sslContext, final HostnameVerifier hostnameVerifier) {
        super(sslContext, hostnameVerifier);
        if (sslContext == null) {
            throw new IllegalArgumentException(
                    "sslContext must not be null. " + "Use SSLContext.getDefault() if you are unsure.");
        }
        this.sslContext = sslContext;
    }

    /**
     * {@inheritDoc} Used to enforce the preferred TLS protocol during SSL handshake.
     */
    @Override
    protected final void prepareSocket(final SSLSocket socket) {
        String[] supported = socket.getSupportedProtocols();
        String[] enabled = socket.getEnabledProtocols();
        if (log.isDebugEnabled()) {
            log.debug("socket.getSupportedProtocols(): {}, socket.getEnabledProtocols(): {}",
                      Arrays.toString(supported),
                      Arrays.toString(enabled));
        }
        List<String> target = new ArrayList<String>();
        if (supported != null) {
            // Append the preferred protocols in descending order of preference
            // but only do so if the protocols are supported
            TlsProtocol[] values = TlsProtocol.values();
            for (int i = 0; i < values.length; i++) {
                final String pname = values[i].getProtocolName();
                if (existsIn(pname, supported)) {
                    target.add(pname);
                }
            }
        }
        if (enabled != null) {
            // Append the rest of the already enabled protocols to the end
            // if not already included in the list
            for (String pname : enabled) {
                if (!target.contains(pname)) {
                    target.add(pname);
                }
            }
        }
        if (target.size() > 0) {
            String[] enabling = target.toArray(new String[target.size()]);
            socket.setEnabledProtocols(enabling);
            if (log.isDebugEnabled()) {
                log.debug("TLS protocol enabled for SSL handshake: {}", Arrays.toString(enabling));
            }
        }
    }

    /**
     * Returns true if the given element exists in the given array; false otherwise.
     */
    private boolean existsIn(String element, String[] a) {
        for (String s : a) {
            if (element.equals(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Socket connectSocket(
            final int connectTimeout,
            final Socket socket,
            final HttpHost host,
            final InetSocketAddress remoteAddress,
            final InetSocketAddress localAddress,
            final HttpContext context) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Connecting to {}:{}", remoteAddress.getAddress(), remoteAddress.getPort());
        }

        Socket connectedSocket = super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);

        if (connectedSocket instanceof SSLSocket) {
            return new SdkSslSocket((SSLSocket) connectedSocket);
        }

        return new SdkSocket(connectedSocket);
    }

}
