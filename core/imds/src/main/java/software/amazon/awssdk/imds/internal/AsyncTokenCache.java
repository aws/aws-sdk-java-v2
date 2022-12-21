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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@SdkInternalApi
final class AsyncTokenCache implements Supplier<CompletableFuture<Token>> {

    /**
     * The currently cached value.
     * Only modified through synchronized block, under the refreshLock.
     */
    private volatile Token cachedToken;

    /**
     * The asynchronous operation that is used to refresh the token.
     */
    private final Supplier<CompletableFuture<Token>> supplier;

    /**
     * The collection of future that are waiting for the refresh call to complete. If a call to {@link AsyncTokenCache#get()}
     * is made while the token request is happening, a future will be added to this collection. All future in this collection
     * are completed once the token request is done.
     */
    private Collection<CompletableFuture<Token>> waitingFutures;

    /**
     * Indicates if the token refresh request is currently running or not.
     */
    private final AtomicBoolean refreshRunning;

    private final Object refreshLock = new Object();

    AsyncTokenCache(Supplier<CompletableFuture<Token>> supplier) {
        this.supplier = supplier;
        this.waitingFutures = new ArrayList<>();
        this.refreshRunning = new AtomicBoolean(false);
    }

    /**
     * @return an already completed future if the cached value is still fresh. If the cached value was stale, the returned future
     * that will be completed once the refresh has been completed.
     * In the case where multiple call to <pre>get</pre> are made while the token is expired, all CompletableFuture returned
     * will be
     * completed once the
     */
    @Override
    public CompletableFuture<Token> get() {
        Token currentValue = cachedToken;
        if (!needsRefresh(currentValue)) {
            return CompletableFuture.completedFuture(currentValue);
        }
        synchronized (refreshLock) { // Make sure the value wasn't refreshed while we were waiting for the lock.
            currentValue = cachedToken;
            if (!needsRefresh(currentValue)) {
                return CompletableFuture.completedFuture(currentValue);
            }
            CompletableFuture<Token> result = new CompletableFuture<>();
            waitingFutures.add(result);
            if (!refreshRunning.get()) {
                CompletableFuture<Token> tokenRequest = startRefresh();
                CompletableFutureUtils.forwardExceptionTo(result, tokenRequest);
            }
            return result;
        }
    }

    private CompletableFuture<Token> startRefresh() {
        CompletableFuture<Token> tokenRequest = supplier.get();
        refreshRunning.set(true); // After supplier.get(), in case that throws an exception
        tokenRequest.whenComplete((token, throwable) -> {
            Collection<CompletableFuture<Token>> toComplete;
            synchronized (refreshLock) {
                // Instead of completing the waiting future while holding the lock, we copy the list reference and
                // release the lock before completing them. This is just in case someone (naughty) is doing something
                // blocking on the complete calls. It's not good that they're doing that, but at least
                // it won't block other threads trying to acquire the lock.
                toComplete = waitingFutures;
                waitingFutures = new ArrayList<>();
                refreshRunning.set(false);
                if (token != null) {
                    cachedToken = token;
                }
            }

            toComplete.forEach(future -> {
                if (throwable == null) {
                    future.complete(token);
                } else {
                    future.completeExceptionally(throwable);
                }
            });
        });
        return tokenRequest;
    }

    private boolean needsRefresh(Token token) {
        return token == null || token.isExpired();
    }

}
