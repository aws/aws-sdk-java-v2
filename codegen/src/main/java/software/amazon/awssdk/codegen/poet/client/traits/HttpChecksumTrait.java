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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.ImmutableMap;

/**
 * The logic for handling the Flexible "httpChecksum" trait within the code generator.
 */
public class HttpChecksumTrait {

    private static final Map<String, Integer> CHECKSUM_ALGORITHM_PRIORITY =
        ImmutableMap.<String, Integer>builder()
                    .put(DefaultChecksumAlgorithm.CRC32C.algorithmId(), 1)
                    .put(DefaultChecksumAlgorithm.CRC32.algorithmId(), 2)
                    .put(DefaultChecksumAlgorithm.CRC64NVME.algorithmId(), 3)
                    .put(DefaultChecksumAlgorithm.SHA1.algorithmId(), 4)
                    .put(DefaultChecksumAlgorithm.SHA256.algorithmId(), 5)
                    .build();

    private HttpChecksumTrait() {
    }

    /**
     * Generate a ".putExecutionAttribute(...)" code-block for the provided operation model. This should be used within the
     * context of initializing {@link ClientExecutionParams}.
     * If HTTP checksums are not required by the operation, this will return an empty code-block.
     */
    public static CodeBlock create(OperationModel operationModel) {

        software.amazon.awssdk.codegen.checksum.HttpChecksum httpChecksum = operationModel.getHttpChecksum();

        if (httpChecksum == null) {
            return CodeBlock.of("");
        }

        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        codeBuilder.add(CodeBlock.of(".putExecutionAttribute($T.HTTP_CHECKSUM, $T.builder()"
                                     + ".requestChecksumRequired($L).isRequestStreaming($L)",
                                     SdkInternalExecutionAttribute.class, HttpChecksum.class,
                                     httpChecksum.isRequestChecksumRequired(),
                                     operationModel.getInputShape().isHasStreamingMember()));


        String requestAlgorithmMember = httpChecksum.getRequestAlgorithmMember();
        if (requestAlgorithmMember != null) {
            configureRequestChecksumCalculation(operationModel, requestAlgorithmMember, codeBuilder);
        }

        String responseValidationModeMember = httpChecksum.getRequestValidationModeMember();
        if (responseValidationModeMember != null) {
            configureResponseChecksumValidation(operationModel, responseValidationModeMember, codeBuilder, httpChecksum);
        }

        return codeBuilder.add(CodeBlock.of(".build())")).build();
    }

    private static void configureResponseChecksumValidation(OperationModel operationModel,
                                                            String responseValidationModeMember,
                                                            CodeBlock.Builder codeBuilder,
                                                            software.amazon.awssdk.codegen.checksum.HttpChecksum httpChecksum) {
        MemberModel checksumMember =
            operationModel.getInputShape().tryFindMemberModelByC2jName(
                responseValidationModeMember, true);

        if (checksumMember == null) {
            throw new IllegalStateException(responseValidationModeMember
                                            + " is not a member in "
                                            + operationModel.getInputShape().getShapeName());
        }
        codeBuilder.add(".requestValidationMode($N.$N())",
                        operationModel.getInput().getVariableName(),
                        checksumMember.getFluentGetterMethodName());

        List<String> responseAlgorithms = httpChecksum.getResponseAlgorithms();
        if (!CollectionUtils.isNullOrEmpty(responseAlgorithms)) {
            addResponseAlgorithmsCodeBlock(responseAlgorithms, codeBuilder);
        }
    }

    private static void configureRequestChecksumCalculation(OperationModel operationModel,
                                                            String requestAlgorithmMember,
                                                            CodeBlock.Builder codeBuilder) {
        MemberModel requestAlgorithm =
            operationModel.getInputShape().tryFindMemberModelByC2jName(
                requestAlgorithmMember, true);

        if (requestAlgorithm == null) {
            throw new IllegalStateException(requestAlgorithmMember
                                            + " is not a member in "
                                            + operationModel.getInputShape().getShapeName());
        }
        codeBuilder.add(".requestAlgorithm($N.$N())",
                        operationModel.getInput().getVariableName(),
                        requestAlgorithm.getFluentGetterMethodName());

        if (requestAlgorithm.getHttp().getMarshallLocation() != MarshallLocation.HEADER) {
            throw new IllegalStateException("Unsupported request algorithm location for " + requestAlgorithm);
        }

        String headerName = requestAlgorithm.getHttp().getMarshallLocationName();

        if (headerName == null) {
            throw new IllegalStateException("Request algorithm header name is null for " + requestAlgorithm);
        }

        codeBuilder.add(".requestAlgorithmHeader($S)", headerName);
    }

    /**
     * Sort the responseAlgorithms provided by the service by the time it takes to calculate a checksum for a given algorithm
     * with the fastest-to-calculate algorithms first.
     */
    private static void addResponseAlgorithmsCodeBlock(List<String> responseAlgorithms, CodeBlock.Builder codeBuilder) {
        responseAlgorithms.sort(Comparator.comparingInt(o -> CHECKSUM_ALGORITHM_PRIORITY.getOrDefault(
            o.toUpperCase(Locale.US), Integer.MAX_VALUE)));

        codeBuilder.add(CodeBlock.of(".responseAlgorithmsV2("));
        List<CodeBlock> responseAlgorithmsCodeBlocks = responseAlgorithmsCodeBlocks(responseAlgorithms);
        for (int i = 0; i < responseAlgorithmsCodeBlocks.size(); i++) {
            CodeBlock code = responseAlgorithmsCodeBlocks.get(i);
            codeBuilder.add(code);
            if (i != responseAlgorithmsCodeBlocks.size() - 1) {
                codeBuilder.add(",");
            }
        }
        codeBuilder.add(CodeBlock.of(")"));
    }

    private static List<CodeBlock> responseAlgorithmsCodeBlocks(List<String> responseAlgorithms) {
        List<CodeBlock> list = new ArrayList<>();
        for (String algo : responseAlgorithms) {
            String algorithmName = algo.toUpperCase(Locale.US);
            if (!CHECKSUM_ALGORITHM_PRIORITY.containsKey(algorithmName)) {
                throw new UnsupportedOperationException("Unsupported algorithm: " + algorithmName);
            }
            CodeBlock codeBlock = CodeBlock.of("$T.$L", DefaultChecksumAlgorithm.class,
                                               algorithmName);
            list.add(codeBlock);
        }
        return list;
    }

    public static boolean hasRequestAlgorithmMember(IntermediateModel model) {
        Predicate<software.amazon.awssdk.codegen.checksum.HttpChecksum> requestCalculation =
            httpChecksum -> httpChecksum.getRequestAlgorithmMember() != null || httpChecksum.isRequestChecksumRequired();
        return model.getOperations().values().stream()
                    .filter(operationModel -> operationModel.getHttpChecksum() != null)
                    .anyMatch(opModel -> requestCalculation.test(opModel.getHttpChecksum()));
    }

    public static boolean hasResponseAlgorithms(IntermediateModel model) {
        return model.getOperations().values().stream()
                    .anyMatch(opModel -> opModel.getHttpChecksum() != null
                                         && opModel.getHttpChecksum().getResponseAlgorithms() != null);
    }
}
