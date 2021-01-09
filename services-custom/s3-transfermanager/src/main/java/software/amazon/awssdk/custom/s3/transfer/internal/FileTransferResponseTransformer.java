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

package software.amazon.awssdk.custom.s3.transfer.internal;

import java.nio.file.Path;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Response transfer for writing objects to disk.
 */
@SdkInternalApi
public final class FileTransferResponseTransformer implements TransferResponseTransformer {
    private final Path path;

    public FileTransferResponseTransformer(Path path) {
        this.path = path;
    }

    @Override
    public AsyncResponseTransformer<GetObjectResponse, ?> transformerForObjectPart(MultipartDownloadContext context) {
        return AsyncResponseTransformer.toFile(path, context.partOffset(), false, false);
    }

    @Override
    public AsyncResponseTransformer<GetObjectResponse, ?> transformerForObject(SinglePartDownloadContext context) {
        return AsyncResponseTransformer.toFile(path);
    }
}
