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

package software.amazon.awssdk.custom.s3.transfer.internal;

import java.nio.file.Path;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * A factory capable of creating the {@link AsyncResponseTransformer} to handle
 * each downloaded object part.
 * <p>
 * There is no ordering or synchronization guarantee between {@link
 * #transformerForObjectPart(MultipartDownloadContext)} invocations.
 */
@SdkInternalApi
public interface TransferResponseTransformer {
    /**
     * Create a {@code AsyncResponseTransformer} to handle a single object part
     * for multipart downloads.
     */
    AsyncResponseTransformer<GetObjectResponse, ?> transformerForObjectPart(MultipartDownloadContext context);

    /**
     * Create a {@code AsyncResponseTransformer} to handle an entire object for
     * a single part download.
     */
    AsyncResponseTransformer<GetObjectResponse, ?> transformerForObject(SinglePartDownloadContext context);

    /**
     * @return A factory capable of creating transformers that will write the
     * object to disk. This supports both single and multipart downloads.
     */
    static TransferResponseTransformer forFile(Path file) {
        return new FileTransferResponseTransformer(file);
    }
}
