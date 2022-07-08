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

package software.amazon.awssdk.codegen.poet.client.traits;

import com.squareup.javapoet.CodeBlock;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;

/**
 * Trait which defines if a given request needs to be authenticated.
 * A request is not authenticated only if it has "auththpe" trait  explicitly marked as "none"
 */
@SdkInternalApi
public class NoneAuthTypeRequestTrait {

    private NoneAuthTypeRequestTrait() {
    }

    /**
     * Generate a ".putExecutionAttribute(...)" code-block for the provided operation model. This should be used within the
     * context of initializing {@link ClientExecutionParams}. If and only if "authType" trait is explicitly set as "none" the set
     * the execution attribute as false.
     */
    public static CodeBlock create(OperationModel operationModel) {

        if (operationModel.getAuthType() == AuthType.NONE) {
            CodeBlock.Builder codeBuilder = CodeBlock.builder();
            codeBuilder.add(CodeBlock.of(".putExecutionAttribute($T.IS_NONE_AUTH_TYPE_REQUEST, $L)",
                                         SdkInternalExecutionAttribute.class, operationModel.getAuthType() != AuthType.NONE));
            return codeBuilder.build();
        } else {
            return CodeBlock.of("");
        }
    }
}