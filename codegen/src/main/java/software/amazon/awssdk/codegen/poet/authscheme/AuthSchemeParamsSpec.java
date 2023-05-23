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

package software.amazon.awssdk.codegen.poet.authscheme;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;

// TODO: reviewer: Params v/s Parameters?
public class AuthSchemeParamsSpec implements ClassSpec {
    private final IntermediateModel intermediateModel;
    private final AuthSchemeSpecUtils authSchemeSpecUtils;

    public AuthSchemeParamsSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.authSchemeSpecUtils = new AuthSchemeSpecUtils(intermediateModel);
    }

    @Override
    public ClassName className() {
        return authSchemeSpecUtils.parametersInterfaceName();
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder b = PoetUtils.createInterfaceBuilder(className())
                                      .addModifiers(Modifier.PUBLIC)
                                      .addAnnotation(SdkPublicApi.class)
                                      .addJavadoc(interfaceJavadoc())
                                      .addMethod(builderMethod())
                                      .addType(builderInterfaceSpec());

        addAccessorMethods(b);
        return b.build();
    }

    private CodeBlock interfaceJavadoc() {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("The parameters object used to resolve the auth schemes for the $N service.",
              intermediateModel.getMetadata().getServiceName());

        return b.build();
    }

    private MethodSpec builderMethod() {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .returns(authSchemeSpecUtils.parametersInterfaceBuilderInterfaceName())
                         .addStatement("return $T.builder()", authSchemeSpecUtils.parametersDefaultImplName())
                         .addJavadoc("Get a new builder for creating a {@link $T}.",
                                   authSchemeSpecUtils.parametersInterfaceName())
                         .build();
    }

    private TypeSpec builderInterfaceSpec() {
        TypeSpec.Builder b = TypeSpec.interfaceBuilder(authSchemeSpecUtils.parametersInterfaceBuilderInterfaceName())
                                     .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                     .addJavadoc("A builder for a {@link $T}.", authSchemeSpecUtils.parametersInterfaceName());

        addBuilderSetterMethods(b);

        b.addMethod(MethodSpec.methodBuilder("build")
                              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                              .returns(className())
                              .addJavadoc("Returns a {@link $T} object that is created from the properties that have been set "
                                          + "on the builder.", authSchemeSpecUtils.parametersInterfaceName())
                              .build());

        return b.build();
    }

    private void addAccessorMethods(TypeSpec.Builder b) {
        b.addMethod(MethodSpec.methodBuilder("operation")
                              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                              .returns(String.class)
                              .addJavadoc("Returns the operation for which to resolve the auth scheme.")
                              .build());

        if (authSchemeSpecUtils.usesSigV4()) {
            b.addMethod(MethodSpec.methodBuilder("region")
                                  .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                  // TODO: reviewer: Should region be Regions (Regions.class isn't available here though)
                                  .returns(ParameterizedTypeName.get(Optional.class, String.class))
                                  .addJavadoc("Returns the region. The region is optional. The region parameter may be used "
                                              + "with $S auth scheme. By default, the region will be empty.", "aws.auth#sigv4")
                                  .build());
        }
    }

    private void addBuilderSetterMethods(TypeSpec.Builder b) {
        b.addMethod(MethodSpec.methodBuilder("operation")
                              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                              .addParameter(ParameterSpec.builder(String.class, "operation").build())
                              .returns(authSchemeSpecUtils.parametersInterfaceBuilderInterfaceName())
                              .addJavadoc("Set the operation for which to resolve the auth scheme.")
                              .build());

        if (authSchemeSpecUtils.usesSigV4()) {
            b.addMethod(MethodSpec.methodBuilder("region")
                                  .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                  .addParameter(ParameterSpec.builder(String.class, "region").build())
                                  .returns(authSchemeSpecUtils.parametersInterfaceBuilderInterfaceName())
                                  .addJavadoc("Set the region. The region parameter may be used with  the $S auth scheme.",
                                              "aws.auth#sigv4") // TODO: Could the known auth schemes be defined somewhere?
                                  .build());
        }
    }
}
