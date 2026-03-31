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

package software.amazon.awssdk.http.apache5.internal.utils;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Wrapper that attempts to cancel the delegate future if the thread is interrupted within get().
 */
@SdkInternalApi
public final class CancelOnInterruptWrapper<ResultT> implements Future<ResultT> {
    private final Future<? extends ResultT> f;

    public CancelOnInterruptWrapper(Future<? extends ResultT> f) {
        this.f = f;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return f.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return f.isCancelled();
    }

    @Override
    public boolean isDone() {
        return f.isDone();
    }

    @Override
    public ResultT get() throws InterruptedException, ExecutionException {
        return f.get();
    }

    // This method attempts to cancel the wrapped future if the thread is interrupted while blocked on get(). This is done by
    // attempting to cancel() the future when InterruptedException is thrown. If the the cancel() is unsuccessful (i.e.
    // the future is completed either successfully or exceptionally), then get the result if present and return it.
    @Override
    public ResultT get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return f.get(timeout, unit);
        } catch (InterruptedException ie) {
            if (!cancel(true)) {
                try {
                    // We couldn't cancel so the result will be available or it failed
                    ResultT entry = f.get();
                    Thread.currentThread().interrupt();
                    return entry;
                } catch (CancellationException | InterruptedException | ExecutionException e) {
                    // no-op, let it fall through to throwing the original interrupted exception
                }
            }
            throw ie;
        }
    }
}