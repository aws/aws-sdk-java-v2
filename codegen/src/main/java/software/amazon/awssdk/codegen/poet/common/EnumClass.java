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

package software.amazon.awssdk.codegen.poet.common;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public final class EnumClass extends AbstractEnumClass {
    private final String enumPackageName;

    public EnumClass(String enumPackage, ShapeModel shape) {
        super(shape);
        this.enumPackageName = enumPackage;
    }

    @Override
    protected void addDeprecated(Builder enumBuilder) {
        PoetUtils.addDeprecated(enumBuilder::addAnnotation, getShape());
    }

    @Override
    protected void addJavadoc(Builder enumBuilder) {
        PoetUtils.addJavadoc(enumBuilder::addJavadoc, getShape());
    }

    @Override
    protected void addEnumConstants(Builder enumBuilder) {
        getShape().getEnums().forEach(
            e -> enumBuilder.addEnumConstant(e.getName(), TypeSpec.anonymousClassBuilder("$S", e.getValue()).build())
        );
    }

    @Override
    public ClassName className() {
        return ClassName.get(enumPackageName, getShape().getShapeName());
    }
}
