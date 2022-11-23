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

package software.amazon.awssdk.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class IntermediateModelBuilderTest {

    @Test
    public void testServiceAndShapeNameCollisions() throws Exception {
        final File modelFile = new File(IntermediateModelBuilderTest.class
                                            .getResource("poet/client/c2j/collision/service-2.json").getFile());
        IntermediateModel testModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, modelFile))
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();

        assertThat(testModel.getShapes().values())
            .extracting(ShapeModel::getShapeName)
            .containsExactlyInAnyOrder("DefaultCollisionException", "DefaultCollisionRequest", "DefaultCollisionResponse");
    }

    @Test
    public void sharedOutputShapesLinkCorrectlyToOperationOutputs() {
        final File modelFile = new File(IntermediateModelBuilderTest.class
                                            .getResource("poet/client/c2j/shared-output/service-2.json").getFile());
        IntermediateModel testModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, modelFile))
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();

        assertEquals("PingResponse", testModel.getOperation("Ping").getOutputShape().getShapeName());
        assertEquals("SecurePingResponse", testModel.getOperation("SecurePing").getOutputShape().getShapeName());
    }

    @Test
    public void defaultEndpointDiscovery_true() {
        final File modelFile = new File(IntermediateModelBuilderTest.class
                                            .getResource("poet/client/c2j/endpointdiscovery/service-2.json").getFile());
        IntermediateModel testModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, modelFile))
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();

        assertTrue(testModel.getEndpointOperation().get().isEndpointCacheRequired());
    }

    @Test
    public void defaultEndpointDiscovery_false() {
        final File modelFile = new File(IntermediateModelBuilderTest.class
                                            .getResource("poet/client/c2j/endpointdiscoveryoptional/service-2.json").getFile());
        IntermediateModel testModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, modelFile))
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();

        assertFalse(testModel.getEndpointOperation().get().isEndpointCacheRequired());
    }

    @Test
    public void assertAwsQueryCompatibleTrait_notNull() {
        final File modelFile = new File(IntermediateModelBuilderTest.class
                                            .getResource("poet/client/c2j/query-to-json-errorcode/service-2.json").getFile());
        IntermediateModel testModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, modelFile))
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();

        assertNotNull(testModel.getMetadata().getAwsQueryCompatible());
    }

}
