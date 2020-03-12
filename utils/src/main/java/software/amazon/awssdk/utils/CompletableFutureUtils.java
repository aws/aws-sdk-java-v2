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

package software.amazon.awssdk.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Utility class for working with {@link CompletableFuture}.
 */
@SdkProtectedApi
public final class CompletableFutureUtils {
    private CompletableFutureUtils() {
    }

    /**
     * Convenience method for creating a future that is immediately completed
     * exceptionally with the given {@code Throwable}.
     * <p>
     * Similar to {@code CompletableFuture#failedFuture} which was added in
     * Java 9.
     *
     * @param t The failure.
     * @param <U> The type of the element.
     * @return The failed future.
     */
    public static <U> CompletableFuture<U> failedFuture(Throwable t) {
        CompletableFuture<U> cf = new CompletableFuture<>();
        cf.completeExceptionally(t);
        return cf;
    }

    /**
     * Wraps the given error in a {@link CompletionException} if necessary.
     * Useful if an exception needs to be rethrown from within {@link
     * CompletableFuture#handle(java.util.function.BiFunction)} or similar
     * methods.
     *
     * @param t The error.
     * @return The error as a CompletionException.
     */
    public static CompletionException errorAsCompletionException(Throwable t) {
        if (t instanceof CompletionException) {
            return (CompletionException) t;
        }
        return new CompletionException(t);
    }

    /**
     * Forward the {@code Throwable} from {@code src} to {@code dst}.

     * @param src The source of the {@code Throwable}.
     * @param dst The destination where the {@code Throwable} will be forwarded to.
     *
     * @return {@code src}.
     */
    public static <T> CompletableFuture<T> forwardExceptionTo(CompletableFuture<T> src, CompletableFuture<?> dst) {
        src.whenComplete((r, e) -> {
            if (e != null) {
                dst.completeExceptionally(e);
            }
        });
        return src;
    }
}
