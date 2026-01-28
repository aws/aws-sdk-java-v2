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

package software.amazon.awssdk.services.sqs.batchmanager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.sqs.internal.batchmanager.IdentifiableMessage;

public class CustomClient {
    public CompletableFuture<BatchResponse> sendBatchAsync(List<IdentifiableMessage<String>> requests, String batchKey) {
        // Implement your asynchronous batch sending logic here
        // Return a CompletableFuture<BatchResponse>
        // For simplicity, return a completed future with a sample response
        List<BatchResponseEntry> entries = requests.stream()
                                                   .map(req -> new BatchResponseEntry(req.id(), req.message()))
                                                   .collect(Collectors.toList());
        return CompletableFuture.completedFuture(new BatchResponse(entries));
    }
}