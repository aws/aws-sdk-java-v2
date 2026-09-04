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


package software.amazon.awssdk.mapper.dynamodb;

import software.amazon.awssdk.annotations.SdkPublicApi;
import java.util.Objects;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;


/**
 * Container object that has information about the batch load request made to DynamoDB.
 *
 * @author avinam
 */
@SdkPublicApi
public final class BatchLoadContext {
    /**
     * The BatchGetItemResponse returned by the DynamoDB client.
     */
    private BatchGetItemResponse batchGetItemResult;
    /**
     * The BatchGetItemRequest.
     */
    private BatchGetItemRequest batchGetItemRequest;
    /**
     * The number of times the request has been retried.
     */
    private int retriesAttempted;

    /**
     * Instantiates a new BatchLoadContext.
     *  @param batchGetItemRequest  see {@link BatchGetItemRequest}.
     * */
    public BatchLoadContext(BatchGetItemRequest batchGetItemRequest) {
        this.batchGetItemRequest = Objects.requireNonNull(batchGetItemRequest, "batchGetItemRequest");
        this.batchGetItemResult = null;
        this.retriesAttempted = 0;
    }

    /**
     * @return the BatchGetItemResponse
     */
    public BatchGetItemResponse getBatchGetItemResult() {
        return batchGetItemResult;
    }

    /**
     * @return the BatchGetItemResponse
     */
    public void setBatchGetItemResult(BatchGetItemResponse batchGetItemResult) {
        this.batchGetItemResult = batchGetItemResult;
    }


    /**
     * @return the BatchGetItemRequest.
     */
    public BatchGetItemRequest getBatchGetItemRequest() {
        return batchGetItemRequest;
    }

    /**
     * Updates the BatchGetItemRequest for the next retry attempt.
     */
    public void setBatchGetItemRequest(BatchGetItemRequest batchGetItemRequest) {
        this.batchGetItemRequest = batchGetItemRequest;
    }

    /**
     * Gets the retriesAttempted.
     *
     * @return the retriesAttempted
     */
    public int getRetriesAttempted() {
        return retriesAttempted;
    }

    /**
     * Sets retriesAttempted.
     *
     * @param retriesAttempted the number of retries attempted
     */
    public void setRetriesAttempted(int retriesAttempted) {
        this.retriesAttempted = retriesAttempted;
    }
}
