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
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class AwsServiceBaseResponseSpecTest {
    private static IntermediateModel intermediateModel;

    @BeforeClass
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
    public void testGeneration() {
        AwsServiceBaseResponseSpec spec = new AwsServiceBaseResponseSpec(intermediateModel);
        assertThat(spec, generatesTo(spec.className().simpleName().toLowerCase() + ".java"));
    }
}
