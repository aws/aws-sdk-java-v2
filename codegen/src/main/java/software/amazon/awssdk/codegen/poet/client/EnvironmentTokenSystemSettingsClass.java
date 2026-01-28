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

package software.amazon.awssdk.codegen.poet.client;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.utils.SystemSetting;

public class EnvironmentTokenSystemSettingsClass implements ClassSpec {
    protected final IntermediateModel model;
    protected final PoetExtension poetExtensions;

    public EnvironmentTokenSystemSettingsClass(IntermediateModel model) {
        this.model = model;
        this.poetExtensions = new PoetExtension(model);
    }

    @Override
    public TypeSpec poetSpec() {
        NamingStrategy namingStrategy = model.getNamingStrategy();

        String systemPropertyName = "aws.bearerToken" + namingStrategy.getSigningNameForSystemProperties();
        String envName = "AWS_BEARER_TOKEN_" + namingStrategy.getSigningNameForEnvironmentVariables();

        return TypeSpec.classBuilder(className())
                       .addModifiers(Modifier.PUBLIC)
                       .addAnnotation(PoetUtils.generatedAnnotation())
                       .addAnnotation(SdkInternalApi.class)
                       .addSuperinterface(SystemSetting.class)
                       .addMethod(MethodSpec.methodBuilder("property")
                                            .addAnnotation(Override.class)
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(String.class)
                                            .addStatement("return $S", systemPropertyName)
                                            .build())
                       .addMethod(MethodSpec.methodBuilder("environmentVariable")
                                            .addAnnotation(Override.class)
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(String.class)
                                            .addStatement("return $S", envName)
                                            .build())
                       .addMethod(MethodSpec.methodBuilder("defaultValue")
                                            .addAnnotation(Override.class)
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(String.class)
                                            .addStatement("return null")
                                            .build())
                       .build();
    }

    @Override
    public ClassName className() {
        return poetExtensions.getEnvironmentTokenSystemSettingsClass();
    }
}
