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

package software.amazon.awssdk.core.internal.sync;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;

/**
 * {@link ContentStreamProvider} implementation for files.
 */
@SdkInternalApi
public final class FileContentStreamProvider implements ContentStreamProvider {
    private final Path filePath;
    private InputStream currentStream;

    public FileContentStreamProvider(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public InputStream newStream() {
        closeCurrentStream();
        currentStream = invokeSafely(() -> Files.newInputStream(filePath));
        return currentStream;
    }

    private void closeCurrentStream() {
        if (currentStream != null) {
            invokeSafely(currentStream::close);
            currentStream = null;
        }
    }
}
