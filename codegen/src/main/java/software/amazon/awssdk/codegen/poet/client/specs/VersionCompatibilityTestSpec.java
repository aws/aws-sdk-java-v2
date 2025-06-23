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
import javax.lang.model.element.Modifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public class VersionCompatibilityTestSpec implements ClassSpec {
    private final IntermediateModel model;

    public VersionCompatibilityTestSpec(IntermediateModel model) {
        this.model = model;
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createClassBuilder(className())
                        .addModifiers(Modifier.PUBLIC)
                        .addMethod(compatibilityTest())
                        .addMethod(isVersionCompatibleMethod())
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

        ClassName versionInfo = ClassName.get("software.amazon.awssdk.core.util", "VersionInfo");
        ClassName assertions = ClassName.get("org.assertj.core.api", "Assertions");

        return MethodSpec.methodBuilder("checkCompatibility")
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Test.class)
                         .returns(void.class)
                         .addStatement("String coreVersion = $T.SDK_VERSION", versionInfo)
                         .addStatement("String serviceVersion = $T.VERSION", serviceVersionInfo)
                         .addStatement("$T.assertThat(isVersionCompatible(coreVersion, serviceVersion))" +
                                       ".withFailMessage(\"Core version %s must be equal to or newer than service version %s\", " +
                                       "coreVersion, serviceVersion).isTrue()",
                                       assertions)
                         .build();
    }

    private MethodSpec isVersionCompatibleMethod() {
        return MethodSpec.methodBuilder("isVersionCompatible")
                         .addModifiers(Modifier.PRIVATE)
                         .returns(boolean.class)
                         .addParameter(String.class, "coreVersion")
                         .addParameter(String.class, "serviceVersion")
                         .addStatement("String normalizedCore = coreVersion.replace(\"-SNAPSHOT\", \"\")")
                         .addStatement("String normalizedService = serviceVersion.replace(\"-SNAPSHOT\", \"\")")
                         .addStatement("String[] coreParts = normalizedCore.split(\"\\\\.\")")
                         .addStatement("String[] serviceParts = normalizedService.split(\"\\\\.\")")
                         .addCode("\n")
                         .addStatement("int coreMajor = Integer.parseInt(coreParts[0])")
                         .addStatement("int serviceMajor = Integer.parseInt(serviceParts[0])")
                         .beginControlFlow("if (coreMajor != serviceMajor)")
                         .addStatement("return coreMajor >= serviceMajor")
                         .endControlFlow()
                         .addCode("\n")
                         .addStatement("int coreMinor = Integer.parseInt(coreParts[1])")
                         .addStatement("int serviceMinor = Integer.parseInt(serviceParts[1])")
                         .beginControlFlow("if (coreMinor != serviceMinor)")
                         .addStatement("return coreMinor >= serviceMinor")
                         .endControlFlow()
                         .addCode("\n")
                         .addStatement("int corePatch = Integer.parseInt(coreParts[2])")
                         .addStatement("int servicePatch = Integer.parseInt(serviceParts[2])")
                         .addStatement("return corePatch >= servicePatch")
                         .build();
    }
}

