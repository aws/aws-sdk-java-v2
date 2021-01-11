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

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;

class MapSetters extends AbstractMemberSetters {

    private final TypeProvider typeProvider;

    MapSetters(IntermediateModel intermediateModel, ShapeModel shapeModel, MemberModel memberModel, TypeProvider typeProvider) {
        super(intermediateModel, shapeModel, memberModel, typeProvider);
        this.typeProvider = typeProvider;
    }

    public List<MethodSpec> fluentDeclarations(TypeName returnType) {
        List<MethodSpec> fluentDeclarations = new ArrayList<>();

        fluentDeclarations.add(fluentAbstractSetterDeclaration(memberAsParameter(), returnType)
                                   .addJavadoc("$L", memberModel().getFluentSetterDocumentation())
                                   .build());

        if (Utils.isMapWithEnumShape(memberModel())) {
            fluentDeclarations.add(fluentAbstractSetterDeclaration(memberModel().getFluentEnumGetterMethodName(),
                                                                   mapWithEnumAsParameter(),
                                                                   returnType)
                                       .addJavadoc("$L", memberModel().getFluentSetterDocumentation())
                                       .build());
        }

        return fluentDeclarations;
    }

    @Override
    public List<MethodSpec> fluent(TypeName returnType) {
        List<MethodSpec> fluent = new ArrayList<>();

        fluent.add(fluentSetterBuilder(returnType).addCode(copySetterBody()
                                                               .toBuilder()
                                                               .addStatement("return this")
                                                               .build())
                                                  .build());

        if (Utils.isMapWithEnumShape(memberModel())) {
            fluent.add(fluentSetterBuilder(memberModel().getFluentEnumGetterMethodName(),
                                           mapWithEnumAsParameter(),
                                           returnType)
                           .addCode(copySetterBodyWithModeledEnumParameter())
                           .addStatement("return this")
                           .build());
        }

        return fluent;
    }

    @Override
    public List<MethodSpec> beanStyle() {
        MethodSpec.Builder builder = beanStyleSetterBuilder()
                .addCode(memberModel().isCollectionWithBuilderMember() ? copySetterBuilderBody() : beanCopySetterBody());

        return Collections.singletonList(builder.build());
    }

    private ParameterSpec mapWithEnumAsParameter() {
        return ParameterSpec.builder(typeProvider.parameterType(memberModel(), true), fieldName())
                            .build();
    }
}
