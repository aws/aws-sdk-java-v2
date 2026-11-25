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

package software.amazon.awssdk.auth.credentials.internal.daemon;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.newsclub.net.unix.AFUNIXSocketChannel;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Unix domain sockets from junixsocket, an optional dependency for Java 8 to 15.
 *
 * <p>
 * This is the only class that refers to junixsocket. {@link CredentialDaemonPlatform} loads it only after it has found
 * junixsocket, because verifying this class loads junixsocket's classes, and that fails when they are missing.
 */
@SdkInternalApi
final class JunixsocketUnixDomainSockets implements UnixDomainSockets {
    /**
     * Returns whether junixsocket's native library loaded for this platform.
     */
    static boolean isSupported() {
        return AFUNIXSocket.isSupported();
    }

    @Override
    public SocketChannel openChannel() throws IOException {
        try {
            return AFUNIXSocketChannel.open();
        } catch (LinkageError e) {
            throw incompatible(e);
        }
    }

    @Override
    public SocketAddress address(Path path) throws IOException {
        try {
            return AFUNIXSocketAddress.of(path);
        } catch (LinkageError e) {
            throw incompatible(e);
        }
    }

    private static RuntimeException incompatible(LinkageError e) {
        return CredentialDaemonPlatform.unsupported("the junixsocket library on the class path could not be used (" + e
                                                    + "). Update " + CredentialDaemonPlatform.JUNIXSOCKET_ARTIFACT
                                                    + " to the version this SDK release is tested with",
                                                    e);
    }
}
