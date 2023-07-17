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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.auth.spi.AuthScheme;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.IdentityProviderConfiguration;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

public final class AuthSchemeInterceptorSpec implements ClassSpec {
    private static final String SMITHY_NO_AUTH = "smithy.auth#noAuth";
    private final AuthSchemeSpecUtils authSchemeSpecUtils;

    public AuthSchemeInterceptorSpec(IntermediateModel intermediateModel) {
        this.authSchemeSpecUtils = new AuthSchemeSpecUtils(intermediateModel);
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
               .addMethod(generateTrySelectAuthScheme());
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
               .addStatement("executionAttributes.putAttribute($T.SELECTED_AUTH_SCHEME, selectedAuthScheme)",
                             SdkInternalExecutionAttribute.class);
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

        builder.addStatement("$T operation = executionAttributes.getAttribute($T.OPERATION_NAME)", String.class,
                             SdkExecutionAttribute.class);
        if (authSchemeSpecUtils.usesSigV4()) {
            builder.addStatement("$T region = executionAttributes.getAttribute($T.AWS_REGION)", Region.class,
                                 AwsExecutionAttribute.class);
        }
        builder.addStatement("$1T authSchemeProvider = $2T.isInstanceOf($1T.class, executionAttributes"
                             + ".getAttribute($3T.AUTH_SCHEME_RESOLVER), $4S)",
                             authSchemeSpecUtils.providerInterfaceName(),
                             Validate.class,
                             SdkInternalExecutionAttribute.class,
                             "Expected an instance of " + authSchemeSpecUtils.providerInterfaceName().simpleName());
        if (authSchemeSpecUtils.usesSigV4()) {
            builder.addStatement("return authSchemeProvider.resolveAuthScheme($T.builder()"
                                 + ".operation(operation)"
                                 + ".region(region)"
                                 + ".build())",
                                 authSchemeSpecUtils.parametersInterfaceName());
        } else {
            builder.addStatement("return authSchemeProvider.resolveAuthScheme($T.builder()"
                                 + ".operation(operation)"
                                 + ".build())",
                                 authSchemeSpecUtils.parametersInterfaceName());
        }
        return builder.build();
    }

    private MethodSpec generateSelectAuthScheme() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("selectAuthScheme")
                                               .addModifiers(Modifier.PRIVATE)
                                               .returns(wildcardSelectedAuthScheme())
                                               .addParameter(listOf(AuthSchemeOption.class),
                                                             "authOptions")
                                               .addParameter(ExecutionAttributes.class,
                                                             "executionAttributes");

        builder.addStatement("$T authSchemes = executionAttributes.getAttribute($T.AUTH_SCHEMES)",
                             mapOf(String.class, wildcardAuthScheme()),
                             SdkInternalExecutionAttribute.class)
               .addStatement("$T identityResolvers = executionAttributes.getAttribute($T.IDENTITY_PROVIDER_CONFIGURATION)",
                             IdentityProviderConfiguration.class, SdkInternalExecutionAttribute.class)
               .addStatement("$T discardedReasons = new $T<>()",
                             listOfStringSuppliers(), ArrayList.class);

        builder.beginControlFlow("for ($T authOption : authOptions)", AuthSchemeOption.class);
        {
            builder.beginControlFlow("if (authOption.schemeId().equals($S))", SMITHY_NO_AUTH);
            {
                addLogDebugDiscardedOptions(builder);
                builder.addStatement("return new $T(null, null, authOption)", SelectedAuthScheme.class)
                       .endControlFlow();
            }
            builder.addStatement("$T authScheme = authSchemes.get(authOption.schemeId())", wildcardAuthScheme())
                   .addStatement("$T selectedAuthScheme = "
                                 + "trySelectAuthScheme(authOption, authScheme, identityResolvers, discardedReasons)",
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

    private MethodSpec generateTrySelectAuthScheme() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("trySelectAuthScheme")
                                               .addModifiers(Modifier.PRIVATE)
                                               .returns(namedSelectedAuthScheme())
                                               .addParameter(AuthSchemeOption.class,
                                                             "authOption")
                                               .addParameter(namedAuthScheme(),
                                                             "authScheme")
                                               .addParameter(IdentityProviderConfiguration.class,
                                                             "identityProviders")
                                               .addParameter(listOfStringSuppliers(),
                                                             "discardedReasons")
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
        builder.addStatement("return new $T<>(identityProvider, authScheme.signer(), authOption)", SelectedAuthScheme.class);
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
}
