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

package software.amazon.awssdk.codegen.poet.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

/**
 * Generate ResponseMetadata class
 */
public class ResponseMetadataSpec implements ClassSpec {
    private PoetExtensions poetExtensions;
    private Map<String, String> headerMetadata = new HashMap<>();

    public ResponseMetadataSpec(IntermediateModel model) {
        if (!CollectionUtils.isNullOrEmpty(model.getCustomizationConfig().getCustomResponseMetadata())) {
            this.headerMetadata.putAll(model.getCustomizationConfig().getCustomResponseMetadata());
        }
        this.poetExtensions = new PoetExtensions(model);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder specBuilder = TypeSpec.classBuilder(className())
                                               .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                               .addAnnotation(PoetUtils.generatedAnnotation())
                                               .addAnnotation(SdkPublicApi.class)
                                               .superclass(AwsResponseMetadata.class)
                                               .addMethod(constructor())
                                               .addMethod(staticFactoryMethod())
                                               .addMethods(metadataMethods());

        List<FieldSpec> fields = headerMetadata.entrySet().stream().map(e ->
                                                                         FieldSpec.builder(String.class, e.getKey())
                                                                                  .addModifiers(Modifier.PRIVATE, Modifier.STATIC,
                                                                                                Modifier.FINAL)
                                                                                  .initializer("$S", e.getValue())
                                                                                  .build()
        ).collect(Collectors.toList());

        specBuilder.addFields(fields);
        return specBuilder.build();
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PRIVATE)
                         .addParameter(AwsResponseMetadata.class, "responseMetadata")
                         .addStatement("super(responseMetadata)")
                         .build();
    }

    private MethodSpec staticFactoryMethod() {
        return MethodSpec.methodBuilder("create")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .addParameter(AwsResponseMetadata.class, "responseMetadata")
                         .addStatement("return new $T(responseMetadata)", className())
                         .returns(className())
                         .build();
    }

    private List<MethodSpec> metadataMethods() {
        return headerMetadata.keySet()
                             .stream()
                             .map(key -> {
                                 String methodName = convertMethodName(key);
                                 MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                                                                              .addModifiers(Modifier.PUBLIC)
                                                                              .addStatement("return getValue($L)", key)
                                                                              .returns(String.class);
                                 if (methodName.equals("requestId")) {
                                     methodBuilder.addAnnotation(Override.class);
                                 }
                                 return methodBuilder.build();
                             }).collect(Collectors.toList());
    }

    /**
     * Convert key (UPPER_CASE) to method name.
     */
    private String convertMethodName(String key) {
        String pascalCase = CodegenNamingUtils.pascalCase(key);
        return StringUtils.uncapitalize(pascalCase);
    }

    @Override
    public ClassName className() {
        return poetExtensions.getResponseMetadataClass();
    }
}
