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
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;

/**
 * The logic for handling the Flexible "httpChecksum" trait within the code generator.
 */
public class HttpChecksumTrait {

    private HttpChecksumTrait() {
    }


    /**
     * Generate a ".putExecutionAttribute(...)" code-block for the provided operation model. This should be used within the
     * context of initializing {@link ClientExecutionParams}.
     * If HTTP checksums are not required by the operation, this will return an empty code-block.
     */
    public static CodeBlock create(OperationModel operationModel) {

        if (operationModel.getHttpChecksum() != null) {

            CodeBlock.Builder codeBuilder = CodeBlock.builder();
            codeBuilder.add(CodeBlock.of(".putExecutionAttribute($T.HTTP_CHECKSUM, $T.builder().requestChecksumRequired($L)",
                                         SdkInternalExecutionAttribute.class, HttpChecksum.class,
                                         operationModel.getHttpChecksum().isRequestChecksumRequired()));

            addFluentGetterToBuilder(operationModel,
                                     codeBuilder,
                                     operationModel.getHttpChecksum().getRequestAlgorithmMember(),
                                     "requestAlgorithm");

            addFluentGetterToBuilder(operationModel,
                                     codeBuilder,
                                     operationModel.getHttpChecksum().getRequestValidationModeMember(),
                                     "requestValidationMode");

            // loop to get the comma separated strings \"literals\"
            List<String> responseAlgorithms = operationModel.getHttpChecksum().getResponseAlgorithms();
            if (responseAlgorithms != null && !responseAlgorithms.isEmpty()) {

                codeBuilder.add(CodeBlock.of(".responseAlgorithms("))
                           .add(
                               CodeBlock.of("$L",
                                            responseAlgorithms.stream().collect(
                                                Collectors.joining("\", \"", "\"", "\""))))
                           .add(CodeBlock.of(")"));
            }
            codeBuilder.add(CodeBlock.of(".isRequestStreaming($L)", operationModel.getInputShape().isHasStreamingMember()));
            return codeBuilder.add(CodeBlock.of(".build())")).build();
        } else {
            return CodeBlock.of("");
        }
    }

    private static void addFluentGetterToBuilder(OperationModel operationModel, CodeBlock.Builder codeBuilder,
                                  String requestValidationModeMemberInModel, String memberBuilderName) {

        if (requestValidationModeMemberInModel != null) {
            MemberModel requestValidationModeMember =
                operationModel.getInputShape().tryFindMemberModelByC2jName(
                    requestValidationModeMemberInModel, true);

            if (requestValidationModeMember == null) {
                throw new IllegalStateException(requestValidationModeMemberInModel
                                                + " is not a member in "
                                                + operationModel.getInputShape().getShapeName());
            }
            codeBuilder.add(".$L($N.$N())",
                            memberBuilderName,
                            operationModel.getInput().getVariableName(),
                            requestValidationModeMember.getFluentGetterMethodName());
        }
    }
}
