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
import java.util.Locale;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.CredentialDaemonException;
import software.amazon.awssdk.utils.JavaSystemSetting;

/**
 * Detects whether this platform can reach the credential daemon, and opens Unix domain socket channels to it.
 *
 * <p>
 * The daemon is supported on Linux and macOS only. The JDK's Unix domain socket channels are used when the Java runtime has
 * them (Java 16 or later). Otherwise junixsocket is used if the application added it to the class path, which lets Java 8
 * to 15 reach the daemon. Without either, on Android (detected from the VM name and vendor) and on other operating systems,
 * {@link #ensureSupported()} raises {@link CredentialDaemonException#UNSUPPORTED_PLATFORM}.
 */
@SdkInternalApi
public final class CredentialDaemonPlatform {
    static final String JDK_ADDRESS_CLASS = "java.net.UnixDomainSocketAddress";
    static final String JUNIXSOCKET_CLASS = "org.newsclub.net.unix.AFUNIXSocketChannel";
    static final String JUNIXSOCKET_ARTIFACT = "com.kohlschutter.junixsocket:junixsocket-core";

    private final String unsupportedReason;
    private final UnixDomainSockets sockets;

    /**
     * @param addressClassName the JDK's Unix domain socket address class. Tests pass a missing class to simulate Java 8 to 15.
     * @param junixsocketClassName a junixsocket class. Tests pass a missing class to simulate an application without
     * junixsocket.
     */
    @SdkTestInternalApi
    CredentialDaemonPlatform(String osName, String vmName, String vendor, String addressClassName,
                             String junixsocketClassName) {
        String reason = null;
        UnixDomainSockets found = null;

        if (OperatingSystem.of(osName) == OperatingSystem.UNSUPPORTED) {
            reason = "the operating system '" + osName + "' is not supported";
        } else if (contains(vmName, "dalvik") || contains(vendor, "android")) {
            reason = "Android is not supported";
        } else {
            found = JdkUnixDomainSockets.find(addressClassName);
            if (found == null) {
                if (!isPresent(junixsocketClassName)) {
                    reason = "this Java runtime has no Unix domain socket support, and junixsocket is not on the class path. "
                             + "To use the credential daemon on Java 8 to 15, add a dependency on " + JUNIXSOCKET_ARTIFACT
                             + " (type pom)";
                } else if (!junixsocketSupported()) {
                    reason = "junixsocket is on the class path, but its native library could not be loaded on this platform";
                } else {
                    found = new JunixsocketUnixDomainSockets();
                }
            }
        }

        this.unsupportedReason = reason;
        this.sockets = found;
    }

    /**
     * Returns the platform this JVM runs on.
     */
    public static CredentialDaemonPlatform current() {
        return DefaultHolder.INSTANCE;
    }

    /**
     * Throws {@link CredentialDaemonException#UNSUPPORTED_PLATFORM} if this platform cannot reach the credential daemon.
     */
    public void ensureSupported() {
        if (unsupportedReason != null) {
            throw unsupported(unsupportedReason, null);
        }
    }

    /**
     * Opens a blocking Unix domain socket channel.
     */
    public SocketChannel openChannel() throws IOException {
        ensureSupported();
        return sockets.openChannel();
    }

    /**
     * Returns the Unix domain socket address of the path.
     *
     * @throws IOException or {@link java.nio.file.InvalidPathException} if the path cannot be used as a socket address.
     */
    public SocketAddress address(Path path) throws IOException {
        ensureSupported();
        return sockets.address(path);
    }

    static CredentialDaemonException unsupported(String reason, Throwable cause) {
        String message = "The credential daemon is reached over Unix domain sockets, which need Linux or macOS, and either "
                         + "Java 16 or later or the junixsocket library, but " + reason + ". See "
                         + CredentialDaemonUtils.CREDENTIAL_DAEMON_DOC + ".";
        return CredentialDaemonUtils.error(CredentialDaemonException.UNSUPPORTED_PLATFORM, message, cause);
    }

    /**
     * Looks for the class with this class's own loader, not the thread's context loader, because that is the loader that
     * resolves junixsocket for {@link JunixsocketUnixDomainSockets}.
     */
    private static boolean isPresent(String className) {
        try {
            Class.forName(className, false, CredentialDaemonPlatform.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static boolean junixsocketSupported() {
        try {
            return JunixsocketUnixDomainSockets.isSupported();
        } catch (LinkageError e) {
            return false;
        }
    }

    private static boolean contains(String value, String part) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(part);
    }

    private enum OperatingSystem {
        LINUX,
        MACOS,
        UNSUPPORTED;

        static OperatingSystem of(String osName) {
            String name = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
            if (name.startsWith("linux")) {
                return LINUX;
            }
            if (name.startsWith("mac") || name.startsWith("darwin")) {
                return MACOS;
            }
            return UNSUPPORTED;
        }
    }

    private static final class DefaultHolder {
        private static final CredentialDaemonPlatform INSTANCE =
            new CredentialDaemonPlatform(JavaSystemSetting.OS_NAME.getStringValue().orElse(null),
                                          JavaSystemSetting.JAVA_VM_NAME.getStringValue().orElse(null),
                                          JavaSystemSetting.JAVA_VENDOR.getStringValue().orElse(null),
                                          JDK_ADDRESS_CLASS,
                                          JUNIXSOCKET_CLASS);
    }
}
