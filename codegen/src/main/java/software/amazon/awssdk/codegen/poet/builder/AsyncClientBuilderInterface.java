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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.utils.StringUtils;

public class AsyncClientBuilderInterface implements ClassSpec {
    static Logger log = LoggerFactory.getLogger(AsyncClientBuilderInterface.class);
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

        boolean multipartEnabled = StringUtils.isNotBlank(model.getCustomizationConfig().getMultipartConfigurationClass());
        if (multipartEnabled) {
            includeMultipartMethod(builder);
        }
        return builder.build();
    }

    private void includeMultipartMethod(TypeSpec.Builder builder) {
        log.debug("Adding multipart config methods to builder interface for service '{}'", model.getMetadata().getServiceId());
        String multipartConfigClass = model.getCustomizationConfig().getMultipartConfigurationClass();
        String multipartMethodJavaDoc = model.getCustomizationConfig().getMultipartMethodDoc();
        ClassName mulitpartConfigClassName = PoetUtils.classNameFromFqcn(multipartConfigClass);
        String multiPartConfigMethodName = "multipartConfiguration";
        builder.addMethod(MethodSpec.methodBuilder(multiPartConfigMethodName)
                                    .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                                    .returns(builderInterfaceName)
                                    .addParameter(ParameterSpec.builder(mulitpartConfigClassName, "multipartConfiguration").build())
                                    .addCode("throw new $T();", UnsupportedOperationException.class)
                                    .addJavadoc(CodeBlock.of(multipartMethodJavaDoc))
                                    .build());

        ClassName mulitpartConfigBuilderClassName = PoetUtils.classNameFromFqcn(multipartConfigClass + ".Builder");

        ParameterizedTypeName consumerBuilderType = ParameterizedTypeName.get(ClassName.get(Consumer.class),
                                                                              mulitpartConfigBuilderClassName);
        builder.addMethod(MethodSpec.methodBuilder(multiPartConfigMethodName)
                                    .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                                    .returns(builderInterfaceName)
                                    .addParameter(ParameterSpec.builder(consumerBuilderType, "multipartConfiguration").build())
                                    .addStatement("$T builder = $T.builder()",
                                                  mulitpartConfigBuilderClassName,
                                                  mulitpartConfigClassName)
                                    .addStatement("multipartConfiguration.accept(builder)")
                                    .addStatement("return multipartConfiguration(builder.build())")
                                    .addJavadoc(CodeBlock.of(multipartMethodJavaDoc))
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
