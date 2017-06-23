/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.protocol.asserts.unmarshalling;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

/**
 * Unmarshalling assertions require some context about the service and operation being exercised.
 */
public class UnmarshallingTestContext {

    private IntermediateModel model;
    private String operationName;
    private String streamedResonse;

    public UnmarshallingTestContext withModel(IntermediateModel model) {
        this.model = model;
        return this;
    }

    public IntermediateModel getModel() {
        return model;
    }

    public UnmarshallingTestContext withOperationName(String operationName) {
        this.operationName = operationName;
        return this;
    }

    public String getOperationName() {
        return operationName;
    }

    /**
     * Streamed response will only be present for operations that have a streaming member in the output. We
     * capture the actual contents if via a custom {@link software.amazon.awssdk.sync.StreamingResponseHandler}.
     */
    public UnmarshallingTestContext withStreamedResponse(String streamedResonse) {
        this.streamedResonse = streamedResonse;
        return this;
    }

    public String getStreamedResponse() {
        return streamedResonse;
    }

}
