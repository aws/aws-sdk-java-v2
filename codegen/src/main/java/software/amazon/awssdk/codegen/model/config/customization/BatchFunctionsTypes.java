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

public class BatchFunctionsTypes {

    /**
     * The batch equivalent of the request method. This is required.
     *
     * Ex. if the request method is sendMessage, batchMethod is sendMessageBatch
     */
    String batchMethod;

    /** Type of a single entry that is contained within a batch request. This is required. */
    String batchRequestEntry;

    /**
     * Type of a successful batch response entry. If a successful and failed batch response entry are the same,
     * successfulBatchEntry indicates the type of that entry. This is required.
     *
     * Ex. SendMessageBatchResultEntry for SQS, PutRecordsResultEntry for kinesis
     */
    String successBatchEntry;

    /** Type of a failed batch response entry. This is optional (depending on service). */
    String errorBatchEntry;

    /**
     * Name of the method used to get/set the destination for a request. This is required.
     *
     * Ex. queueUrl for SQS, streamName for Kinesis
     */
    String destinationMethod;

    /**
     * Name of the method used to extract the successful responses from a batch response. If the method to extract successful
     * and failed entries are the same, successEntriesMethod indicates the name of that method. This is required.
     *
     * Ex. successful for SQS, records for kinesis, responses for dynamodb etc.
     */
    String successEntriesMethod;

    /** Name of the method used to extract failed responses from a batch responses. This is optional (depending on service). */
    String errorEntriesMethod;

    /**
     * Name of the method used to extract the status code from a failed batch entry.
     *
     * Ex. code for SQS, errorCode for Kinesis.
     */
    String errorCodeMethod;

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

    public String getDestinationMethod() {
        return destinationMethod;
    }

    public void setDestinationMethod(String destinationMethod) {
        this.destinationMethod = destinationMethod;
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

    public String getErrorCodeMethod() {
        return errorCodeMethod;
    }

    public void setErrorCodeMethod(String errorCodeMethod) {
        this.errorCodeMethod = errorCodeMethod;
    }
}
