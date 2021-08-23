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
}
