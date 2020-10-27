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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.utils.JavaSystemSetting.SSL_KEY_STORE;
import static software.amazon.awssdk.utils.JavaSystemSetting.SSL_KEY_STORE_PASSWORD;
import static software.amazon.awssdk.utils.JavaSystemSetting.SSL_KEY_STORE_TYPE;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.Security;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SystemPropertyTlsKeyManagersProviderTest extends ClientTlsAuthTestBase {
    private static final SystemPropertyTlsKeyManagersProvider PROVIDER = SystemPropertyTlsKeyManagersProvider.create();

    @BeforeClass
    public static void setUp() throws IOException {
        ClientTlsAuthTestBase.setUp();
    }

    @After
    public void methodTeardown() {
        System.clearProperty(SSL_KEY_STORE.property());
        System.clearProperty(SSL_KEY_STORE_TYPE.property());
        System.clearProperty(SSL_KEY_STORE_PASSWORD.property());
    }

    @AfterClass
    public static void teardown() throws IOException {
        ClientTlsAuthTestBase.teardown();
    }

    @Test
    public void propertiesNotSet_returnsNull() {
        assertThat(PROVIDER.keyManagers()).isNull();
    }

    @Test
    public void propertiesSet_createsKeyManager() {
        System.setProperty(SSL_KEY_STORE.property(), clientKeyStore.toAbsolutePath().toString());
        System.setProperty(SSL_KEY_STORE_TYPE.property(), CLIENT_STORE_TYPE);
        System.setProperty(SSL_KEY_STORE_PASSWORD.property(), STORE_PASSWORD);

        assertThat(PROVIDER.keyManagers()).hasSize(1);
    }

    @Test
    public void storeDoesNotExist_returnsNull() {
        System.setProperty(SSL_KEY_STORE.property(), Paths.get("does", "not", "exist").toAbsolutePath().toString());
        System.setProperty(SSL_KEY_STORE_TYPE.property(), CLIENT_STORE_TYPE);
        System.setProperty(SSL_KEY_STORE_PASSWORD.property(), STORE_PASSWORD);

        assertThat(PROVIDER.keyManagers()).isNull();
    }

    @Test
    public void invalidStoreType_returnsNull() {
        System.setProperty(SSL_KEY_STORE.property(), clientKeyStore.toAbsolutePath().toString());
        System.setProperty(SSL_KEY_STORE_TYPE.property(), "invalid");
        System.setProperty(SSL_KEY_STORE_PASSWORD.property(), STORE_PASSWORD);

        assertThat(PROVIDER.keyManagers()).isNull();
    }

    @Test
    public void passwordIncorrect_returnsNull() {
        System.setProperty(SSL_KEY_STORE.property(), clientKeyStore.toAbsolutePath().toString());
        System.setProperty(SSL_KEY_STORE_TYPE.property(), CLIENT_STORE_TYPE);
        System.setProperty(SSL_KEY_STORE_PASSWORD.property(), "not correct password");

        assertThat(PROVIDER.keyManagers()).isNull();
    }

    @Test
    public void customKmfAlgorithmSetInProperty_usesAlgorithm() {
        System.setProperty(SSL_KEY_STORE.property(), clientKeyStore.toAbsolutePath().toString());
        System.setProperty(SSL_KEY_STORE_TYPE.property(), CLIENT_STORE_TYPE);
        System.setProperty(SSL_KEY_STORE_PASSWORD.property(), STORE_PASSWORD);

        assertThat(PROVIDER.keyManagers()).isNotNull();

        String property = "ssl.KeyManagerFactory.algorithm";
        String previousValue = Security.getProperty(property);
        Security.setProperty(property, "some-bogus-value");

        try {
            // This would otherwise be non-null if using the right algorithm,
            // i.e. not setting the algorithm property will cause the assertion
            // to fail
            assertThat(PROVIDER.keyManagers()).isNull();
        } finally {
            Security.setProperty(property, previousValue);
        }
    }
}
