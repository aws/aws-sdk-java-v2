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

package software.amazon.awssdk.codegen.poet.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;

class MapSetters extends AbstractMemberSetters {

    MapSetters(IntermediateModel intermediateModel, ShapeModel shapeModel, MemberModel memberModel, TypeProvider typeProvider) {
        super(intermediateModel, shapeModel, memberModel, typeProvider);
    }

    public List<MethodSpec> fluentDeclarations(TypeName returnType) {
        return Collections.singletonList(fluentAbstractSetterDeclaration(memberAsParameter(), returnType)
                .addJavadoc("$L", memberModel().getFluentSetterDocumentation())
                .build());
    }

    @Override
    public List<MethodSpec> fluent(TypeName returnType) {
        return Collections.singletonList(fluentSetterBuilder(returnType)
                .addCode(copySetterBody()
                        .toBuilder()
                        .addStatement("return this").build())
                .build());
    }

    @Override
    public MethodSpec beanStyle() {
        MethodSpec.Builder builder = beanStyleSetterBuilder()
                .addCode(memberModel().isCollectionWithBuilderMember() ? copySetterBuilderBody() : copySetterBody());

        if (annotateJsonProperty()) {
            builder.addAnnotation(
                    AnnotationSpec.builder(JsonProperty.class)
                            .addMember("value", "$S", memberModel().getHttp().getMarshallLocationName()).build());
        }

        return builder.build();
    }
}
