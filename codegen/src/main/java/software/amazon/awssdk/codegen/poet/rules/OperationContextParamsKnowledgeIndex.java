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

package software.amazon.awssdk.codegen.poet.rules;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.jr.stree.JrsString;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.waiters.JmesPathExpressionConverter;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Knowledge index to get access to operation context parameters.
 */
public final class OperationContextParamsKnowledgeIndex {
    private static final String MAIN_METHOD_NAME = "setOperationContextParams";
    private final IntermediateModel intermediateModel;
    private final PoetExtension poetExtension;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;
    private final Boolean hasOperationContextParams;
    private final JmesPathExpressionConverter jmesPathGenerator;

    private OperationContextParamsKnowledgeIndex(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.poetExtension = new PoetExtension(intermediateModel);
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
        this.hasOperationContextParams = hasOperationContextParams();
        this.jmesPathGenerator = new JmesPathExpressionConverter(poetExtension.jmesPathRuntimeClass());
    }

    /**
     * Creates a new {@link OperationContextParamsKnowledgeIndex} using the given {@code intermediateModel}.
     */
    public static OperationContextParamsKnowledgeIndex of(IntermediateModel intermediateModel) {
        return new OperationContextParamsKnowledgeIndex(intermediateModel);
    }

    public static String mainMethodName() {
        return MAIN_METHOD_NAME;
    }

    boolean operationContextParamsExistForService() {
        return hasOperationContextParams;
    }

    List<MethodSpec> getOperationContextMethods() {
        List<MethodSpec> operationContextMethods = new ArrayList<>();
        operationContextMethods.add(mainMethod());

        List<MethodSpec> operationMethods = intermediateModel.getOperations().values().stream()
                                                             .filter(this::operationHasContextParams)
                                                             .map(this::setOperationContextParamMethod)
                                                             .collect(Collectors.toList());

        operationContextMethods.addAll(operationMethods);
        return operationContextMethods;
    }

    private MethodSpec mainMethod() {
        Map<String, OperationModel> operations = intermediateModel.getOperations();

        MethodSpec.Builder b = MethodSpec.methodBuilder(MAIN_METHOD_NAME)
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .addParameter(endpointRulesSpecUtils.paramsBuilderClass(), "params")
                                         .addParameter(String.class, "operationName")
                                         .addParameter(SdkRequest.class, "request")
                                         .returns(void.class);

        boolean generateSwitch = operations.values().stream().anyMatch(this::operationHasContextParams);
        if (generateSwitch) {
            b.beginControlFlow("switch (operationName)");

            operations.forEach((n, m) -> {
                if (!operationHasContextParams(m)) {
                    return;
                }

                String requestClassName = intermediateModel.getNamingStrategy().getRequestClassName(m.getOperationName());
                ClassName requestClass = poetExtension.getModelClass(requestClassName);

                b.addCode("case $S:", n);
                b.addStatement("setOperationContextParams(params, ($T) request)", requestClass);
                b.addStatement("break");
            });
            b.addCode("default:");
            b.addStatement("break");
            b.endControlFlow();
        }

        return b.build();
    }

    private boolean operationHasContextParams(OperationModel opModel) {
        return CollectionUtils.isNotEmpty(opModel.getOperationContextParams());
    }

    private MethodSpec setOperationContextParamMethod(OperationModel opModel) {
        String requestClassName = intermediateModel.getNamingStrategy().getRequestClassName(opModel.getOperationName());
        ClassName requestClass = poetExtension.getModelClass(requestClassName);

        MethodSpec.Builder b = MethodSpec.methodBuilder("setOperationContextParams")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .addParameter(endpointRulesSpecUtils.paramsBuilderClass(), "params")
                                         .addParameter(requestClass, "request")
                                         .returns(void.class);

        b.addStatement("$1T input = new $1T(request)", poetExtension.jmesPathRuntimeClass().nestedClass("Value"));

        opModel.getOperationContextParams().forEach((key, value) -> {
            if (Objects.requireNonNull(value.getValue().asToken()) == JsonToken.VALUE_STRING) {
                String setterName = endpointRulesSpecUtils.paramMethodName(key);

                String jmesPathString = ((JrsString) value.getValue()).getValue();
                CodeBlock addParam = CodeBlock.builder()
                                              .add("params.$N(", setterName)
                                              .add(jmesPathGenerator.interpret(jmesPathString, "input"))
                                              .add(matchToParameterType(key))
                                              .add(")")
                                              .build();

                b.addStatement(addParam);
            } else {
                throw new RuntimeException("Invalid operation context parameter path for " + opModel.getOperationName() +
                                           ". Expected VALUE_STRING, but got " + value.getValue().asToken());
            }

        });

        return b.build();
    }

    private CodeBlock matchToParameterType(String paramName) {
        Map<String, ParameterModel> parameters = intermediateModel.getEndpointRuleSetModel().getParameters();
        Optional<ParameterModel> endpointParameter = parameters.entrySet().stream()
                                                               .filter(e -> e.getKey().toLowerCase(Locale.US)
                                                                             .equals(paramName.toLowerCase(Locale.US)))
                                                               .map(Map.Entry::getValue)
                                                               .findFirst();
        return endpointParameter.map(this::convertValueToParameterType).orElseGet(() -> CodeBlock.of(""));
    }

    private CodeBlock convertValueToParameterType(ParameterModel parameterModel) {
        switch (parameterModel.getType().toLowerCase(Locale.US)) {
            case "boolean":
                return CodeBlock.of(".booleanValue()");
            case "string":
                return CodeBlock.of(".stringValue()");
            case "stringarray":
                return CodeBlock.of(".stringValues()");
            default:
                throw new UnsupportedOperationException(
                    "Supported types are boolean, string and stringarray. Given type was " + parameterModel.getType());
        }
    }

    private boolean hasOperationContextParams() {
        return intermediateModel.getOperations().values().stream()
                                .anyMatch(op -> !CollectionUtils.isNullOrEmpty(op.getOperationContextParams()));
    }
}
