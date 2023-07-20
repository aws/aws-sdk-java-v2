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

        boolean multipartEnabled = model.getCustomizationConfig().getServiceConfig().hasMultipartEnabledProperty();
        if (multipartEnabled) {
            includeMultipartMethod(builder);
        }
        return builder.build();
    }

    private void includeMultipartMethod(TypeSpec.Builder builder) {
        log.info("!!!!!!!!!!!!! Adding multipart config methods to builder interface for service '{}'",
                  model.getMetadata().getServiceId());
        ClassName mulitpartConfigClassName =
            PoetUtils.classNameFromFqcn(model.getCustomizationConfig().getMultipartConfigurationClass());
        builder.addMethod(MethodSpec.methodBuilder("multipartConfiguration")
                                    .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                                    .returns(builderInterfaceName)
                                    .addParameter(ParameterSpec.builder(mulitpartConfigClassName, "multipartConfig").build())
                                    .addComment("by default, do nothing.")
                                    .addComment("Subclasses can override this method and implement the required logic.")
                                    .addCode("return this;")
                                    .build());

        ParameterizedTypeName consumerBuilder = ParameterizedTypeName.get(ClassName.get(Consumer.class),
                                                                          mulitpartConfigClassName);
        builder.addMethod(MethodSpec.methodBuilder("multipartConfiguration")
                                    .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                                    .returns(builderInterfaceName)
                                    .addParameter(ParameterSpec.builder(consumerBuilder, "multipartConfiguration").build())
                                    .addComment("by default, do nothing.")
                                    .addComment("Subclasses can override this method and implement the required logic.")
                                    .addCode("return this;")
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
