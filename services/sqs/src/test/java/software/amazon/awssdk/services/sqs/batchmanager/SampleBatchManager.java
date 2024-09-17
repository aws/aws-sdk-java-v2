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

import static software.amazon.awssdk.services.sqs.internal.batchmanager.ResponseBatchConfiguration.MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.services.sqs.internal.batchmanager.IdentifiableMessage;
import software.amazon.awssdk.services.sqs.internal.batchmanager.RequestBatchConfiguration;
import software.amazon.awssdk.services.sqs.internal.batchmanager.RequestBatchManager;
import software.amazon.awssdk.utils.Either;

public class SampleBatchManager extends RequestBatchManager<String, String, BatchResponse> {
    private final CustomClient client;


    protected SampleBatchManager(BatchOverrideConfiguration batchOverrideConfiguration,
                                 ScheduledExecutorService executorService,
                                 CustomClient client) {
        super(RequestBatchConfiguration.builder(batchOverrideConfiguration).build(), executorService);
        this.client = client;
    }

    @Override
    protected CompletableFuture<BatchResponse> batchAndSend(List<IdentifiableMessage<String>> identifiedRequests, String batchKey) {
        return client.sendBatchAsync(identifiedRequests, batchKey);
    }

    @Override
    protected String getBatchKey(String request) {
        return request.substring(0, request.indexOf(':'));
    }

    @Override
    protected List<Either<IdentifiableMessage<String>, IdentifiableMessage<Throwable>>> mapBatchResponse(BatchResponse batchResponse) {
        List<Either<IdentifiableMessage<String>, IdentifiableMessage<Throwable>>> mappedResponses = new ArrayList<>();
        batchResponse.getResponses().forEach(batchResponseEntry -> {
            IdentifiableMessage<String> response = new IdentifiableMessage<>(batchResponseEntry.getId(), batchResponseEntry.getMessage());
            mappedResponses.add(Either.left(response));
        });
        return mappedResponses;    }


}