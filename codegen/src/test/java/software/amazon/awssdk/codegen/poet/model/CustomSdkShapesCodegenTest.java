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


import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class CustomSdkShapesCodegenTest {

    private IntermediateModel intermediateModel;

    @Before
    public void setUp() throws IOException {
        File serviceModelFile = new File(AwsModelSpecTest.class.getResource("service-2.json").getFile());
        File customizationConfigFile = new File(AwsModelSpecTest.class
                                                    .getResource("customization.config")
                                                    .getFile());
        ServiceModel serviceModel = ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile);
        CustomizationConfig basicConfig = ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationConfigFile);

        intermediateModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(serviceModel)
                     .customizationConfig(basicConfig)
                     .build())
            .build();
    }


    @Test
    public void setAndInjectCustomShapes_areAddedAsMembersToBaseTypeShape() {
        ShapeModel baseTypeShape = intermediateModel.getShapes().get("BaseType");
        List<MemberModel> baseTypeShapeMembers = baseTypeShape.getMembers();
        List<String> memberNames = new ArrayList<>();
        for (MemberModel member: baseTypeShapeMembers) {
            memberNames.add(member.getName());
        }
        assertThat(memberNames).contains("CustomShape1","CustomShape2");
    }

    @Test
    public void trustedStructureConfiguration_rejectsUnknownShape() throws IOException {
        IntermediateModel model = model(Collections.emptyMap(),
                                        singletonMap("MissingShape", singletonList("SimpleList")));

        assertThatThrownBy(() -> source(model, "AllTypesRequest"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void trustedStructureConfiguration_rejectsNonCollectionMember() throws IOException {
        IntermediateModel model = model(Collections.emptyMap(),
                                        singletonMap("AllTypesRequest", singletonList("StringMember")));

        assertThatThrownBy(() -> source(model, "AllTypesRequest"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void trustedUnionConfiguration_requiresDirectUnionConstructor() throws IOException {
        IntermediateModel model = model(singletonMap("AllTypesRequest", singletonList("SimpleList")),
                                        Collections.emptyMap());

        assertThatThrownBy(() -> source(model, "AllTypesUnionStructure"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static IntermediateModel model(Map<String, List<String>> trustedUnions,
                                           Map<String, List<String>> trustedStructures) throws IOException {
        File serviceModelFile = new File(AwsModelSpecTest.class.getResource("service-2.json").getFile());
        File customizationConfigFile = new File(AwsModelSpecTest.class
                                                    .getResource("customization.config")
                                                    .getFile());
        ServiceModel serviceModel = ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile);
        CustomizationConfig config = ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationConfigFile);
        config.setGenerateTrustedUnionCollectionConstructors(trustedUnions);
        config.setGenerateTrustedStructureCollectionBuilders(trustedStructures);

        return new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(serviceModel)
                     .customizationConfig(config)
                     .build())
            .build();
    }

    private static String source(IntermediateModel model, String shapeName) {
        return new AwsServiceModel(model, model.getShapes().get(shapeName)).poetSpec().toString();
    }
}
