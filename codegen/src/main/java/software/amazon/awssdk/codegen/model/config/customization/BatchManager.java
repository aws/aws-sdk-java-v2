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

import java.util.Map;

/**
 * Config required to generate a batchManager method that returns an instance of a BatchManager in addition to any required
 * executors or scheduledExecutors.
 */
public class BatchManager {

    public static final String METHOD_NAME = "batchManager";

    /** Name of the method used to extract failed responses from a batch responses. This is optional (depending on service). */
    String errorEntriesMethod;

    /**
     * Name of the method used to extract the status code from a failed batch entry.
     *
     * Ex. code for SQS, errorCode for Kinesis.
     */
    String errorCodeMethod;

    /** Type of a failed batch response entry. This is optional (depending on service). */
    String errorBatchEntry;

    /**
     * Name of the method used to extract the successful responses from a batch response. If the method to extract successful
     * and failed entries are the same, successEntriesMethod indicates the name of that method. This is required.
     *
     * Ex. successful for SQS, records for kinesis, responses for dynamodb etc.
     */
    String successEntriesMethod;

    /**
     * Stores a map of methods that have a batch counterpart, and maps them to the required types needed to configure the
     * BatchingFunctions file. The key used is the request type for a single request (not a batch request).
     *
     * ex. For SQS, we can use a key of: sendMessage, meanwhile the batchFunctionsTypes will store the types
     * SendMessageRequest, SendMessageResponse, and SendMessageBatchResponse.
     */
    private Map<String, BatchFunctionsTypes> batchableFunctions;

    public Map<String, BatchFunctionsTypes> getBatchableFunctions() {
        return batchableFunctions;
    }

    public void setBatchableFunctions(Map<String, BatchFunctionsTypes> batchableFunctions) {
        this.batchableFunctions = batchableFunctions;
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
}
