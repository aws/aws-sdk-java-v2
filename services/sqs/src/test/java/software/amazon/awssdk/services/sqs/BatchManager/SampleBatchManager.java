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

package software.amazon.awssdk.services.sqs.BatchManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.sqs.internal.batchmanager.BatchAndSend;
import software.amazon.awssdk.services.sqs.internal.batchmanager.BatchKeyMapper;
import software.amazon.awssdk.services.sqs.internal.batchmanager.BatchResponseMapper;
import software.amazon.awssdk.services.sqs.internal.batchmanager.IdentifiableMessage;
import software.amazon.awssdk.services.sqs.internal.batchmanager.RequestBatchManager;
import software.amazon.awssdk.utils.Either;

public class SampleBatchManager extends RequestBatchManager<String, String, BatchResponse> {
    protected SampleBatchManager(DefaultBuilder builder) {
        super(builder
                  .batchFunction(SampleBatchManager.sampleBatchAsyncFunction(builder.client))
                  .responseMapper(SampleBatchManager.sampleResponseMapper())
                  .batchKeyMapper(SampleBatchManager.sampleBatchKeyMapper()));
    }

    public static DefaultBuilder builder() {
        return new DefaultBuilder();
    }

    public static class DefaultBuilder extends RequestBatchManager.DefaultBuilder<String, String, BatchResponse, DefaultBuilder> {
        private CustomClient client;

        public DefaultBuilder client(CustomClient client) {
            this.client = client;
            return this;
        }

        @Override
        public SampleBatchManager build() {
            return new SampleBatchManager(this);
        }
    }

    public static BatchAndSend<String, BatchResponse> sampleBatchAsyncFunction(CustomClient client) {
        return (requests, batchKey) -> {
            // Implement your batch sending logic here using the client
            // For example, send the batch request and return a CompletableFuture<BatchResponse>
            return client.sendBatchAsync(requests, batchKey);
        };
    }

    public static BatchResponseMapper<BatchResponse, String> sampleResponseMapper() {
        return batchResponse -> {
            List<Either<IdentifiableMessage<String>, IdentifiableMessage<Throwable>>> mappedResponses = new ArrayList<>();
            batchResponse.getResponses().forEach(batchResponseEntry -> {
                IdentifiableMessage<String> response = new IdentifiableMessage<>(batchResponseEntry.getId(), batchResponseEntry.getMessage());
                mappedResponses.add(Either.left(response));
            });
            return mappedResponses;
        };
    }


    public static BatchKeyMapper<String> sampleBatchKeyMapper() {
        return request -> {
            return request.substring(0, request.indexOf(":"));
        };
    }
}