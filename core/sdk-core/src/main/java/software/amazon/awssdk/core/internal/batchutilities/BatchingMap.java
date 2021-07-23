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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Outer map maps a batch group ID (ex. queueUrl, overrideConfig etc.) to a nested map.
 * Inner map maps batch id to a request/CompletableFuture response.
 * @param <RequestT> the type of an outgoing response
 */
@SdkInternalApi
public class BatchingMap<RequestT, ResponseT> {

    private final Map<String, BatchingGroupMap<RequestT, ResponseT>> batchContextMap;

    public BatchingMap() {
        this.batchContextMap = new ConcurrentHashMap<>();
    }

    public BatchingGroupMap<RequestT, ResponseT> getNestedMap(String destination) {
        return batchContextMap.computeIfAbsent(destination, k -> new BatchingGroupMap<>());
    }

    public boolean containsKey(String key) {
        return batchContextMap.containsKey(key);
    }

    public BatchingGroupMap<RequestT, ResponseT> get(String key) {
        return batchContextMap.get(key);
    }

    public BatchingGroupMap<RequestT, ResponseT> remove(String key) {
        return batchContextMap.remove(key);
    }

    public Collection<BatchingGroupMap<RequestT, ResponseT>> values() {
        return batchContextMap.values();
    }

    public void forEach(BiConsumer<String, BatchingGroupMap<RequestT, ResponseT>> action) {
        batchContextMap.forEach(action);
    }
}
