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

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomSdkShapes;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.awssdk.codegen.poet.model.AwsModelSpecTest;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class CustomSdkShapesProcessorTest {

    CustomizationConfig config;
    ServiceModel serviceModel;

    @BeforeEach
    public void setUp() throws IOException {
        File serviceModelFile = new File(AwsModelSpecTest.class.getResource("service-2.json").getFile());
        File customizationConfigFile = new File(AwsModelSpecTest.class.getResource("customization.config").getFile());
        serviceModel = ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile);
        config = ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationConfigFile);
    }

    @Test
    public void setCustomStringShape_isAddedToModel() {
        CustomSdkShapes customSdkShapes = config.getCustomSdkShapes();
        Map<String, Shape> shapes = new HashMap<>();
        shapes.putAll(customSdkShapes.getShapes());

        Shape stringShape = new Shape();
        stringShape.setType("string");

        String shapeName = "StringShape";
        shapes.put(shapeName, stringShape);
        customSdkShapes.setShapes(shapes);
        config.setCustomSdkShapes(customSdkShapes);

        new IntermediateModelBuilder(C2jModels.builder()
                                              .serviceModel(serviceModel)
                                              .customizationConfig(config)
                                              .build())
            .build();

        assertThat(serviceModel.getShapes()).containsKey(shapeName);
        assertThat(serviceModel.getShapes()).containsValue(stringShape);
        assertThat(serviceModel.getShape(shapeName).getType()).isEqualTo("string");
    }

    @Test
    public void setCustomListShape_isAddedToModel() {
        CustomSdkShapes customSdkShapes = config.getCustomSdkShapes();
        Map<String, Shape> shapes = new HashMap<>();
        shapes.putAll(customSdkShapes.getShapes());

        Shape listShape = new Shape();
        listShape.setType("list");

        Member member = new Member();
        String memberName = "Event";
        member.setShape(memberName);
        listShape.setMember(member);

        String shapeName = "ListShape";
        shapes.put(shapeName, listShape);
        customSdkShapes.setShapes(shapes);
        config.setCustomSdkShapes(customSdkShapes);

        new IntermediateModelBuilder(C2jModels.builder()
                                              .serviceModel(serviceModel)
                                              .customizationConfig(config)
                                              .build())
            .build();

        assertThat(serviceModel.getShapes()).containsKey(shapeName);
        assertThat(serviceModel.getShapes()).containsValue(listShape);
        assertThat(serviceModel.getShape(shapeName).getType()).isEqualTo("list");
        assertThat(serviceModel.getShape(shapeName).getListMember()).isEqualTo(member);
        assertThat(serviceModel.getShape(shapeName).getListMember().getShape()).isEqualTo("Event");
    }

    @Test
    public void setCustomCustomizedShape_isAddedToModel() {
        CustomSdkShapes customSdkShapes = config.getCustomSdkShapes();
        Map<String, Shape> shapes = new HashMap<>();
        shapes.putAll(customSdkShapes.getShapes());

        Shape stringShape = new Shape();
        stringShape.setType("string");

        String shapeName = "StringShape";
        shapes.put(shapeName, stringShape);

        Shape customizedShape = new Shape();
        customizedShape.setType(shapeName);
        String customizedShapeName = "CustomizedShape";
        shapes.put(customizedShapeName, customizedShape);


        customSdkShapes.setShapes(shapes);
        config.setCustomSdkShapes(customSdkShapes);

        new IntermediateModelBuilder(C2jModels.builder()
                                              .serviceModel(serviceModel)
                                              .customizationConfig(config)
                                              .build())
            .build();

        assertThat(serviceModel.getShapes()).containsKey(customizedShapeName);
        assertThat(serviceModel.getShapes()).containsValue(customizedShape);
        assertThat(serviceModel.getShape(customizedShapeName).getType()).isEqualTo(shapeName);
    }

    @Test
    public void setCustomNullShape_throwsNullPointerException() {
        CustomSdkShapes customSdkShapes = config.getCustomSdkShapes();
        Map<String, Shape> shapes = new HashMap<>();
        shapes.putAll(customSdkShapes.getShapes());

        Shape stringShape = new Shape();
        stringShape.setType(null);

        String shapeName = "NullShape";
        shapes.put(shapeName, stringShape);
        customSdkShapes.setShapes(shapes);
        config.setCustomSdkShapes(customSdkShapes);

        assertThatNullPointerException()
            .isThrownBy(() ->  new IntermediateModelBuilder(C2jModels.builder()
                                                                     .serviceModel(serviceModel)
                                                                     .customizationConfig(config)
                                                                     .build()).
                build());
    }
}
