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

package software.amazon.awssdk.core.internal.capacity;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.retry.conditions.TokenBucketRetryCondition.Capacity;
import software.amazon.awssdk.utils.Validate;

/**
 * A lock-free implementation of a token bucket. Tokens can be acquired from the bucket as long as there is sufficient capacity
 * in the bucket.
 */
@SdkInternalApi
public class TokenBucket {
    private final int maxCapacity;
    private final AtomicInteger capacity;

    /**
     * Create a bucket containing the specified number of tokens.
     */
    public TokenBucket(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.capacity = new AtomicInteger(maxCapacity);
    }

    /**
     * Try to acquire a certain number of tokens from this bucket. If there aren't sufficient tokens in this bucket,
     * {@link Optional#empty()} is returned.
     */
    public Optional<Capacity> tryAcquire(int amountToAcquire) {
        Validate.isTrue(amountToAcquire >= 0, "Amount must not be negative.");

        if (amountToAcquire == 0) {
            return Optional.of(Capacity.builder()
                                       .capacityAcquired(0)
                                       .capacityRemaining(capacity.get())
                                       .build());
        }

        int currentCapacity;
        int newCapacity;
        do {
            currentCapacity = capacity.get();
            newCapacity = currentCapacity - amountToAcquire;

            if (newCapacity < 0) {
                return Optional.empty();
            }
        } while (!capacity.compareAndSet(currentCapacity, newCapacity));

        return Optional.of(Capacity.builder()
                                   .capacityAcquired(amountToAcquire)
                                   .capacityRemaining(newCapacity)
                                   .build());
    }

    /**
     * Release a certain number of tokens back to this bucket. If this number of tokens would exceed the maximum number of tokens
     * configured for the bucket, the bucket is instead set to the maximum value and the additional tokens are discarded.
     */
    public void release(int amountToRelease) {
        Validate.isTrue(amountToRelease >= 0, "Amount must not be negative.");

        if (amountToRelease == 0) {
            return;
        }

        int currentCapacity;
        int newCapacity;
        do {
            currentCapacity = capacity.get();

            if (currentCapacity == maxCapacity) {
                return;
            }

            newCapacity = Math.min(currentCapacity + amountToRelease, maxCapacity);
        } while (!capacity.compareAndSet(currentCapacity, newCapacity));
    }

    /**
     * Retrieve a snapshot of the current number of tokens in the bucket. Because this number is constantly changing, it's
     * recommended to refer to the {@link Capacity#capacityRemaining()} returned by the {@link #tryAcquire(int)} method whenever
     * possible.
     */
    public int currentCapacity() {
        return capacity.get();
    }

    /**
     * Retrieve the maximum capacity of the bucket configured when the bucket was created.
     */
    public int maxCapacity() {
        return maxCapacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TokenBucket that = (TokenBucket) o;

        if (maxCapacity != that.maxCapacity) {
            return false;
        }
        return capacity.get() == that.capacity.get();
    }

    @Override
    public int hashCode() {
        int result = maxCapacity;
        result = 31 * result + capacity.get();
        return result;
    }
}
