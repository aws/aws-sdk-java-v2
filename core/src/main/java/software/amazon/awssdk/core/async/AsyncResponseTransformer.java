/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.async;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import software.amazon.awssdk.core.ResponseBytes;

/**
 * Callback interface to handle a streaming asynchronous response.
 *
 * @param <ResponseT> POJO response type.
 * @param <ReturnT>   Type this response handler produces. I.E. the type you are transforming the response into.
 */
public interface AsyncResponseTransformer<ResponseT, ReturnT>
    extends BaseAsyncResponseTransformer<ResponseT, ByteBuffer, ReturnT> {

    /**
     * Creates an {@link AsyncResponseTransformer} that writes all the content to the given file. In the event of an error,
     * the SDK will attempt to delete the file (whatever has been written to it so far). If the file already exists, an
     * exception will be thrown.
     *
     * @param path        Path to file to write to.
     * @param <ResponseT> Pojo Response type.
     * @return AsyncResponseTransformer instance.
     */
    static <ResponseT> AsyncResponseTransformer<ResponseT, ResponseT> toFile(Path path) {
        return new FileAsyncResponseTransformer<>(path);
    }

    /**
     * Creates an {@link AsyncResponseTransformer} that writes all the content to the given file. In the event of an error,
     * the SDK will attempt to delete the file (whatever has been written to it so far). If the file already exists, an
     * exception will be thrown.
     *
     * @param file        File to write to.
     * @param <ResponseT> Pojo Response type.
     * @return AsyncResponseTransformer instance.
     */
    static <ResponseT> AsyncResponseTransformer<ResponseT, ResponseT> toFile(File file) {
        return toFile(file.toPath());
    }

    /**
     * Creates an {@link AsyncResponseTransformer} that writes all content to a byte array.
     *
     * @param <ResponseT> Pojo response type.
     * @return AsyncResponseTransformer instance.
     */
    static <ResponseT> AsyncResponseTransformer<ResponseT, ResponseBytes<ResponseT>> toBytes() {
        return new ByteArrayAsyncResponseTransformer<>();
    }
}
