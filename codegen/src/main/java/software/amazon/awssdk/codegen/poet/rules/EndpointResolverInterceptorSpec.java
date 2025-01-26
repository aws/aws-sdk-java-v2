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
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.jr.stree.JrsBoolean;
import com.fasterxml.jackson.jr.stree.JrsString;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.auth.signer.SignerLoader;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.awscore.util.SignerOverrideUtils;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.EndpointAuthSchemeConfig;
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
import software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeSpecUtils;
import software.amazon.awssdk.codegen.poet.auth.scheme.ModelAuthSchemeClassesKnowledgeIndex;
import software.amazon.awssdk.codegen.poet.waiters.JmesPathAcceptorGenerator;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.HostnameValidator;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

public class EndpointResolverInterceptorSpec implements ClassSpec {

    private final IntermediateModel model;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;
    private final EndpointParamsKnowledgeIndex endpointParamsKnowledgeIndex;
    private final PoetExtension poetExtension;
    private final JmesPathAcceptorGenerator jmesPathGenerator;
    private final boolean dependsOnHttpAuthAws;
    private final boolean useSraAuth;
    private final boolean multiAuthSigv4a;


    public EndpointResolverInterceptorSpec(IntermediateModel model) {
        this.model = model;
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
        this.endpointParamsKnowledgeIndex = EndpointParamsKnowledgeIndex.of(model);
        this.poetExtension = new PoetExtension(model);
        this.jmesPathGenerator = new JmesPathAcceptorGenerator(poetExtension.jmesPathRuntimeClass());

        // We need to know whether the service has a dependency on the http-auth-aws module. Because we can't check that
        // directly, assume that if they're using AwsV4AuthScheme or AwsV4aAuthScheme that it's available.
        Set<Class<?>> supportedAuthSchemes =
            ModelAuthSchemeClassesKnowledgeIndex.of(model).serviceConcreteAuthSchemeClasses();
        this.dependsOnHttpAuthAws = supportedAuthSchemes.contains(AwsV4AuthScheme.class) ||
                                    supportedAuthSchemes.contains(AwsV4aAuthScheme.class);

        this.useSraAuth = new AuthSchemeSpecUtils(model).useSraAuth();
        this.multiAuthSigv4a = new AuthSchemeSpecUtils(model).usesSigV4a();
    }

    @Override
    public TypeSpec poetSpec() {
        FieldSpec endpointAuthSchemeStrategyFieldSpec = endpointAuthSchemeStrategyFieldSpec();
        TypeSpec.Builder b = PoetUtils.createClassBuilder(className())
                                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                      .addAnnotation(SdkInternalApi.class)
                                      .addSuperinterface(ExecutionInterceptor.class);

        if (!useSraAuth) {
            b.addField(endpointAuthSchemeStrategyFieldSpec);
            b.addMethod(constructorMethodSpec(endpointAuthSchemeStrategyFieldSpec.name));
        }
        b.addMethod(modifyRequestMethod(endpointAuthSchemeStrategyFieldSpec.name));
        b.addMethod(modifyHttpRequestMethod());
        b.addMethod(ruleParams());

        b.addMethod(setContextParams());
        addContextParamMethods(b);

        b.addMethod(setStaticContextParamsMethod());
        addStaticContextParamMethods(b);

        b.addMethod(authSchemeWithEndpointSignerPropertiesMethod());

        if (hasClientContextParams()) {
            b.addMethod(setClientContextParamsMethod());
        }

        b.addMethod(setOperationContextParams());
        addOperationContextParamMethods(b);

        b.addMethod(hostPrefixMethod());

        if (!useSraAuth) {
            b.addMethod(signerProviderMethod());
        }

        if (multiAuthSigv4a) {
            b.addMethod(createHasRegionSetMethod());
            b.addMethod(createUpdateAuthSchemeWithRegionSetMethod());
        }
        endpointParamsKnowledgeIndex.addAccountIdMethodsIfPresent(b);
        return b.build();
    }

    @Override
    public ClassName className() {
        return endpointRulesSpecUtils.resolverInterceptorName();
    }

    private FieldSpec endpointAuthSchemeStrategyFieldSpec() {
        return FieldSpec.builder(endpointRulesSpecUtils.rulesRuntimeClassName("EndpointAuthSchemeStrategy"),
                                 "endpointAuthSchemeStrategy", Modifier.PRIVATE, Modifier.FINAL)
                        .build();
    }

    private MethodSpec modifyRequestMethod(String endpointAuthSchemeStrategyFieldName) {

        MethodSpec.Builder b = MethodSpec.methodBuilder("modifyRequest")
                                         .addModifiers(Modifier.PUBLIC)
                                         .addAnnotation(Override.class)
                                         .returns(SdkRequest.class)
                                         .addParameter(Context.ModifyRequest.class, "context")
                                         .addParameter(ExecutionAttributes.class, "executionAttributes");

        String providerVar = "provider";

        b.addStatement("$T result = context.request()", SdkRequest.class);
        // We skip resolution if the source of the endpoint is the endpoint discovery call
        b.beginControlFlow("if ($1T.endpointIsDiscovered(executionAttributes))",
                           endpointRulesSpecUtils.rulesRuntimeClassName("AwsEndpointProviderUtils"));
        b.addStatement("return result");
        b.endControlFlow();

        b.addStatement("$1T $2N = ($1T) executionAttributes.getAttribute($3T.ENDPOINT_PROVIDER)",
                       endpointRulesSpecUtils.providerInterfaceName(), providerVar, SdkInternalExecutionAttribute.class);
        b.beginControlFlow("try");
        b.addStatement("long resolveEndpointStart = $T.nanoTime()", System.class);
        b.addStatement("$T endpointParams = ruleParams(result, executionAttributes)",
                       endpointRulesSpecUtils.parametersClassName());
        b.addStatement("$T endpoint = $N.resolveEndpoint(endpointParams).join()",
                       Endpoint.class, providerVar);
        b.addStatement("$1T resolveEndpointDuration = $1T.ofNanos($2T.nanoTime() - resolveEndpointStart)", Duration.class,
                       System.class);
        b.addStatement("$T metricCollector = executionAttributes.getOptionalAttribute($T.API_CALL_METRIC_COLLECTOR)",
                       ParameterizedTypeName.get(Optional.class, MetricCollector.class), SdkExecutionAttribute.class);
        b.addStatement("metricCollector.ifPresent(mc -> mc.reportMetric($T.ENDPOINT_RESOLVE_DURATION, resolveEndpointDuration))",
                       CoreMetric.class);
        b.beginControlFlow("if (!$T.disableHostPrefixInjection(executionAttributes))",
                           endpointRulesSpecUtils.rulesRuntimeClassName("AwsEndpointProviderUtils"));
        b.addStatement("$T hostPrefix = hostPrefix(executionAttributes.getAttribute($T.OPERATION_NAME), result)",
                       ParameterizedTypeName.get(Optional.class, String.class), SdkExecutionAttribute.class);
        b.beginControlFlow("if (hostPrefix.isPresent())");
        b.addStatement("endpoint = $T.addHostPrefix(endpoint, hostPrefix.get())",
                       endpointRulesSpecUtils.rulesRuntimeClassName("AwsEndpointProviderUtils"));
        b.endControlFlow();
        b.endControlFlow();


        // If the endpoint resolver returns auth settings, use them as signer properties.
        // This effectively works to set the preSRA Signer ExecutionAttributes, so it is not conditional on useSraAuth.
        b.addStatement("$T<$T> endpointAuthSchemes = endpoint.attribute($T.AUTH_SCHEMES)",
                       List.class, EndpointAuthScheme.class, AwsEndpointAttribute.class);
        b.addStatement("$T<?> selectedAuthScheme = executionAttributes.getAttribute($T.SELECTED_AUTH_SCHEME)",
                       SelectedAuthScheme.class, SdkInternalExecutionAttribute.class);
        b.beginControlFlow("if (endpointAuthSchemes != null && selectedAuthScheme != null)");
        b.addStatement("selectedAuthScheme = authSchemeWithEndpointSignerProperties(endpointAuthSchemes, selectedAuthScheme)");
        if (multiAuthSigv4a) {
            b.beginControlFlow("if(!hasRegionSet(selectedAuthScheme))");
            b.addStatement("selectedAuthScheme = updateAuthSchemeWithRegionSet(selectedAuthScheme, endpointParams)");
            b.endControlFlow();
        }
        b.addStatement("executionAttributes.putAttribute($T.SELECTED_AUTH_SCHEME, selectedAuthScheme)",
                       SdkInternalExecutionAttribute.class);
        b.endControlFlow();

        // For pre SRA client, use Signer as determined by endpoint resolved auth scheme
        if (!useSraAuth) {
            b.beginControlFlow("if (endpointAuthSchemes != null)");
            b.addStatement("$T chosenAuthScheme = $N.chooseAuthScheme(endpointAuthSchemes)", EndpointAuthScheme.class,
                           endpointAuthSchemeStrategyFieldName);
            b.addStatement("$T<$T> signerProvider = signerProvider(chosenAuthScheme)", Supplier.class, Signer.class);
            b.addStatement("result = $T.overrideSignerIfNotOverridden(result, executionAttributes, signerProvider)",
                           SignerOverrideUtils.class);
            b.endControlFlow();
        }

        b.addStatement("executionAttributes.putAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT, endpoint)");
        b.addStatement("return result");
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

    private MethodSpec modifyHttpRequestMethod() {
        MethodSpec.Builder b = MethodSpec.methodBuilder("modifyHttpRequest")
                                         .addModifiers(Modifier.PUBLIC)
                                         .addAnnotation(Override.class)
                                         .returns(SdkHttpRequest.class)
                                         .addParameter(Context.ModifyHttpRequest.class, "context")
                                         .addParameter(ExecutionAttributes.class, "executionAttributes");

        b.addStatement("$T resolvedEndpoint = executionAttributes.getAttribute($T.RESOLVED_ENDPOINT)",
                       Endpoint.class, SdkInternalExecutionAttribute.class);
        b.beginControlFlow("if (resolvedEndpoint.headers().isEmpty())");
        b.addStatement("return context.httpRequest()");
        b.endControlFlow();

        b.addStatement("$T httpRequestBuilder = context.httpRequest().toBuilder()", SdkHttpRequest.Builder.class);
        b.addCode("resolvedEndpoint.headers().forEach((name, values) -> {");
        b.addStatement("values.forEach(v -> httpRequestBuilder.appendHeader(name, v))");
        b.addCode("});");
        b.addStatement("return httpRequestBuilder.build()");

        return b.build();
    }

    private MethodSpec ruleParams() {
        MethodSpec.Builder b = MethodSpec.methodBuilder("ruleParams")
                                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                         .returns(endpointRulesSpecUtils.parametersClassName())
                                         .addParameter(SdkRequest.class, "request")
                                         .addParameter(ExecutionAttributes.class, "executionAttributes");

        b.addStatement("$T builder = $T.builder()", paramsBuilderClass(), endpointRulesSpecUtils.parametersClassName());

        Map<String, ParameterModel> parameters = model.getEndpointRuleSetModel().getParameters();

        parameters.forEach((n, m) -> {
            if (m.getBuiltInEnum() == null) {
                return;
            }
            String setter = Utils.unCapitalize(CodegenNamingUtils.pascalCase(n));
            switch (m.getBuiltInEnum()) {
                case AWS_REGION:
                    b.addStatement(endpointProviderUtilsSetter("regionBuiltIn", setter));
                    break;
                case AWS_USE_DUAL_STACK:
                    b.addStatement(endpointProviderUtilsSetter("dualStackEnabledBuiltIn", setter));
                    break;
                case AWS_USE_FIPS:
                    b.addStatement(endpointProviderUtilsSetter("fipsEnabledBuiltIn", setter));
                    break;
                case SDK_ENDPOINT:
                    b.addStatement(endpointProviderUtilsSetter("endpointBuiltIn", setter));
                    break;
                case AWS_AUTH_ACCOUNT_ID:
                    b.addStatement("builder.$N(resolveAndRecordAccountIdFromIdentity(executionAttributes))", setter);
                    break;
                case AWS_AUTH_ACCOUNT_ID_ENDPOINT_MODE:
                    b.addStatement("builder.$N(recordAccountIdEndpointMode(executionAttributes))", setter);
                    break;
                case AWS_S3_USE_GLOBAL_ENDPOINT:
                    b.addStatement("builder.$N(executionAttributes.getAttribute($T.$N))",
                                   setter, AwsExecutionAttribute.class, model.getNamingStrategy().getEnumValueName(n));
                    break;
                // The S3 specific built-ins are set through the existing S3Configuration and set through client context params
                case AWS_S3_ACCELERATE:
                case AWS_S3_DISABLE_MULTI_REGION_ACCESS_POINTS:
                case AWS_S3_FORCE_PATH_STYLE:
                case AWS_S3_USE_ARN_REGION:
                case AWS_S3_CONTROL_USE_ARN_REGION:
                // end of S3 specific builtins

                // V2 doesn't support this, only regional endpoints
                case AWS_STS_USE_GLOBAL_ENDPOINT:
                    return;
                default:
                    throw new RuntimeException("Don't know how to set built-in " + m.getBuiltInEnum());
            }
        });

        if (hasClientContextParams()) {
            b.addStatement("setClientContextParams(builder, executionAttributes)");
        }
        b.addStatement("setContextParams(builder, executionAttributes.getAttribute($T.OPERATION_NAME), request)",
                       AwsExecutionAttribute.class);
        b.addStatement("setStaticContextParams(builder, executionAttributes.getAttribute($T.OPERATION_NAME))",
                       AwsExecutionAttribute.class);
        b.addStatement("setOperationContextParams(builder, executionAttributes.getAttribute($T.OPERATION_NAME), request)",
                       AwsExecutionAttribute.class);

        b.addStatement("return builder.build()");
        return b.build();
    }

    private CodeBlock endpointProviderUtilsSetter(String builtInFn, String setterName) {
        return CodeBlock.of("builder.$N($T.$N(executionAttributes))", setterName,
                            endpointRulesSpecUtils.rulesRuntimeClassName("AwsEndpointProviderUtils"), builtInFn);
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

    private boolean hasOperationContextParams(OperationModel opModel) {
        return CollectionUtils.isNotEmpty(opModel.getOperationContextParams());
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

    private void addOperationContextParamMethods(TypeSpec.Builder classBuilder) {
        Map<String, OperationModel> operations = model.getOperations();
        operations.forEach((n, m) -> {
            if (hasOperationContextParams(m)) {
                classBuilder.addMethod(setOperationContextParamsMethod(m));
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

    private MethodSpec setOperationContextParams() {
        Map<String, OperationModel> operations = model.getOperations();

        MethodSpec.Builder b = MethodSpec.methodBuilder("setOperationContextParams")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .addParameter(paramsBuilderClass(), "params")
                                         .addParameter(String.class, "operationName")
                                         .addParameter(SdkRequest.class, "request")
                                         .returns(void.class);

        boolean generateSwitch = operations.values().stream().anyMatch(this::hasOperationContextParams);
        if (generateSwitch) {
            b.beginControlFlow("switch (operationName)");

            operations.forEach((n, m) -> {
                if (!hasOperationContextParams(m)) {
                    return;
                }

                String requestClassName = model.getNamingStrategy().getRequestClassName(m.getOperationName());
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

    private MethodSpec setOperationContextParamsMethod(OperationModel opModel) {
        String requestClassName = model.getNamingStrategy().getRequestClassName(opModel.getOperationName());
        ClassName requestClass = poetExtension.getModelClass(requestClassName);

        MethodSpec.Builder b = MethodSpec.methodBuilder("setOperationContextParams")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .addParameter(paramsBuilderClass(), "params")
                                         .addParameter(requestClass, "request")
                                         .returns(void.class);

        b.addStatement("$1T input = new $1T(request)", poetExtension.jmesPathRuntimeClass().nestedClass("Value"));

        opModel.getOperationContextParams().forEach((key, value) -> {
            if (Objects.requireNonNull(value.getPath().asToken()) == JsonToken.VALUE_STRING) {
                String setterName = endpointRulesSpecUtils.paramMethodName(key);

                String jmesPathString = ((JrsString) value.getPath()).getValue();
                CodeBlock addParam = CodeBlock.builder()
                                              .add("params.$N(", setterName)
                                              .add(jmesPathGenerator.interpret(jmesPathString, "input"))
                                              .add(matchToParameterType(key))
                                              .add(")")
                                              .build();

                b.addStatement(addParam);
            } else {
                throw new RuntimeException("Invalid operation context parameter path for " + opModel.getOperationName() +
                                           ". Expected VALUE_STRING, but got " + value.getPath().asToken());
            }

        });

        return b.build();
    }

    private CodeBlock matchToParameterType(String paramName) {
        Map<String, ParameterModel> parameters = model.getEndpointRuleSetModel().getParameters();
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

    private MethodSpec authSchemeWithEndpointSignerPropertiesMethod() {
        TypeVariableName tExtendsIdentity = TypeVariableName.get("T", Identity.class);
        TypeName selectedAuthSchemeOfT = ParameterizedTypeName.get(ClassName.get(SelectedAuthScheme.class),
                                                                   TypeVariableName.get("T"));
        TypeName listOfEndpointAuthScheme = ParameterizedTypeName.get(List.class, EndpointAuthScheme.class);

        MethodSpec.Builder method =
            MethodSpec.methodBuilder("authSchemeWithEndpointSignerProperties")
                      .addModifiers(Modifier.PRIVATE)
                      .addTypeVariable(tExtendsIdentity)
                      .returns(selectedAuthSchemeOfT)
                      .addParameter(listOfEndpointAuthScheme, "endpointAuthSchemes")
                      .addParameter(selectedAuthSchemeOfT, "selectedAuthScheme");

        method.beginControlFlow("for ($T endpointAuthScheme : endpointAuthSchemes)", EndpointAuthScheme.class);

        if (useSraAuth) {
            // Don't include signer properties for auth options that don't match our selected auth scheme
            method.beginControlFlow("if (!endpointAuthScheme.schemeId()"
                                    + ".equals(selectedAuthScheme.authSchemeOption().schemeId()))");
            method.addStatement("continue");
            method.endControlFlow();
        }

        method.addStatement("$T option = selectedAuthScheme.authSchemeOption().toBuilder()", AuthSchemeOption.Builder.class);

        if (dependsOnHttpAuthAws) {
            method.addCode(copyV4EndpointSignerPropertiesToAuth());
            method.addCode(copyV4aEndpointSignerPropertiesToAuth());
            if (endpointRulesSpecUtils.useS3Express()) {
                method.addCode(copyS3ExpressEndpointSignerPropertiesToAuth());
            }
        }

        method.addStatement("throw new $T(\"Endpoint auth scheme '\" + endpointAuthScheme.name() + \"' cannot be mapped to the "
                            + "SDK auth scheme. Was it declared in the service's model?\")",
                            IllegalArgumentException.class);

        method.endControlFlow();

        method.addStatement("return selectedAuthScheme");

        return method.build();
    }

    private static CodeBlock copyV4EndpointSignerPropertiesToAuth() {
        CodeBlock.Builder code = CodeBlock.builder();
        code.beginControlFlow("if (endpointAuthScheme instanceof $T)", SigV4AuthScheme.class);
        code.addStatement("$1T v4AuthScheme = ($1T) endpointAuthScheme", SigV4AuthScheme.class);

        code.beginControlFlow("if (v4AuthScheme.isDisableDoubleEncodingSet())");
        code.addStatement("option.putSignerProperty($T.DOUBLE_URL_ENCODE, !v4AuthScheme.disableDoubleEncoding())",
                          AwsV4HttpSigner.class);
        code.endControlFlow();

        code.beginControlFlow("if (v4AuthScheme.signingRegion() != null)");
        code.addStatement("option.putSignerProperty($T.REGION_NAME, v4AuthScheme.signingRegion())",
                          AwsV4HttpSigner.class);
        code.endControlFlow();

        code.beginControlFlow("if (v4AuthScheme.signingName() != null)");
        code.addStatement("option.putSignerProperty($T.SERVICE_SIGNING_NAME, v4AuthScheme.signingName())",
                          AwsV4HttpSigner.class);
        code.endControlFlow();

        code.addStatement("return new $T<>(selectedAuthScheme.identity(), selectedAuthScheme.signer(), option.build())",
                          SelectedAuthScheme.class);
        code.endControlFlow();
        return code.build();
    }

    private  CodeBlock copyV4aEndpointSignerPropertiesToAuth() {
        CodeBlock.Builder code = CodeBlock.builder();

        code.beginControlFlow("if (endpointAuthScheme instanceof $T)", SigV4aAuthScheme.class);
        code.addStatement("$1T v4aAuthScheme = ($1T) endpointAuthScheme", SigV4aAuthScheme.class);

        code.beginControlFlow("if (v4aAuthScheme.isDisableDoubleEncodingSet())");
        code.addStatement("option.putSignerProperty($T.DOUBLE_URL_ENCODE, !v4aAuthScheme.disableDoubleEncoding())",
                          AwsV4aHttpSigner.class);
        code.endControlFlow();
        if (multiAuthSigv4a) {
            code.beginControlFlow("if (!hasRegionSet(selectedAuthScheme) && v4aAuthScheme.signingRegionSet() != null)");
        } else {
            code.beginControlFlow("if (v4aAuthScheme.signingRegionSet() != null)");
        }
        code.addStatement("$1T regionSet = $1T.create(v4aAuthScheme.signingRegionSet())", RegionSet.class);
        code.addStatement("option.putSignerProperty($T.REGION_SET, regionSet)", AwsV4aHttpSigner.class);
        code.endControlFlow();

        code.beginControlFlow("if (v4aAuthScheme.signingName() != null)");
        code.addStatement("option.putSignerProperty($T.SERVICE_SIGNING_NAME, v4aAuthScheme.signingName())",
                          AwsV4aHttpSigner.class);
        code.endControlFlow();

        code.addStatement("return new $T<>(selectedAuthScheme.identity(), selectedAuthScheme.signer(), option.build())",
                          SelectedAuthScheme.class);
        code.endControlFlow();
        return code.build();
    }

    private CodeBlock copyS3ExpressEndpointSignerPropertiesToAuth() {
        CodeBlock.Builder code = CodeBlock.builder();
        ClassName s3ExpressEndpointAuthSchemeClassName = ClassName.get(
            model.getMetadata().getFullClientPackageName() + ".endpoints.authscheme",
            "S3ExpressEndpointAuthScheme");
        code.beginControlFlow("if (endpointAuthScheme instanceof $T)", s3ExpressEndpointAuthSchemeClassName);
        code.addStatement("$1T s3ExpressAuthScheme = ($1T) endpointAuthScheme", s3ExpressEndpointAuthSchemeClassName);

        code.beginControlFlow("if (s3ExpressAuthScheme.isDisableDoubleEncodingSet())");
        code.addStatement("option.putSignerProperty($T.DOUBLE_URL_ENCODE, !s3ExpressAuthScheme.disableDoubleEncoding())",
                          AwsV4HttpSigner.class);
        code.endControlFlow();

        code.beginControlFlow("if (s3ExpressAuthScheme.signingRegion() != null)");
        code.addStatement("option.putSignerProperty($T.REGION_NAME, s3ExpressAuthScheme.signingRegion())",
                          AwsV4HttpSigner.class);
        code.endControlFlow();

        code.beginControlFlow("if (s3ExpressAuthScheme.signingName() != null)");
        code.addStatement("option.putSignerProperty($T.SERVICE_SIGNING_NAME, s3ExpressAuthScheme.signingName())",
                          AwsV4HttpSigner.class);
        code.endControlFlow();

        code.addStatement("return new $T<>(selectedAuthScheme.identity(), selectedAuthScheme.signer(), option.build())",
                          SelectedAuthScheme.class);
        code.endControlFlow();
        return code.build();
    }

    private MethodSpec signerProviderMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("signerProvider")
                                               .addModifiers(Modifier.PRIVATE)
                                               .addParameter(EndpointAuthScheme.class, "authScheme")
                                               .returns(ParameterizedTypeName.get(Supplier.class, Signer.class));

        builder.beginControlFlow("switch (authScheme.name())");
        builder.addCode("case $S:", "sigv4");
        if (endpointRulesSpecUtils.isS3() || endpointRulesSpecUtils.isS3Control()) {
            builder.addStatement("return $T::create", AwsS3V4Signer.class);
        } else {
            builder.addStatement("return $T::create", Aws4Signer.class);
        }

        builder.addCode("case $S:", "sigv4a");
        if (endpointRulesSpecUtils.isS3() || endpointRulesSpecUtils.isS3Control()) {
            builder.addStatement("return $T::getS3SigV4aSigner", SignerLoader.class);
        } else {
            builder.addStatement("return $T::getSigV4aSigner", SignerLoader.class);
        }

        builder.addCode("default:");
        builder.addStatement("break");
        builder.endControlFlow();

        builder.addStatement("throw $T.create($S + authScheme.name())",
                             SdkClientException.class,
                             "Don't know how to create signer for auth scheme: ");

        return builder.build();
    }

    private MethodSpec constructorMethodSpec(String endpointAuthSchemeFieldName) {
        MethodSpec.Builder b = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        EndpointAuthSchemeConfig endpointAuthSchemeConfig = model.getCustomizationConfig().getEndpointAuthSchemeConfig();
        String factoryLocalVarName = "endpointAuthSchemeStrategyFactory";
        if (endpointAuthSchemeConfig != null && endpointAuthSchemeConfig.getAuthSchemeStrategyFactoryClass() != null) {
            b.addStatement("$T $N = new $T()",
                           endpointRulesSpecUtils.rulesRuntimeClassName("EndpointAuthSchemeStrategyFactory"),
                           factoryLocalVarName,
                           PoetUtils.classNameFromFqcn(endpointAuthSchemeConfig.getAuthSchemeStrategyFactoryClass()));
        } else {
            b.addStatement("$T $N = new $T()",
                           endpointRulesSpecUtils.rulesRuntimeClassName("EndpointAuthSchemeStrategyFactory"),
                           factoryLocalVarName,
                           endpointRulesSpecUtils.rulesRuntimeClassName("DefaultEndpointAuthSchemeStrategyFactory"));
        }
        b.addStatement("this.$N = $N.endpointAuthSchemeStrategy()", endpointAuthSchemeFieldName, factoryLocalVarName);
        return b.build();
    }

    private MethodSpec createHasRegionSetMethod() {
        TypeVariableName tExtendsIdentity = TypeVariableName.get("T", Identity.class);
        TypeName selectedAuthSchemeOfT = ParameterizedTypeName.get(ClassName.get(SelectedAuthScheme.class),
                                                                   TypeVariableName.get("T"));

        return
            MethodSpec.methodBuilder("hasRegionSet")
                      .addModifiers(Modifier.PRIVATE)
                      .addTypeVariable(tExtendsIdentity)
                      .returns(boolean.class)
                      .addParameter(selectedAuthSchemeOfT, "selectedAuthScheme")
                      .addCode(
                          CodeBlock.builder()
                                   .addStatement("return selectedAuthScheme.authSchemeOption().schemeId().equals($T.SCHEME_ID)"
                                                 + " && selectedAuthScheme.authSchemeOption().signerProperty($T.REGION_SET) != "
                                                 + "null", AwsV4aAuthScheme.class, AwsV4aHttpSigner.class)
                                   .build())
                      .build();
    }

    private MethodSpec createUpdateAuthSchemeWithRegionSetMethod() {
        TypeVariableName tExtendsIdentity = TypeVariableName.get("T", Identity.class);
        TypeName selectedAuthSchemeOfT = ParameterizedTypeName.get(
            ClassName.get(SelectedAuthScheme.class),
            TypeVariableName.get("T")
        );

        return MethodSpec.methodBuilder("updateAuthSchemeWithRegionSet")
                         .addModifiers(Modifier.PRIVATE)
                         .addTypeVariable(tExtendsIdentity)
                         .returns(selectedAuthSchemeOfT)
                         .addParameter(selectedAuthSchemeOfT, "selectedAuthScheme")
                         .addParameter(endpointRulesSpecUtils.parametersClassName(), "endpointParams")
                         .addCode(CodeBlock.builder()
                                           .addStatement("$T optionBuilder = selectedAuthScheme.authSchemeOption().toBuilder()",
                                                         ClassName.get(AuthSchemeOption.Builder.class))
                                           .addStatement("$T regionSet = $T.create(endpointParams.region().id())",
                                                         RegionSet.class, RegionSet.class)
                                           .addStatement("optionBuilder.putSignerProperty($T.REGION_SET, regionSet)",
                                                         AwsV4aHttpSigner.class)
                                           .addStatement("return new $T<>(selectedAuthScheme.identity(), " +
                                                         "selectedAuthScheme.signer(), optionBuilder.build())",
                                                         SelectedAuthScheme.class)
                                           .build())
                         .build();
    }

}
