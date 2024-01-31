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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.progress.listener.ProgressListener;

public class CaptureProgressListener implements ProgressListener {
    public Boolean requestPrepared() {
        return requestPrepared;
    }

    public Boolean requestHeaderSent() {
        return requestHeaderSent;
    }

    public Boolean responseHeaderReceived() {
        return responseHeaderReceived;
    }

    public Boolean executionSuccess() {
        return executionSuccess;
    }

    public List<Double> ratioTransferredList() {
        return Collections.unmodifiableList(ratioTransferredList);
    }

    public CompletableFuture<Void> completionFuture() {
        return completionFuture;
    }

    public Throwable exceptionCaught() {
        return exceptionCaught;
    }

    private volatile boolean requestPrepared = false;
    private volatile boolean requestHeaderSent = false;
    private volatile boolean responseHeaderReceived = false;
    private volatile boolean executionSuccess = false;
    CompletableFuture<Void> completionFuture = new CompletableFuture<>();

    private final List<Double> ratioTransferredList = new ArrayList<>();
    private Throwable exceptionCaught;

    @Override
    public void requestPrepared(Context.RequestPrepared context) {
        requestPrepared = true;
        context.uploadProgressSnapshot().ratioTransferred().ifPresent(ratioTransferredList::add);

    }

    @Override
    public void requestHeaderSent(Context.RequestHeaderSent context) {
        requestHeaderSent = true;
        context.uploadProgressSnapshot().ratioTransferred().ifPresent(ratioTransferredList::add);
    }

    @Override
    public void requestBytesSent(Context.RequestBytesSent context) {
        context.uploadProgressSnapshot().ratioTransferred().ifPresent(ratioTransferredList::add);
    }

    @Override
    public void responseHeaderReceived(Context.ResponseHeaderReceived context) {
        responseHeaderReceived = true;
        context.uploadProgressSnapshot().ratioTransferred().ifPresent(ratioTransferredList::add);
    }

    @Override
    public void responseBytesReceived(Context.ResponseBytesReceived context) {
        context.uploadProgressSnapshot().ratioTransferred().ifPresent(ratioTransferredList::add);
    }

    @Override
    public void executionSuccess(Context.ExecutionSuccess context) {
        context.uploadProgressSnapshot().ratioTransferred().ifPresent(ratioTransferredList::add);
        executionSuccess = true;
        completionFuture.complete(null);

    }

    @Override
    public void executionFailure(Context.ExecutionFailure context) {
        exceptionCaught = context.exception();
        completionFuture.completeExceptionally(exceptionCaught);
    }
}

