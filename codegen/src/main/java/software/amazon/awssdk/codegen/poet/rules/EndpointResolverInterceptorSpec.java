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

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.jr.stree.JrsBoolean;
import com.fasterxml.jackson.jr.stree.JrsString;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.service.ClientContextParam;
import software.amazon.awssdk.codegen.model.service.ContextParam;
import software.amazon.awssdk.codegen.model.service.EndpointTrait;
import software.amazon.awssdk.codegen.model.service.HostPrefixProcessor;
import software.amazon.awssdk.codegen.model.service.StaticContextParam;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.HostnameValidator;
import software.amazon.awssdk.utils.StringUtils;

public class EndpointResolverInterceptorSpec implements ClassSpec {
    private final IntermediateModel model;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;
    private final PoetExtension poetExtension;

    public EndpointResolverInterceptorSpec(IntermediateModel model) {
        this.model = model;
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
        this.poetExtension = new PoetExtension(model);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder b = PoetUtils.createClassBuilder(className())
                                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                      .addAnnotation(SdkInternalApi.class)
                                      .addSuperinterface(ExecutionInterceptor.class);

        b.addMethod(modifyRequestMethod());
        b.addMethod(ruleParams());

        b.addMethod(setContextParams());
        addContextParamMethods(b);

        b.addMethod(setStaticContextParamsMethod());
        addStaticContextParamMethods(b);

        if (hasClientContextParams()) {
            b.addMethod(setClientContextParamsMethod());
        }

        b.addMethod(hostPrefixMethod());

        return b.build();
    }

    @Override
    public ClassName className() {
        return endpointRulesSpecUtils.resolverInterceptorName();
    }

    private MethodSpec modifyRequestMethod() {

        MethodSpec.Builder b = MethodSpec.methodBuilder("modifyRequest")
                                         .addModifiers(Modifier.PUBLIC)
                                         .addAnnotation(Override.class)
                                         .returns(SdkRequest.class)
                                         .addParameter(Context.ModifyRequest.class, "context")
                                         .addParameter(ExecutionAttributes.class, "executionAttributes");

        String providerVar = "provider";

        // We skip resolution if the source of the endpoint is the endpoint discovery call
        b.beginControlFlow("if ($1T.endpointIsDiscovered(executionAttributes))",
                           endpointRulesSpecUtils.rulesRuntimeClassName("AwsEndpointProviderUtils"));
        b.addStatement("return context.request()");
        b.endControlFlow();

        b.addStatement("$1T $2N = ($1T) executionAttributes.getAttribute($3T.ENDPOINT_PROVIDER)",
                       endpointRulesSpecUtils.providerInterfaceName(), providerVar, SdkInternalExecutionAttribute.class);
        b.beginControlFlow("try");
        b.addStatement("$T result = $N.resolveEndpoint(ruleParams(context, executionAttributes)).join()", Endpoint.class,
                       providerVar);
        b.beginControlFlow("if (!$T.disableHostPrefixInjection(executionAttributes))",
                           endpointRulesSpecUtils.rulesRuntimeClassName("AwsEndpointProviderUtils"));
        b.addStatement("$T hostPrefix = hostPrefix(executionAttributes.getAttribute($T.OPERATION_NAME), context.request())",
                       ParameterizedTypeName.get(Optional.class, String.class), SdkExecutionAttribute.class);
        b.beginControlFlow("if (hostPrefix.isPresent())");
        b.addStatement("result = $T.addHostPrefix(result, hostPrefix.get())",
                       endpointRulesSpecUtils.rulesRuntimeClassName("AwsEndpointProviderUtils"));
        b.endControlFlow();
        b.endControlFlow();
        b.addStatement("executionAttributes.putAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT, result)");
        b.addStatement("return context.request()");
        b.endControlFlow();
        b.beginControlFlow("catch ($T e)", CompletionException.class);
        b.addStatement("$T cause = e.getCause()", Throwable.class);
        b.beginControlFlow("if (cause instanceof $T)", SdkClientException.class);
        b.addStatement("throw ($T) cause", SdkClientException.class);
        b.endControlFlow();
        b.beginControlFlow("else");
        b.addStatement("throw $T.create($S, cause)", SdkClientException.class, "Endpoint resolution failed");
        b.endControlFlow();
        b.endControlFlow();
        return b.build();
    }

    private MethodSpec ruleParams() {
        MethodSpec.Builder b = MethodSpec.methodBuilder("ruleParams")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .returns(endpointRulesSpecUtils.parametersClassName())
                                         .addParameter(Context.ModifyRequest.class, "context")
                                         .addParameter(ExecutionAttributes.class, "executionAttributes");

        b.addStatement("$T builder = $T.builder()", paramsBuilderClass(), endpointRulesSpecUtils.parametersClassName());

        Map<String, ParameterModel> parameters = model.getEndpointRuleSetModel().getParameters();

        parameters.forEach((n, m) -> {
            if (m.getBuiltInEnum() == null) {
                return;
            }

            String setterName = endpointRulesSpecUtils.paramMethodName(n);
            String builtInFn;
            switch (m.getBuiltInEnum()) {
                case AWS_REGION:
                    builtInFn = "regionBuiltIn";
                    break;
                case AWS_USE_DUAL_STACK:
                    builtInFn = "dualStackEnabledBuiltIn";
                    break;
                case AWS_USE_FIPS:
                    builtInFn = "fipsEnabledBuiltIn";
                    break;
                case SDK_ENDPOINT:
                    builtInFn = "endpointBuiltIn";
                    break;
                case AWS_S3_USE_GLOBAL_ENDPOINT:
                    builtInFn = "useGlobalEndpointBuiltIn";
                    break;
                // The S3 specific built-ins are set through the existing S3Configuration which is handled above
                case AWS_S3_ACCELERATE:
                case AWS_S3_DISABLE_MULTI_REGION_ACCESS_POINTS:
                case AWS_S3_FORCE_PATH_STYLE:
                case AWS_S3_USE_ARN_REGION:
                case AWS_S3_CONTROL_USE_ARN_REGION:
                    // end of S3 specific builtins
                case AWS_STS_USE_GLOBAL_ENDPOINT:
                    // V2 doesn't support this, only regional endpoints
                    return;
                default:
                    throw new RuntimeException("Don't know how to set built-in " + m.getBuiltInEnum());
            }

            b.addStatement("builder.$N($T.$N(executionAttributes))", setterName,
                           endpointRulesSpecUtils.rulesRuntimeClassName("AwsEndpointProviderUtils"), builtInFn);
        });

        if (hasClientContextParams()) {
            b.addStatement("setClientContextParams(builder, executionAttributes)");
        }
        b.addStatement("setContextParams(builder, executionAttributes.getAttribute($T.OPERATION_NAME), context.request())",
                       AwsExecutionAttribute.class);
        b.addStatement("setStaticContextParams(builder, executionAttributes.getAttribute($T.OPERATION_NAME))",
                       AwsExecutionAttribute.class);

        b.addStatement("return builder.build()");
        return b.build();
    }

    private ClassName paramsBuilderClass() {
        return endpointRulesSpecUtils.parametersClassName().nestedClass("Builder");
    }

    private MethodSpec addStaticContextParamsMethod(OperationModel opModel) {
        String methodName = staticContextParamsMethodName(opModel);

        MethodSpec.Builder b = MethodSpec.methodBuilder(methodName)
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .returns(void.class)
                                         .addParameter(paramsBuilderClass(), "params");

        opModel.getStaticContextParams().forEach((n, m) -> {
            String setterName = endpointRulesSpecUtils.paramMethodName(n);
            TreeNode value = m.getValue();
            switch (value.asToken()) {
                case VALUE_STRING:
                    b.addStatement("params.$N($S)", setterName, ((JrsString) value).getValue());
                    break;
                case VALUE_TRUE:
                case VALUE_FALSE:
                    b.addStatement("params.$N($L)", setterName, ((JrsBoolean) value).booleanValue());
                    break;
                default:
                    throw new RuntimeException("Don't know how to set parameter of type " + value.asToken());
            }
        });

        return b.build();
    }

    private String staticContextParamsMethodName(OperationModel opModel) {
        return opModel.getMethodName() + "StaticContextParams";
    }

    private boolean hasStaticContextParams(OperationModel opModel) {
        Map<String, StaticContextParam> staticContextParams = opModel.getStaticContextParams();
        return staticContextParams != null && !staticContextParams.isEmpty();
    }

    private void addStaticContextParamMethods(TypeSpec.Builder classBuilder) {
        Map<String, OperationModel> operations = model.getOperations();

        operations.forEach((n, m) -> {
            if (hasStaticContextParams(m)) {
                classBuilder.addMethod(addStaticContextParamsMethod(m));
            }
        });
    }

    private void addContextParamMethods(TypeSpec.Builder classBuilder) {
        Map<String, OperationModel> operations = model.getOperations();

        operations.forEach((n, m) -> {
            if (hasContextParams(m)) {
                classBuilder.addMethod(setContextParamsMethod(m));
            }
        });
    }

    private MethodSpec setStaticContextParamsMethod() {
        Map<String, OperationModel> operations = model.getOperations();

        MethodSpec.Builder b = MethodSpec.methodBuilder("setStaticContextParams")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .addParameter(paramsBuilderClass(), "params")
                                         .addParameter(String.class, "operationName")
                                         .returns(void.class);

        boolean generateSwitch = operations.values().stream().anyMatch(this::hasStaticContextParams);
        if (generateSwitch) {
            b.beginControlFlow("switch (operationName)");

            operations.forEach((n, m) -> {
                if (!hasStaticContextParams(m)) {
                    return;
                }

                b.addCode("case $S:", n);
                b.addStatement("$N(params)", staticContextParamsMethodName(m));
                b.addStatement("break");
            });
            b.addCode("default:");
            b.addStatement("break");
            b.endControlFlow();
        }

        return b.build();
    }

    private MethodSpec setContextParams() {
        Map<String, OperationModel> operations = model.getOperations();

        MethodSpec.Builder b = MethodSpec.methodBuilder("setContextParams")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .addParameter(paramsBuilderClass(), "params")
                                         .addParameter(String.class, "operationName")
                                         .addParameter(SdkRequest.class, "request")
                                         .returns(void.class);

        boolean generateSwitch = operations.values().stream().anyMatch(this::hasContextParams);
        if (generateSwitch) {
            b.beginControlFlow("switch (operationName)");

            operations.forEach((n, m) -> {
                if (!hasContextParams(m)) {
                    return;
                }

                String requestClassName = model.getNamingStrategy().getRequestClassName(m.getOperationName());
                ClassName requestClass = poetExtension.getModelClass(requestClassName);

                b.addCode("case $S:", n);
                b.addStatement("setContextParams(params, ($T) request)", requestClass);
                b.addStatement("break");
            });
            b.addCode("default:");
            b.addStatement("break");
            b.endControlFlow();
        }

        return b.build();
    }

    private MethodSpec setContextParamsMethod(OperationModel opModel) {
        String requestClassName = model.getNamingStrategy().getRequestClassName(opModel.getOperationName());
        ClassName requestClass = poetExtension.getModelClass(requestClassName);

        MethodSpec.Builder b = MethodSpec.methodBuilder("setContextParams")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .addParameter(paramsBuilderClass(), "params")
                                         .addParameter(requestClass, "request")
                                         .returns(void.class);

        opModel.getInputShape().getMembers().forEach(m -> {
            ContextParam param = m.getContextParam();
            if (param == null) {
                return;
            }

            String setterName = endpointRulesSpecUtils.paramMethodName(param.getName());

            b.addStatement("params.$N(request.$N())", setterName, m.getFluentGetterMethodName());
        });

        return b.build();
    }

    private boolean hasContextParams(OperationModel opModel) {
        return opModel.getInputShape().getMembers().stream()
                      .anyMatch(m -> m.getContextParam() != null);
    }

    private boolean hasClientContextParams() {
        Map<String, ClientContextParam> clientContextParams = model.getClientContextParams();
        return clientContextParams != null && !clientContextParams.isEmpty();
    }

    private MethodSpec setClientContextParamsMethod() {
        MethodSpec.Builder b = MethodSpec.methodBuilder("setClientContextParams")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .addParameter(paramsBuilderClass(), "params")
                                         .addParameter(ExecutionAttributes.class, "executionAttributes")
                                         .returns(void.class);

        b.addStatement("$T clientContextParams = executionAttributes.getAttribute($T.CLIENT_CONTEXT_PARAMS)",
                       AttributeMap.class, SdkInternalExecutionAttribute.class);

        ClassName paramsClass = endpointRulesSpecUtils.clientContextParamsName();
        Map<String, ClientContextParam> params = model.getClientContextParams();

        params.forEach((n, m) -> {
            String attrName = endpointRulesSpecUtils.clientContextParamName(n);
            b.addStatement("$T.ofNullable(clientContextParams.get($T.$N)).ifPresent(params::$N)", Optional.class, paramsClass,
                           attrName,
                           endpointRulesSpecUtils.paramMethodName(n));
        });

        return b.build();
    }


    private MethodSpec hostPrefixMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("hostPrefix")
                                               .returns(ParameterizedTypeName.get(Optional.class, String.class))
                                               .addParameter(String.class, "operationName")
                                               .addParameter(SdkRequest.class, "request")
                                               .addModifiers(Modifier.PRIVATE, Modifier.STATIC);

        boolean generateSwitch =
            model.getOperations().values().stream().anyMatch(opModel -> StringUtils.isNotBlank(getHostPrefix(opModel)));

        if (!generateSwitch) {
            builder.addStatement("return $T.empty()", Optional.class);
        } else {
            builder.beginControlFlow("switch (operationName)");

            model.getOperations().forEach((name, opModel) -> {
                String hostPrefix = getHostPrefix(opModel);
                if (StringUtils.isBlank(hostPrefix)) {
                    return;
                }

                builder.beginControlFlow("case $S:", name);
                HostPrefixProcessor processor = new HostPrefixProcessor(hostPrefix);

                if (processor.c2jNames().isEmpty()) {
                    builder.addStatement("return $T.of($S)", Optional.class, processor.hostWithStringSpecifier());
                } else {
                    String requestVar = opModel.getInput().getVariableName();
                    processor.c2jNames().forEach(c2jName -> {
                        builder.addStatement("$1T.validateHostnameCompliant(request.getValueForField($2S, $3T.class)"
                                             + ".orElse(null), $2S, $4S)",
                                             HostnameValidator.class,
                                             c2jName,
                                             String.class,
                                             requestVar);
                    });

                    builder.addCode("return $T.of($T.format($S, ", Optional.class, String.class,
                                    processor.hostWithStringSpecifier());
                    Iterator<String> c2jNamesIter = processor.c2jNames().listIterator();
                    while (c2jNamesIter.hasNext()) {
                        builder.addCode("request.getValueForField($S, $T.class).get()", c2jNamesIter.next(), String.class);
                        if (c2jNamesIter.hasNext()) {
                            builder.addCode(",");
                        }
                    }
                    builder.addStatement("))");
                }
                builder.endControlFlow();
            });

            builder.addCode("default:");
            builder.addStatement("return $T.empty()", Optional.class);
            builder.endControlFlow();
        }

        return builder.build();
    }

    private String getHostPrefix(OperationModel opModel) {
        EndpointTrait endpointTrait = opModel.getEndpointTrait();
        if (endpointTrait == null) {
            return null;
        }

        return endpointTrait.getHostPrefix();
    }
}
