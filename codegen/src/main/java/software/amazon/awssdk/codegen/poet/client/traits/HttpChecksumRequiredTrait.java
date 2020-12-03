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
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksumRequired;

/**
 * The logic for handling the "httpChecksumRequired" trait within the code generator.
 */
public class HttpChecksumRequiredTrait {
    private HttpChecksumRequiredTrait() {
    }

    /**
     * Generate a ".putExecutionAttribute(...)" code-block for the provided operation model. This should be used within the
     * context of initializing {@link ClientExecutionParams}. If HTTP checksums are not required by the operation, this will
     * return an empty code-block.
     */
    public static CodeBlock putHttpChecksumAttribute(OperationModel operationModel) {
        if (operationModel.isHttpChecksumRequired()) {
            return CodeBlock.of(".putExecutionAttribute($T.HTTP_CHECKSUM_REQUIRED, $T.create())\n",
                                SdkInternalExecutionAttribute.class, HttpChecksumRequired.class);
        }

        return CodeBlock.of("");
    }
}
