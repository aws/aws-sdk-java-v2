/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import software.amazon.awssdk.client.builder.SyncClientBuilder;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public class SyncClientBuilderInterface implements ClassSpec {
    private final ClassName builderInterfaceName;
    private final ClassName clientInterfaceName;
    private final ClassName baseBuilderInterfaceName;

    public SyncClientBuilderInterface(IntermediateModel model) {
        String basePackage = model.getMetadata().getFullClientPackageName();
        this.clientInterfaceName = ClassName.get(basePackage, model.getMetadata().getSyncInterface());
        this.builderInterfaceName = ClassName.get(basePackage, model.getMetadata().getSyncBuilderInterface());
        this.baseBuilderInterfaceName = ClassName.get(basePackage, model.getMetadata().getBaseBuilderInterface());
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createInterfaceBuilder(builderInterfaceName)
                        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(SyncClientBuilder.class),
                                                                     builderInterfaceName, clientInterfaceName))
                        .addSuperinterface(ParameterizedTypeName.get(baseBuilderInterfaceName,
                                                                     builderInterfaceName, clientInterfaceName))
                        .addJavadoc(getJavadoc())
                        .build();
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
