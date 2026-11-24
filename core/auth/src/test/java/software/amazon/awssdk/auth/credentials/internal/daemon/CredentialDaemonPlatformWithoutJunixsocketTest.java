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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static software.amazon.awssdk.auth.credentials.internal.daemon.CredentialDaemonPlatform.JDK_ADDRESS_CLASS;
import static software.amazon.awssdk.auth.credentials.internal.daemon.CredentialDaemonPlatform.JUNIXSOCKET_CLASS;
import static software.amazon.awssdk.auth.credentials.internal.daemon.CredentialDaemonPlatformTest.MISSING_ADDRESS_CLASS;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.CredentialDaemonException;
import software.amazon.awssdk.auth.credentials.FakeCredentialDaemon;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Loads the daemon classes in a class loader that cannot see junixsocket, as in an application that did not add it. Every
 * other test runs with junixsocket on the class path, because it is an optional dependency of this module.
 */
class CredentialDaemonPlatformWithoutJunixsocketTest {
    @Test
    void ensureSupported_beforeJava16WithoutJunixsocket_raisesUnsupportedPlatformNamingJunixsocket() throws Exception {
        Object platform = platformWithoutJunixsocket(MISSING_ADDRESS_CLASS);

        assertThatThrownBy(() -> invoke(platform, "ensureSupported"))
            .isInstanceOfSatisfying(CredentialDaemonException.class, e -> {
                assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.UNSUPPORTED_PLATFORM);
                assertThat(e.getMessage()).contains("junixsocket is not on the class path")
                                          .contains("add a dependency on com.kohlschutter.junixsocket:junixsocket-core");
            });
    }

    @Test
    void openChannel_onJava16WithoutJunixsocket_usesTheJavaRuntime() throws Throwable {
        assumeTrue(FakeCredentialDaemon.isSupported() && FakeCredentialDaemon.hasJdkUnixDomainSockets());
        Object platform = platformWithoutJunixsocket(JDK_ADDRESS_CLASS);

        try (SocketChannel channel = (SocketChannel) invoke(platform, "openChannel")) {
            assertThat(channel.getClass().getName()).doesNotStartWith("org.newsclub.");
        }
    }

    private static Object platformWithoutJunixsocket(String addressClass) throws Exception {
        ClassLoader loader = new WithoutJunixsocketClassLoader(CredentialDaemonPlatform.class.getClassLoader());
        Class<?> platformClass = Class.forName(CredentialDaemonPlatform.class.getName(), true, loader);
        assertThat(platformClass).isNotSameAs(CredentialDaemonPlatform.class);
        Constructor<?> constructor = platformClass.getDeclaredConstructor(String.class, String.class, String.class,
                                                                         String.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(System.getProperty("os.name"), System.getProperty("java.vm.name"),
                                       System.getProperty("java.vendor"), addressClass, JUNIXSOCKET_CLASS);
    }

    private static Object invoke(Object target, String method) throws Throwable {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * Defines this package's main classes itself, so that they resolve their references through this loader, and refuses
     * junixsocket's classes. Every other class comes from the parent.
     */
    private static final class WithoutJunixsocketClassLoader extends ClassLoader {
        private static final String DAEMON_PACKAGE = CredentialDaemonPlatform.class.getPackage().getName() + ".";

        WithoutJunixsocketClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("org.newsclub.")) {
                throw new ClassNotFoundException(name);
            }
            if (!name.startsWith(DAEMON_PACKAGE) || name.contains("Test")) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    byte[] bytes = classBytes(name);
                    loaded = defineClass(name, bytes, 0, bytes.length);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

        private byte[] classBytes(String name) throws ClassNotFoundException {
            try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                if (in == null) {
                    throw new ClassNotFoundException(name);
                }
                return IoUtils.toByteArray(in);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
