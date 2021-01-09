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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.AfterClass;
import org.junit.BeforeClass;

abstract class ClientTlsAuthTestBase {
    protected static final String STORE_PASSWORD = "password";
    protected static final String CLIENT_STORE_TYPE = "pkcs12";
    protected static final String TEST_KEY_STORE = "/software/amazon/awssdk/http/server-keystore";
    protected static final String CLIENT_KEY_STORE = "/software/amazon/awssdk/http/client1.p12";

    protected static Path tempDir;
    protected static Path serverKeyStore;
    protected static Path clientKeyStore;

    @BeforeClass
    public static void setUp() throws IOException {
        tempDir = Files.createTempDirectory(ClientTlsAuthTestBase.class.getSimpleName());
        copyCertsToTmpDir();
    }

    @AfterClass
    public static void teardown() throws IOException {
        Files.deleteIfExists(serverKeyStore);
        Files.deleteIfExists(clientKeyStore);
        Files.deleteIfExists(tempDir);
    }

    private static void copyCertsToTmpDir() throws IOException {
        InputStream sksStream = ClientTlsAuthTestBase.class.getResourceAsStream(TEST_KEY_STORE);
        Path sks = copyToTmpDir(sksStream, "server-keystore");

        InputStream cksStream = ClientTlsAuthTestBase.class.getResourceAsStream(CLIENT_KEY_STORE);
        Path cks = copyToTmpDir(cksStream, "client1.p12");

        serverKeyStore = sks;
        clientKeyStore = cks;
    }

    private static Path copyToTmpDir(InputStream srcStream, String name) throws IOException {
        Path dst = tempDir.resolve(name);
        Files.copy(srcStream, dst);
        return dst;
    }
}
