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

import java.nio.file.Path;
import javax.net.ssl.KeyManager;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.internal.http.AbstractFileStoreTlsKeyManagersProvider;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Implementation of {@link FileStoreTlsKeyManagersProvider} that loads a the
 * key store from a file.
 */
@SdkPublicApi
public final class FileStoreTlsKeyManagersProvider extends AbstractFileStoreTlsKeyManagersProvider {
    private static final Logger log = Logger.loggerFor(FileStoreTlsKeyManagersProvider.class);

    private final Path storePath;
    private final String storeType;
    private final char[] password;

    private FileStoreTlsKeyManagersProvider(Path storePath, String storeType, char[] password) {
        this.storePath = Validate.paramNotNull(storePath, "storePath");
        this.storeType = Validate.paramNotBlank(storeType, "storeType");
        this.password = password;
    }

    @Override
    public KeyManager[] keyManagers() {
        try {
            return createKeyManagers(storePath, storeType, password);
        } catch (Exception e) {
            log.warn(() -> String.format("Unable to create KeyManagers from file %s", storePath), e);
            return null;
        }
    }

    public static FileStoreTlsKeyManagersProvider create(Path path, String type, String password) {
        char[] passwordChars = password != null ? password.toCharArray() : null;
        return new FileStoreTlsKeyManagersProvider(path, type, passwordChars);
    }
}
