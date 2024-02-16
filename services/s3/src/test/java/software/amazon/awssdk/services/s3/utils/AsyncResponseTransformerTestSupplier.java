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

package software.amazon.awssdk.services.s3.utils;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.internal.async.FileAsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Contains the {@link AsyncResponseTransformer} to be used in a test as well as logic on how to
 * retrieve the body content of the request for that specific transformer.
 * @param <T> the type returned of the future associated with the {@link AsyncResponseTransformer}
 */
public interface AsyncResponseTransformerTestSupplier<T> {

    /**
     * Call this method to retrieve the AsyncResponseTransformer required to perform the test
     * @param path
     * @return
     */
    AsyncResponseTransformer<GetObjectResponse, T> transformer(Path path);

    /**
     * Implementation of this method whould retreive the whole body of the request made using the AsyncResponseTransformer
     * as a byte array.
     * @param response the response the {@link AsyncResponseTransformerTestSupplier#transformer}
     * @return
     */
    byte[] body(T response);

    /**
     * Sonce {@link FileAsyncResponseTransformer} works with file, some test might need to initialize an in-memory
     * {@link FileSystem} with jimfs.
     * @return true if the test using this class requires setup with jimfs
     */
    default boolean requiresJimfs() {
        return false;
    }

}
