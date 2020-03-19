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

package software.amazon.awssdk.core.internal.http;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Wrapper for a {@link TransformingAsyncResponseHandler} that allows attachment to an external scope and given a way
 * of evaluating whether that scope has changed or not will only allow prepare() to be called on its delegate once
 * per state change and will cache and reserve the future that is returned by the delegate the rest of the time.
 * <p>
 * One application of this wrapper is to ensure that prepare() is not called on the underlying response handler more
 * than once per retry no matter where or how many times its invoked.
 * <p>
 * This class is asserted to be thread-safe and it should be fine to have multiple threads call prepare()
 * simultaneously.
 *
 * @param <T> The type of the wrapped {@link TransformingAsyncResponseHandler}
 * @param <R> The type of the object used to determine scope.
 */
@SdkInternalApi
@ThreadSafe
public class IdempotentAsyncResponseHandler<T, R> implements TransformingAsyncResponseHandler<T> {
    private final Supplier<R> newScopeSupplier;
    private final BiPredicate<R, R> newScopeInRangePredicate;
    private final TransformingAsyncResponseHandler<T> wrappedHandler;

    // Taking advantage of the volatile properties of AtomicReference
    private final AtomicReference<R> cachedScope = new AtomicReference<>();
    private final AtomicReference<CompletableFuture<T>> cachedPreparedFuture = new AtomicReference<>();

    private IdempotentAsyncResponseHandler(TransformingAsyncResponseHandler<T> wrappedHandler,
                                           Supplier<R> newScopeSupplier,
                                           BiPredicate<R, R> newScopeInRangePredicate) {
        this.newScopeSupplier = newScopeSupplier;
        this.newScopeInRangePredicate = newScopeInRangePredicate;
        this.wrappedHandler = wrappedHandler;
    }

    /**
     * Creates a new wrapped {@link TransformingAsyncResponseHandler}
     * @param wrappedHandler A different handler to wrap.
     * @param preparedScopeSupplier A function that retrieves the current scope to determine if a new future can be
     *                              prepared.
     * @param scopeInRangePredicate A predicate that can be used to test the cached scope against the newly retrieved
     *                             scope to determine if a new future can be prepared.
     * @param <T> The type of the wrapped {@link TransformingAsyncResponseHandler}
     * @param <R> The type of the object used to determine scope.
     */
    public static <T, R> IdempotentAsyncResponseHandler<T, R> create(
            TransformingAsyncResponseHandler<T> wrappedHandler,
            Supplier<R> preparedScopeSupplier,
            BiPredicate<R, R> scopeInRangePredicate) {

        return new IdempotentAsyncResponseHandler<>(wrappedHandler,
                                                    preparedScopeSupplier,
                                                    scopeInRangePredicate);
    }

    @Override
    public CompletableFuture<T> prepare() {
        // Test if the new scope is in range of the cached scope; if not delegate and cache result and new scope
        if (cachedPreparedFuture.get() == null ||
            !newScopeInRangePredicate.test(newScopeSupplier.get(), cachedScope.get())) {

            synchronized (this) {
                // The scope needs to be queried and tested again in case it was changed by another thread
                R newScope = newScopeSupplier.get();

                if (cachedPreparedFuture.get() == null || !newScopeInRangePredicate.test(newScope, cachedScope.get())) {
                    cachedPreparedFuture.set(this.wrappedHandler.prepare());
                    cachedScope.set(newScope);
                }
            }
        }

        return cachedPreparedFuture.get();
    }

    @Override
    public void onHeaders(SdkHttpResponse headers) {
        this.wrappedHandler.onHeaders(headers);
    }

    @Override
    public void onStream(Publisher<ByteBuffer> stream) {
        this.wrappedHandler.onStream(stream);
    }

    @Override
    public void onError(Throwable error) {
        this.wrappedHandler.onError(error);
    }
}
