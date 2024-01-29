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

package software.amazon.awssdk.core.internal.progress.listener;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.listener.AsyncRequestBodyListener;
import software.amazon.awssdk.core.internal.progress.ProgressListenerContext;
import software.amazon.awssdk.core.internal.progress.ProgressListenerFailedContext;
import software.amazon.awssdk.core.internal.progress.snapshot.DefaultProgressSnapshot;
import software.amazon.awssdk.core.progress.listener.SdkRequestProgress;
import software.amazon.awssdk.core.progress.snapshot.ProgressSnapshot;

/**
 * ProgressUpdater exposes methods that invokes listener methods to update and store request progress state
 */
@SdkInternalApi
public class ProgressUpdater {
    private final DefaultSdkRequestProgress uploadProgress;
    private final DefaultSdkRequestProgress downloadProgress;
    private final ProgressListenerContext context;
    private final ProgressListenerInvoker listenerInvoker;
    private final CompletableFuture<Void> endOfStreamFuture;

    public ProgressUpdater(SdkRequest sdkRequest,
                           Long contentLength) {
        DefaultProgressSnapshot.Builder uploadProgressSnapshotBuilder = DefaultProgressSnapshot.builder();
        uploadProgressSnapshotBuilder.transferredBytes(0L);
        Optional.ofNullable(contentLength).ifPresent(uploadProgressSnapshotBuilder::totalBytes);

        ProgressSnapshot uploadProgressSnapshot = uploadProgressSnapshotBuilder.build();
        uploadProgress = new DefaultSdkRequestProgress(uploadProgressSnapshot);

        DefaultProgressSnapshot.Builder downloadProgressSnapshotBuilder = DefaultProgressSnapshot.builder();
        downloadProgressSnapshotBuilder.transferredBytes(0L);
        Optional.ofNullable(contentLength).ifPresent(downloadProgressSnapshotBuilder::totalBytes);

        ProgressSnapshot downloadProgressSnapshot = downloadProgressSnapshotBuilder.build();
        downloadProgress = new DefaultSdkRequestProgress(downloadProgressSnapshot);

        context = ProgressListenerContext.builder()
                                         .request(sdkRequest)
                                         .uploadProgressSnapshot(uploadProgressSnapshot)
                                         .downloadProgressSnapshot(downloadProgressSnapshot)
                                         .build();

        listenerInvoker = sdkRequest.overrideConfiguration().get().progressListeners() == null
                          ? new ProgressListenerInvoker((Collections.emptyList()))
                          : new ProgressListenerInvoker(sdkRequest.overrideConfiguration().get().progressListeners());

        endOfStreamFuture = new CompletableFuture<>();
    }

    public SdkRequestProgress uploadProgress() {
        return uploadProgress;
    }

    public SdkRequestProgress downloadProgress() {
        return downloadProgress;
    }

    public AsyncRequestBody wrapUploadRequestBody(AsyncRequestBody requestBody) {
        return AsyncRequestBodyListener.wrap(
            requestBody,
            new AsyncRequestBodyListener() {
                final AtomicBoolean done = new AtomicBoolean(false);

                @Override
                public void publisherSubscribe(Subscriber<? super ByteBuffer> subscriber) {
                    resetBytesSent();
                }

                @Override
                public void subscriberOnNext(ByteBuffer byteBuffer) {
                    incrementBytesSent(byteBuffer.limit());
                    uploadProgress.progressSnapshot().ratioTransferred().ifPresent(ratioTransferred -> {
                        if (Double.compare(ratioTransferred, 1.0) == 0) {
                            endOfStreamFutureCompleted();
                        }
                    });
                }

                @Override
                public void subscriberOnError(Throwable t) {
                    attemptFailure(t);
                }

                @Override
                public void subscriberOnComplete() {
                    endOfStreamFutureCompleted();
                }

                private void endOfStreamFutureCompleted() {
                    if (done.compareAndSet(false, true)) {
                        endOfStreamFuture.complete(null);
                    }
                }
            });
    }

    public AsyncRequestBody wrapDownloadRequestBody(AsyncRequestBody requestBody) {
        return AsyncRequestBodyListener.wrap(
            requestBody,
            new AsyncRequestBodyListener() {
                final AtomicBoolean done = new AtomicBoolean(false);

                @Override
                public void publisherSubscribe(Subscriber<? super ByteBuffer> subscriber) {
                    resetBytesReceived();
                }

                @Override
                public void subscriberOnNext(ByteBuffer byteBuffer) {
                    incrementBytesReceived(byteBuffer.limit());
                    downloadProgress.progressSnapshot().ratioTransferred().ifPresent(ratioTransferred -> {
                        if (Double.compare(ratioTransferred, 1.0) == 0) {
                            endOfStreamFutureCompleted();
                        }
                    });
                }

                @Override
                public void subscriberOnError(Throwable t) {
                    attemptFailure(t);
                }

                @Override
                public void subscriberOnComplete() {
                    endOfStreamFutureCompleted();
                }

                private void endOfStreamFutureCompleted() {
                    if (done.compareAndSet(false, true)) {
                        endOfStreamFuture.complete(null);
                    }
                }
            });
    }

    public void requestPrepared() {
        listenerInvoker.requestPrepared(context);
    }

    public void requestHeaderSent() {
        listenerInvoker.requestHeaderSent(context);
    }

    public void resetBytesSent() {
        uploadProgress.updateAndGet(b -> b.transferredBytes(0L));
    }

    public void resetBytesReceived() {
        downloadProgress.updateAndGet(b -> b.transferredBytes(0L));
    }

    public void incrementBytesSent(long numBytes) {
        long uploadBytes = uploadProgress.progressSnapshot().transferredBytes();

        ProgressSnapshot snapshot = uploadProgress.updateAndGet(b -> b.transferredBytes(uploadBytes + numBytes));
        listenerInvoker.requestBytesSent(context.copy(b -> b.uploadProgressSnapshot(snapshot)));
    }

    public void incrementBytesReceived(long numBytes) {
        long downloadedBytes = downloadProgress.progressSnapshot().transferredBytes();

        ProgressSnapshot snapshot = downloadProgress.updateAndGet(b -> b.transferredBytes(downloadedBytes + numBytes));
        listenerInvoker.responseBytesReceived(context.copy(b -> b.downloadProgressSnapshot(snapshot)));
    }

    public void registerCompletion(CompletableFuture<? extends SdkResponse> future) {
        future.whenComplete((r, t) -> {
            if (t == null) {
                endOfStreamFuture.whenComplete((r2, t2) -> {
                    if (t2 == null) {
                        executionSuccess(r);
                    } else {
                        attemptFailure(t2);
                    }
                });
            } else {
                executionFailure(t);
            }
        });
    }

    public void responseHeaderReceived() {
        listenerInvoker.responseHeaderReceived(context);
    }

    public void executionSuccess(SdkResponse response) {

        listenerInvoker.executionSuccess(context.copy(b -> b.response(response)));
    }

    private void executionFailure(Throwable t) {
        listenerInvoker.executionFailure(ProgressListenerFailedContext.builder()
                                                                      .progressListenerContext(
                                                                          context.copy(
                                                                              b -> {
                                                                                  b.uploadProgressSnapshot(
                                                                                      uploadProgress.progressSnapshot());
                                                                                  b.downloadProgressSnapshot(
                                                                                      downloadProgress.progressSnapshot());
                                                                              }))
                                                                      .exception(t)
                                                                      .build());
    }

    private void attemptFailure(Throwable t) {
        listenerInvoker.executionFailure(ProgressListenerFailedContext.builder()
                                                                      .progressListenerContext(
                                                                          context.copy(
                                                                              b -> {
                                                                                  b.uploadProgressSnapshot(
                                                                                      uploadProgress.progressSnapshot());
                                                                                  b.downloadProgressSnapshot(
                                                                                      downloadProgress.progressSnapshot());
                                                                              }))
                                                                      .exception(t)
                                                                      .build());
    }
}
