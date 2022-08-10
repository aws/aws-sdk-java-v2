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

package software.amazon.awssdk.codegen.model.intermediate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.AwsQueryCompatible;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class IntermediateModelTest {

    private static final String ERROR_CODE = "fooErrorCode";
    private static final String QUERY_ERROR_CODE = "queryErrorCode";
    private final AwsQueryCompatible model = new AwsQueryCompatible(ERROR_CODE);
    private final Map<String, AwsQueryCompatible> awsQueryCompatible = new HashMap() {
        {
            put(QUERY_ERROR_CODE, model);
        }
    };

    @Test
    public void cannotFindShapeWhenNoShapesExist() {

        ServiceMetadata metadata = new ServiceMetadata();
        metadata.setProtocol(Protocol.REST_JSON.getValue());
        metadata.setServiceId("empty-service");
        metadata.setSignatureVersion("V4");

        IntermediateModel testModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(new ServiceModel(metadata,
                                                    Collections.emptyMap(),
                                                    Collections.emptyMap(),
                                                    Collections.emptyMap(),
                                                    Collections.emptyMap()))
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();

        assertThatThrownBy(() -> testModel.getShapeByNameAndC2jName("AnyShape", "AnyShape"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("C2J shape AnyShape with shape name AnyShape does not exist in the intermediate model.");
    }

    @Test
    public void getShapeByNameAndC2jNameVerifiesC2JName() {
        final File modelFile = new File(IntermediateModelTest.class
                                            .getResource("../../poet/client/c2j/shared-output/service-2.json").getFile());
        IntermediateModel testModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, modelFile))
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();



        assertThatThrownBy(() -> testModel.getShapeByNameAndC2jName("PingResponse", "AnyShape"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("C2J shape AnyShape with shape name PingResponse does not exist in the intermediate model.");
    }

    @Test
    public void validateAwsQueryCompatible() {

        ServiceMetadata metadata = new ServiceMetadata();
        metadata.setProtocol(Protocol.REST_JSON.getValue());
        metadata.setServiceId("empty-service");
        metadata.setSignatureVersion("V4");

        IntermediateModel testModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(new ServiceModel(metadata,
                                                    Collections.emptyMap(),
                                                    Collections.emptyMap(),
                                                    Collections.emptyMap(),
                                                    awsQueryCompatible))
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();

        assertEquals(awsQueryCompatible, testModel.getAwsQueryCompatible());
        assertNotNull(testModel.getAwsQueryCompatible().get(QUERY_ERROR_CODE));
        AwsQueryCompatible awsQueryCompatible = testModel.getAwsQueryCompatible().get(QUERY_ERROR_CODE);
        Assert.assertEquals(ERROR_CODE, awsQueryCompatible.getErrorCode());
    }
}
