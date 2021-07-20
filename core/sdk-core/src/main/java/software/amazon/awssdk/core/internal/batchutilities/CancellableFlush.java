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

package software.amazon.awssdk.core.internal.batchutilities;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CancellableFlush<T> implements Runnable {

    private final Object lock = new Object();
    private final BiFunction<String, Runnable, Future<CompletableFuture<T>>> flushBuffer;
    private final String destination;
    private boolean hasExecuted = false;
    private boolean isCancelled = false;

    public CancellableFlush(BiFunction<String, Runnable, Future<CompletableFuture<T>>> flushBuffer, String destination) {
        this.flushBuffer = flushBuffer;
        this.destination = destination;
    }

    @Override
    public void run() {
        synchronized (this.lock) {
            if (isCancelled) {
                return;
            }
            hasExecuted = true;
            try {
                flushBuffer.apply(destination, () -> {
                    hasExecuted = false;
                    isCancelled = false;
                });
            } catch (Exception e) {
                System.err.println("Exception: " + e);
            }
        }
    }

    public void cancel() {
        synchronized (this.lock) {
            isCancelled = true;
        }
    }

    public boolean hasExecuted() {
        synchronized (this.lock) {
            return hasExecuted;
        }
    }
}
