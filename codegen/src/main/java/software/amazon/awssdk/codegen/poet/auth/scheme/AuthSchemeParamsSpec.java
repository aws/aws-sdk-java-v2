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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

public final class AuthSchemeParamsSpec implements ClassSpec {
    private final IntermediateModel intermediateModel;
    private final AuthSchemeSpecUtils authSchemeSpecUtils;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public AuthSchemeParamsSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.authSchemeSpecUtils = new AuthSchemeSpecUtils(intermediateModel);
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
    }

    @Override
    public ClassName className() {
        return authSchemeSpecUtils.parametersInterfaceName();
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder b = PoetUtils.createInterfaceBuilder(className())
                                      .addSuperinterface(toCopyableBuilderInterface())
                                      .addModifiers(Modifier.PUBLIC)
                                      .addAnnotation(SdkPublicApi.class)
                                      .addJavadoc(interfaceJavadoc())
                                      .addMethod(builderMethod())
                                      .addType(builderInterfaceSpec());

        addAccessorMethods(b);
        addToBuilder(b);
        return b.build();
    }

    private CodeBlock interfaceJavadoc() {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("The parameters object used to resolve the auth schemes for the $N service.",
              intermediateModel.getMetadata().getServiceName());

        return b.build();
    }

    private MethodSpec builderMethod() {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .returns(authSchemeSpecUtils.parametersInterfaceBuilderInterfaceName())
                         .addStatement("return $T.builder()", authSchemeSpecUtils.parametersDefaultImplName())
                         .addJavadoc("Get a new builder for creating a {@link $T}.",
                                     authSchemeSpecUtils.parametersInterfaceName())
                         .build();
    }

    private TypeSpec builderInterfaceSpec() {
        TypeSpec.Builder b = TypeSpec.interfaceBuilder(authSchemeSpecUtils.parametersInterfaceBuilderInterfaceName())
                                     .addSuperinterface(copyableBuilderInterface())
                                     .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                     .addJavadoc("A builder for a {@link $T}.", authSchemeSpecUtils.parametersInterfaceName());

        addBuilderSetterMethods(b);

        b.addMethod(MethodSpec.methodBuilder("build")
                              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                              .returns(className())
                              .addJavadoc("Returns a {@link $T} object that is created from the properties that have been set "
                                          + "on the builder.", authSchemeSpecUtils.parametersInterfaceName())
                              .build());

        return b.build();
    }

    private void addAccessorMethods(TypeSpec.Builder b) {
        b.addMethod(MethodSpec.methodBuilder("operation")
                              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                              .returns(String.class)
                              .addJavadoc("Returns the operation for which to resolve the auth scheme.")
                              .build());

        if (authSchemeSpecUtils.usesSigV4()) {
            b.addMethod(MethodSpec.methodBuilder("region")
                                  .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                  .returns(Region.class)
                                  .addJavadoc("Returns the region. The region parameter may be used with the $S auth scheme.",
                                              AwsV4AuthScheme.SCHEME_ID)
                                  .build());

        }

        if (authSchemeSpecUtils.generateEndpointBasedParams()) {
            parameters().forEach((name, model) -> {
                if (authSchemeSpecUtils.includeParam(name)) {
                    List<MethodSpec> methods = endpointRulesSpecUtils.parameterInterfaceAccessorMethods(name, model);
                    if (model.getDocumentation() != null) {
                        methods = methods.stream()
                                         .map(m -> m.toBuilder()
                                                    .addJavadoc(model.getDocumentation())
                                                    .build())
                                         .collect(Collectors.toList());
                    }
                    b.addMethods(methods);
                }
            });
        }
    }

    private void addToBuilder(TypeSpec.Builder b) {
        ClassName builderClassName = authSchemeSpecUtils.parametersInterfaceBuilderInterfaceName();
        b.addMethod(MethodSpec.methodBuilder("toBuilder")
                              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                              .returns(builderClassName)
                              .addJavadoc("Returns a {@link $T} to customize the parameters.", builderClassName)
                              .build());

    }

    private void addBuilderSetterMethods(TypeSpec.Builder b) {
        b.addMethod(MethodSpec.methodBuilder("operation")
                              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                              .addParameter(ParameterSpec.builder(String.class, "operation").build())
                              .returns(authSchemeSpecUtils.parametersInterfaceBuilderInterfaceName())
                              .addJavadoc("Set the operation for which to resolve the auth scheme.")
                              .build());

        if (authSchemeSpecUtils.usesSigV4()) {
            b.addMethod(MethodSpec.methodBuilder("region")
                                  .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                  .addParameter(ParameterSpec.builder(Region.class, "region").build())
                                  .returns(authSchemeSpecUtils.parametersInterfaceBuilderInterfaceName())
                                  .addJavadoc("Set the region. The region parameter may be used with the $S auth scheme.",
                                              AwsV4AuthScheme.SCHEME_ID)
                                  .build());

        }

        if (authSchemeSpecUtils.generateEndpointBasedParams()) {
            parameters().forEach((name, model) -> {
                if (authSchemeSpecUtils.includeParam(name)) {
                    ClassName parametersInterfaceName = authSchemeSpecUtils.parametersInterfaceName();
                    MethodSpec setter = endpointRulesSpecUtils
                        .parameterBuilderSetterMethodDeclaration(parametersInterfaceName, name, model);
                    if (model.getDocumentation() != null) {
                        setter = setter.toBuilder().addJavadoc(model.getDocumentation()).build();
                    }
                    b.addMethod(setter);
                }
            });
        }
    }

    private Map<String, ParameterModel> parameters() {
        return intermediateModel.getEndpointRuleSetModel().getParameters();
    }

    private TypeName toCopyableBuilderInterface() {
        return ParameterizedTypeName.get(ClassName.get(ToCopyableBuilder.class),
                                         className().nestedClass("Builder"),
                                         className());
    }

    private TypeName copyableBuilderInterface() {
        return ParameterizedTypeName.get(ClassName.get(CopyableBuilder.class),
                                         className().nestedClass("Builder"),
                                         className());
    }
}
