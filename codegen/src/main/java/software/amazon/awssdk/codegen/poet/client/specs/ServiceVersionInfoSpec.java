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

package software.amazon.awssdk.codegen.poet.client.specs;

import static software.amazon.awssdk.core.util.VersionInfo.SDK_VERSION;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public class ServiceVersionInfoSpec implements ClassSpec {
    private final PoetExtension poetExtension;
    private final IntermediateModel model;

    public ServiceVersionInfoSpec(IntermediateModel model) {
        this.poetExtension = new PoetExtension(model);
        this.model = model;
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("ServiceVersionInfo")
                                           .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                           .addAnnotation(PoetUtils.generatedAnnotation())
                                           .addAnnotation(SdkInternalApi.class)
                                           .addField(FieldSpec.builder(
                                               String.class, "VERSION", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                                              .initializer("$S", SDK_VERSION)
                                                              .addJavadoc("Returns the current version for the AWS SDK in which"
                                                                          + " this class is running.")
                                                              .build())
                                           .addField(userAgentField())
                                           .addMethod(privateConstructor());

        return builder.build();
    }

    protected MethodSpec privateConstructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PRIVATE)
                         .build();
    }

    private FieldSpec userAgentField() {
        return FieldSpec.builder(String.class, "USER_AGENT", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", transformServiceId(model.getMetadata().getServiceId()) + "#" + SDK_VERSION)
                        .addAnnotation(SdkInternalApi.class)
                        .addJavadoc("Returns a user agent containing the service and "
                                    + "version info")
                        .build();
    }

    private String transformServiceId(String serviceId) {
        // According to User Agent 2.0 spec, replace spaces with underscores
        return serviceId.replace(" ", "_");
    }

    @Override
    public ClassName className() {
        return poetExtension.getServiceVersionInfoClass();
    }
}
