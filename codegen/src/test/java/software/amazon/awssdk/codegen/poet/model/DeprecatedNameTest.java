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

import java.io.File;
import org.junit.Test;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class DeprecatedNameTest {
    @Test(expected = IllegalStateException.class)
    public void throwsOnListDeprecation() {
        runTest("listdeprecationfailure");
    }

    @Test(expected = IllegalStateException.class)
    public void throwsOnMapDeprecation() {
        runTest("mapdeprecationfailure");
    }

    @Test(expected = IllegalStateException.class)
    public void throwsOnEnumDeprecation() {
        runTest("enumdeprecationfailure");
    }

    private void runTest(String testName) {
        File serviceModelFile = new File(getClass().getResource("./deprecatedname/" + testName + ".json").getFile());
        File customizationConfigFile = new File(getClass()
                                                    .getResource("./deprecatedname/" + testName + ".customization")
                                                    .getFile());
        ServiceModel serviceModel = ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile);
        CustomizationConfig basicConfig = ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationConfigFile);

        new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(serviceModel)
                     .customizationConfig(basicConfig)
                     .build())
            .build();
    }
}
