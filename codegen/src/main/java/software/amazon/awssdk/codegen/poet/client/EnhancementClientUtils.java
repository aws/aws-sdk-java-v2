/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet.client;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.config.customization.EnhancementClientConfig;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Utils to generate the custom methods in the low-level client that delegate to a
 * enhanced (hand-written) client.
 */
final class EnhancementClientUtils {
    static final String ENHANCEMENT_CLIENT = "enhancementClient";

    private EnhancementClientUtils() {
    }

    /**
     * @return Field spec to declare the enhancement client
     */
    static FieldSpec classFieldSpec(String interfaceName) {
        return FieldSpec.builder(PoetUtils.classNameFromFqcn(interfaceName), ENHANCEMENT_CLIENT, PRIVATE, FINAL)
                        .build();
    }

    /**
     * Initializes the enhancement client in the given {@link MethodSpec}.
     */
    static void initializeClientMember(MethodSpec.Builder builder, String classImplName, String protocolFactory) {
        builder.addStatement("this.$1N = $2T.builder().protocolFactory($3N)"
                             + ".sdkClientConfiguration(clientConfiguration).build()",
                             ENHANCEMENT_CLIENT,
                             PoetUtils.classNameFromFqcn(classImplName),
                             protocolFactory);
    }

    /**
     * Returns the list of {@link MethodSpec}s to generate the custom operations in sync client.
     * All these operations delegate to the respective method in the enhancement client.
     */
    static List<MethodSpec> syncEnhancementMethods(EnhancementClientConfig enhancementClientConfig) {
        return enhancementMethods(enhancementClientConfig, returnType -> PoetUtils.classNameFromFqcn(returnType));
    }

    /**
     * Returns the list of {@link MethodSpec}s to generate the custom operations in async client.
     * All these operations delegate to the respective method in the enhancement client.
     */
    static List<MethodSpec> asyncEnhancementMethods(EnhancementClientConfig enhancementClientConfig) {
        return enhancementMethods(enhancementClientConfig, returnType -> ParameterizedTypeName.get(
            ClassName.get(CompletableFuture.class), PoetUtils.classNameFromFqcn(returnType)));
    }

    private static List<MethodSpec> enhancementMethods(EnhancementClientConfig enhancementClientConfig,
                                                       Function<String, TypeName> methodReturnType) {
        if (enhancementClientConfig == null || enhancementClientConfig.getEnhancementMethods() == null) {
            return new ArrayList<>();
        }

        return enhancementClientConfig.getEnhancementMethods()
                                      .stream()
                                      .map(config -> {
                                          MethodSpec.Builder builder = MethodSpec.methodBuilder(config.getName())
                                                                                 .addAnnotation(Override.class)
                                                                                 .addModifiers(Modifier.PUBLIC)
                                                                                 .returns(methodReturnType.apply(
                                                                                     config.getReturnType()));

                                          builder.addExceptions(config.getExceptions().stream()
                                                                      .map(e -> PoetUtils.classNameFromFqcn(e))
                                                                      .collect(Collectors.toList()));

                                          config.getParameters()
                                                .forEach(p -> builder.addParameter(PoetUtils.classNameFromFqcn(p.getType()),
                                                                                   p.getName()));

                                          builder.addStatement("return $1L.$2L($3L)",
                                                               ENHANCEMENT_CLIENT,
                                                               StringUtils.uncapitalize(config.getName()),
                                                               config.getParameters().stream()
                                                                     .map(p -> p.getName())
                                                                     .collect(Collectors.joining(",")));

                                          return builder.build();
                                      })
                                      .collect(Collectors.toList());
    }
}
