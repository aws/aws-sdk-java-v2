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

package software.amazon.awssdk.retries.internal.circuitbreaker;

import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A lock-free implementation of a token bucket. Tokens can be acquired from the bucket as long as there is sufficient capacity in
 * the bucket.
 */
@SdkInternalApi
public final class TokenBucket {
    private final int maxCapacity;
    private final AtomicInteger capacity;

    /**
     * Create a bucket containing the specified number of tokens.
     */
    TokenBucket(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.capacity = new AtomicInteger(maxCapacity);
    }

    /**
     * Try to acquire a certain number of tokens from this bucket. If there aren't sufficient tokens in this bucket then
     * {@link AcquireResponse#acquisitionFailed()} returns {@code true}.
     */
    public AcquireResponse tryAcquire(int amountToAcquire) {
        Validate.isNotNegative(amountToAcquire, "amountToAcquire");
        AcquireResponse.Builder responseBuilder = AcquireResponse.builder()
                                                                 .maxCapacity(maxCapacity)
                                                                 .capacityRequested(amountToAcquire);

        if (amountToAcquire == 0) {
            return responseBuilder
                .acquisitionFailed(false)
                .capacityAcquired(0)
                .capacityRemaining(capacity.get())
                .build();
        }

        int currentCapacity;
        int newCapacity;
        do {
            currentCapacity = capacity.get();
            newCapacity = currentCapacity - amountToAcquire;
            if (newCapacity < 0) {
                return responseBuilder
                    .acquisitionFailed(true)
                    .capacityAcquired(0)
                    .capacityRemaining(capacity.get())
                    .build();

            }
        } while (!capacity.compareAndSet(currentCapacity, newCapacity));

        return responseBuilder
            .acquisitionFailed(false)
            .capacityAcquired(amountToAcquire)
            .capacityRemaining(newCapacity)
            .build();
    }

    /**
     * Release a certain number of tokens back to this bucket. If this number of tokens would exceed the maximum number of tokens
     * configured for the bucket, the bucket is instead set to the maximum value and the additional tokens are discarded.
     */
    public ReleaseResponse release(int amountToRelease) {
        Validate.isTrue(amountToRelease >= 0, "Amount must not be negative.");
        ReleaseResponse.Builder builder =
            ReleaseResponse.builder()
                           .capacityReleased(amountToRelease)
                           .maxCapacity(maxCapacity);

        if (amountToRelease == 0) {
            return builder.currentCapacity(capacity.get())
                          .build();
        }

        int currentCapacity;
        int newCapacity;
        do {
            currentCapacity = capacity.get();
            newCapacity = Math.min(currentCapacity + amountToRelease, maxCapacity);
        } while (!capacity.compareAndSet(currentCapacity, newCapacity));

        return builder.currentCapacity(newCapacity)
                      .build();
    }

    /**
     * Retrieve a snapshot of the current number of tokens in the bucket. Because this number is constantly changing, it's
     * recommended to refer to the {@link AcquireResponse#capacityRemaining()} returned by the {@link #tryAcquire(int)} method
     * whenever possible.
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
    public String toString() {
        return ToString.builder("TokenBucket")
                       .add("maxCapacity", maxCapacity)
                       .add("capacity", capacity)
                       .build();
    }
}
