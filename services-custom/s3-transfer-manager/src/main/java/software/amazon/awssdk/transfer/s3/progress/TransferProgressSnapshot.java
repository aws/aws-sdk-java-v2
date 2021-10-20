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

package software.amazon.awssdk.transfer.s3.progress;

import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.Download;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.TransferRequest;
import software.amazon.awssdk.transfer.s3.Upload;

/**
 * {@link TransferProgressSnapshot} is an <b>immutable</b>, point-in-time representation of the progress of a given transfer
 * initiated by {@link S3TransferManager}. {@link TransferProgressSnapshot} offers several helpful methods for checking the
 * progress of a transfer, like {@link #bytesTransferred()} and {@link #ratioTransferred()}.
 * <p>
 * {@link TransferProgressSnapshot}'s methods that return {@link Optional} are dependent upon the size of a transfer (i.e., the
 * {@code Content-Length}) being known. In the case of file-based {@link Upload}s, transfer sizes are known up front and
 * immediately available. In the case of {@link Download}s, the transfer size is not known until {@link S3TransferManager}
 * receives a {@link GetObjectResponse} from Amazon S3.
 * <p>
 * The recommended way to receive updates of the latest {@link TransferProgressSnapshot} is to implement the {@link
 * TransferListener} interface. See the {@link TransferListener} documentation for usage instructions. A {@link
 * TransferProgressSnapshot} can also be obtained from a {@link TransferProgress} returned as part of the result from a {@link
 * TransferRequest}.
 *
 * @see TransferProgress
 * @see TransferListener
 * @see S3TransferManager
 */
@Immutable
@ThreadSafe
@SdkPublicApi
@SdkPreviewApi
public interface TransferProgressSnapshot {

    /**
     * The total number of bytes that have been transferred so far.
     */
    long bytesTransferred();

    /**
     * The total size of the transfer, in bytes, or {@link Optional#empty()} if unknown.
     * <p>
     * In the case of file-based {@link Upload}s, transfer sizes are known up front and immediately available. In the case of
     * {@link Download}s, the transfer size is not known until {@link S3TransferManager} receives a {@link GetObjectResponse} from
     * Amazon S3.
     */
    Optional<Long> transferSizeInBytes();

    /**
     * The ratio of the {@link #transferSizeInBytes()} that has been transferred so far, or {@link Optional#empty()} if unknown.
     * This method depends on the {@link #transferSizeInBytes()} being known in order to return non-empty.
     * <p>
     * Ratio is computed as {@link #bytesTransferred()} {@code /} {@link #transferSizeInBytes()}, where a transfer that is
     * half-complete would return {@code 0.5}.
     *
     * @see #transferSizeInBytes()
     */
    Optional<Double> ratioTransferred();

    /**
     * The total number of bytes that are remaining to be transferred, or {@link Optional#empty()} if unknown. This method depends
     * on the {@link #transferSizeInBytes()} being known in order to return non-empty.
     *
     * @see #transferSizeInBytes()
     */
    Optional<Long> bytesRemaining();
}
