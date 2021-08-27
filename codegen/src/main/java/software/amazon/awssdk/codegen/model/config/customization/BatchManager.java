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

package software.amazon.awssdk.codegen.model.config.customization;

/**
 * Config to define a batchable method. The key is a method that has a batch counterpart
 *
 * ex. For SQS, we can use a key of: sendMessage, meanwhile the batchFunctionsTypes will store the types
 * SendMessageRequest, SendMessageResponse, and SendMessageBatchResponse.
 */
public class BatchManager {

    /**
     * The batch equivalent of the request method. This is required.
     *
     * Ex. if the request method is sendMessage, batchMethod is sendMessageBatch
     */
    private String batchMethod;

    /** Type of a single entry that is contained within a batch request. This is required. */
    private String batchRequestEntry;

    /**
     * Type of a successful batch response entry. If a successful and failed batch response entry are the same,
     * successfulBatchEntry indicates the type of that entry. This is required.
     *
     * Ex. SendMessageBatchResultEntry for SQS, PutRecordsResultEntry for kinesis
     */
    private String successBatchEntry;

    /** Type of a failed batch response entry. This is optional (depending on service). */
    private String errorBatchEntry;

    /**
     * Name of the method used to extract the successful responses from a batch response. If the method to extract successful
     * and failed entries are the same, successEntriesMethod indicates the name of that method. This is required.
     *
     * Ex. successful for SQS, records for kinesis, responses for dynamodb etc.
     */
    private String successEntriesMethod;

    /** Name of the method used to extract failed responses from a batch responses. This is optional (depending on service). */
    private String errorEntriesMethod;

    /**
     * Name of the method used to get/set the destination for a request. This is required.
     *
     * Ex. queueUrl for SQS, streamName for Kinesis
     */
    private String batchKey;

    /**
     * Name of the method used to extract the status code from a failed batch entry.
     *
     * Ex. code for SQS, errorCode for Kinesis.
     */
    private String errorCodeMethod;

    /**
     * Name of the method used to extract the status code from a failed batch entry.
     *
     * Ex. code for SQS, errorCode for Kinesis.
     */
    private String errorMessageMethod;

    /**
     * Name of the method used to set or extract the request identifier used to identify entries within a batch request.
     *
     * Ex. id for SQS
     */
    private String batchRequestIdentifier;

    public String getBatchMethod() {
        return batchMethod;
    }

    public void setBatchMethod(String batchMethod) {
        this.batchMethod = batchMethod;
    }

    public String getBatchRequestEntry() {
        return batchRequestEntry;
    }

    public void setBatchRequestEntry(String batchRequestEntry) {
        this.batchRequestEntry = batchRequestEntry;
    }

    public String getSuccessBatchEntry() {
        return successBatchEntry;
    }

    public void setSuccessBatchEntry(String successBatchEntry) {
        this.successBatchEntry = successBatchEntry;
    }

    public String getErrorBatchEntry() {
        return errorBatchEntry;
    }

    public void setErrorBatchEntry(String errorBatchEntry) {
        this.errorBatchEntry = errorBatchEntry;
    }

    public String getSuccessEntriesMethod() {
        return successEntriesMethod;
    }

    public void setSuccessEntriesMethod(String successEntriesMethod) {
        this.successEntriesMethod = successEntriesMethod;
    }

    public String getErrorEntriesMethod() {
        return errorEntriesMethod;
    }

    public void setErrorEntriesMethod(String errorEntriesMethod) {
        this.errorEntriesMethod = errorEntriesMethod;
    }

    public String getBatchKey() {
        return batchKey;
    }

    public void setBatchKey(String batchKey) {
        this.batchKey = batchKey;
    }

    public String getErrorCodeMethod() {
        return errorCodeMethod;
    }

    public void setErrorCodeMethod(String errorCodeMethod) {
        this.errorCodeMethod = errorCodeMethod;
    }

    public String getErrorMessageMethod() {
        return errorMessageMethod;
    }

    public void setErrorMessageMethod(String errorMessageMethod) {
        this.errorMessageMethod = errorMessageMethod;
    }

    public String getBatchRequestIdentifier() {
        return batchRequestIdentifier;
    }

    public void setBatchRequestIdentifier(String requestIdentifier) {
        this.batchRequestIdentifier = requestIdentifier;
    }
}
