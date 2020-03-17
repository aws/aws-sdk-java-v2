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
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public class SyncClientBuilderClass implements ClassSpec {
    private final IntermediateModel model;
    private final ClassName clientInterfaceName;
    private final ClassName clientClassName;
    private final ClassName builderInterfaceName;
    private final ClassName builderClassName;
    private final ClassName builderBaseClassName;

    public SyncClientBuilderClass(IntermediateModel model) {
        String basePackage = model.getMetadata().getFullClientPackageName();
        this.model = model;
        this.clientInterfaceName = ClassName.get(basePackage, model.getMetadata().getSyncInterface());
        this.clientClassName = ClassName.get(basePackage, model.getMetadata().getSyncClient());
        this.builderInterfaceName = ClassName.get(basePackage, model.getMetadata().getSyncBuilderInterface());
        this.builderClassName = ClassName.get(basePackage, model.getMetadata().getSyncBuilder());
        this.builderBaseClassName = ClassName.get(basePackage, model.getMetadata().getBaseBuilder());
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder =
                PoetUtils.createClassBuilder(builderClassName)
                         .addAnnotation(SdkInternalApi.class)
                         .addModifiers(Modifier.FINAL)
                         .superclass(ParameterizedTypeName.get(builderBaseClassName, builderInterfaceName, clientInterfaceName))
                         .addSuperinterface(builderInterfaceName)
                         .addJavadoc("Internal implementation of {@link $T}.", builderInterfaceName);

        if (model.getEndpointOperation().isPresent()) {
            builder.addMethod(enableEndpointDiscovery());
        }

        return builder.addMethod(buildClientMethod()).build();
    }

    private MethodSpec enableEndpointDiscovery() {
        return MethodSpec.methodBuilder("enableEndpointDiscovery")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(builderClassName)
                         .addStatement("endpointDiscoveryEnabled = true")
                         .addStatement("return this")
                         .build();
    }

    private MethodSpec buildClientMethod() {
        return MethodSpec.methodBuilder("buildClient")
                             .addAnnotation(Override.class)
                             .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                             .returns(clientInterfaceName)
                             .addCode("return new $T(super.syncClientConfiguration());", clientClassName)
                             .build();
    }

    @Override
    public ClassName className() {
        return builderClassName;
    }
}
