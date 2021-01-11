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
import software.amazon.awssdk.core.async.AsyncRequestBody;

/**
 * A factory capable of creating the streams for individual parts of a given
 * object to be uploaded to S3.
 * <p>
 * There is no ordering guarantee for when {@link
 * #requestBodyForPart(MultipartUploadContext)} is called.
 */
@SdkInternalApi
public interface TransferRequestBody {

    static TransferRequestBody fromFile(Path file) {
        return new FileTransferRequestBody(file);
    }

    long contentLength();

    /**
     * Return the stream for the object part described by given {@link
     * MultipartUploadContext}.
     *
     * @param context The context describing the part to be uploaded.
     * @return The part stream.
     */
    AsyncRequestBody requestBodyForPart(MultipartUploadContext context);

    /**
     * Return the stream for a entire object to be uploaded as a single part.
     */
    AsyncRequestBody requestBodyForObject(SinglePartUploadContext context);
}