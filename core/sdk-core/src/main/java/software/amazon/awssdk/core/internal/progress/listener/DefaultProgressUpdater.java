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

import java.util.Collections;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.internal.progress.ProgressListenerContext;
import software.amazon.awssdk.core.internal.progress.ProgressListenerFailedContext;
import software.amazon.awssdk.core.internal.progress.snapshot.DefaultProgressSnapshot;
import software.amazon.awssdk.core.progress.listener.SdkExchangeProgress;
import software.amazon.awssdk.core.progress.snapshot.ProgressSnapshot;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * ProgressUpdater exposes methods that invokes listener methods to update and store request progress state
 */
@SdkInternalApi
public class DefaultProgressUpdater implements ProgressUpdater {
    private final DefaultSdkExchangeProgress requestBodyProgress;
    private final DefaultSdkExchangeProgress responseBodyProgress;
    private ProgressListenerContext context;
    private final ProgressListenerInvoker listenerInvoker;

    public DefaultProgressUpdater(SdkRequest sdkRequest,
                                  Long requestContentLength) {
        DefaultProgressSnapshot.Builder uploadProgressSnapshotBuilder = DefaultProgressSnapshot.builder();
        uploadProgressSnapshotBuilder.transferredBytes(0L);
        Optional.ofNullable(requestContentLength).ifPresent(uploadProgressSnapshotBuilder::totalBytes);

        ProgressSnapshot uploadProgressSnapshot = uploadProgressSnapshotBuilder.build();
        requestBodyProgress = new DefaultSdkExchangeProgress(uploadProgressSnapshot);

        DefaultProgressSnapshot.Builder downloadProgressSnapshotBuilder = DefaultProgressSnapshot.builder();
        downloadProgressSnapshotBuilder.transferredBytes(0L);
        ProgressSnapshot downloadProgressSnapshot = downloadProgressSnapshotBuilder.build();
        responseBodyProgress = new DefaultSdkExchangeProgress(downloadProgressSnapshot);

        context = ProgressListenerContext.builder()
                                         .request(sdkRequest)
                                         .uploadProgressSnapshot(uploadProgressSnapshot)
                                         .downloadProgressSnapshot(downloadProgressSnapshot)
                                         .build();

        listenerInvoker = new ProgressListenerInvoker(sdkRequest.overrideConfiguration()
                                                                .map(RequestOverrideConfiguration::progressListeners)
                                                                .orElse(Collections.emptyList()));
    }

    @Override
    public void updateRequestContentLength(Long requestContentLength) {
        requestBodyProgress.updateAndGet(b -> b.totalBytes(requestContentLength));
    }

    @Override
    public void updateResponseContentLength(Long responseContentLength) {
        responseBodyProgress.updateAndGet(b -> b.totalBytes(responseContentLength));
    }

    public SdkExchangeProgress requestBodyProgress() {
        return requestBodyProgress;
    }

    public SdkExchangeProgress responseBodyProgress() {
        return responseBodyProgress;
    }

    @Override
    public void requestPrepared(SdkHttpRequest httpRequest) {
        listenerInvoker.requestPrepared(context.copy(b -> b.httpRequest(httpRequest)));
    }

    @Override
    public void requestHeaderSent() {
        listenerInvoker.requestHeaderSent(context);
    }

    @Override
    public void resetBytesSent() {
        requestBodyProgress.updateAndGet(b -> b.transferredBytes(0L));
    }

    @Override
    public void resetBytesReceived() {
        responseBodyProgress.updateAndGet(b -> b.transferredBytes(0L));
    }

    @Override
    public void incrementBytesSent(long numBytes) {
        long uploadBytes = requestBodyProgress.progressSnapshot().transferredBytes();

        ProgressSnapshot snapshot = requestBodyProgress.updateAndGet(b -> b.transferredBytes(uploadBytes + numBytes));
        listenerInvoker.requestBytesSent(context.copy(b -> b.uploadProgressSnapshot(snapshot)));
    }

    @Override
    public void incrementBytesReceived(long numBytes) {
        long downloadedBytes = responseBodyProgress.progressSnapshot().transferredBytes();

        ProgressSnapshot snapshot = responseBodyProgress.updateAndGet(b -> b.transferredBytes(downloadedBytes + numBytes));
        listenerInvoker.responseBytesReceived(context.copy(b -> b.downloadProgressSnapshot(snapshot)));
    }

    @Override
    public void responseHeaderReceived() {
        listenerInvoker.responseHeaderReceived(context);
    }

    @Override
    public void executionSuccess(SdkResponse response) {

        listenerInvoker.executionSuccess(context.copy(b -> b.response(response)));
    }

    @Override
    public void executionFailure(Throwable t) {
        listenerInvoker.executionFailure(ProgressListenerFailedContext.builder()
                                                                      .progressListenerContext(
                                                                          context.copy(
                                                                              b -> {
                                                                                  b.uploadProgressSnapshot(
                                                                                      requestBodyProgress.progressSnapshot());
                                                                                  b.downloadProgressSnapshot(
                                                                                      responseBodyProgress.progressSnapshot());
                                                                              }))
                                                                      .exception(t)
                                                                      .build());
    }

    @Override
    public void attemptFailure(Throwable t) {
        listenerInvoker.attemptFailure(ProgressListenerFailedContext.builder()
                                                                              .progressListenerContext(
                                                                          context.copy(
                                                                              b -> {
                                                                                  b.uploadProgressSnapshot(
                                                                                      requestBodyProgress.progressSnapshot());
                                                                                  b.downloadProgressSnapshot(
                                                                                      responseBodyProgress.progressSnapshot());
                                                                              }))
                                                                              .exception(t)
                                                                              .build());
    }

    @Override
    public void attemptFailureResponseBytesReceived(Throwable t) {
        listenerInvoker.attemptFailureResponseBytesReceived(ProgressListenerFailedContext.builder()
                                                                    .progressListenerContext(
                                                                        context.copy(
                                                                            b -> {
                                                                                b.uploadProgressSnapshot(
                                                                                    requestBodyProgress.progressSnapshot());
                                                                                b.downloadProgressSnapshot(
                                                                                    responseBodyProgress.progressSnapshot());
                                                                            }))
                                                                    .exception(t)
                                                                    .build());
    }
}
