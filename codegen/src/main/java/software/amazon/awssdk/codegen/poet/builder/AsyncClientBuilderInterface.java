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
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.codegen.model.config.customization.MultipartCustomization;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

public class AsyncClientBuilderInterface implements ClassSpec {
    private static final Logger log = Logger.loggerFor(AsyncClientBuilderInterface.class);

    private final ClassName builderInterfaceName;
    private final ClassName clientInterfaceName;
    private final ClassName baseBuilderInterfaceName;
    private final IntermediateModel model;

    public AsyncClientBuilderInterface(IntermediateModel model) {
        String basePackage = model.getMetadata().getFullClientPackageName();
        this.clientInterfaceName = ClassName.get(basePackage, model.getMetadata().getAsyncInterface());
        this.builderInterfaceName = ClassName.get(basePackage, model.getMetadata().getAsyncBuilderInterface());
        this.baseBuilderInterfaceName = ClassName.get(basePackage, model.getMetadata().getBaseBuilderInterface());
        this.model = model;
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils
            .createInterfaceBuilder(builderInterfaceName)
            .addSuperinterface(ParameterizedTypeName.get(ClassName.get(AwsAsyncClientBuilder.class),
                                                         builderInterfaceName, clientInterfaceName))
            .addSuperinterface(ParameterizedTypeName.get(baseBuilderInterfaceName,
                                                         builderInterfaceName, clientInterfaceName))
            .addJavadoc(getJavadoc());

        MultipartCustomization multipartCustomization = model.getCustomizationConfig().getMultipartCustomization();
        if (multipartCustomization != null) {
            includeMultipartMethod(builder, multipartCustomization);
        }
        return builder.build();
    }

    private void includeMultipartMethod(TypeSpec.Builder builder, MultipartCustomization multipartCustomization) {
        log.debug(() -> String.format("Adding multipart config methods to builder interface for service '%s'",
                  model.getMetadata().getServiceId()));

        // .multipartEnabled(Boolean)
        builder.addMethod(
            MethodSpec.methodBuilder("multipartEnabled")
                      .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                      .returns(builderInterfaceName)
                      .addParameter(Boolean.class, "enabled")
                      .addCode("throw new $T();", UnsupportedOperationException.class)
                      .addJavadoc(CodeBlock.of(multipartCustomization.getMultipartEnableMethodDoc()))
                      .build());

        // .multipartConfiguration(MultipartConfiguration)
        String multiPartConfigMethodName = "multipartConfiguration";
        String multipartConfigClass = Validate.notNull(multipartCustomization.getMultipartConfigurationClass(),
                                                       "'multipartConfigurationClass' must be defined");
        ClassName mulitpartConfigClassName = PoetUtils.classNameFromFqcn(multipartConfigClass);
        builder.addMethod(
            MethodSpec.methodBuilder(multiPartConfigMethodName)
                      .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                      .returns(builderInterfaceName)
                      .addParameter(ParameterSpec.builder(mulitpartConfigClassName, "multipartConfiguration").build())
                      .addCode("throw new $T();", UnsupportedOperationException.class)
                      .addJavadoc(CodeBlock.of(multipartCustomization.getMultipartConfigMethodDoc()))
                      .build());

        // .multipartConfiguration(Consumer<MultipartConfiguration>)
        ClassName mulitpartConfigBuilderClassName = PoetUtils.classNameFromFqcn(multipartConfigClass + ".Builder");
        ParameterizedTypeName consumerBuilderType = ParameterizedTypeName.get(ClassName.get(Consumer.class),
                                                                              mulitpartConfigBuilderClassName);
        builder.addMethod(
            MethodSpec.methodBuilder(multiPartConfigMethodName)
                      .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                      .returns(builderInterfaceName)
                      .addParameter(ParameterSpec.builder(consumerBuilderType, "multipartConfiguration").build())
                      .addStatement("$T builder = $T.builder()",
                                    mulitpartConfigBuilderClassName,
                                    mulitpartConfigClassName)
                      .addStatement("multipartConfiguration.accept(builder)")
                      .addStatement("return multipartConfiguration(builder.build())")
                      .addJavadoc(CodeBlock.of(multipartCustomization.getMultipartConfigMethodDoc()))
                      .build());
    }

    @Override
    public ClassName className() {
        return builderInterfaceName;
    }

    private CodeBlock getJavadoc() {
        return CodeBlock.of("A builder for creating an instance of {@link $1T}. This can be created with the static "
                            + "{@link $1T#builder()} method.", clientInterfaceName);
    }
}
