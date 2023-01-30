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

package software.amazon.awssdk.transfer.s3.model;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * A download transfer of a single object from S3.
 */
@SdkPublicApi
@ThreadSafe
public interface FileDownload extends ObjectTransfer {

    /**
     * Pause the current download operation and returns the information that can
     * be used to resume the download at a later time.
     * <p>
     * The information object is serializable for persistent storage until it should be resumed.
     * See {@link ResumableFileDownload} for supported formats.
     *
     * @return A {@link ResumableFileDownload} that can be used to resume the download.
     */
    ResumableFileDownload pause();

    @Override
    CompletableFuture<CompletedFileDownload> completionFuture();
}
