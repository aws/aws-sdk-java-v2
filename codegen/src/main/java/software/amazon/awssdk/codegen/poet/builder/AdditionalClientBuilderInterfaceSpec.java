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

package software.amazon.awssdk.codegen.poet.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.AdditionalClientBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.SdkBuilder;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

/**
 * Spec for generating additional client builders for a service client. Works in conjunction with
 * {@link CustomizationConfig#getAdditionalClientBuilders()}.
 */
public final class AdditionalClientBuilderInterfaceSpec implements ClassSpec {
    private final IntermediateModel model;
    private final String name;
    private final AdditionalClientBuilder clientBuilderModel;
    private final PoetExtension poetExt;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public AdditionalClientBuilderInterfaceSpec(IntermediateModel model, String name,
                                                AdditionalClientBuilder clientBuilderModel) {
        this.model = model;
        this.name = name;
        this.clientBuilderModel = clientBuilderModel;
        this.poetExt = new PoetExtension(model);
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createInterfaceBuilder(className());

        builder.addAnnotation(SdkPublicApi.class);
        ParameterizedTypeName superInterface = ParameterizedTypeName.get(
            ClassName.get(SdkBuilder.class), className(), classToBuild());
        builder.addSuperinterface(superInterface);

        propertySetters().forEach(builder::addMethod);
        clientContextParamSetterMethods().forEach(builder::addMethod);
        builder.addMethod(buildMethod());

        return builder.build();
    }

    @Override
    public ClassName className() {
        return poetExt.getClientClass(name);
    }

    private ClassName classToBuild() {
        switch (clientBuilderModel.getClientType()) {
            case ASYNC:
                return poetExt.getClientClass(model.getMetadata().getAsyncInterface());
            case SYNC:
                return poetExt.getClientClass(model.getMetadata().getSyncInterface());
            default:
                throw new RuntimeException("Unknown client type: " + clientBuilderModel.getClientType());
        }
    }

    private List<MethodSpec> propertySetters() {
        return clientBuilderModel.getProperties()
            .entrySet()
            .stream()
            .flatMap(p -> propertySetterMethods(CodegenNamingUtils.pascalCase(p.getKey()), p.getValue()).stream())
            .collect(Collectors.toList());
    }

    private List<MethodSpec> propertySetterMethods(String name, AdditionalClientBuilder.BuilderProperty property) {
        name = Utils.unCapitalize(CodegenNamingUtils.pascalCase(name));
        ClassName type = PoetUtils.classNameFromFqcn(property.getClassFqcn());

        List<MethodSpec> specs = new ArrayList<>();

        specs.add(MethodSpec.methodBuilder(name)
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addParameter(type, name)
                            .addJavadoc(property.getJavadoc())
                            .returns(className())
                            .build());

        if (property.isGenerateConsumerBuilder()) {
            ClassName propertyBuilder = type.nestedClass("Builder");
            String builderName = name + "Builder";
            ParameterizedTypeName consumer = ParameterizedTypeName.get(ClassName.get(Consumer.class), propertyBuilder);
            specs.add(MethodSpec.methodBuilder(name)
                          .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                          .addParameter(consumer, builderName)
                          .addStatement("$1T.paramNotNull($2N, $2S)", Validate.class, builderName)
                          .addStatement("return $N($T.builder().applyMutation($N).build())", name, type, builderName)
                          .returns(className())
                          .build());
        }

        return specs;
    }

    private List<MethodSpec> clientContextParamSetterMethods() {
        return model.getClientContextParams()
                    .entrySet()
                    .stream()
                    .map(e -> endpointRulesSpecUtils.clientContextParamSetterMethodDeclaration(e.getKey(), e.getValue(),
                                                                                               className()))
                    .collect(Collectors.toList());
    }

    private MethodSpec buildMethod() {
        return MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addAnnotation(Override.class)
            .returns(classToBuild())
            .build();
    }
}
