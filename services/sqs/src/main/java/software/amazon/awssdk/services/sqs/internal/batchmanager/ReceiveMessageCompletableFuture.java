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

package software.amazon.awssdk.services.sqs.internal.batchmanager;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@SdkInternalApi
public class ReceiveMessageCompletableFuture {

    private final long waitTimeDeadlineNano;
    private final CompletableFuture<ReceiveMessageResponse> responseCompletableFuture = new CompletableFuture<>();
    private final int requestedSize;

    private volatile ScheduledFuture<?> timeoutFuture;


    public ReceiveMessageCompletableFuture(int paramSize, Duration waitTime) {
        this.requestedSize = paramSize;
        this.waitTimeDeadlineNano = System.nanoTime() + waitTime.toNanos();
    }


    public void startWaitTimer(ScheduledExecutorService executorService) {
        if (timeoutFuture != null || responseCompletableFuture.isDone()) {
            return;
        }

        long remaining = waitTimeDeadlineNano - System.nanoTime();
        if (remaining <= 0) {
            timeout();
        } else {
            timeoutFuture = executorService.schedule(this::timeout, remaining, TimeUnit.NANOSECONDS);
        }
    }

    public boolean isExpired() {
        return System.nanoTime() > waitTimeDeadlineNano;
    }


    public void setSuccess(ReceiveMessageResponse result) {
        cancelTimeout();
        responseCompletableFuture.complete(result);
    }


    public void setFailure(Throwable exception) {
        cancelTimeout();
        responseCompletableFuture.completeExceptionally(exception);
    }

    private void timeout() {
        responseCompletableFuture.complete(ReceiveMessageResponse.builder().build());
    }

    public int requestedSize() {
        return requestedSize;
    }


    public CompletableFuture<ReceiveMessageResponse> responseCompletableFuture() {
        return responseCompletableFuture;
    }

    private synchronized void cancelTimeout() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
    }

}