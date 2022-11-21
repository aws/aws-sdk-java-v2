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

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class AwsServiceBaseRequestSpecTest {

    private static IntermediateModel intermediateModel;

    @BeforeAll
    public static void setUp() throws IOException {
        File serviceModelFile = new File(AwsModelSpecTest.class.getResource("service-2.json").getFile());
        File customizationConfigFile = new File(AwsModelSpecTest.class
                                                    .getResource("customization.config")
                                                    .getFile());

        intermediateModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile))
                     .customizationConfig(ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationConfigFile))
                     .build())
            .build();
    }

    @Test
    void testGeneration() {
        AwsServiceBaseRequestSpec spec = new AwsServiceBaseRequestSpec(intermediateModel);
        assertThat(spec, generatesTo(spec.className().simpleName().toLowerCase() + ".java"));
    }

    @Test
    void buildJavaFile_memberRequiredByShape_addsTraitToGeneratedCode() {
        String requestShapeName = "QueryParameterOperationRequest";
        String queryParamName = "QueryParamOne";

        ShapeModel requestShapeModel = intermediateModel.getShapes().get(requestShapeName);
        AwsServiceModel spec = new AwsServiceModel(intermediateModel, requestShapeModel);
        String codeString = PoetUtils.buildJavaFile(spec).toString();

        String uploadIdDeclarationString = findTraitDeclarationString(codeString, queryParamName).get();
        Assertions.assertThat(uploadIdDeclarationString).contains("RequiredTrait.create()");
    }

    @Test
    void buildJavaFile_memberNotRequiredByShape_doesNotAddTraitToGeneratedCode() {
        String requestShapeName = "QueryParameterOperationRequest";
        String queryParamName = "QueryParamTwo";

        ShapeModel requestShapeModel = intermediateModel.getShapes().get(requestShapeName);
        AwsServiceModel spec = new AwsServiceModel(intermediateModel, requestShapeModel);
        String codeString = PoetUtils.buildJavaFile(spec).toString();

        String uploadIdDeclarationString = findTraitDeclarationString(codeString, queryParamName).get();
        Assertions.assertThat(uploadIdDeclarationString).doesNotContain("RequiredTrait.create()");
    }


    private static Optional<String> findTraitDeclarationString(String javaFileString, String fieldName) {
        return Arrays.stream(Pattern.compile("\n\n").split(javaFileString))
                     .filter(block -> block.contains(fieldName))
                     .filter(block -> block.contains("traits"))
                     .findFirst();
    }

}
