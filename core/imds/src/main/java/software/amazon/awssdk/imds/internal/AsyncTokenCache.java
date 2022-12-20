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

package software.amazon.awssdk.imds.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@SdkInternalApi
final class AsyncTokenCache implements Supplier<CompletableFuture<Token>> {

    private volatile Token cachedToken;
    private final Supplier<CompletableFuture<Token>> supplier;
    private final Object refreshLock = new Object();

    // TODO [DRAFT] add the ability to pass in the execution service
    private final ExecutorService executorService = ForkJoinPool.commonPool();

    AsyncTokenCache(Supplier<CompletableFuture<Token>> supplier) {
        this.supplier = supplier;
    }

    /**
     * @return an already completed future if the cached value is still fresh. If the cached value was stale, the returned future
     * that will be completed once the refresh has been completed.
     */
    @Override
    public CompletableFuture<Token> get() {
        CompletableFuture<Token> returnFuture = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> refreshIfNeededAndComplete(returnFuture), executorService);
        return returnFuture;
    }

    private void refreshIfNeededAndComplete(CompletableFuture<Token> parentFuture) {
        if (needsRefresh()) {
            synchronized (refreshLock) {
                // the value might have been refreshed while waiting on the lock,
                // if so, refresh it only if it has already expired agan.
                if (needsRefresh()) {
                    try {
                        CompletableFuture<Token> tokenFuture = supplier.get();
                        CompletableFutureUtils.forwardExceptionTo(parentFuture, tokenFuture);
                        cachedToken = tokenFuture.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        parentFuture.completeExceptionally(e);
                    } catch (ExecutionException e) {
                        Throwable completionError = e.getCause() == null ? e : e.getCause();
                        parentFuture.completeExceptionally(completionError);
                    } catch (Exception e) {
                        parentFuture.completeExceptionally(e);
                    }
                }
            }
        }
        parentFuture.complete(cachedToken);
    }

    private boolean needsRefresh() {
        return cachedToken == null || cachedToken.isExpired();
    }

}
