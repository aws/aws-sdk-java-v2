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

import java.util.concurrent.CompletionException;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.CompletedObjectTransfer;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.TransferObjectRequest;
import software.amazon.awssdk.transfer.s3.model.TransferRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

/**
 * The {@link TransferListener} interface may be implemented by your application in order to receive event-driven updates on the
 * progress of a transfer initiated by {@link S3TransferManager}. When you construct an {@link UploadFileRequest} or {@link
 * DownloadFileRequest} request to submit to {@link S3TransferManager}, you may provide a variable number of {@link
 * TransferListener}s to be associated with that request. Then, throughout the lifecycle of the request, {@link S3TransferManager}
 * will invoke the provided {@link TransferListener}s when important events occur, like additional bytes being transferred,
 * allowing you to monitor the ongoing progress of the transfer.
 * <p>
 * Each {@link TransferListener} callback is invoked with an immutable {@link Context} object. Depending on the current lifecycle
 * of the request, different {@link Context} objects have different attributes available (indicated by the provided context
 * interface). Most notably, every callback is given access to the current {@link TransferProgressSnapshot}, which contains
 * helpful progress-related methods like {@link TransferProgressSnapshot#transferredBytes()} and {@link
 * TransferProgressSnapshot#ratioTransferred()}.
 * <p>
 * A successful transfer callback lifecycle is sequenced as follows:
 * <ol>
 *     <li>{@link #transferInitiated(Context.TransferInitiated)} - A new transfer has been initiated. This method is called
 *     exactly once per transfer.</li>
 *     <ul>Available context attributes:
 *         <li>{@link Context.TransferInitiated#request()}</li>
 *         <li>{@link Context.TransferInitiated#progressSnapshot()}</li>
 *     </ul>
 *     <li>{@link #bytesTransferred(Context.BytesTransferred)} - Additional bytes have been submitted or received. This method
 *     may be called many times per transfer, depending on the transfer size and I/O buffer sizes.
 *     <li>{@link #transferComplete(Context.TransferComplete)} - The transfer has completed successfully. This method is called
 *     exactly once for a successful transfer.</li>
 *     <ul>Additional available context attributes:
 *         <li>{@link Context.TransferComplete#completedTransfer()}</li>
 *     </ul>
 * </ol>
 * In the case of a failed transfer, both {@link #transferInitiated(Context.TransferInitiated)} and
 * {@link #transferFailed(Context.TransferFailed)} will be called exactly once. There are no guarantees on whether any other
 * callbacks are invoked.
 * <p>
 * There are a few important rules and best practices that govern the usage of {@link TransferListener}s:
 * <ol>
 *     <li>{@link TransferListener} implementations should not block, sleep, or otherwise delay the calling thread. If you need
 *     to perform blocking operations, you should schedule them in a separate thread or executor that you control.</li>
 *     <li>Be mindful that {@link #bytesTransferred(Context.BytesTransferred)} may be called extremely often (subject to I/O
 *     buffer sizes). Be careful in implementing expensive operations as a side effect. Consider rate-limiting your side
 *     effect operations, if needed.</li>
 *     <li>In the case of uploads, there may be some delay between the bytes being fully transferred and the transfer
 *     successfully completing. Internally, {@link S3TransferManager} uses the Amazon S3
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html">multipart upload API</a>
 *     and must finalize uploads with a {@link CompleteMultipartUploadRequest}.</li>
 *     <li>{@link TransferListener}s may be invoked by different threads. If your {@link TransferListener} is stateful,
 *     ensure that it is also thread-safe.</li>
 *     <li>{@link TransferListener}s are not intended to be used for control flow, and therefore your implementation
 *     should not <i>throw</i>. Any thrown exceptions will be suppressed and logged as an error.</li>
 * </ol>
 * <p>
 * A classical use case of {@link TransferListener} is to create a progress bar to monitor an ongoing transfer's progress.
 * Refer to the implementation of {@link LoggingTransferListener} for a basic example, or test it in your application by providing
 * the listener as part of your {@link TransferRequest}. E.g.,
 * <pre>{@code
 * Upload upload = tm.upload(UploadRequest.builder()
 *                                        .putObjectRequest(b -> b.bucket("bucket").key("key"))
 *                                        .source(Paths.get(...))
 *                                        .addTransferListener(LoggingTransferListener.create())
 *                                        .build());
 * }</pre>
 * And then a successful transfer may output something similar to:
 * <pre>
 * Transfer initiated...
 * |                    | 0.0%
 * |==                  | 12.5%
 * |=====               | 25.0%
 * |=======             | 37.5%
 * |==========          | 50.0%
 * |============        | 62.5%
 * |===============     | 75.0%
 * |=================   | 87.5%
 * |====================| 100.0%
 * Transfer complete!
 * </pre>
 */
@SdkPublicApi
public interface TransferListener {

    /**
     * A new transfer has been initiated. This method is called exactly once per transfer.
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.TransferInitiated#request()}</li>
     *     <li>{@link Context.TransferInitiated#progressSnapshot()}</li>
     * </ol>
     */
    default void transferInitiated(Context.TransferInitiated context) {
    }

    /**
     * Additional bytes have been submitted or received. This method may be called many times per transfer, depending on the
     * transfer size and I/O buffer sizes.
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.BytesTransferred#request()}</li>
     *     <li>{@link Context.BytesTransferred#progressSnapshot()}</li>
     * </ol>
     */
    default void bytesTransferred(Context.BytesTransferred context) {
    }

    /**
     * The transfer has completed successfully. This method is called exactly once for a successful transfer.
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.TransferComplete#request()}</li>
     *     <li>{@link Context.TransferComplete#progressSnapshot()}</li>
     *     <li>{@link Context.TransferComplete#completedTransfer()}</li>
     * </ol>
     */
    default void transferComplete(Context.TransferComplete context) {
    }

    /**
     * The transfer failed. This method is called exactly once for a failed transfer.
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.TransferFailed#request()}</li>
     *     <li>{@link Context.TransferFailed#progressSnapshot()}</li>
     *     <li>{@link Context.TransferFailed#exception()}</li>
     * </ol>
     */
    default void transferFailed(Context.TransferFailed context) {
    }

    /**
     * A wrapper class that groups together the different context interfaces that are exposed to {@link TransferListener}s.
     * <p>
     * Successful transfer interface hierarchy:
     * <ol>
     *     <li>{@link TransferInitiated}</li>
     *     <li>{@link BytesTransferred}</li>
     *     <li>{@link TransferComplete}</li>
     * </ol>
     * Failed transfer interface hierarchy:
     * <ol>
     *     <li>{@link TransferInitiated}</li>
     *     <li>{@link TransferFailed}</li>
     * </ol>
     *
     * @see TransferListener
     */
    @SdkProtectedApi
    final class Context {
        private Context() {
        }

        /**
         * A new transfer has been initiated.
         * <p>
         * Available context attributes:
         * <ol>
         *     <li>{@link TransferInitiated#request()}</li>
         *     <li>{@link TransferInitiated#progressSnapshot()}</li>
         * </ol>
         */
        @Immutable
        @ThreadSafe
        @SdkPublicApi
        @SdkPreviewApi
        public interface TransferInitiated {
            /**
             * The {@link TransferRequest} that was submitted to {@link S3TransferManager}, i.e., the {@link UploadFileRequest} or
             * {@link DownloadFileRequest}.
             */
            TransferObjectRequest request();

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
         *     <li>{@link BytesTransferred#request()}</li>
         *     <li>{@link BytesTransferred#progressSnapshot()}</li>
         * </ol>
         */
        @Immutable
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
         *     <li>{@link TransferComplete#request()}</li>
         *     <li>{@link TransferComplete#progressSnapshot()}</li>
         *     <li>{@link TransferComplete#completedTransfer()}</li>
         * </ol>
         */
        @Immutable
        @ThreadSafe
        @SdkPublicApi
        @SdkPreviewApi
        public interface TransferComplete extends BytesTransferred {
            /**
             * The completed transfer, i.e., the {@link CompletedFileUpload} or {@link CompletedFileDownload}.
             */
            CompletedObjectTransfer completedTransfer();
        }

        /**
         * The transfer failed.
         * <p>
         * Available context attributes:
         * <ol>
         *     <li>{@link TransferFailed#request()}</li>
         *     <li>{@link TransferFailed#progressSnapshot()}</li>
         *     <li>{@link TransferFailed#exception()}</li>
         * </ol>
         */
        @Immutable
        @ThreadSafe
        @SdkPublicApi
        @SdkPreviewApi
        public interface TransferFailed extends TransferInitiated {
            /**
             * The exception associated with the failed transfer.
             * <p>
             * Note that this would be the <i>cause</i> of a {@link CompletionException}, and not a {@link CompletionException}
             * itself.
             */
            Throwable exception();
        }
    }
}
