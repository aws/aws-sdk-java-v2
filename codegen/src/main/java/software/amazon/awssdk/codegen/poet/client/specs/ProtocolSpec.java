/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet.client.specs;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;

public interface ProtocolSpec {

    FieldSpec protocolFactory(IntermediateModel model);

    MethodSpec initProtocolFactory(IntermediateModel model);

    CodeBlock responseHandler(IntermediateModel model, OperationModel opModel);

    CodeBlock errorResponseHandler(OperationModel opModel);

    CodeBlock executionHandler(OperationModel opModel);

    /**
     * Execution handler invocation only differs for protocols that support streaming outputs (REST-JSON, REST-XML).
     */
    default CodeBlock asyncExecutionHandler(OperationModel opModel) {
        return executionHandler(opModel);
    }

    default Class<? extends SyncClientHandler> getClientHandlerClass() {
        return AwsSyncClientHandler.class;
    }

    Optional<MethodSpec> createErrorResponseHandler();

    List<CodeBlock> errorUnmarshallers(IntermediateModel model);

    default List<MethodSpec> additionalMethods() {
        return new ArrayList<>();
    }
}
