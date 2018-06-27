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

package software.amazon.awssdk.core.util;

import java.util.concurrent.CompletableFuture;

/**
 * Utility class for working with {@link CompletableFuture}.
 */
public final class CompletableFutures {
    private CompletableFutures() {
    }

    /**
     * Convenience method for creating a future that is immediately completed
     * exceptionally with the given {@code Throwable}.
     * <p />
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
}
