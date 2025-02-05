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
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

public class EndpointBasedAuthSchemeProviderSpec implements ClassSpec {
    private final AuthSchemeSpecUtils authSchemeSpecUtils;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;
    private final SigV4AuthSchemeCodegenKnowledgeIndex sigV4AuthSchemeCodegenKnowledgeIndex;

    public EndpointBasedAuthSchemeProviderSpec(IntermediateModel intermediateModel) {
        this.authSchemeSpecUtils = new AuthSchemeSpecUtils(intermediateModel);
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
        this.sigV4AuthSchemeCodegenKnowledgeIndex = SigV4AuthSchemeCodegenKnowledgeIndex.of(intermediateModel);
    }

    @Override
    public ClassName className() {
        return authSchemeSpecUtils.defaultAuthSchemeProviderName();
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className())
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .addAnnotation(SdkInternalApi.class)
                                            .addSuperinterface(authSchemeSpecUtils.providerInterfaceName())
                                            .addMethod(constructor())
                                            .addField(defaultInstance())
                                            .addField(modeledResolverInstance())
                                            .addField(endpointDelegateInstance())
                                            .addMethod(createMethod())
                                            .addMethod(resolveAuthSchemeMethod())
                                            .addMethod(endpointProvider());

        boolean applyServiceDefaults = sigV4AuthSchemeCodegenKnowledgeIndex.hasSigV4Overrides();
        if (applyServiceDefaults) {
            builder.addMethod(addV4Defaults());
        }
        return builder.build();
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build();
    }

    private FieldSpec defaultInstance() {
        return FieldSpec.builder(className(), "DEFAULT")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T()", className())
                        .build();
    }

    private FieldSpec endpointDelegateInstance() {
        return FieldSpec.builder(endpointRulesSpecUtils.providerInterfaceName(), "DELEGATE")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.defaultProvider()", endpointRulesSpecUtils.providerInterfaceName())
                        .build();
    }

    private MethodSpec endpointProvider() {
        ClassName endpointProviderClass = endpointRulesSpecUtils.providerInterfaceName();
        MethodSpec.Builder builder = MethodSpec.methodBuilder("endpointProvider")
                                               .addModifiers(Modifier.PRIVATE)
                                               .returns(endpointProviderClass)
                                               .addParameter(authSchemeSpecUtils.parametersInterfaceName(), "params");

        ClassName endpointAwareParams = authSchemeSpecUtils.parametersEndpointAwareDefaultImplName();
        builder.beginControlFlow("if (params instanceof $T)", endpointAwareParams);
        builder.addStatement("$1T endpointAwareParams = ($1T) params", endpointAwareParams);
        builder.addStatement("$T endpointProvider = endpointAwareParams.endpointProvider()", endpointProviderClass);
        builder.beginControlFlow("if (endpointProvider != null)");
        builder.addStatement("return endpointProvider");
        builder.endControlFlow();
        builder.endControlFlow();
        builder.addStatement("return DELEGATE");
        return builder.build();
    }

    private FieldSpec modeledResolverInstance() {
        return FieldSpec.builder(authSchemeSpecUtils.providerInterfaceName(), "MODELED_RESOLVER")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.create()", authSchemeSpecUtils.modeledAuthSchemeProviderName())
                        .build();
    }

    private MethodSpec createMethod() {
        return MethodSpec.methodBuilder("create")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .returns(authSchemeSpecUtils.providerInterfaceName())
                         .addStatement("return DEFAULT")
                         .build();
    }

    private MethodSpec resolveAuthSchemeMethod() {
        MethodSpec.Builder spec = MethodSpec.methodBuilder("resolveAuthScheme")
                                            .addModifiers(Modifier.PUBLIC)
                                            .addAnnotation(Override.class)
                                            .returns(authSchemeSpecUtils.resolverReturnType())
                                            .addParameter(authSchemeSpecUtils.parametersInterfaceName(), "params");

        spec.addCode("$1T endpointParameters = $1T.builder()\n$>",
                     endpointRulesSpecUtils.parametersClassName());

        parameters().forEach((name, model) -> {
            if (authSchemeSpecUtils.includeParamForProvider(name)) {
                spec.addCode(".$1L(params.$1L())\n", endpointRulesSpecUtils.paramMethodName(name));
            }
        });
        spec.addStatement(".build()");
        spec.addStatement("$T endpoint = $T.joinLikeSync(endpointProvider(params).resolveEndpoint(endpointParameters))",
                          Endpoint.class, CompletableFutureUtils.class);
        spec.addStatement("$T authSchemes = endpoint.attribute($T.AUTH_SCHEMES)",
                          ParameterizedTypeName.get(List.class, EndpointAuthScheme.class), AwsEndpointAttribute.class);
        spec.beginControlFlow("if (authSchemes == null)");
        spec.addStatement("return MODELED_RESOLVER.resolveAuthScheme(params)");
        spec.endControlFlow();


        spec.addStatement("$T options = new $T<>()", ParameterizedTypeName.get(List.class, AuthSchemeOption.class),
                          TypeName.get(ArrayList.class));
        spec.beginControlFlow("for ($T authScheme : authSchemes)", EndpointAuthScheme.class);
        addAuthSchemeSwitch(spec);
        spec.endControlFlow();
        return spec.addStatement("return $T.unmodifiableList(options)", Collections.class)
                   .build();
    }

    private void addAuthSchemeSwitch(MethodSpec.Builder spec) {
        spec.addStatement("$T name = authScheme.name()", String.class);
        spec.beginControlFlow("switch(name)");
        addAuthSchemeSwitchSigV4Case(spec);
        addAuthSchemeSwitchSigV4aCase(spec);
        if (endpointRulesSpecUtils.useS3Express()) {
            addAuthSchemeSwitchS3ExpressCase(spec);
        }
        addAuthSchemeSwitchDefaultCase(spec);
        spec.endControlFlow();
    }

    private void addAuthSchemeSwitchSigV4Case(MethodSpec.Builder spec) {
        spec.addCode("case $S:", "sigv4");
        spec.addStatement("$T sigv4AuthScheme = $T.isInstanceOf($T.class, authScheme, $S, authScheme.getClass().getName())",
                          SigV4AuthScheme.class, Validate.class, SigV4AuthScheme.class,
                          "Expecting auth scheme of class SigV4AuthScheme, got instead object of class %s");

        CodeBlock.Builder block = CodeBlock.builder();
        block.add("$T.builder()", AuthSchemeOption.class)
             .add("\n.schemeId($T.SCHEME_ID)", AwsV4AuthScheme.class)
             .add("\n.putSignerProperty($T.SERVICE_SIGNING_NAME, sigv4AuthScheme.signingName())", AwsV4HttpSigner.class)
             .add("\n.putSignerProperty($T.REGION_NAME, sigv4AuthScheme.signingRegion())", AwsV4HttpSigner.class)
             .add("\n.putSignerProperty($T.DOUBLE_URL_ENCODE, !sigv4AuthScheme.disableDoubleEncoding())",
                  AwsV4HttpSigner.class);

        if (sigV4AuthSchemeCodegenKnowledgeIndex.hasSigV4Overrides()) {
            spec.addCode("$1T sigv4AuthSchemeOption = applySigV4FamilyDefaults(", AuthSchemeOption.class)
                .addCode(block.build())
                .addCode(", params)")
                .addStatement(".build()");
        } else {
            spec.addCode("$1T sigv4AuthSchemeOption = ", AuthSchemeOption.class)
                .addCode(block.build())
                .addStatement(".build()");
        }
        spec.addStatement("options.add(sigv4AuthSchemeOption)");
        spec.addStatement("break");
    }

    private void addAuthSchemeSwitchSigV4aCase(MethodSpec.Builder spec) {
        spec.addCode("case $S:", "sigv4a");

        spec.addStatement("$T sigv4aAuthScheme = $T.isInstanceOf($T.class, authScheme, $S, authScheme.getClass().getName())",
                          SigV4aAuthScheme.class, Validate.class, SigV4aAuthScheme.class,
                          "Expecting auth scheme of class SigV4AuthScheme, got instead object of class %s");

        spec.addStatement("$1T regionSet = $2T.isNullOrEmpty(sigv4aAuthScheme.signingRegionSet()) ? null : $1T.create"
                          + "(sigv4aAuthScheme.signingRegionSet())", RegionSet.class, CollectionUtils.class);

        CodeBlock.Builder block = CodeBlock.builder();
        block.add("$1T.builder().schemeId($2T.SCHEME_ID)", AuthSchemeOption.class,
                  AwsV4aAuthScheme.class)
             .add("\n.putSignerProperty($T.SERVICE_SIGNING_NAME, sigv4aAuthScheme.signingName())", AwsV4HttpSigner.class)
             .add("\n.putSignerProperty($T.REGION_SET, regionSet)", AwsV4aHttpSigner.class)
             .add("\n.putSignerProperty($T.DOUBLE_URL_ENCODE, !sigv4aAuthScheme.disableDoubleEncoding())", AwsV4HttpSigner.class);

        if (sigV4AuthSchemeCodegenKnowledgeIndex.hasSigV4Overrides()) {
            spec.addCode("$1T sigv4aAuthSchemeOption = applySigV4FamilyDefaults(", AuthSchemeOption.class)
                .addCode(block.build())
                .addCode(", params)")
                .addStatement(".build()");

        } else {
            spec.addCode("$1T sigv4aAuthSchemeOption = ", AuthSchemeOption.class)
                .addCode(block.build())
                .addStatement(".build()");
        }
        spec.addStatement("options.add(sigv4aAuthSchemeOption)");
        spec.addStatement("break");
    }

    private void addAuthSchemeSwitchS3ExpressCase(MethodSpec.Builder spec) {
        spec.addCode("case $S:", "sigv4-s3express");
        ClassName s3ExpressEndpointAuthScheme = ClassName.get(
            authSchemeSpecUtils.baseClientPackageName() + ".endpoints.authscheme",
            "S3ExpressEndpointAuthScheme");

        spec.addStatement("$T s3ExpressAuthScheme = $T.isInstanceOf($T.class, authScheme, $S, authScheme.getClass().getName())",
                          s3ExpressEndpointAuthScheme, Validate.class, s3ExpressEndpointAuthScheme,
                          "Expecting auth scheme of class S3ExpressAuthScheme, got instead object of class %s");

        ClassName s3ExpressAuthScheme = ClassName.get(authSchemeSpecUtils.baseClientPackageName() + ".s3express",
                                                      "S3ExpressAuthScheme");

        CodeBlock.Builder block = CodeBlock.builder();
        block.add("$1T.builder().schemeId($2T.SCHEME_ID)", AuthSchemeOption.class, s3ExpressAuthScheme)
             .add("\n.putSignerProperty($T.SERVICE_SIGNING_NAME, s3ExpressAuthScheme.signingName())", AwsV4HttpSigner.class)
             .add("\n.putSignerProperty($T.REGION_NAME, s3ExpressAuthScheme.signingRegion())", AwsV4HttpSigner.class)
             .add("\n.putSignerProperty($T.DOUBLE_URL_ENCODE, !s3ExpressAuthScheme.disableDoubleEncoding())",
                  AwsV4HttpSigner.class);

        if (sigV4AuthSchemeCodegenKnowledgeIndex.hasSigV4Overrides()) {
            spec.addCode("$1T s3ExpressAuthSchemeOption = applySigV4FamilyDefaults(", AuthSchemeOption.class)
                .addCode(block.build())
                .addCode(", params)")
                .addStatement(".build()");
        } else {
            spec.addCode("$1T s3ExpressAuthSchemeOption = ", AuthSchemeOption.class)
                .addCode(block.build())
                .addStatement(".build()");
        }
        spec.addStatement("options.add(s3ExpressAuthSchemeOption)");
        spec.addStatement("break");

    }

    private void addAuthSchemeSwitchDefaultCase(MethodSpec.Builder spec) {
        spec.addCode("default:");
        spec.addStatement("throw new $T($S + name)", IllegalArgumentException.class, "Unknown auth scheme: ");
    }


    private MethodSpec addV4Defaults() {
        MethodSpec.Builder spec = MethodSpec.methodBuilder("applySigV4FamilyDefaults")
                                            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                            .returns(AuthSchemeOption.Builder.class)
                                            .addParameter(AuthSchemeOption.Builder.class, "option")
                                            .addParameter(authSchemeSpecUtils.parametersInterfaceName(), "params");

        // All the operations share the same set of auth schemes, no need to create a switch statement.
        if (!sigV4AuthSchemeCodegenKnowledgeIndex.hasPerOperationSigV4Overrides()) {
            AuthSchemeCodegenMetadata authType = sigV4AuthSchemeCodegenKnowledgeIndex.serviceSigV4Overrides();
            addAuthTypeProperties(spec, authType);
            return spec.build();
        }
        spec.beginControlFlow("switch(params.operation())");
        sigV4AuthSchemeCodegenKnowledgeIndex.forEachOperationsOverridesGroup((ops, scheme) -> {
            if (!ops.isEmpty()) {
                addCasesForOperations(spec, ops, scheme);
            }
        });
        AuthSchemeCodegenMetadata authType = sigV4AuthSchemeCodegenKnowledgeIndex.serviceSigV4Overrides();
        if (authType != null) {
            addCasesForDefault(spec, authType);
        }
        spec.endControlFlow();
        return spec.build();
    }

    private void addCasesForOperations(MethodSpec.Builder spec, List<String> operations,
                                       AuthSchemeCodegenMetadata metadata) {
        for (String name : operations) {
            spec.addCode("case $S:\n", name);
        }
        addAuthTypeProperties(spec, metadata);
    }

    private void addCasesForDefault(MethodSpec.Builder spec,
                                    AuthSchemeCodegenMetadata metadata) {
        spec.addCode("default:\n");
        addAuthTypeProperties(spec, metadata);
    }

    private void addAuthTypeProperties(MethodSpec.Builder spec, AuthSchemeCodegenMetadata metadata) {
        spec.addCode("option");
        spec.addCode(AuthSchemeCodegenMetadataExt.codegenSignerPropertiesIfAbsent(authSchemeSpecUtils, metadata.properties()));
        spec.addStatement("");
        spec.addStatement("return option");
    }

    private Map<String, ParameterModel> parameters() {
        return endpointRulesSpecUtils.parameters();
    }
}
