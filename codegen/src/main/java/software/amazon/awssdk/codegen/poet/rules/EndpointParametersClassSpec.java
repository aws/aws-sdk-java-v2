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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.Map;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

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
                                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        parameters().forEach((name, model) -> {
            b.addField(fieldSpec(name, model).toBuilder().addModifiers(Modifier.FINAL).build());
            b.addMethod(accessorMethod(name, model));
        });

        return b.build();
    }

    @Override
    public ClassName className() {
        return endpointRulesSpecUtils.parametersClassName();
    }

    private TypeSpec builderInterfaceSpec() {
        TypeSpec.Builder b = TypeSpec.interfaceBuilder(builderInterfaceName())
                                         .addModifiers(Modifier.PUBLIC);

        parameters().forEach((name, model) -> {
            b.addMethod(setterMethodDeclaration(name, model));
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

        parameters().forEach((name, model) -> {
            b.addField(fieldSpec(name, model));
            b.addMethod(builderSetterMethod(name, model));
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

    private ParameterSpec parameterSpec(String name, ParameterModel model) {
        return ParameterSpec.builder(endpointRulesSpecUtils.parameterType(model), name).build();
    }

    private FieldSpec fieldSpec(String name, ParameterModel model) {
        return FieldSpec.builder(endpointRulesSpecUtils.parameterType(model), name)
                        .addModifiers(Modifier.PRIVATE)
                        .build();
    }

    private MethodSpec setterMethodDeclaration(String name, ParameterModel model) {
        return MethodSpec.methodBuilder(paramMethodName(name))
                         .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                         .addParameter(parameterSpec(name, model))
                         .returns(builderInterfaceName())
                         .build();
    }

    private MethodSpec accessorMethod(String name, ParameterModel model) {
        return MethodSpec.methodBuilder(paramMethodName(name))
                         .returns(endpointRulesSpecUtils.parameterType(model))
                         .addModifiers(Modifier.PUBLIC)
                         .addStatement("return $N", name)
                         .build();
    }

    private MethodSpec builderSetterMethod(String name, ParameterModel model) {
        return MethodSpec.methodBuilder(paramMethodName(name))
                         .addParameter(parameterSpec(name, model))
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(builderInterfaceName())
                         .addCode(CodeBlock.builder()
                                           .addStatement("this.$1N = $1N", name)
                                           .addStatement("return this")
                                           .build())
                         .build();
    }

    private MethodSpec ctor() {
        MethodSpec.Builder b = MethodSpec.constructorBuilder()
                                         .addModifiers(Modifier.PRIVATE)
                                         .addParameter(builderClassName(), "builder");

        parameters().forEach((name, model) -> {
            b.addStatement("this.$1N = builder.$1N", name);
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

    private String paramMethodName(String name) {
        return Utils.unCapitalize(CodegenNamingUtils.pascalCase(name));
    }

}
