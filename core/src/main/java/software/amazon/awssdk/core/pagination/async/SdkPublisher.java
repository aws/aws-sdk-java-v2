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

package software.amazon.awssdk.core.pagination.async;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;

/**
 * Interface that is implemented by the Async auto-paginated responses.
 */
public interface SdkPublisher<T> extends Publisher<T> {

    // TODO Should we return the last response instead of Void?
    default CompletableFuture<Void> forEach(Consumer<T> consumer) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        subscribe(new SequentialSubscriber<>(consumer, future));
        return future;
    }
}
