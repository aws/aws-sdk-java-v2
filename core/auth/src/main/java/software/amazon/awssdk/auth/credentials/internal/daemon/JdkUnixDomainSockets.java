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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Unix domain sockets from the JDK: {@code java.net.UnixDomainSocketAddress} and
 * {@code SocketChannel.open(StandardProtocolFamily.UNIX)}. They need Java 16, and the SDK is compiled for Java 8, so this
 * class finds them through reflection.
 */
@SdkInternalApi
final class JdkUnixDomainSockets implements UnixDomainSockets {
    private final Method addressOf;
    private final Method openChannel;
    private final ProtocolFamily unixFamily;

    private JdkUnixDomainSockets(Method addressOf, Method openChannel, ProtocolFamily unixFamily) {
        this.addressOf = addressOf;
        this.openChannel = openChannel;
        this.unixFamily = unixFamily;
    }

    /**
     * Returns the JDK's Unix domain sockets, or null if this Java runtime has none, as before Java 16.
     *
     * @param addressClassName the name of the JDK's Unix domain socket address class. Tests pass a missing class to simulate
     * an older runtime.
     */
    static JdkUnixDomainSockets find(String addressClassName) {
        try {
            return new JdkUnixDomainSockets(Class.forName(addressClassName).getMethod("of", Path.class),
                                            SocketChannel.class.getMethod("open", ProtocolFamily.class),
                                            StandardProtocolFamily.valueOf("UNIX"));
        } catch (ReflectiveOperationException | IllegalArgumentException | UnsupportedOperationException
                 | SecurityException | LinkageError e) {
            return null;
        }
    }

    @Override
    public SocketChannel openChannel() throws IOException {
        return (SocketChannel) invoke(openChannel, unixFamily);
    }

    @Override
    public SocketAddress address(Path path) throws IOException {
        return (SocketAddress) invoke(addressOf, path);
    }

    /**
     * Calls a static method found by reflection. An {@link IOException} or {@link RuntimeException} it throws is rethrown as
     * is, except {@link UnsupportedOperationException}, and every other failure is raised as
     * {@link software.amazon.awssdk.auth.credentials.CredentialDaemonException#UNSUPPORTED_PLATFORM}.
     */
    private static Object invoke(Method method, Object... args) throws IOException {
        Throwable failure;
        try {
            return method.invoke(null, args);
        } catch (InvocationTargetException e) {
            failure = e.getCause();
            if (failure instanceof IOException) {
                throw (IOException) failure;
            }
            if (failure instanceof RuntimeException && !(failure instanceof UnsupportedOperationException)) {
                throw (RuntimeException) failure;
            }
        } catch (IllegalAccessException e) {
            failure = e;
        }
        throw CredentialDaemonPlatform.unsupported("this Java runtime cannot use Unix domain sockets", failure);
    }
}
