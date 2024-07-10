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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Map;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

public class EndpointParametersClassSpec implements ClassSpec {
    private final IntermediateModel intermediateModel;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public EndpointParametersClassSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder b = PoetUtils.createClassBuilder(className())
                                      .addJavadoc("The parameters object used to resolve an endpoint for the $L service.",
                                                  intermediateModel.getMetadata().getServiceName())
                                      .addMethod(ctor())
                                      .addMethod(builderMethod())
                                      .addType(builderInterfaceSpec())
                                      .addType(builderImplSpec())
                                      .addAnnotation(SdkPublicApi.class)
                                      .addSuperinterface(toCopyableBuilderInterface())
                                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        parameters().forEach((name, model) -> {
            b.addField(endpointRulesSpecUtils.parameterClassField(name, model));
            b.addMethods(endpointRulesSpecUtils.parameterClassAccessorMethods(name, model));
        });

        b.addMethod(toBuilderMethod());

        return b.build();
    }

    @Override
    public ClassName className() {
        return endpointRulesSpecUtils.parametersClassName();
    }

    private TypeSpec builderInterfaceSpec() {
        TypeSpec.Builder b = TypeSpec.interfaceBuilder(builderInterfaceName())
            .addSuperinterface(copyableBuilderExtendsInterface())
                                         .addModifiers(Modifier.PUBLIC);

        parameters().forEach((name, model) -> {
            b.addMethod(endpointRulesSpecUtils.parameterBuilderSetterMethodDeclaration(className(), name, model));
        });

        b.addMethod(MethodSpec.methodBuilder("build")
                              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                              .returns(className())
                              .build());

        return b.build();
    }

    private TypeSpec builderImplSpec() {
        TypeSpec.Builder b = TypeSpec.classBuilder(builderClassName())
                                     .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                     .addSuperinterface(builderInterfaceName());

        b.addMethod(MethodSpec.constructorBuilder()
                              .addModifiers(Modifier.PRIVATE)
                              .build());
        b.addMethod(toBuilderConstructor().build());

        parameters().forEach((name, model) -> {
            b.addField(endpointRulesSpecUtils.parameterBuilderFieldSpec(name, model));
            b.addMethod(endpointRulesSpecUtils.parameterBuilderSetterMethod(className(), name, model));
        });

        b.addMethod(MethodSpec.methodBuilder("build")
                              .addModifiers(Modifier.PUBLIC)
                              .addAnnotation(Override.class)
                              .returns(className())
                              .addCode(CodeBlock.builder()
                                                .addStatement("return new $T(this)", className())
                                                .build())
                              .build());

        return b.build();
    }

    private ClassName builderInterfaceName() {
        return className().nestedClass("Builder");
    }

    private ClassName builderClassName() {
        return className().nestedClass("BuilderImpl");
    }

    private Map<String, ParameterModel> parameters() {
        return intermediateModel.getEndpointRuleSetModel().getParameters();
    }

    private MethodSpec ctor() {
        MethodSpec.Builder b = MethodSpec.constructorBuilder()
                                         .addModifiers(Modifier.PRIVATE)
                                         .addParameter(builderClassName(), "builder");

        parameters().forEach((name, model) -> {
            b.addStatement("this.$1N = builder.$1N", variableName(name));
        });

        return b.build();
    }

    private MethodSpec builderMethod() {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .returns(builderInterfaceName())
                         .addStatement("return new $T()", builderClassName())
                         .build();
    }

    private MethodSpec toBuilderMethod() {
        return MethodSpec.methodBuilder("toBuilder")
                         .addModifiers(Modifier.PUBLIC)
                         .returns(builderInterfaceName())
                         .addStatement("return new $T(this)", builderClassName())
                         .build();
    }

    private String variableName(String name) {
        return intermediateModel.getNamingStrategy().getVariableName(name);
    }

    private MethodSpec.Builder toBuilderConstructor() {
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
        constructorBuilder.addModifiers(Modifier.PRIVATE);
        constructorBuilder.addParameter(className(), "builder");
        parameters().forEach((name, model) -> {
            constructorBuilder.addStatement("this.$1N = builder.$1N", variableName(name));
        });
        return constructorBuilder;
    }

    private TypeName toCopyableBuilderInterface() {
        return ParameterizedTypeName.get(ClassName.get(ToCopyableBuilder.class),
                                         className().nestedClass(builderInterfaceName().simpleName()),
                                         className());
    }

    private TypeName copyableBuilderExtendsInterface() {
        return ParameterizedTypeName.get(ClassName.get(CopyableBuilder.class),
                                         builderInterfaceName(), className());
    }
}
