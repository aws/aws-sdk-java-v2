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

import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.transfer.s3.Download;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.Transfer;
import software.amazon.awssdk.transfer.s3.Upload;

/**
 * {@link TransferProgress} is a <b>stateful</b> representation of the progress of a transfer initiated by {@link
 * S3TransferManager}. {@link TransferProgress} offers the ability to take a {@link #snapshot()} of the current progress,
 * represented by an immutable {@link TransferProgressSnapshot}, which contains helpful progress-related methods like {@link
 * TransferProgressSnapshot#bytesTransferred()} and {@link TransferProgressSnapshot#ratioTransferred()}. {@link TransferProgress}
 * is attached to {@link Transfer} objects, namely {@link Upload} and {@link Download}.
 * <p>
 * Where possible, it is typically recommended to <b>avoid</b> directly querying {@link TransferProgress} and to instead leverage
 * the {@link TransferListener} interface to receive event-driven updates of the latest {@link TransferProgressSnapshot}. See the
 * {@link TransferListener} documentation for usage instructions. However, if desired, {@link TransferProgress} can be used for
 * poll-like checking of the current progress. E.g.,
 * <pre>{@code
 * Upload upload = tm.upload(...);
 * while (!upload.completionFuture().isDone()) {
 *     upload.progress().snapshot().ratioTransferred().ifPresent(System.out::println);
 *     Thread.sleep(1000);
 * }
 * }</pre>
 *
 * @see TransferProgressSnapshot
 * @see TransferListener
 * @see S3TransferManager
 */
@Mutable
@ThreadSafe
@SdkPublicApi
@SdkPreviewApi
public interface TransferProgress {

    /**
     * Take a snapshot of the current progress, represented by an immutable {@link TransferProgressSnapshot}.
     */
    TransferProgressSnapshot snapshot();
}
