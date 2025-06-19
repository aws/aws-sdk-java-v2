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

package software.amazon.awssdk.codegen.poet.client.specs;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;

import javax.lang.model.element.Modifier;
import software.amazon.awssdk.core.rules.testing.BaseVersionCompatibilityTest;

public class VersionCompatibilityTestSpec implements ClassSpec {
    private final IntermediateModel model;

    public VersionCompatibilityTestSpec(IntermediateModel model) {
        this.model = model;
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createClassBuilder(className())
                        .superclass(BaseVersionCompatibilityTest.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addMethod(compatibilityTest())
                        .build();
    }

    @Override
    public ClassName className() {
        return ClassName.get(model.getMetadata().getFullClientPackageName(), "VersionCompatibilityTest");
    }

    private MethodSpec compatibilityTest() {
        ClassName serviceVersionInfo = ClassName.get(
            model.getMetadata().getFullInternalPackageName(),
            "ServiceVersionInfo"
        );

        return MethodSpec.methodBuilder("checkCompatibility")
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Test.class)
                         .returns(void.class)
                         .addStatement("verifyVersionCompatibility($T.VERSION)", serviceVersionInfo)
                         .build();
    }
}
