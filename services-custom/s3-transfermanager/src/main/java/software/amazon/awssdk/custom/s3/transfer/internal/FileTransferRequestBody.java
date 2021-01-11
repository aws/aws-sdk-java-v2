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

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.Validate.paramNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.internal.async.FileAsyncRequestBody;

/**
 * Request transfer from a file
 */
@SdkInternalApi
public final class FileTransferRequestBody implements TransferRequestBody {
    private final Path path;

    FileTransferRequestBody(Path path) {
        this.path = paramNotNull(path, "path");
    }

    @Override
    public long contentLength() {
        return invokeSafely(() -> Files.size(path));
    }

    @Override
    public AsyncRequestBody requestBodyForPart(MultipartUploadContext context) {
        return FileAsyncRequestBody.builder()
                                   .path(path)
                                   .position(context.partOffset())
                                   .build();
    }

    @Override
    public AsyncRequestBody requestBodyForObject(SinglePartUploadContext context) {
        return AsyncRequestBody.fromFile(path);
    }
}
