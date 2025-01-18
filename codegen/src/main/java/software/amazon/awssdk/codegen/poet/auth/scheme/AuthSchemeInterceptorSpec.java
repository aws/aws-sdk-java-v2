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

package software.amazon.awssdk.codegen.poet.auth.scheme;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.identity.SdkIdentityProperty;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

public final class AuthSchemeInterceptorSpec implements ClassSpec {
    private final AuthSchemeSpecUtils authSchemeSpecUtils;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public AuthSchemeInterceptorSpec(IntermediateModel intermediateModel) {
        this.authSchemeSpecUtils = new AuthSchemeSpecUtils(intermediateModel);
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
    }

    @Override
    public ClassName className() {
        return authSchemeSpecUtils.authSchemeInterceptor();
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className())
                                            .addSuperinterface(ExecutionInterceptor.class)
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .addAnnotation(SdkInternalApi.class);

        builder.addField(FieldSpec.builder(Logger.class, "LOG", Modifier.PRIVATE, Modifier.STATIC)
                                  .initializer("$T.loggerFor($T.class)", Logger.class, className())
                                  .build());

        builder.addMethod(generateBeforeExecution())
               .addMethod(generateResolveAuthOptions())
               .addMethod(generateSelectAuthScheme())
               .addMethod(generateAuthSchemeParams())
               .addMethod(generateTrySelectAuthScheme())
               .addMethod(generateGetIdentityMetric())
               .addMethod(putSelectedAuthSchemeMethodSpec());
        return builder.build();
    }

    private MethodSpec generateBeforeExecution() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("beforeExecution")
                                               .addAnnotation(Override.class)
                                               .addModifiers(Modifier.PUBLIC)
                                               .addParameter(Context.BeforeExecution.class,
                                                             "context")
                                               .addParameter(ExecutionAttributes.class,
                                                             "executionAttributes");

        builder.addStatement("$T authOptions = resolveAuthOptions(context, executionAttributes)",
                             listOf(AuthSchemeOption.class))
               .addStatement("$T selectedAuthScheme = selectAuthScheme(authOptions, executionAttributes)",
                             wildcardSelectedAuthScheme())
               .addStatement("putSelectedAuthScheme(executionAttributes, selectedAuthScheme)");
        return builder.build();
    }

    private MethodSpec generateResolveAuthOptions() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveAuthOptions")
                                               .addModifiers(Modifier.PRIVATE)
                                               .returns(listOf(AuthSchemeOption.class))
                                               .addParameter(Context.BeforeExecution.class,
                                                             "context")
                                               .addParameter(ExecutionAttributes.class,
                                                             "executionAttributes");

        builder.addStatement("$1T authSchemeProvider = $2T.isInstanceOf($1T.class, executionAttributes"
                             + ".getAttribute($3T.AUTH_SCHEME_RESOLVER), $4S)",
                             authSchemeSpecUtils.providerInterfaceName(),
                             Validate.class,
                             SdkInternalExecutionAttribute.class,
                             "Expected an instance of " + authSchemeSpecUtils.providerInterfaceName().simpleName());
        builder.addStatement("$T params = authSchemeParams(context.request(), executionAttributes)",
                             authSchemeSpecUtils.parametersInterfaceName());
        builder.addStatement("return authSchemeProvider.resolveAuthScheme(params)");
        return builder.build();
    }

    private MethodSpec generateAuthSchemeParams() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("authSchemeParams")
                                               .addModifiers(Modifier.PRIVATE)
                                               .returns(authSchemeSpecUtils.parametersInterfaceName())
                                               .addParameter(SdkRequest.class, "request")
                                               .addParameter(ExecutionAttributes.class, "executionAttributes");

        if (!authSchemeSpecUtils.useEndpointParamsInAuthScheme()) {
            builder.addStatement("$T operation = executionAttributes.getAttribute($T.OPERATION_NAME)", String.class,
                                 SdkExecutionAttribute.class);
            if (authSchemeSpecUtils.usesSigV4()) {
                builder.addStatement("$T region = executionAttributes.getAttribute($T.AWS_REGION)", Region.class,
                                     AwsExecutionAttribute.class);
                builder.addStatement("return $T.builder()"
                                     + ".operation(operation)"
                                     + ".region(region)"
                                     + ".build()",
                                     authSchemeSpecUtils.parametersInterfaceName());
            } else {
                builder.addStatement("return $T.builder()"
                                     + ".operation(operation)"
                                     + ".build()",
                                     authSchemeSpecUtils.parametersInterfaceName());
            }
            return builder.build();
        }

        builder.addStatement("$T endpointParams = $T.ruleParams(request, executionAttributes)",
                             endpointRulesSpecUtils.parametersClassName(),
                             endpointRulesSpecUtils.resolverInterceptorName());
        builder.addStatement("$1T.Builder builder = $1T.builder()", authSchemeSpecUtils.parametersInterfaceName());
        boolean regionIncluded = false;
        for (String paramName : endpointRulesSpecUtils.parameters().keySet()) {
            if (!authSchemeSpecUtils.includeParamForProvider(paramName)) {
                continue;
            }
            regionIncluded = regionIncluded || paramName.equalsIgnoreCase("region");
            String methodName = endpointRulesSpecUtils.paramMethodName(paramName);
            builder.addStatement("builder.$1N(endpointParams.$1N())", methodName);
        }

        builder.addStatement("$T operation = executionAttributes.getAttribute($T.OPERATION_NAME)", String.class,
                             SdkExecutionAttribute.class);
        builder.addStatement("builder.operation(operation)");
        if (authSchemeSpecUtils.usesSigV4() && !regionIncluded) {
            builder.addStatement("$T region = executionAttributes.getAttribute($T.AWS_REGION)", Region.class,
                                 AwsExecutionAttribute.class);
            builder.addStatement("builder.region(region)");
        }
        ClassName paramsBuilderClass = authSchemeSpecUtils.parametersEndpointAwareDefaultImplName().nestedClass("Builder");
        builder.beginControlFlow("if (builder instanceof $T)",
                                 paramsBuilderClass);
        ClassName endpointProviderClass = endpointRulesSpecUtils.providerInterfaceName();
        builder.addStatement("$T endpointProvider = executionAttributes.getAttribute($T.ENDPOINT_PROVIDER)",
                             EndpointProvider.class,
                             SdkInternalExecutionAttribute.class);
        builder.beginControlFlow("if (endpointProvider instanceof $T)", endpointProviderClass);
        builder.addStatement("(($T)builder).endpointProvider(($T)endpointProvider)", paramsBuilderClass, endpointProviderClass);
        builder.endControlFlow();
        builder.endControlFlow();
        if (authSchemeSpecUtils.hasMultiAuthSigvOrSigv4a()) {
            generateSigv4aRegionSet(builder);
        }
        builder.addStatement("return builder.build()");
        return builder.build();
    }

    private MethodSpec generateSelectAuthScheme() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("selectAuthScheme")
                                               .addModifiers(Modifier.PRIVATE)
                                               .returns(wildcardSelectedAuthScheme())
                                               .addParameter(listOf(AuthSchemeOption.class), "authOptions")
                                               .addParameter(ExecutionAttributes.class, "executionAttributes");

        builder.addStatement("$T metricCollector = executionAttributes.getAttribute($T.API_CALL_METRIC_COLLECTOR)",
                             MetricCollector.class, SdkExecutionAttribute.class)
               .addStatement("$T authSchemes = executionAttributes.getAttribute($T.AUTH_SCHEMES)",
                             mapOf(String.class, wildcardAuthScheme()),
                             SdkInternalExecutionAttribute.class)
               .addStatement("$T identityProviders = executionAttributes.getAttribute($T.IDENTITY_PROVIDERS)",
                             IdentityProviders.class, SdkInternalExecutionAttribute.class)
               .addStatement("$T discardedReasons = new $T<>()",
                             listOfStringSuppliers(), ArrayList.class);

        builder.beginControlFlow("for ($T authOption : authOptions)", AuthSchemeOption.class);
        {
            builder.addStatement("$T authScheme = authSchemes.get(authOption.schemeId())", wildcardAuthScheme())
                   .addStatement("$T selectedAuthScheme = trySelectAuthScheme(authOption, authScheme, identityProviders, "
                                 + "discardedReasons, metricCollector, executionAttributes)",
                                 wildcardSelectedAuthScheme());
            builder.beginControlFlow("if (selectedAuthScheme != null)");
            {
                addLogDebugDiscardedOptions(builder);
                builder.addStatement("return selectedAuthScheme")
                       .endControlFlow();
            }
            // end foreach
            builder.endControlFlow();
        }
        builder.addStatement("throw $T.builder()"
                             + ".message($S + discardedReasons.stream().map($T::get).collect($T.joining(\", \")))"
                             + ".build()",
                             SdkException.class,
                             "Failed to determine how to authenticate the user: ",
                             Supplier.class,
                             Collectors.class);
        return builder.build();
    }

    //TODO (s3express) Review "general" identity properties and their propagation
    private MethodSpec generateTrySelectAuthScheme() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("trySelectAuthScheme")
                                               .addModifiers(Modifier.PRIVATE)
                                               .returns(namedSelectedAuthScheme())
                                               .addParameter(AuthSchemeOption.class, "authOption")
                                               .addParameter(namedAuthScheme(), "authScheme")
                                               .addParameter(IdentityProviders.class, "identityProviders")
                                               .addParameter(listOfStringSuppliers(), "discardedReasons")
                                               .addParameter(MetricCollector.class, "metricCollector")
                                               .addParameter(ExecutionAttributes.class, "executionAttributes")
                                               .addTypeVariable(TypeVariableName.get("T", Identity.class));

        builder.beginControlFlow("if (authScheme == null)");
        {
            builder.addStatement("discardedReasons.add(() -> String.format($S, authOption.schemeId()))",
                                 "'%s' is not enabled for this request.")
                   .addStatement("return null")
                   .endControlFlow();
        }
        builder.addStatement("$T identityProvider = authScheme.identityProvider(identityProviders)",
                             namedIdentityProvider());

        builder.beginControlFlow("if (identityProvider == null)");
        {
            builder.addStatement("discardedReasons.add(() -> String.format($S, authOption.schemeId()))",
                                 "'%s' does not have an identity provider configured.")
                   .addStatement("return null")
                   .endControlFlow();
        }

        builder.addStatement("$T signer",
                             ParameterizedTypeName.get(ClassName.get(HttpSigner.class), TypeVariableName.get("T")));
        builder.beginControlFlow("try");
        {
            builder.addStatement("signer = authScheme.signer()");
            builder.nextControlFlow("catch (RuntimeException e)");
            builder.addStatement("discardedReasons.add(() -> String.format($S, authOption.schemeId(), e.getMessage()))",
                                 "'%s' signer could not be retrieved: %s")
                   .addStatement("return null")
                   .endControlFlow();
        }


        builder.addStatement("$T.Builder identityRequestBuilder = $T.builder()",
                             ResolveIdentityRequest.class,
                             ResolveIdentityRequest.class);
        builder.addStatement("authOption.forEachIdentityProperty(identityRequestBuilder::putProperty)");
        if (endpointRulesSpecUtils.isS3()) {
            builder.addStatement("identityRequestBuilder.putProperty($T.SDK_CLIENT, "
                                 + "executionAttributes.getAttribute($T.SDK_CLIENT))",
                                 SdkIdentityProperty.class,
                                 SdkInternalExecutionAttribute.class);
        }
        builder.addStatement("$T identity", namedIdentityFuture());
        builder.addStatement("$T metric = getIdentityMetric(identityProvider)", durationSdkMetric());
        builder.beginControlFlow("if (metric == null)")
               .addStatement("identity = identityProvider.resolveIdentity(identityRequestBuilder.build())")
               .nextControlFlow("else")
               .addStatement("identity = $T.reportDuration("
                             + "() -> identityProvider.resolveIdentity(identityRequestBuilder.build()), metricCollector, metric)",
                             MetricUtils.class)
               .endControlFlow();

        builder.addStatement("return new $T<>(identity, signer, authOption)", SelectedAuthScheme.class);
        return builder.build();
    }

    private MethodSpec generateGetIdentityMetric() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("getIdentityMetric")
                                               .addModifiers(Modifier.PRIVATE)
                                               .returns(durationSdkMetric())
                                               .addParameter(wildcardIdentityProvider(), "identityProvider");

        builder.addStatement("Class<?> identityType = identityProvider.identityType()")
               .beginControlFlow("if (identityType == $T.class)", AwsCredentialsIdentity.class)
               .addStatement("return $T.CREDENTIALS_FETCH_DURATION", CoreMetric.class)
               .endControlFlow()
               .beginControlFlow("if (identityType == $T.class)", TokenIdentity.class)
               .addStatement("return $T.TOKEN_FETCH_DURATION", CoreMetric.class)
               .endControlFlow()
               .addStatement("return null");

        return builder.build();
    }

    private MethodSpec putSelectedAuthSchemeMethodSpec() {
        String attributeParamName = "attributes";
        String selectedAuthSchemeParamName = "selectedAuthScheme";
        MethodSpec.Builder builder = MethodSpec.methodBuilder("putSelectedAuthScheme")
                                               .addModifiers(Modifier.PRIVATE)
                                               .addTypeVariable(TypeVariableName.get("T", Identity.class))
                                               .addParameter(ExecutionAttributes.class, attributeParamName)
                                               .addParameter(ParameterSpec.builder(
                                                   ParameterizedTypeName.get(ClassName.get(SelectedAuthScheme.class),
                                                                             TypeVariableName.get("T")),
                                                   selectedAuthSchemeParamName).build());
        builder.addStatement("$T existingAuthScheme = $N.getAttribute($T.SELECTED_AUTH_SCHEME)",
                             ParameterizedTypeName.get(ClassName.get(SelectedAuthScheme.class),
                                                       WildcardTypeName.subtypeOf(Object.class)),
                             attributeParamName,
                             SdkInternalExecutionAttribute.class);

        builder.beginControlFlow("if (existingAuthScheme != null)")
               .addStatement("$T selectedOption = $N.authSchemeOption().toBuilder()",
                             AuthSchemeOption.Builder.class, selectedAuthSchemeParamName)
               .addStatement("existingAuthScheme.authSchemeOption().forEachIdentityProperty"
                             + "(selectedOption::putIdentityPropertyIfAbsent)")
               .addStatement("existingAuthScheme.authSchemeOption().forEachSignerProperty"
                             + "(selectedOption::putSignerPropertyIfAbsent)")
               .addStatement("$N = new $T<>($N.identity(), $N.signer(), selectedOption.build())",
                             selectedAuthSchemeParamName,
                             SelectedAuthScheme.class,
                             selectedAuthSchemeParamName,
                             selectedAuthSchemeParamName);
        builder.endControlFlow();

        builder.addStatement("$N.putAttribute($T.SELECTED_AUTH_SCHEME, $N)",
                             attributeParamName, SdkInternalExecutionAttribute.class, selectedAuthSchemeParamName);

        return builder.build();
    }

    private void addLogDebugDiscardedOptions(MethodSpec.Builder builder) {
        builder.beginControlFlow("if (!discardedReasons.isEmpty())");
        {
            builder.addStatement("LOG.debug(() -> String.format(\"%s auth will be used, discarded: '%s'\", "
                                 + "authOption.schemeId(), "
                                 + "discardedReasons.stream().map($T::get).collect($T.joining(\", \"))))",
                                 Supplier.class, Collectors.class)
                   .endControlFlow();
        }
    }

    // IdentityProvider<T>
    private TypeName namedIdentityProvider() {
        return ParameterizedTypeName.get(ClassName.get(IdentityProvider.class), TypeVariableName.get("T"));
    }

    // IdentityProvider<?>
    private TypeName wildcardIdentityProvider() {
        return ParameterizedTypeName.get(ClassName.get(IdentityProvider.class), WildcardTypeName.subtypeOf(Object.class));
    }

    // CompletableFuture<? extends T>
    private TypeName namedIdentityFuture() {
        return ParameterizedTypeName.get(ClassName.get(CompletableFuture.class),
                                         WildcardTypeName.subtypeOf(TypeVariableName.get("T")));
    }

    // AuthScheme<T>
    private TypeName namedAuthScheme() {
        return ParameterizedTypeName.get(ClassName.get(AuthScheme.class),
                                         TypeVariableName.get("T", Identity.class));
    }

    // AuthScheme<?>
    private TypeName wildcardAuthScheme() {
        return ParameterizedTypeName.get(ClassName.get(AuthScheme.class),
                                         WildcardTypeName.subtypeOf(Object.class));
    }

    // SelectedAuthScheme<T>
    private TypeName namedSelectedAuthScheme() {
        return ParameterizedTypeName.get(ClassName.get(SelectedAuthScheme.class),
                                         TypeVariableName.get("T", Identity.class));
    }

    // SelectedAuthScheme<?>
    private TypeName wildcardSelectedAuthScheme() {
        return ParameterizedTypeName.get(ClassName.get(SelectedAuthScheme.class),
                                         WildcardTypeName.subtypeOf(Identity.class));
    }

    // List<Supplier<String>>
    private TypeName listOfStringSuppliers() {
        return listOf(ParameterizedTypeName.get(Supplier.class, String.class));
    }

    // Map<key, value>
    private TypeName mapOf(Object keyType, Object valueType) {
        return ParameterizedTypeName.get(ClassName.get(Map.class), toTypeName(keyType), toTypeName(valueType));
    }

    // List<values>
    private TypeName listOf(Object valueType) {
        return ParameterizedTypeName.get(ClassName.get(List.class), toTypeName(valueType));
    }

    // SdkMetric<Duration>
    private ParameterizedTypeName durationSdkMetric() {
        return ParameterizedTypeName.get(ClassName.get(SdkMetric.class), toTypeName(Duration.class));
    }

    private TypeName toTypeName(Object valueType) {
        TypeName result;
        if (valueType instanceof Class<?>) {
            result = ClassName.get((Class<?>) valueType);
        } else if (valueType instanceof TypeName) {
            result = (TypeName) valueType;
        } else {
            throw new IllegalArgumentException("Don't know how to convert " + valueType + " to TypeName");
        }
        return result;
    }

    private void generateSigv4aRegionSet(MethodSpec.Builder builder) {
        if (authSchemeSpecUtils.usesSigV4a()) {
            builder.addStatement(
                "executionAttributes.getOptionalAttribute($T.AWS_SIGV4A_SIGNING_REGION_SET)\n" +
                "                   .filter(regionSet -> !$T.isNullOrEmpty(regionSet))\n" +
                "                   .ifPresent(nonEmptyRegionSet -> builder.regionSet($T.create(nonEmptyRegionSet)))",
                AwsExecutionAttribute.class,
                CollectionUtils.class,
                RegionSet.class
            );
        }
    }
}
