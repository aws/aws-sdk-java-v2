/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileStoreTlsKeyManagersProviderTest extends ClientTlsAuthTestBase {

    @BeforeClass
    public static void setUp() throws IOException {
        ClientTlsAuthTestBase.setUp();
    }

    @AfterClass
    public static void teardown() throws IOException {
        ClientTlsAuthTestBase.teardown();
    }

    @Test(expected = NullPointerException.class)
    public void storePathNull_throwsValidationException() {
        FileStoreTlsKeyManagersProvider.create(null, CLIENT_STORE_TYPE, STORE_PASSWORD);
    }

    @Test(expected = NullPointerException.class)
    public void storeTypeNull_throwsValidationException() {
        FileStoreTlsKeyManagersProvider.create(clientKeyStore, null, STORE_PASSWORD);
    }

    @Test(expected = IllegalArgumentException.class)
    public void storeTypeEmpty_throwsValidationException() {
        FileStoreTlsKeyManagersProvider.create(clientKeyStore, "", STORE_PASSWORD);
    }

    @Test
    public void passwordNotGiven_doesNotThrowValidationException() {
        FileStoreTlsKeyManagersProvider.create(clientKeyStore, CLIENT_STORE_TYPE, null);
    }

    @Test
    public void paramsValid_createsKeyManager() {
        FileStoreTlsKeyManagersProvider provider = FileStoreTlsKeyManagersProvider.create(clientKeyStore, CLIENT_STORE_TYPE, STORE_PASSWORD);
        assertThat(provider.keyManagers()).hasSize(1);
    }

    @Test
    public void storeDoesNotExist_returnsNull() {
        FileStoreTlsKeyManagersProvider provider = FileStoreTlsKeyManagersProvider.create(Paths.get("does", "not", "exist"), CLIENT_STORE_TYPE, STORE_PASSWORD);
        assertThat(provider.keyManagers()).isNull();
    }

    @Test
    public void invalidStoreType_returnsNull() {
        FileStoreTlsKeyManagersProvider provider = FileStoreTlsKeyManagersProvider.create(clientKeyStore, "invalid", STORE_PASSWORD);
        assertThat(provider.keyManagers()).isNull();
    }

    @Test
    public void passwordIncorrect_returnsNull() {
        FileStoreTlsKeyManagersProvider provider = FileStoreTlsKeyManagersProvider.create(clientKeyStore, CLIENT_STORE_TYPE, "not correct password");
        assertThat(provider.keyManagers()).isNull();
    }
}
