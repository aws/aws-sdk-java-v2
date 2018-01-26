/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;

/**
 * Provides Poet specs related to shape models.
 */
class ShapeModelSpec {
    private final ShapeModel shapeModel;
    private final TypeProvider typeProvider;
    private final PoetExtensions poetExtensions;

    ShapeModelSpec(ShapeModel shapeModel, TypeProvider typeProvider, PoetExtensions poetExtensions) {
        this.shapeModel = shapeModel;
        this.typeProvider = typeProvider;
        this.poetExtensions = poetExtensions;
    }

    ClassName className() {
        return poetExtensions.getModelClass(shapeModel.getShapeName());
    }

    public List<FieldSpec> fields() {
        return fields(Modifier.PRIVATE, Modifier.FINAL);
    }

    public List<FieldSpec> fields(Modifier... modifiers) {
        return shapeModel.getNonStreamingMembers().stream()
                         .map(m -> asField(m, modifiers))
                         .collect(Collectors.toList());
    }

    public FieldSpec asField(MemberModel memberModel, Modifier... modifiers) {
        FieldSpec.Builder builder = FieldSpec.builder(typeProvider.fieldType(memberModel),
                                                      memberModel.getVariable().getVariableName());

        if (modifiers != null) {
            builder.addModifiers(modifiers);
        }

        return builder.build();
    }

}
