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

package software.amazon.awssdk.codegen.poet.common;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.util.VersionInfo;

public class UserAgentUtilsSpec implements ClassSpec {

    private static final String PAGINATOR_USER_AGENT = "PAGINATED";

    protected final IntermediateModel model;
    protected final PoetExtension poetExtensions;

    public UserAgentUtilsSpec(IntermediateModel model) {
        this.model = model;
        this.poetExtensions = new PoetExtension(model);
    }

    @Override
    public TypeSpec poetSpec() {
        return TypeSpec.classBuilder(className())
                       .addModifiers(Modifier.PUBLIC)
                       .addAnnotation(PoetUtils.generatedAnnotation())
                       .addAnnotation(SdkInternalApi.class)
                       .addMethod(privateConstructor())
                       .addMethod(applyUserAgentInfoMethod())
                       .addMethod(applyPaginatorUserAgentMethod())
                       .build();
    }

    @Override
    public ClassName className() {
        return poetExtensions.getUserAgentClass();
    }

    protected MethodSpec privateConstructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PRIVATE)
                         .build();
    }

    private MethodSpec applyUserAgentInfoMethod() {

        TypeVariableName typeVariableName =
            TypeVariableName.get("T", poetExtensions.getModelClass(model.getSdkRequestBaseClassName()));

        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName
            .get(ClassName.get(Consumer.class), ClassName.get(AwsRequestOverrideConfiguration.Builder.class));

        CodeBlock codeBlock = CodeBlock.builder()
                                       .addStatement("$T overrideConfiguration =\n"
                                                     + "            request.overrideConfiguration().map(c -> c.toBuilder()"
                                                     + ".applyMutation"
                                                     + "(userAgentApplier).build())\n"
                                                     + "            .orElse((AwsRequestOverrideConfiguration.builder()"
                                                     + ".applyMutation"
                                                     + "(userAgentApplier).build()))", AwsRequestOverrideConfiguration.class)
                                       .addStatement("return (T) request.toBuilder().overrideConfiguration"
                                                     + "(overrideConfiguration).build()")
                                       .build();

        return MethodSpec.methodBuilder("applyUserAgentInfo")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .addParameter(typeVariableName, "request")
                         .addParameter(parameterizedTypeName, "userAgentApplier")
                         .addTypeVariable(typeVariableName)
                         .addCode(codeBlock)
                         .returns(typeVariableName)
                         .build();
    }

    private MethodSpec applyPaginatorUserAgentMethod() {
        TypeVariableName typeVariableName =
            TypeVariableName.get("T", poetExtensions.getModelClass(model.getSdkRequestBaseClassName()));

        return MethodSpec.methodBuilder("applyPaginatorUserAgent")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .addParameter(typeVariableName, "request")
                         .addTypeVariable(typeVariableName)
                         .addStatement("return applyUserAgentInfo(request, b -> b.addApiName($T.builder()"
                                       + ".version($T.SDK_VERSION)"
                                       + ".name($S)"
                                       + ".build()))",
                                       ApiName.class,
                                       VersionInfo.class,
                                       PAGINATOR_USER_AGENT)
                         .returns(typeVariableName)
                         .build();
    }
}
