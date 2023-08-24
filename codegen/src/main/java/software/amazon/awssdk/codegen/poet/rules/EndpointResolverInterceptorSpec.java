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
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
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
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionScope;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.HostnameValidator;
import software.amazon.awssdk.utils.StringUtils;

public class EndpointResolverInterceptorSpec implements ClassSpec {
    private final IntermediateModel model;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;
    private final PoetExtension poetExtension;
    private final boolean dependsOnHttpAuthAws;

    public EndpointResolverInterceptorSpec(IntermediateModel model) {
        this.model = model;
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
        this.poetExtension = new PoetExtension(model);

        // We need to know whether the service has a dependency on the http-auth-aws module. Because we can't check that
        // directly, assume that if they're using AwsV4AuthScheme or AwsV4aAuthScheme that it's available.
        Set<Class<?>> supportedAuthSchemes = new AuthSchemeSpecUtils(model).allServiceConcreteAuthSchemeClasses();
        this.dependsOnHttpAuthAws = supportedAuthSchemes.contains(AwsV4AuthScheme.class) ||
                                    supportedAuthSchemes.contains(AwsV4aAuthScheme.class);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder b = PoetUtils.createClassBuilder(className())
                                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                      .addAnnotation(SdkInternalApi.class)
                                      .addSuperinterface(ExecutionInterceptor.class);

        b.addMethod(modifyRequestMethod());
        b.addMethod(modifyHttpRequestMethod());
        b.addMethod(ruleParams());

        b.addMethod(setContextParams());
        addContextParamMethods(b);

        b.addMethod(setStaticContextParamsMethod());
        addStaticContextParamMethods(b);

        b.addMethod(authSchemeWithEndpointSignerPropertiesMethod());
        b.addMethod(copySignerPropertiesToAttributesMethod());

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

        b.addStatement("$T result = context.request()", SdkRequest.class);
        // We skip resolution if the source of the endpoint is the endpoint discovery call
        b.beginControlFlow("if ($1T.endpointIsDiscovered(executionAttributes))",
                           endpointRulesSpecUtils.rulesRuntimeClassName("AwsEndpointProviderUtils"));
        b.addStatement("return result");
        b.endControlFlow();

        b.addStatement("$1T $2N = ($1T) executionAttributes.getAttribute($3T.ENDPOINT_PROVIDER)",
                       endpointRulesSpecUtils.providerInterfaceName(), providerVar, SdkInternalExecutionAttribute.class);
        b.beginControlFlow("try");
        b.addStatement("$T endpoint = $N.resolveEndpoint(ruleParams(result, executionAttributes)).join()",
                       Endpoint.class, providerVar);
        b.beginControlFlow("if (!$T.disableHostPrefixInjection(executionAttributes))",
                           endpointRulesSpecUtils.rulesRuntimeClassName("AwsEndpointProviderUtils"));
        b.addStatement("$T hostPrefix = hostPrefix(executionAttributes.getAttribute($T.OPERATION_NAME), result)",
                       ParameterizedTypeName.get(Optional.class, String.class), SdkExecutionAttribute.class);
        b.beginControlFlow("if (hostPrefix.isPresent())");
        b.addStatement("endpoint = $T.addHostPrefix(endpoint, hostPrefix.get())",
                       endpointRulesSpecUtils.rulesRuntimeClassName("AwsEndpointProviderUtils"));
        b.endControlFlow();
        b.endControlFlow();


        // If the endpoint resolver returns auth settings, use them as signer properties
        b.addStatement("$T<$T> endpointAuthSchemes = endpoint.attribute($T.AUTH_SCHEMES)",
                       List.class, EndpointAuthScheme.class, AwsEndpointAttribute.class);
        b.addStatement("$T<?> selectedAuthScheme = executionAttributes.getAttribute($T.SELECTED_AUTH_SCHEME)",
                       SelectedAuthScheme.class, SdkInternalExecutionAttribute.class);
        b.beginControlFlow("if (endpointAuthSchemes != null && selectedAuthScheme != null)");
        b.addStatement("selectedAuthScheme = authSchemeWithEndpointSignerProperties(endpointAuthSchemes, selectedAuthScheme)");

        b.addStatement("executionAttributes.putAttribute($T.SELECTED_AUTH_SCHEME, selectedAuthScheme)",
                       SdkInternalExecutionAttribute.class);
        b.endControlFlow();


        // Backwards-compatibility with old signers.
        b.beginControlFlow("if (selectedAuthScheme != null)");
        b.addStatement("copySignerPropertiesToAttributes(selectedAuthScheme.authSchemeOption(), executionAttributes)");
        b.endControlFlow();

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
        b.addStatement("setContextParams(builder, executionAttributes.getAttribute($T.OPERATION_NAME), request)",
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

        // Don't include signer properties for auth options that don't match our selected auth scheme
        method.beginControlFlow("if (!endpointAuthScheme.schemeId().equals(selectedAuthScheme.authSchemeOption().schemeId()))");
        method.addStatement("continue");
        method.endControlFlow();

        method.addStatement("$T option = selectedAuthScheme.authSchemeOption().toBuilder()", AuthSchemeOption.Builder.class);

        if (dependsOnHttpAuthAws) {
            method.addCode(copyV4EndpointSignerPropertiesToAuth());
            method.addCode(copyV4aEndpointSignerPropertiesToAuth());
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

    private static CodeBlock copyV4aEndpointSignerPropertiesToAuth() {
        CodeBlock.Builder code = CodeBlock.builder();

        code.beginControlFlow("if (endpointAuthScheme instanceof $T)", SigV4aAuthScheme.class);
        code.addStatement("$1T v4aAuthScheme = ($1T) endpointAuthScheme", SigV4aAuthScheme.class);

        code.beginControlFlow("if (v4aAuthScheme.isDisableDoubleEncodingSet())");
        code.addStatement("option.putSignerProperty($T.DOUBLE_URL_ENCODE, !v4aAuthScheme.disableDoubleEncoding())",
                          AwsV4aHttpSigner.class);
        code.endControlFlow();

        code.beginControlFlow("if (v4aAuthScheme.signingRegionSet() != null)");
        code.addStatement("option.putSignerProperty($T.REGION_NAME, $T.join(\",\", v4aAuthScheme.signingRegionSet()))",
                          AwsV4aHttpSigner.class, String.class);
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

    private MethodSpec copySignerPropertiesToAttributesMethod() {
        MethodSpec.Builder method =
            MethodSpec.methodBuilder("copySignerPropertiesToAttributes")
                      .addModifiers(Modifier.PRIVATE)
                      .addParameter(AuthSchemeOption.class, "authOption")
                      .addParameter(ExecutionAttributes.class, "executionAttributes");

        if (dependsOnHttpAuthAws) {
            method.addCode(copyV4SignerPropertiesToAttributes());
            method.addCode(copyV4aSignerPropertiesToAttributes());
        }

        return method.build();
    }

    private static CodeBlock copyV4SignerPropertiesToAttributes() {
        CodeBlock.Builder code = CodeBlock.builder();
        code.beginControlFlow("if (authOption.schemeId().equals($T.SCHEME_ID))", AwsV4AuthScheme.class);

        code.beginControlFlow("if (authOption.signerProperty($T.DOUBLE_URL_ENCODE) != null)", AwsV4HttpSigner.class);
        code.addStatement("executionAttributes.putAttribute($T.SIGNER_DOUBLE_URL_ENCODE, authOption.signerProperty($T"
                            + ".DOUBLE_URL_ENCODE))",
                            AwsSignerExecutionAttribute.class, AwsV4HttpSigner.class);
        code.endControlFlow();

        code.beginControlFlow("if (authOption.signerProperty($T.SERVICE_SIGNING_NAME) != null)", AwsV4HttpSigner.class);
        code.addStatement("executionAttributes.putAttribute($T.SERVICE_SIGNING_NAME, authOption.signerProperty($T"
                            + ".SERVICE_SIGNING_NAME))",
                            AwsSignerExecutionAttribute.class, AwsV4HttpSigner.class);
        code.endControlFlow();

        code.beginControlFlow("if (authOption.signerProperty($T.REGION_NAME) != null)", AwsV4HttpSigner.class);
        code.addStatement("executionAttributes.putAttribute($T.SIGNING_REGION, $T.of(authOption.signerProperty($T"
                            + ".REGION_NAME)))",
                          AwsSignerExecutionAttribute.class, Region.class, AwsV4HttpSigner.class);
        code.endControlFlow();

        code.addStatement("return");
        code.endControlFlow();
        return code.build();
    }

    private static CodeBlock copyV4aSignerPropertiesToAttributes() {
        CodeBlock.Builder code = CodeBlock.builder();
        code.beginControlFlow("if (authOption.schemeId().equals($T.SCHEME_ID))", AwsV4aAuthScheme.class);

        code.beginControlFlow("if (authOption.signerProperty($T.DOUBLE_URL_ENCODE) != null)", AwsV4aHttpSigner.class);
        code.addStatement("executionAttributes.putAttribute($T.SIGNER_DOUBLE_URL_ENCODE, authOption.signerProperty($T"
                          + ".DOUBLE_URL_ENCODE))",
                          AwsSignerExecutionAttribute.class, AwsV4aHttpSigner.class);
        code.endControlFlow();

        code.beginControlFlow("if (authOption.signerProperty($T.SERVICE_SIGNING_NAME) != null)", AwsV4aHttpSigner.class);
        code.addStatement("executionAttributes.putAttribute($T.SERVICE_SIGNING_NAME, authOption.signerProperty($T"
                          + ".SERVICE_SIGNING_NAME))",
                          AwsSignerExecutionAttribute.class, AwsV4aHttpSigner.class);
        code.endControlFlow();

        code.beginControlFlow("if (authOption.signerProperty($T.REGION_NAME) != null)", AwsV4aHttpSigner.class);
        code.addStatement("executionAttributes.putAttribute($T.SIGNING_REGION_SCOPE, $T.create(authOption.signerProperty($T"
                          + ".REGION_NAME)))",
                          AwsSignerExecutionAttribute.class, RegionScope.class, AwsV4aHttpSigner.class);
        code.endControlFlow();

        code.addStatement("return");
        code.endControlFlow();
        return code.build();
    }
}
