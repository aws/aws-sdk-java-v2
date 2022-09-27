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

package software.amazon.awssdk.codegen.poet.rules;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.ClientContextParam;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.utils.AttributeMap;

public class ClientContextParamsClassSpec implements ClassSpec {
    private final IntermediateModel model;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public ClientContextParamsClassSpec(IntermediateModel model) {
        this.model = model;
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder b = PoetUtils.createClassBuilder(endpointRulesSpecUtils.clientContextParamsName())
                                      .superclass(ParameterizedTypeName.get(
                                          ClassName.get(AttributeMap.class).nestedClass("Key"), TypeVariableName.get("T")))
                                      .addAnnotation(SdkInternalApi.class)
                                      .addTypeVariable(TypeVariableName.get("T"))
                                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        b.addMethod(ctor());

        model.getClientContextParams().forEach((n, m) -> {
            b.addField(paramDeclaration(n, m));
        });

        return b.build();
    }

    @Override
    public ClassName className() {
        return endpointRulesSpecUtils.clientContextParamsName();
    }

    private MethodSpec ctor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PRIVATE)
                         .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")),
                                       "valueClass")
                         .addStatement("super(valueClass)")
                         .build();
    }

    private FieldSpec paramDeclaration(String name, ClientContextParam param) {
        String fieldName = endpointRulesSpecUtils.clientContextParamName(name);
        TypeName type = endpointRulesSpecUtils.toJavaType(param.getType());

        FieldSpec.Builder b = FieldSpec.builder(ParameterizedTypeName.get(className(), type), fieldName)
                                       .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

        b.initializer("new $T<>($T.class)", className(), type);
        PoetUtils.addJavadoc(b::addJavadoc, param.getDocumentation());
        return b.build();
    }
}
