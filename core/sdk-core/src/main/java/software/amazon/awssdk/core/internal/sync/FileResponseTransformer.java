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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.internal.http.InterruptMonitor;
import software.amazon.awssdk.core.internal.util.FileDestinationPreflight;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.utils.Logger;

/**
 * Writes all response content to a file that must not already exist.
 *
 * @param <ResponseT> Type of unmarshalled response POJO.
 */
@SdkInternalApi
public final class FileResponseTransformer<ResponseT> implements ResponseTransformer<ResponseT, ResponseT> {

    private static final Logger log = Logger.loggerFor(FileResponseTransformer.class);

    private final Path path;

    public FileResponseTransformer(Path path) {
        this.path = path;
    }

    /**
     * Wrapped as {@link #transform} would wrap it, so a caller sees the same exception wherever the rejection happens.
     */
    public void validateBeforeRequest() throws IOException {
        try {
            FileDestinationPreflight.validateCreateNew(path);
        } catch (IOException e) {
            throw new IOException(ResponseTransformer.copyErrorMessage(path, e), e);
        }
    }

    @Override
    public ResponseT transform(ResponseT response, AbortableInputStream inputStream) throws Exception {
        try {
            InterruptMonitor.checkInterrupted();
            Files.copy(inputStream, path);
            return response;
        } catch (IOException copyException) {
            String copyError = ResponseTransformer.copyErrorMessage(path, copyException);

            if (ResponseTransformer.shouldThrowIOException(copyException)) {
                throw new IOException(copyError, copyException);
            }

            // Try to clean up the file so that we can retry the request. If we can't delete it, don't retry the request.
            try {
                Files.deleteIfExists(path);
            } catch (IOException deletionException) {
                log.error(() -> "Failed to delete destination file '" + path + "' after reading the service response "
                                + "failed.", deletionException);

                throw new IOException(copyError + ". Additionally, the file could not be cleaned up ("
                                      + deletionException.getMessage() + "), so the request will not be retried.",
                                      copyException);
            }

            // Retry the request
            throw RetryableException.builder().message(copyError).cause(copyException).build();
        }
    }

    @Override
    public String name() {
        return ResponseTransformer.TransformerType.FILE.getName();
    }
}
