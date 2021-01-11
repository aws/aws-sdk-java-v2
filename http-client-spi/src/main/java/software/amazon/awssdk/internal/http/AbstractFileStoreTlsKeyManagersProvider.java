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

package software.amazon.awssdk.internal.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.TlsKeyManagersProvider;

/**
 * Abstract {@link TlsKeyManagersProvider} that loads the key store from a
 * a given file path.
 * <p>
 * This uses {@link KeyManagerFactory#getDefaultAlgorithm()} to determine the
 * {@code KeyManagerFactory} algorithm to use.
 */
@SdkInternalApi
public abstract class AbstractFileStoreTlsKeyManagersProvider implements TlsKeyManagersProvider {

    protected final KeyManager[] createKeyManagers(Path storePath, String storeType, char[] password)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException {
        KeyStore ks = createKeyStore(storePath, storeType, password);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password);
        return kmf.getKeyManagers();
    }

    private KeyStore createKeyStore(Path storePath, String storeType, char[] password)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore ks = KeyStore.getInstance(storeType);
        try (InputStream storeIs = Files.newInputStream(storePath)) {
            ks.load(storeIs, password);
            return ks;
        }
    }
}
