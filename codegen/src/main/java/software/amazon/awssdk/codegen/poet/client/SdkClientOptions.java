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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.client.config.ClientOption;

public class SdkClientOptions implements ClassSpec {
    private final IntermediateModel model;
    // private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public SdkClientOptions(IntermediateModel model) {
        this.model = model;
        // this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className())
                                            .addTypeVariable(TypeVariableName.get("T"))
                                            .addModifiers(Modifier.PUBLIC)
                                            .addAnnotation(SdkInternalApi.class)
                                            .superclass(ParameterizedTypeName.get(ClassName.get(ClientOption.class),
                                                                                  TypeVariableName.get("T")));

        builder.addMethod(ctor());

        return builder.build();
    }

    @Override
    public ClassName className() {
        return ClassName.get(model.getMetadata().getFullClientInternalPackageName(),
                             model.getMetadata().getServiceName() + "ClientOption");
    }

    private MethodSpec ctor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PRIVATE)
                         .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")),
                                       "valueClass")
                         .addStatement("super(valueClass)")
                         .build();
    }
}
