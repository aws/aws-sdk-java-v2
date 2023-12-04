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

package software.amazon.awssdk.core.progress.listener;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.progress.listener.snapshot.ProgressSnapshot;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * The {@link ProgressListener} interface may be implemented by your application in order to receive event-driven updates on
 * the progress of a service call. When you construct an {@link PutObjectRequest} or {@link
 * UploadPartRequest} request submitted to the Sdk, you may provide a variable number of {@link
 * ProgressListener}s to be associated with that request. Then, throughout the lifecycle of the request,
 * the Sdk
 * will invoke the provided {@link ProgressListener}s when important events occur, like additional bytes being transferred,
 * allowing you to monitor the ongoing progress of the transaction.
 * <p>
 * Each {@link ProgressListener} callback is invoked with an immutable {@link Context} object. Depending on the current
 * lifecycle
 * of the request, different {@link Context} objects have different attributes available (indicated by the provided context
 * interface). Most notably, every callback is given access to the current {@link ProgressSnapshot}, which contains
 * helpful progress-related methods like {@link ProgressSnapshot#transferredBytes()} and {@link
 * ProgressSnapshot#ratioTransferred()}.
 * <p>
 * A successful transfer callback lifecycle is sequenced as follows:
 * <ol>
 *     <li>{@link #requestPrepared(Context.RequestPrepared)} - A new Request has been initiated. This method is called
 *     exactly once per transfer.</li>
 *     <ul>Available context attributes:
 *         <li>{@link Context.RequestPrepared#request()}</li>
 *         <li>{@link Context.RequestPrepared#progressSnapshot()}</li>
 *     </ul>
 *     <li>{@link #requestBytesSent(Context.RequestBytesSent)} - Additional bytes have been sent. This
 *     method may be called many times per request, depending on the request payload size and I/O buffer sizes.
 *     <li>{@link #responseBytesReceived(Context.ResponseBytesReceived)} - Additional bytes have been received. This
 *     method may be called many times per request, depending on the response payload size and I/O buffer sizes.
 *     <li>{@link #executionSuccess(Context.ExecutionSuccess)} - The transfer has completed successfully. This method is called
 *     exactly once for a successful transfer.</li>
 * </ol>
 * For every failed attempt {@link #attemptFailure(Context.AttemptFailure)}  will be called exactly once.
 *
 * <p>
 * There are a few important rules and best practices that govern the usage of {@link ProgressListener}s:
 * <ol>
 *     <li>{@link ProgressListener} implementations should not block, sleep, or otherwise delay the calling thread. If you need
 *     to perform blocking operations, you should schedule them in a separate thread or executor that you control.</li>
 *     <li>Be mindful that {@link #requestBytesSent(Context.RequestBytesSent)} or
 *     {@link #responseBytesReceived(Context.ResponseBytesReceived)}
 *     may be called extremely often for large payloads
 *     (subject to I/O buffer sizes). Be careful in implementing expensive operations as a side effect. Consider rate-limiting
 *     your side effect operations, if needed.</li>
 *     <li>{@link ProgressListener}s may be invoked by different threads. If your {@link ProgressListener} is stateful,
 *     ensure that it is also thread-safe.</li>
 *     <li>{@link ProgressListener}s are not intended to be used for control flow, and therefore your implementation
 *     should not <i>throw</i>. Any thrown exceptions will be suppressed and logged as an error.</li>
 * </ol>
 * <p>
 * A classical use case of {@link ProgressListener} is to create a progress bar to monitor an ongoing transfer's progress.
 * Refer to the implementation of {@link LoggingProgressListener} for a basic example, or test it in your application by providing
 * the listener as part of your {@link SdkHttpRequest}. E.g.,
 * <pre>{@code
 * AmazonS3Client s3Client = new AmazonS3Client(awsCredentials);
 * PutObjectRequest putObjectRequest = PutObjectRequest.builder()
 *                                        .bucket("bucket")
 *                                        .key("key")
 *                                        .overrideConfiguration(o -> o.addProgressListener(LoggingTransferListener.create())
 *                                        .build();
 * s3Client.putObject(putObjectRequest);
 * }</pre>
 * And then a successful transfer may output something similar to:
 * <pre>
 * Request initiated...
 * |                    | 0.0%
 * |==                  | 12.5%
 * |=====               | 25.0%
 * |=======             | 37.5%
 * |==========          | 50.0%
 * |============        | 62.5%
 * |===============     | 75.0%
 * |=================   | 87.5%
 * |====================| 100.0%
 * Request execution successful!
 * </pre>
 */
@SdkPublicApi
public interface ProgressListener {

    /**
     * This method is called right after a request object is marshalled and ready to be sent to the service
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.RequestPrepared#request()}</li>
     *     <li>{@link Context.RequestPrepared#progressSnapshot()}</li>
     * </ol>
     */
    default void requestPrepared(Context.RequestPrepared context) {
    }

    /**
     * This method is called after the request transaction is initiated, i.e. request header is sent to the service
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.RequestHeaderSent#request()}</li>
     *     <li>{@link Context.RequestHeaderSent#progressSnapshot()}</li>
     * </ol>
     */
    default void requestHeaderSent(Context.RequestHeaderSent context) {
    }

    /**
     * This method is called with any additional payload bytes sent; it may be called many times per request, depending on the
     * payload size.
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.RequestBytesSent#request()}</li>
     *     <li>{@link Context.RequestBytesSent#progressSnapshot()}</li>
     * </ol>
     */
    default void requestBytesSent(Context.RequestBytesSent context) {
    }

    /**
     * The service returns the response headers
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.ResponseHeaderReceived#request()}</li>
     *     <li>{@link Context.ResponseHeaderReceived#progressSnapshot()}</li>
     * </ol>
     */
    default void responseHeaderReceived(Context.ResponseHeaderReceived context) {
    }

    /**
     * Additional bytes received
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.ResponseBytesReceived#request()}</li>
     *     <li>{@link Context.ResponseBytesReceived#progressSnapshot()}</li>
     * </ol>
     */
    default void responseBytesReceived(Context.ResponseBytesReceived context) {
    }

     /**
     * Additional bytes received
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.ResponseBytesReceived#request()}</li>
     *     <li>{@link Context.ResponseBytesReceived#progressSnapshot()}</li>
     * </ol>
     */
    default void attemptFailureResponseBytesReceived(Context.AttemptFailureResponseBytesReceived context) {
    }

    /**
     * Successful request execution
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.ExecutionSuccess#request()}</li>
     *     <li>{@link Context.ExecutionSuccess#progressSnapshot()}</li>
     *     <li>{@link Context.ExecutionSuccess#executionSuccess()} ()}</li>
     * </ol>
     */
    default void executionSuccess(Context.ExecutionSuccess context) {
    }

    /**
     * This method is called for every failure of a request attempt.
     * An ideal implementation would invoke executionFailure for a number of attemptFailures greater than a threshold
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.AttemptFailure#request()}</li>
     *     <li>{@link Context.AttemptFailure#progressSnapshot()}</li>
     * </ol>
     */
    default void attemptFailure(Context.AttemptFailure context) {
    }

    /**
     * A wrapper class that groups together the different context interfaces that are exposed to {@link ProgressListener}s.
     * <p>
     * Successful transfer interface hierarchy:
     * <ol>
     *     <li>{@link RequestPrepared}</li>
     *     <li>{@link RequestHeaderSent}</li>
     *     <li>{@link RequestBytesSent}</li>
     *     <li>{@link ResponseHeaderReceived}</li>
     *     <li>{@link ResponseBytesReceived}</li>
     *     <li>{@link ExecutionSuccess}</li>
     * </ol>
     * Failed transfer interface hierarchy:
     * <ol>
     *     <li>{@link RequestPrepared}</li>
     *     <li>{@link AttemptFailure}</li>
     *     <li>{@link ExecutionFailure}</li>
     * </ol>
     *
     * @see ProgressListener
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
         *     <li>{@link RequestPrepared#request()}</li>
         *     <li>{@link RequestPrepared#progressSnapshot()}</li>
         * </ol>
         */
        @Immutable
        @ThreadSafe
        @SdkPublicApi
        @SdkPreviewApi
        public interface RequestPrepared {
            /**
             * The {@link SdkHttppRequest} that was submitted to SDK, i.e., the {@link PutObjectRequest} or
             * {@link GetObjectRequest}.
             */
            SdkHttpRequest request();

            /**
             * The immutable {@link ProgressSnapahot} for this specific update.
             */
            ProgressSnapshot progressSnapshot();
        }

        /**
         * The submitted {@link SdkHttppRequest} request header was successfully sent to the service
         * <p>
         * Available context attributes:
         * <ol>
         *     <li>{@link RequestHeaderSent#request()}</li>
         *     <li>{@link RequestHeaderSent#progressSnapshot()}</li>
         * </ol>
         */
        @Immutable
        @ThreadSafe
        @SdkPublicApi
        @SdkPreviewApi
        public interface RequestHeaderSent extends RequestPrepared {
        }

        /**
         * Additional bytes sent
         * <p>
         * Available context attributes:
         * <ol>
         *     <li>{@link RequestBytesSent#request()}</li>
         *     <li>{@link RequestBytesSent#progressSnapshot()}</li>
         * </ol>
         */
        @Immutable
        @ThreadSafe
        @SdkPublicApi
        @SdkPreviewApi
        public interface RequestBytesSent extends RequestHeaderSent {
        }

        /**
         * Service has sent back a response header, denoting the start of response reception
         * <p>
         * Available context attributes:
         * <ol>
         *     <li>{@link ResponseHeaderReceived#request()}</li>
         *     <li>{@link ResponseHeaderReceived#progressSnapshot()}</li>
         * </ol>
         */
        @Immutable
        @ThreadSafe
        @SdkPublicApi
        @SdkPreviewApi
        public interface ResponseHeaderReceived extends RequestBytesSent {
        }

        /**
         * Additional bytes received
         * <p>
         * Available context attributes:
         * <ol>
         *     <li>{@link ResponseBytesReceived#request()}</li>
         *     <li>{@link ResponseBytesReceived#progressSnapshot()}</li>
         * </ol>
         */
        @Immutable
        @ThreadSafe
        @SdkPublicApi
        @SdkPreviewApi
        public interface ResponseBytesReceived extends ResponseHeaderReceived {
        }

        /**
         * The request execution is successful.
         * <p>
         * Available context attributes:
         * <ol>
         *     <li>{@link ExecutionSuccess#request()}</li>
         *     <li>{@link ExecutionSuccess#progressSnapshot()}</li>
         *     <li>{@link ExecutionSuccess#executionSuccess()} ()}</li>
         * </ol>
         */
        @Immutable
        @ThreadSafe
        @SdkPublicApi
        @SdkPreviewApi
        public interface ExecutionSuccess extends ResponseBytesReceived {
            /**
             * The successful completion of a request submitted to the Sdk
             */
            ExecutionSuccessObjectRequest executionSuccess();
        }

        /**
         * For Expect: 100-continue embedded requests, the service returning anything other than 100 continue
         * indicates a service error. The progress state captured indicates that no bytes are received.
         * <p>
         * Available context attributes:
         * <ol>
         *     <li>{@link AttemptFailureResponseBytesReceived#request()}</li>
         *     <li>{@link AttemptFailureResponseBytesReceived#progressSnapshot()}</li>
         * </ol>
         */
        @Immutable
        @ThreadSafe
        @SdkPublicApi
        @SdkPreviewApi
        public interface AttemptFailureResponseBytesReceived extends ResponseHeaderReceived {
        }

        /**
         * The request execution attempt failed.
         * <p>
         * Available context attributes:
         * <ol>
         *     <li>{@link AttemptFailure#request()}</li>
         *     <li>{@link AttemptFailure#progressSnapshot()}</li>
         * </ol>
         */
        @Immutable
        @ThreadSafe
        @SdkPublicApi
        @SdkPreviewApi
        public interface AttemptFailure extends RequestPrepared {
        }

        /**
         * The request execution failed.
         * <p>
         * Available context attributes:
         * <ol>
         *     <li>{@link ExecutionFailure#request()}</li>
         *     <li>{@link ExecutionFailure#progressSnapshot()}</li>
         *     <li>{@link ExecutionFailure#exception()}</li>
         * </ol>
         */
        @Immutable
        @ThreadSafe
        @SdkPublicApi
        @SdkPreviewApi
        public interface ExecutionFailure extends RequestPrepared {
            /**
             * The exception associated with the failed request.
             */
            Throwable exception();
        }
    }
}