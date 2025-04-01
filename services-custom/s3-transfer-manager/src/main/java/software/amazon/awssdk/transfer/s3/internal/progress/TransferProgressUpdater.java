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

package software.amazon.awssdk.transfer.s3.internal.progress;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.listener.AsyncRequestBodyListener;
import software.amazon.awssdk.core.async.listener.AsyncResponseTransformerListener;
import software.amazon.awssdk.core.async.listener.PublisherListener;
import software.amazon.awssdk.crt.s3.S3MetaRequestProgress;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.model.CompletedObjectTransfer;
import software.amazon.awssdk.transfer.s3.model.TransferObjectRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;

/**
 * An SDK-internal helper class that facilitates updating a {@link TransferProgress} and invoking {@link TransferListener}s.
 */
@SdkInternalApi
public class TransferProgressUpdater {
    private final DefaultTransferProgress progress;
    private final TransferListenerContext context;
    private final TransferListenerInvoker listenerInvoker;
    private final CompletableFuture<Void> endOfStreamFuture;

    public TransferProgressUpdater(TransferObjectRequest request,
                                   Long contentLength) {
        DefaultTransferProgressSnapshot.Builder snapshotBuilder = DefaultTransferProgressSnapshot.builder();
        snapshotBuilder.transferredBytes(0L);
        Optional.ofNullable(contentLength).ifPresent(snapshotBuilder::totalBytes);
        TransferProgressSnapshot snapshot = snapshotBuilder.build();
        progress = new DefaultTransferProgress(snapshot);
        context = TransferListenerContext.builder()
                                         .request(request)
                                         .progressSnapshot(snapshot)
                                         .build();

        listenerInvoker = request.transferListeners() == null
                          ? new TransferListenerInvoker(Collections.emptyList())
                          : new TransferListenerInvoker(request.transferListeners());

        endOfStreamFuture = new CompletableFuture<>();
    }

    public TransferProgress progress() {
        return progress;
    }

    public void transferInitiated() {
        listenerInvoker.transferInitiated(context);
    }

    public AsyncRequestBody wrapRequestBody(AsyncRequestBody requestBody) {
        return AsyncRequestBodyListener.wrap(
            requestBody,
            new AsyncRequestBodyListener() {
                final AtomicBoolean done = new AtomicBoolean(false);

                @Override
                public void publisherSubscribe(Subscriber<? super ByteBuffer> subscriber) {
                    resetBytesTransferred();
                }

                @Override
                public void subscriberOnNext(ByteBuffer byteBuffer) {
                    incrementBytesTransferred(byteBuffer.limit());
                    progress.snapshot().ratioTransferred().ifPresent(ratioTransferred -> {
                        if (Double.compare(ratioTransferred, 1.0) == 0) {
                            endOfStreamFutureCompleted();
                        }
                    });
                }

                @Override
                public void subscriberOnError(Throwable t) {
                    transferFailed(t);
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

    /**
     * Progress listener for Java-based S3Client with multipart enabled.
     */
    public PublisherListener<Long> multipartClientProgressListener() {

        return new PublisherListener<Long>() {
            @Override
            public void publisherSubscribe(Subscriber<? super Long> subscriber) {
                resetBytesTransferred();
            }

            @Override
            public void subscriberOnNext(Long contentLength) {
                incrementBytesTransferred(contentLength);
            }

            @Override
            public void subscriberOnError(Throwable t) {
                transferFailed(t);
            }

            @Override
            public void subscriberOnComplete() {
                endOfStreamFuture.complete(null);
            }
        };
    }

    public PublisherListener<S3MetaRequestProgress> crtProgressListener() {
        return new PublisherListener<S3MetaRequestProgress>() {
            @Override
            public void publisherSubscribe(Subscriber<? super S3MetaRequestProgress> subscriber) {
                resetBytesTransferred();
            }

            @Override
            public void subscriberOnNext(S3MetaRequestProgress s3MetaRequestProgress) {
                if (!progress.snapshot().totalBytes().isPresent() && s3MetaRequestProgress.getContentLength() != 0) {
                    progress.updateAndGet(b -> b.totalBytes(s3MetaRequestProgress.getContentLength()));
                }
                incrementBytesTransferred(s3MetaRequestProgress.getBytesTransferred());
            }

            @Override
            public void subscriberOnError(Throwable t) {
                transferFailed(t);
            }

            @Override
            public void subscriberOnComplete() {
                endOfStreamFuture.complete(null);
            }
        };
    }

    public <ResultT> AsyncResponseTransformer<GetObjectResponse, ResultT> wrapResponseTransformerForMultipartDownload(
        AsyncResponseTransformer<GetObjectResponse, ResultT> responseTransformer, GetObjectRequest request) {
        return AsyncResponseTransformerListener.wrap(
            responseTransformer,
            new BaseAsyncResponseTransformerListener() {
                @Override
                public void transformerOnResponse(GetObjectResponse response) {
                    // if the GetObjectRequest is a range-get, the Content-Length headers of the response needs to be used
                    // to update progress since the Content-Range would incorrectly upgrade progress with the whole object
                    // size.
                    if (request.range() != null) {
                        if (response.contentLength() != null) {
                            progress.updateAndGet(b -> b.totalBytes(response.contentLength()).sdkResponse(response));
                        }
                    } else {
                        // if the GetObjectRequest is not a range-get, it might be a part-get. In that case, we need to parse
                        // the Content-Range header to get the correct totalByte amount.
                        ContentRangeParser
                            .totalBytes(response.contentRange())
                            .ifPresent(totalBytes -> progress.updateAndGet(b -> b.totalBytes(totalBytes).sdkResponse(response)));
                    }
                }
            }
        );
    }

    public <ResultT> AsyncResponseTransformer<GetObjectResponse, ResultT> wrapResponseTransformer(
        AsyncResponseTransformer<GetObjectResponse, ResultT> responseTransformer) {
        return AsyncResponseTransformerListener.wrap(
            responseTransformer,
            new BaseAsyncResponseTransformerListener() {
                @Override
                public void transformerOnResponse(GetObjectResponse response) {
                    if (response.contentLength() != null) {
                        progress.updateAndGet(b -> b.totalBytes(response.contentLength()).sdkResponse(response));
                    }
                }
            });
    }

    private void resetBytesTransferred() {
        progress.updateAndGet(b -> b.transferredBytes(0L));
    }

    private void incrementBytesTransferred(long numBytes) {
        TransferProgressSnapshot snapshot = progress.updateAndGet(b -> {
            b.transferredBytes(b.getTransferredBytes() + numBytes);
        });
        listenerInvoker.bytesTransferred(context.copy(b -> b.progressSnapshot(snapshot)));
    }

    public void registerCompletion(CompletableFuture<? extends CompletedObjectTransfer> future) {
        future.whenComplete((r, t) -> {
            if (t == null) {
                endOfStreamFuture.whenComplete((r2, t2) -> {
                    if (t2 == null) {
                        transferComplete(r);
                    } else {
                        transferFailed(t2);
                    }
                });
            } else {
                transferFailed(t);
            }
        });
    }

    private void transferComplete(CompletedObjectTransfer r) {
        listenerInvoker.transferComplete(context.copy(b -> {
            TransferProgressSnapshot snapshot = progress.snapshot();
            if (!snapshot.sdkResponse().isPresent()) {
                snapshot = progress.updateAndGet(p -> p.sdkResponse(r.response()));
            }

            b.progressSnapshot(snapshot);
            b.completedTransfer(r);
        }));
    }

    private void transferFailed(Throwable t) {
        listenerInvoker.transferFailed(TransferListenerFailedContext.builder()
                                                                    .transferContext(
                                                                        context.copy(
                                                                            b -> b.progressSnapshot(progress.snapshot())))
                                                                    .exception(t)
                                                                    .build());
    }

    private class BaseAsyncResponseTransformerListener implements AsyncResponseTransformerListener<GetObjectResponse> {
        @Override
        public void transformerOnResponse(GetObjectResponse response) {
            if (response.contentLength() != null) {
                progress.updateAndGet(b -> b.totalBytes(response.contentLength()).sdkResponse(response));
            }
        }

        @Override
        public void transformerExceptionOccurred(Throwable t) {
            transferFailed(t);
        }

        @Override
        public void publisherSubscribe(Subscriber<? super ByteBuffer> subscriber) {
            resetBytesTransferred();
        }

        @Override
        public void subscriberOnNext(ByteBuffer byteBuffer) {
            incrementBytesTransferred(byteBuffer.limit());
        }

        @Override
        public void subscriberOnError(Throwable t) {
            transferFailed(t);
        }

        @Override
        public void subscriberOnComplete() {
            endOfStreamFuture.complete(null);
        }

    }
}
