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

package software.amazon.awssdk.codegen.poet.transform;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

@RunWith(Parameterized.class)
public class MarshallerSpecTest {
    private static IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        invokeSafely(MarshallerSpecTest::setUp);
        return intermediateModel.getShapes().values().stream()
                                .filter(shape -> "Request".equals(shape.getType()) || shape.isEvent())
                                .map(shape -> new Object[] {shape}).collect(toList());
    }

    public MarshallerSpecTest(ShapeModel shapeModel) {
        this.shapeModel = shapeModel;
    }

    @Test
    public void basicGeneration() {
        assertThat(new MarshallerSpec(intermediateModel, shapeModel), generatesTo(referenceFileForShape()));
    }

    private String referenceFileForShape() {
        return shapeModel.getShapeName().toLowerCase(Locale.ENGLISH) + "marshaller.java";
    }

    private static void setUp() throws IOException {
        File serviceModelFile = new File(MarshallerSpecTest.class.getResource("service-2.json").getFile());
        File customizationConfigFile = new File(MarshallerSpecTest.class
                                                    .getResource("customization.config")
                                                    .getFile());

        intermediateModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile))
                     .customizationConfig(ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationConfigFile))
                     .build())
            .build();
    }
}
