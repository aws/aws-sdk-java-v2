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

package software.amazon.awssdk.codegen.customization.processors;


import java.io.File;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

/**
 * Tests for {@link UseLegacyEventGenerationSchemeProcessor}
 */
public class UseLegacyEventGenerationSchemeProcessorTest {
    private static final String RESOURCE_ROOT = "/software/amazon/awssdk/codegen/emitters/customizations/processors/uselegacyeventgenerationscheme";

    private static final UseLegacyEventGenerationSchemeProcessor processor = new UseLegacyEventGenerationSchemeProcessor();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static ServiceModel serviceModel;


    @BeforeClass
    public static void setup() {
        String c2jFilePath = UseLegacyEventGenerationSchemeProcessorTest.class.getResource(RESOURCE_ROOT + "/service-2.json").getFile();
        File c2jFile = new File(c2jFilePath);

        serviceModel = ModelLoaderUtils.loadModel(ServiceModel.class, c2jFile);
    }

    @Test
    public void testPostProcess_customizationDeclaredForMultipleMembersWithSameShape_throws() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("targets more than one member with the shape");

        IntermediateModel intermediateModel = intermediateModelWithConfig("multiple-event-types-same-shape.config");

        processor.postprocess(intermediateModel);
    }

    @Test
    public void testPostProcess_customizationIsValid_succeeds() {
        IntermediateModel intermediateModel = intermediateModelWithConfig("happy-case-customization.config");
        processor.postprocess(intermediateModel);
    }


    private static IntermediateModel intermediateModelWithConfig(String configName) {
        IntermediateModel intermediateModel = new IntermediateModelBuilder(C2jModels.builder()
                .serviceModel(serviceModel)
                .customizationConfig(CustomizationConfig.create())
                .build())
                .build();

        intermediateModel.setCustomizationConfig(loadCustomizationConfig(configName));

        return intermediateModel;
    }

    private static CustomizationConfig loadCustomizationConfig(String configName) {
        String c2jFilePath = UseLegacyEventGenerationSchemeProcessorTest.class.getResource(RESOURCE_ROOT + "/" + configName).getFile();
        File file = new File(c2jFilePath);
        return ModelLoaderUtils.loadModel(CustomizationConfig.class, file);
    }
}
