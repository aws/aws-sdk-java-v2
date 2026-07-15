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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Lock-free bounded pool. {@link #acquire()} returns a reused instance if one is available, otherwise creates a fresh
 * one via the supplier. {@link #release(Object)} returns an instance to the pool, dropping it on the floor if the pool
 * is at capacity.
 *
 * <p>The pool is intentionally non-blocking: {@code acquire} never waits. Callers absorb the cost of fresh allocation
 * if the pool is empty. This keeps signing latency stable under contention.
 */
@SdkInternalApi
final class Pool<T> {

    private final Supplier<T> supplier;
    private final int maxSize;
    private final ConcurrentLinkedQueue<T> pool;

    Pool(int maxItems, Supplier<T> supplier) {
        this.supplier = supplier;
        this.maxSize = maxItems;
        this.pool = new ConcurrentLinkedQueue<>();
    }

    T acquire() {
        T cached = pool.poll();
        return cached != null ? cached : supplier.get();
    }

    void release(T obj) {
        if (pool.size() < maxSize) {
            pool.offer(obj);
        }
    }
}
