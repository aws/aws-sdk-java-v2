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

package software.amazon.awssdk.services.sqs.BatchManager.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchAndSend;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchKeyMapper;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchResponseMapper;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.IdentifiableMessage;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Logger;

public class BatchManagerTestUtils {

    public static final int DEFAULT_MAX_BATCH_OPEN = 200;

    private static final Logger log = Logger.loggerFor(BatchManagerTestUtils.class);

    public static final BatchAndSend<String, BatchResponse> batchFunction =
        (identifiableRequests, destination) -> {
            BatchResponse entries = new BatchResponse();
            identifiableRequests.forEach(identifiableRequest -> {
                String id = identifiableRequest.id();
                String request = identifiableRequest.message();
                entries.add(new MessageWithId(id, request));
            });
            return CompletableFuture.supplyAsync(() -> {
                waitForTime(DEFAULT_MAX_BATCH_OPEN - 50);
                return entries;
            });
        };

    public static boolean waitForTime(int msToWait) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            return countDownLatch.await(msToWait, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.warn(() -> String.valueOf(e));
        }
        return false;
    }

    //Object to mimic a both a batch request and batch response entry
    public static class MessageWithId {

        private final String id;
        private final String message;

        public MessageWithId(String id, String message) {
            this.id = id;
            this.message = message;
        }

        public String getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class BatchResponse {
        private final List<MessageWithId> responses;

        public BatchResponse() {
            responses = new ArrayList<>();
        }

        public void add(MessageWithId response) {
            responses.add(response);
        }

        public List<MessageWithId> getResponses() {
            return responses;
        }
    }


    public static final BatchResponseMapper<BatchResponse, String> responseMapper =
        requestBatchResponse -> {
            List<Either<IdentifiableMessage<String>, IdentifiableMessage<Throwable>>> identifiableResponses = new ArrayList<>();
            for (MessageWithId requestWithId : requestBatchResponse.getResponses()) {
                IdentifiableMessage<String> response = new IdentifiableMessage<>(requestWithId.getId(),
                                                                                 requestWithId.getMessage());
                identifiableResponses.add(Either.left(response));
            }
            return identifiableResponses;
        };

    public static final BatchKeyMapper<String> batchKeyMapper = request -> request.substring(0, 5);


}
