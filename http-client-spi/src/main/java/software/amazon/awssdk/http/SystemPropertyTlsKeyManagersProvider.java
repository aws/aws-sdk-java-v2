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

package software.amazon.awssdk.http;

import static software.amazon.awssdk.utils.JavaSystemSetting.SSL_KEY_STORE;
import static software.amazon.awssdk.utils.JavaSystemSetting.SSL_KEY_STORE_PASSWORD;
import static software.amazon.awssdk.utils.JavaSystemSetting.SSL_KEY_STORE_TYPE;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Optional;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.internal.http.AbstractFileStoreTlsKeyManagersProvider;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.internal.SystemSettingUtils;

/**
 * Implementation of {@link TlsKeyManagersProvider} that gets the information
 * about the KeyStore to load from the system properties.
 * <p>
 * This provider checks the standard {@code javax.net.ssl.keyStore},
 * {@code javax.net.ssl.keyStorePassword}, and
 * {@code javax.net.ssl.keyStoreType} properties defined by the
 * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html">JSSE</a>.
 * <p>
 * This uses {@link KeyManagerFactory#getDefaultAlgorithm()} to determine the
 * {@code KeyManagerFactory} algorithm to use.
 */
@SdkPublicApi
public final class SystemPropertyTlsKeyManagersProvider extends AbstractFileStoreTlsKeyManagersProvider {
    private static final Logger log = Logger.loggerFor(SystemPropertyTlsKeyManagersProvider.class);

    private SystemPropertyTlsKeyManagersProvider() {
    }

    @Override
    public KeyManager[] keyManagers() {
        return getKeyStore().map(p -> {
            Path path = Paths.get(p);
            String type = getKeyStoreType();
            char[] password = getKeyStorePassword().map(String::toCharArray).orElse(null);
            try {
                return createKeyManagers(path, type, password);
            } catch (Exception e) {
                log.warn(() -> String.format("Unable to create KeyManagers from %s property value '%s'",
                                             SSL_KEY_STORE.property(), p), e);
                return null;
            }
        }).orElse(null);
    }

    public static SystemPropertyTlsKeyManagersProvider create() {
        return new SystemPropertyTlsKeyManagersProvider();
    }

    private static Optional<String> getKeyStore() {
        return SystemSettingUtils.resolveSetting(SSL_KEY_STORE);
    }

    private static String getKeyStoreType() {
        return SystemSettingUtils.resolveSetting(SSL_KEY_STORE_TYPE)
                .orElse(KeyStore.getDefaultType());
    }

    private static Optional<String> getKeyStorePassword() {
        return SystemSettingUtils.resolveSetting(SSL_KEY_STORE_PASSWORD);
    }
}
