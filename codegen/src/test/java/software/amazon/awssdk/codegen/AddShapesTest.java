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

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.Location;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.poet.model.AwsModelSpecTest;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

class AddShapesTest {

    private static IntermediateModel intermediateModel;

    @BeforeAll
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
    void generateShapeModel_memberRequiredByShape_setsMemberModelAsRequired() {
        String requestShapeName = "QueryParameterOperationRequest";
        String queryParamName = "QueryParamOne";

        ShapeModel requestShapeModel = intermediateModel.getShapes().get(requestShapeName);
        MemberModel requiredMemberModel = requestShapeModel.findMemberModelByC2jName(queryParamName);

        assertThat(requestShapeModel.getRequired()).contains(queryParamName);
        assertThat(requiredMemberModel.getHttp().getLocation()).isEqualTo(Location.QUERY_STRING);
        assertThat(requiredMemberModel.isRequired()).isTrue();
    }

    @Test
    void generateShapeModel_memberNotRequiredByShape_doesNotSetMemberModelAsRequired() {
        String requestShapeName = "QueryParameterOperationRequest";
        String queryParamName = "QueryParamTwo";

        ShapeModel requestShapeModel = intermediateModel.getShapes().get(requestShapeName);
        MemberModel requiredMemberModel = requestShapeModel.findMemberModelByC2jName(queryParamName);

        assertThat(requestShapeModel.getRequired()).doesNotContain(queryParamName);
        assertThat(requiredMemberModel.getHttp().getLocation()).isEqualTo(Location.QUERY_STRING);
        assertThat(requiredMemberModel.isRequired()).isFalse();
    }

    @Test
    void generateShapeModel_memberRequiredByNestedShape_setsMemberModelAsRequired() {
        String requestShapeName = "NestedQueryParameterOperation";
        String queryParamName = "QueryParamOne";

        ShapeModel requestShapeModel = intermediateModel.getShapes().get(requestShapeName);
        MemberModel requiredMemberModel = requestShapeModel.findMemberModelByC2jName(queryParamName);

        assertThat(requestShapeModel.getRequired()).contains(queryParamName);
        assertThat(requiredMemberModel.getHttp().getLocation()).isEqualTo(Location.QUERY_STRING);
        assertThat(requiredMemberModel.isRequired()).isTrue();
    }

}
