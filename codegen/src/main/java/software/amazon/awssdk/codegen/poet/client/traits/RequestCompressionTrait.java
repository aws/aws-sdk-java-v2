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
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.interceptor.trait.RequestCompression;

/**
 * The logic for handling the "requestCompression" trait within the code generator.
 */
public class RequestCompressionTrait {

    private RequestCompressionTrait() {
    }

    /**
     * Generate a ".putExecutionAttribute(...)" code-block for the provided operation model. This should be used within the
     * context of initializing {@link ClientExecutionParams}. If request compression is not required by the operation, this will
     * return an empty code-block.
     */
    public static CodeBlock create(OperationModel operationModel, IntermediateModel model) {
        if (operationModel.getRequestCompression() == null) {
            return CodeBlock.of("");
        }

        // TODO : remove once:
        //  1) S3 checksum interceptors are moved to occur after CompressRequestStage
        //  2) Transfer-Encoding:chunked is supported in S3
        if (model.getMetadata().getServiceName().equals("S3")) {
            throw new IllegalStateException("Request compression for S3 is not yet supported in the AWS SDK for Java.");
        }

        List<String> encodings = operationModel.getRequestCompression().getEncodings();

        return CodeBlock.of(".putExecutionAttribute($T.REQUEST_COMPRESSION, "
                            + "$T.builder().encodings($L).isStreaming($L).build())",
                            SdkInternalExecutionAttribute.class, RequestCompression.class,
                            encodings.stream().collect(Collectors.joining("\", \"", "\"", "\"")),
                            operationModel.hasStreamingInput());
    }
}
