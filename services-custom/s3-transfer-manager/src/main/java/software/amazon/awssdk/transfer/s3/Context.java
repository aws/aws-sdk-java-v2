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

package software.amazon.awssdk.transfer.s3;

import java.util.concurrent.CompletionException;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * A wrapper class that groups together the different context objects that are exposed to {@link TransferListener}s.
 *
 * @see TransferListener
 */
@SdkProtectedApi
public final class Context {
    private Context() {
    }

    /**
     * A new transfer has been initiated.
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.TransferInitiated#request()}</li>
     *     <li>{@link Context.TransferInitiated#progressSnapshot()}</li>
     * </ol>
     */
    @ThreadSafe
    @SdkPublicApi
    @SdkPreviewApi
    public interface TransferInitiated {
        /**
         * The {@link TransferRequest} that was submitted to {@link S3TransferManager}, i.e., the {@link UploadRequest} or {@link
         * DownloadRequest}.
         */
        TransferRequest request();

        /**
         * The immutable {@link TransferProgressSnapshot} for this specific update.
         */
        TransferProgressSnapshot progressSnapshot();
    }

    /**
     * Additional bytes have been submitted or received.
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.BytesTransferred#request()}</li>
     *     <li>{@link Context.BytesTransferred#progressSnapshot()}</li>
     * </ol>
     */
    @ThreadSafe
    @SdkPublicApi
    @SdkPreviewApi
    public interface BytesTransferred extends TransferInitiated {
    }

    /**
     * The transfer has completed successfully.
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.TransferComplete#request()}</li>
     *     <li>{@link Context.TransferComplete#progressSnapshot()}</li>
     *     <li>{@link Context.TransferComplete#completedTransfer()}</li>
     * </ol>
     */
    @ThreadSafe
    @SdkPublicApi
    @SdkPreviewApi
    public interface TransferComplete extends BytesTransferred {
        /**
         * The completed transfer, i.e., the {@link CompletedUpload} or {@link CompletedDownload}.
         */
        CompletedTransfer completedTransfer();
    }

    /**
     * The transfer failed.
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.TransferFailed#request()}</li>
     *     <li>{@link Context.TransferFailed#progressSnapshot()}</li>
     *     <li>{@link Context.TransferFailed#exception()}</li>
     * </ol>
     */
    @ThreadSafe
    @SdkPublicApi
    @SdkPreviewApi
    public interface TransferFailed extends TransferInitiated {
        /**
         * The exception associated with the failed transfer.
         * <p>
         * Note that this would be the <i>cause</i>> of a {@link CompletionException}, and not a {@link CompletionException}
         * itself.
         */
        Throwable exception();
    }
}
