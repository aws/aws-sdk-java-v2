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
import java.util.Locale;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.CredentialDaemonException;
import software.amazon.awssdk.utils.JavaSystemSetting;

/**
 * Detects whether this platform can reach the credential daemon, and opens Unix domain socket channels to it.
 *
 * <p>
 * The daemon is supported on Linux and macOS only. Unix domain socket channels ({@code java.net.UnixDomainSocketAddress} and
 * {@code SocketChannel.open(StandardProtocolFamily.UNIX)}) need Java 16, and the SDK is compiled for Java 8, so this class
 * finds them through reflection. On Java 8 to 15, on Android (detected from the VM name and vendor) and on other operating
 * systems, {@link #ensureSupported()} raises {@link CredentialDaemonException#UNSUPPORTED_PLATFORM}.
 */
@SdkInternalApi
public final class CredentialDaemonPlatform {
    private static final String ADDRESS_CLASS = "java.net.UnixDomainSocketAddress";

    private final String unsupportedReason;
    private final Method addressOf;
    private final Method openChannel;
    private final ProtocolFamily unixFamily;

    @SdkTestInternalApi
    CredentialDaemonPlatform(String osName, String vmName, String vendor, String addressClassName) {
        OperatingSystem os = OperatingSystem.of(osName);
        String reason = null;
        Method of = null;
        Method open = null;
        ProtocolFamily family = null;

        if (os == OperatingSystem.UNSUPPORTED) {
            reason = "the operating system '" + osName + "' is not supported";
        } else if (contains(vmName, "dalvik") || contains(vendor, "android")) {
            reason = "Android is not supported";
        } else {
            try {
                of = Class.forName(addressClassName).getMethod("of", Path.class);
                open = SocketChannel.class.getMethod("open", ProtocolFamily.class);
                family = StandardProtocolFamily.valueOf("UNIX");
            } catch (ReflectiveOperationException | IllegalArgumentException | UnsupportedOperationException
                     | SecurityException | LinkageError e) {
                reason = "this Java runtime has no Unix domain socket support";
                of = null;
                open = null;
                family = null;
            }
        }

        this.unsupportedReason = reason;
        this.addressOf = of;
        this.openChannel = open;
        this.unixFamily = family;
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
        return (SocketChannel) invoke(openChannel, unixFamily);
    }

    /**
     * Returns the Unix domain socket address of the path.
     *
     * @throws java.nio.file.InvalidPathException if the path cannot be used as a socket address.
     */
    public SocketAddress address(Path path) {
        ensureSupported();
        try {
            return (SocketAddress) invoke(addressOf, path);
        } catch (IOException e) {
            throw unsupported("this Java runtime cannot create Unix domain socket addresses", e);
        }
    }

    /**
     * Calls a static method found by reflection. An {@link IOException} or {@link RuntimeException} it throws is rethrown as
     * is, except {@link UnsupportedOperationException}, and every other failure is raised as
     * {@link CredentialDaemonException#UNSUPPORTED_PLATFORM}.
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
        throw unsupported("this Java runtime cannot use Unix domain sockets", failure);
    }

    private static boolean contains(String value, String part) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(part);
    }

    private static CredentialDaemonException unsupported(String reason, Throwable cause) {
        String message = "The credential daemon is reached over Unix domain sockets, which require Linux or macOS and Java 16 "
                         + "or later, but " + reason + ". See " + CredentialDaemonUtils.CREDENTIAL_DAEMON_DOC + ".";
        return CredentialDaemonUtils.error(CredentialDaemonException.UNSUPPORTED_PLATFORM, message, cause);
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
                                          ADDRESS_CLASS);
    }
}
