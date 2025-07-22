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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class SmithyRpcV2CborProtocolProcessorTest {

    private static CustomizationConfig config;
    private static ServiceModel serviceModel;

    @BeforeAll
    public static void setUp() throws IOException {
        File serviceModelFile = new File(SmithyRpcV2CborProtocolProcessorTest.class.getResource("rpcv2-service-2.json").getFile());
        serviceModel = ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile);
        File customizationConfigFile = new File(SmithyRpcV2CborProtocolProcessorTest.class.getResource("customization.config").getFile());
        serviceModel = ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile);
        config = ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationConfigFile);
    }

    @Test
    public void specificRpcV2Uri_isSetInTheModel() {
        assertThat(serviceModel.getOperation("Slash").getHttp().getRequestUri()).isEqualTo("/");
        assertThat(serviceModel.getOperation("Custom").getHttp().getRequestUri()).isEqualTo("/Foo");
        assertThat(serviceModel.getOperation("None").getHttp()).isNull();
        IntermediateModel intermediateModel = new IntermediateModelBuilder(C2jModels.builder()
                                                                                    .serviceModel(serviceModel)
                                                                                    .customizationConfig(config)
                                                                                    .build())
            .build();

        assertThat(serviceModel.getOperation("Slash").getHttp().getRequestUri())
            .isEqualTo("/service/RpcV2Protocol/operation/Slash");
        assertThat(serviceModel.getOperation("Custom").getHttp().getRequestUri())
            .isEqualTo("/service/RpcV2Protocol/operation/Custom");
        assertThat(serviceModel.getOperation("None").getHttp().getRequestUri())
            .isEqualTo("/service/RpcV2Protocol/operation/None");
    }
}
