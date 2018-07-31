/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.internal.http.timers;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Asynchronous implementations of {@link TimeoutTask} to be scheduled to fail
 * the {@link CompletableFuture} and abort the asynchronous requests.
 */
@SdkInternalApi
public class AsyncTimeoutTask implements TimeoutTask {
    private static final Logger log = Logger.loggerFor(AsyncTimeoutTask.class);
    private final SdkException exception;
    private volatile boolean hasExecuted;

    private final CompletableFuture<?> completableFuture;
    private Abortable abortable;

    /**
     * Constructs a new {@link AsyncTimeoutTask}.
     *
     * @param completableFuture the {@link CompletableFuture} to fail
     * @param exception the exception to thrown
     */
    public AsyncTimeoutTask(CompletableFuture<?> completableFuture, SdkException exception) {
        this.completableFuture = Validate.paramNotNull(completableFuture, "completableFuture");
        this.exception = Validate.paramNotNull(exception, "exception");
    }

    @Override
    public void abortable(Abortable abortable) {
        this.abortable = abortable;
    }

    @Override
    public void run() {
        hasExecuted = true;
        if (!completableFuture.isDone()) {
            completableFuture.completeExceptionally(exception);
        }

        if (abortable != null) {
            abortable.abort();
        }
    }

    @Override
    public boolean hasExecuted() {
        return hasExecuted;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
