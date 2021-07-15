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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Outer map maps destination to nested map. Inner map maps batch id to future response.
 * @param <T> the type of an outgoing response
 */
public class ResponseMap<T> {

    private final Map<String, Map<String, CompletableFuture<T>>> batchGroupIdToIdToResponse;

    public ResponseMap() {
        this.batchGroupIdToIdToResponse = new ConcurrentHashMap<>();
    }

    public Map<String, CompletableFuture<T>> getResponseMap(String destination) {
        return batchGroupIdToIdToResponse.computeIfAbsent(destination, k -> new HashMap<>());
    }
}
