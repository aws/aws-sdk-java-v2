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
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedTransfer;
import software.amazon.awssdk.transfer.s3.DownloadRequest;
import software.amazon.awssdk.transfer.s3.TransferRequestOverrideConfiguration;
import software.amazon.awssdk.transfer.s3.UploadRequest;
import software.amazon.awssdk.transfer.s3.internal.progress.NotifyingAsyncRequestBody.AsyncRequestBodyListener;
import software.amazon.awssdk.transfer.s3.internal.progress.NotifyingAsyncResponseTransformer.AsyncResponseTransformerListener;
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
    private final TransferListenerInvoker listeners;

    public TransferProgressUpdater(UploadRequest request, AsyncRequestBody requestBody) {
        DefaultTransferProgressSnapshot.Builder snapshotBuilder = DefaultTransferProgressSnapshot.builder();
        getContentLengthSafe(requestBody).ifPresent(snapshotBuilder::transferSizeInBytes);
        TransferProgressSnapshot snapshot = snapshotBuilder.build();
        progress = new DefaultTransferProgress(snapshot);
        context = TransferListenerContext.builder()
                                         .request(request)
                                         .progressSnapshot(snapshot)
                                         .build();
        listeners = new TransferListenerInvoker(request.overrideConfiguration()
                                                       .map(TransferRequestOverrideConfiguration::listeners)
                                                       .orElseGet(Collections::emptyList));
    }

    public TransferProgressUpdater(DownloadRequest request) {
        TransferProgressSnapshot snapshot = DefaultTransferProgressSnapshot.builder().build();
        progress = new DefaultTransferProgress(snapshot);
        context = TransferListenerContext.builder()
                                         .request(request)
                                         .progressSnapshot(snapshot)
                                         .build();
        listeners = new TransferListenerInvoker(request.overrideConfiguration()
                                                       .map(TransferRequestOverrideConfiguration::listeners)
                                                       .orElseGet(Collections::emptyList));
    }

    public TransferProgress progress() {
        return progress;
    }

    public void transferInitiated() {
        listeners.transferInitiated(context);
    }

    public AsyncRequestBody wrapRequestBody(AsyncRequestBody requestBody) {
        return new NotifyingAsyncRequestBody(
            requestBody,
            new AsyncRequestBodyListener() {
                @Override
                public void beforeSubscribe(Subscriber<? super ByteBuffer> subscriber) {
                    progress.updateAndGet(b -> b.bytesTransferred(0));
                }

                @Override
                public void beforeOnNext(ByteBuffer byteBuffer) {
                    TransferProgressSnapshot snapshot = progress.updateAndGet(b -> {
                        b.bytesTransferred(b.getBytesTransferred() + byteBuffer.limit());
                    });
                    listeners.bytesTransferred(context.copy(b -> b.progressSnapshot(snapshot)));
                }
            });
    }

    public AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> wrapResponseTransformer(
        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer) {
        return new NotifyingAsyncResponseTransformer<>(
            responseTransformer,
            new AsyncResponseTransformerListener<GetObjectResponse, GetObjectResponse>() {
                @Override
                public void beforeOnResponse(GetObjectResponse response) {
                    if (response.contentLength() != null) {
                        progress.updateAndGet(b -> b.transferSizeInBytes(response.contentLength()));
                    }
                }

                @Override
                public void beforeSubscribe(Subscriber<? super ByteBuffer> subscriber) {
                    progress.updateAndGet(b -> b.bytesTransferred(0));
                }

                @Override
                public void beforeOnNext(ByteBuffer byteBuffer) {
                    TransferProgressSnapshot snapshot = progress.updateAndGet(b -> {
                        b.bytesTransferred(b.getBytesTransferred() + byteBuffer.limit());
                    });
                    listeners.bytesTransferred(context.copy(b -> b.progressSnapshot(snapshot)));
                }
            });
    }

    public void registerCompletion(CompletableFuture<? extends CompletedTransfer> future) {
        future.whenComplete((r, t) -> {
            if (t == null) {
                listeners.transferComplete(context.copy(b -> {
                    b.progressSnapshot(progress.snapshot());
                    b.completedTransfer(r);
                }));
            } else {
                listeners.transferFailed(TransferListenerFailedContext.builder()
                                                                      .transferContext(context.copy(b -> {
                                                                          b.progressSnapshot(progress.snapshot());
                                                                      }))
                                                                      .exception(t)
                                                                      .build());
            }
        });
    }

    private static Optional<Long> getContentLengthSafe(AsyncRequestBody requestBody) {
        // requestBody.contentLength() may throw if the file does not exist.
        // We ignore any potential exception here to defer failure
        // to the s3CrtAsyncClient call and its associated future.
        try {
            return requestBody.contentLength();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
