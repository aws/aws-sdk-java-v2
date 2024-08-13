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

import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.internal.progress.listener.LoggingProgressListener;
import software.amazon.awssdk.core.progress.snapshot.ProgressSnapshot;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * The {@link ProgressListener} interface may be implemented by your application in order to receive event-driven updates on
 * the progress of a service call. When an {@link SdkRequest} like {@link PutObjectRequest} or {@link
 * UploadPartRequest} is submitted to the Sdk, you may provide a variable number of {@link
 * ProgressListener}s to be associated with that request.
 * <p>
 * While ExecutionInterceptors are focused on the lifecycle of the
 * request within the SDK, ProgressListeners are focused on the lifecycle of the request within the HTTP client. Throughout
 * the lifecycle of the client request to which it is attached, the Sdk will invoke the provided {@link ProgressListener}s when
 * important events occur, like additional bytes being transferred, allowing you to monitor the ongoing progress of the
 * transaction.
 * <p>
 * Each {@link ProgressListener} callback is invoked with an immutable {@link Context} object. Depending on the current
 * lifecycle of the request, different {@link Context} objects have different attributes available (indicated by the provided
 * context interface). Most notably, every callback is given access to the current {@link ProgressSnapshot}, which contains
 * helpful progress-related methods like {@link ProgressSnapshot#transferredBytes()} and {@link
 * ProgressSnapshot#ratioTransferred()}.
 * <p>
 * A successful transfer callback lifecycle is sequenced as follows:
 * <ol>
 *     <li>{@link #requestPrepared(Context.RequestPrepared)} - This method is called for every newly initiated SdkRequest,
 *     after it is marshalled and signed, before it is sent to the service  </li>
 *     <ul>Available context attributes:
 *         <li>{@link Context.RequestPrepared#request()}</li>
 *         <li>{@link Context.RequestPrepared#uploadProgressSnapshot()}</li>
 *     </ul>
 *     <li>{@link #requestBytesSent(Context.RequestBytesSent)} - Additional bytes have been sent. This
 *     method may be called many times per request, depending on the request payload size and I/O buffer sizes.
 *     <li>{@link #responseBytesReceived(Context.ResponseBytesReceived)} - Additional bytes have been received. This
 *     method may be called many times per request, depending on the response payload size and I/O buffer sizes.
 *     <li>{@link #executionSuccess(Context.ExecutionSuccess)} - The transfer has completed successfully. This method is called
 *     for every successful transfer.</li>
 * </ol>
 * For every failed attempt {@link #attemptFailure(Context.ExecutionFailure)}.
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
     * This method is called right after a {@link SdkRequest} is marshalled, signed, transformed into an
     * {@link SdkHttpRequest} and ready to be sent to the service
     * After this method has returned, either requestHeaderSent or executionFailure will always be invoked
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.RequestPrepared#request()}</li>
     *     <li>{@link Context.RequestPrepared#httpRequest()}</li>
     *     <li>{@link Context.RequestPrepared#uploadProgressSnapshot()}</li>
     * </ol>
     */
    default void requestPrepared(Context.RequestPrepared context) {
    }

    /**
     * This method is called after the request transaction is initiated, i.e. request header is sent to the service
     * After this method, one among requestBytesSent, responseHeaderReceived, and attemptFailure will be always be
     * invoked
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.RequestHeaderSent#request()}</li>
     *     <li>{@link Context.RequestHeaderSent#httpRequest()}</li>
     *     <li>{@link Context.RequestHeaderSent#uploadProgressSnapshot()}</li>
     * </ol>
     */
    default void requestHeaderSent(Context.RequestHeaderSent context) {
    }

    /**
     * This method is called with any additional payload bytes sent; it may be called many times per request, depending on the
     * payload size.
     * After this method, either responseHeaderReceived or attemptFailure will always be invoked
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.RequestBytesSent#request()}</li>
     *     <li>{@link Context.RequestBytesSent#httpRequest()}</li>
     *     <li>{@link Context.RequestBytesSent#uploadProgressSnapshot()}</li>
     * </ol>
     */
    default void requestBytesSent(Context.RequestBytesSent context) {
    }

    /**
     * The service returns the response headers
     * SdkHttpResponse httpResponse() denotes the unmarshalled response sent back by the service
     * After this, one among  responseBytesReceived, attemptFailureResponseBytesReceived and attemptFailure will always be invoked
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.ResponseHeaderReceived#request()}</li>
     *     <li>{@link Context.ResponseHeaderReceived#httpRequest()}</li>
     *     <li>{@link Context.ResponseHeaderReceived#uploadProgressSnapshot()}</li>
     *     <li>{@link Context.ResponseHeaderReceived#httpResponse()} ()}</li>
     *     <li>{@link Context.ResponseHeaderReceived#downloadProgressSnapshot()}</li>
     * </ol>
     */
    default void responseHeaderReceived(Context.ResponseHeaderReceived context) {
    }

    /**
     * Additional bytes received
     * After this, either executionSuccess or attemptFailure will always be invoked
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.ResponseBytesReceived#request()}</li>
     *     <li>{@link Context.ResponseBytesReceived#httpRequest()}</li>
     *     <li>{@link Context.ResponseBytesReceived#uploadProgressSnapshot()}</li>
     *     <li>{@link Context.ResponseBytesReceived#httpResponse()}</li>
     *     <li>{@link Context.ResponseBytesReceived#downloadProgressSnapshot()}</li>
     * </ol>
     */
    default void responseBytesReceived(Context.ResponseBytesReceived context) {
    }

    /**
    * For Expect: 100-continue embedded requests, the service returning anything other than 100 continue
    * indicates a request failure. This method captures the error in the payload
    * After this it will either be an executionFailure or a retry the request.
    * <p>
    * Available context attributes:
    * <ol>
     *     <li>{@link Context.ExecutionFailure#request()}</li>
     *     <li>{@link Context.ExecutionFailure#httpRequest()}</li>
     *     <li>{@link Context.ExecutionFailure#uploadProgressSnapshot()}</li>
     *     <li>{@link Context.ExecutionFailure#httpResponse()} ()}</li>
     *     <li>{@link Context.ExecutionFailure#downloadProgressSnapshot()}</li>
     *     <li>{@link Context.ExecutionFailure#exception()}</li>
    * </ol>
    */
    default void attemptFailureResponseBytesReceived(Context.ExecutionFailure context) {
    }

    /**
     * Successful request execution
     * SdkResponse response() denotes the marshalled response
     * This marks the end of the request path.
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.ExecutionSuccess#request()}</li>
     *     <li>{@link Context.ExecutionSuccess#httpRequest()}</li>
     *     <li>{@link Context.ExecutionSuccess#uploadProgressSnapshot()}</li>
     *     <li>{@link Context.ExecutionSuccess#httpResponse()}</li>
     *     <li>{@link Context.ExecutionSuccess#downloadProgressSnapshot()}</li>
     *     <li>{@link Context.ExecutionSuccess#response()}</li>
     * </ol>
     */
    default void executionSuccess(Context.ExecutionSuccess context) {
    }

    /**
     * This method is called for every failure of a request attempt
     * This method is followed by either a retry attempt which would be requestHeaderSent,
     * or an executionFailure if it has exceeded the maximum number of retries configured
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.ExecutionFailure#request()}</li>
     *     <li>{@link Context.ExecutionFailure#httpRequest()}</li>
     *     <li>{@link Context.ExecutionFailure#uploadProgressSnapshot()}</li>
     *     <li>{@link Context.ExecutionFailure#httpResponse()} ()}</li>
     *     <li>{@link Context.ExecutionFailure#downloadProgressSnapshot()}</li>
     *     <li>{@link Context.ExecutionFailure#exception()}</li>
     * </ol>
     */
    default void attemptFailure(Context.ExecutionFailure context) {
    }

    /**
     * This method is called for every failed request execution
     * This marks end of the request path with an exception being throw with the appropriate message
     * <p>
     * Available context attributes:
     * <ol>
     *     <li>{@link Context.ExecutionFailure#request()}</li>
     *     <li>{@link Context.ExecutionFailure#httpRequest()}</li>
     *     <li>{@link Context.ExecutionFailure#uploadProgressSnapshot()}</li>
     *     <li>{@link Context.ExecutionFailure#httpResponse()} ()}</li>
     *     <li>{@link Context.ExecutionFailure#downloadProgressSnapshot()}</li>
     *     <li>{@link Context.ExecutionFailure#exception()}</li>
     * </ol>
     */
    default void executionFailure(Context.ExecutionFailure context) {
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
     * Failed transfer method hierarchy:
     * <ol>
     *     <li>{@link RequestPrepared}</li>
     *     <li>{@link ExecutionFailure}</li>
     * </ol>
     * If the request header includes an Expect: 100-Continue and the service returns a different value, the method invokation
     * hierarchy is as follows :
     * <ol>
     *     <li>{@link RequestPrepared}</li>
     *     <li>{@link RequestHeaderSent}</li>
     *     <li>{@link RequestBytesSent}</li>
     *     <li>{@link ResponseHeaderReceived}</li>
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
         *     <li>{@link RequestPrepared#uploadProgressSnapshot()} </li>
         * </ol>
         */
        @Immutable
        @ThreadSafe
        @SdkPublicApi
        @SdkPreviewApi
        public interface RequestPrepared {
            /**
             * The {@link SdkRequest} that was submitted to SDK, i.e., the {@link PutObjectRequest} or
             * {@link GetObjectRequest}
             */
            SdkRequest request();

            /**
             * The {@link SdkRequest} that was submitted to SDK, i.e., the {@link PutObjectRequest} or
             * {@link GetObjectRequest} is marshalled, signed and transformed into an {@link SdkHttpRequest}
             *
             */
            SdkHttpRequest httpRequest();

            /**
             * The immutable {@link ProgressSnapshot} to track upload progress state
             */
            ProgressSnapshot uploadProgressSnapshot();
        }

        /**
         * The submitted {@link SdkHttpRequest} request header was successfully sent to the service
         * <p>
         * Available context attributes:
         * <ol>
         *     <li>{@link RequestHeaderSent#request()}</li>
         *     <li>{@link RequestHeaderSent#httpRequest()}</li>
         *     <li>{@link RequestHeaderSent#uploadProgressSnapshot()}</li>
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
         *     <li>{@link RequestBytesSent#httpRequest()}</li>
         *     <li>{@link RequestBytesSent#uploadProgressSnapshot()}</li>
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
         *     <li>{@link ResponseHeaderReceived#httpRequest()}</li>
         *     <li>{@link ResponseHeaderReceived#uploadProgressSnapshot()}</li>
         *     <li>{@link ResponseHeaderReceived#httpResponse()}</li>
         * </ol>
         */
        @Immutable
        @ThreadSafe
        @SdkPublicApi
        @SdkPreviewApi
        public interface ResponseHeaderReceived extends RequestBytesSent {
            SdkHttpResponse httpResponse();

            /**
             * The immutable {@link ProgressSnapshot} to track download progress state
             */
            ProgressSnapshot downloadProgressSnapshot();
        }

        /**
         * Additional bytes received
         * <p>
         * Available context attributes:
         * <ol>
         *     <li>{@link ResponseBytesReceived#request()}</li>
         *     <li>{@link ResponseBytesReceived#httpRequest()} ()}</li>
         *     <li>{@link ResponseBytesReceived#uploadProgressSnapshot()}</li>
         *     <li>{@link ResponseBytesReceived#httpResponse()}</li>
         *     <li>{@link ResponseBytesReceived#downloadProgressSnapshot()}</li>
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
         *     <li>{@link ExecutionSuccess#httpRequest()}</li>
         *     <li>{@link ExecutionSuccess#uploadProgressSnapshot()}</li>
         *     <li>{@link ExecutionSuccess#httpResponse()}</li>
         *     <li>{@link ExecutionSuccess#downloadProgressSnapshot()}</li>
         *     <li>{@link ExecutionSuccess#response()}</li>
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
            Optional<SdkResponse> response();
        }

        /**
         * The request execution failed.
         * <p>
         * Available context attributes:
         * <ol>
         *     <li>{@link ExecutionFailure#request()}</li>
         *     <li>{@link ExecutionFailure#httpRequest()}</li>
         *     <li>{@link ExecutionFailure#uploadProgressSnapshot()}</li>
         *     <li>{@link ExecutionFailure#httpResponse()} ()}</li>
         *     <li>{@link ExecutionFailure#downloadProgressSnapshot()}</li>
         *     <li>{@link ExecutionFailure#exception()}</li>
         * </ol>
         */
        @Immutable
        @ThreadSafe
        @SdkPublicApi
        @SdkPreviewApi
        public interface ExecutionFailure extends ResponseBytesReceived {
            /**
             * The exception associated with the failed request.
             */
            Throwable exception();
        }
    }
}