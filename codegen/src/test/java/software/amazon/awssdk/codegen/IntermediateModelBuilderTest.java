/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.codegen;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import org.junit.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class IntermediateModelBuilderTest {

    @Test(expected = RuntimeException.class)
    public void modelWithUnsupportedPayloadMethods_ThrowsException() {
        File serviceModel = new File(getClass().getResource("poet/client/c2j/unsupportedpayloads/service-2.json").getFile());
        File customizationModel = new File(getClass().getResource("poet/client/c2j/unsupportedpayloads/customization.config").getFile());

        C2jModels models = C2jModels.builder()
                                    .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, serviceModel))
                                    .customizationConfig(ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationModel))
                                    .build();

        try {
            new IntermediateModelBuilder(models).build();
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("GetBlacklistReportsRequest"));
            assertTrue(e.getMessage().contains("GetDedicatedIpsRequest"));
            throw e;
        }

        fail("Expected RuntimeException");
    }

}
