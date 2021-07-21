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
 * @param <T> the type of an outgoing response
 */
@SdkInternalApi
public class BatchingMap<T> {

    private final Map<String, Map<String, T>> batchGroupIdToIdToMessage;

    public BatchingMap() {
        this.batchGroupIdToIdToMessage = new ConcurrentHashMap<>();
    }

    public Map<String, T> getNestedMap(String destination) {
        return batchGroupIdToIdToMessage.computeIfAbsent(destination, k -> new ConcurrentHashMap<>());
    }

    public int size() {
        return batchGroupIdToIdToMessage.size();
    }

    /**
     * Only empty if every batchGroupId has an empty map since it is possible for a batchGroupId to exist but point to an empty
     * map.
     */
    public boolean isEmpty() {
        for (Map<String, T> idToMessage : batchGroupIdToIdToMessage.values()) {
            if (!idToMessage.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean containsKey(String key) {
        return batchGroupIdToIdToMessage.containsKey(key);
    }

    public Map<String, T> get(String key) {
        return batchGroupIdToIdToMessage.get(key);
    }

    public Map<String, T> put(String key, Map<String, T> value) {
        return batchGroupIdToIdToMessage.put(key, value);
    }

    public Map<String, T> remove(String key) {
        return batchGroupIdToIdToMessage.remove(key);
    }

    public Collection<Map<String, T>> values() {
        return batchGroupIdToIdToMessage.values();
    }

    public void clear() {
        for (Map<String, T> idToMessage : batchGroupIdToIdToMessage.values()) {
            idToMessage.clear();
        }
        batchGroupIdToIdToMessage.clear();
    }

    public void forEach(BiConsumer<String, Map<String, T>> action) {
        batchGroupIdToIdToMessage.forEach(action);
    }
}
