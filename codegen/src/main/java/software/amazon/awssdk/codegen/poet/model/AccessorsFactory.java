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

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import java.util.List;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;

class AccessorsFactory {

    private final ShapeModel shapeModel;
    private final TypeProvider typeProvider;
    private final IntermediateModel intermediateModel;
    private final BeanGetterHelper getterHelper;

    AccessorsFactory(ShapeModel shapeModel,
                     IntermediateModel intermediateModel,
                     TypeProvider typeProvider,
                     PoetExtensions poetExtensions) {
        this.shapeModel = shapeModel;
        this.typeProvider = typeProvider;
        this.intermediateModel = intermediateModel;
        this.getterHelper = new BeanGetterHelper(poetExtensions, typeProvider);
    }

    public MethodSpec beanStyleGetter(MemberModel memberModel) {
        return getterHelper.beanStyleGetter(memberModel);
    }

    public List<MethodSpec> fluentSetterDeclarations(MemberModel memberModel, TypeName returnType) {
        if (memberModel.isList()) {
            return new ListSetters(intermediateModel, shapeModel, memberModel, typeProvider).fluentDeclarations(returnType);
        }

        if (memberModel.isMap()) {
            return new MapSetters(intermediateModel, shapeModel, memberModel, typeProvider).fluentDeclarations(returnType);
        }

        return new NonCollectionSetters(intermediateModel, shapeModel, memberModel, typeProvider).fluentDeclarations(returnType);
    }

    public List<MethodSpec> fluentSetters(MemberModel memberModel, TypeName returnType) {
        if (memberModel.isList()) {
            return new ListSetters(intermediateModel, shapeModel, memberModel, typeProvider).fluent(returnType);
        }

        if (memberModel.isMap()) {
            return new MapSetters(intermediateModel, shapeModel, memberModel, typeProvider).fluent(returnType);
        }

        return new NonCollectionSetters(intermediateModel, shapeModel, memberModel, typeProvider).fluent(returnType);
    }

    public MethodSpec beanStyleSetter(MemberModel memberModel) {
        if (memberModel.isList()) {
            return new ListSetters(intermediateModel, shapeModel, memberModel, typeProvider).beanStyle();
        }

        if (memberModel.isMap()) {
            return new MapSetters(intermediateModel, shapeModel, memberModel, typeProvider).beanStyle();
        }

        return new NonCollectionSetters(intermediateModel, shapeModel, memberModel, typeProvider).beanStyle();
    }
}
