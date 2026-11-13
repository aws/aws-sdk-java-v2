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

package software.amazon.awssdk.codegen.smithy.customization.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

class ApiGatewayAddSdkTypesPositionTest {
    private static final ShapeId SDK_TYPES = ShapeId.from("com.amazonaws.apigateway#SdkTypes");

    @Test
    void addsPositionAsTheFirstMember() {
        StructureShape sdkTypes = sdkTypes("structure SdkTypes { items: ListOfString }\n");

        assertThat(sdkTypes.getMemberNames()).containsExactly("position", "items");
        assertThat(sdkTypes.getMember("position").get().getTarget())
            .isEqualTo(ShapeId.from("com.amazonaws.apigateway#String"));
    }

    @Test
    void leavesSdkTypesAloneWhenItHasPosition() {
        StructureShape sdkTypes = sdkTypes("structure SdkTypes { items: ListOfString, position: String }\n");

        assertThat(sdkTypes.getMemberNames()).containsExactly("items", "position");
    }

    private static StructureShape sdkTypes(String structure) {
        Model model = Model.assembler()
                           .addUnparsedModel("apigateway.smithy",
                                             "$version: \"2.0\"\nnamespace com.amazonaws.apigateway\n"
                                             + "service BackplaneControlService { version: \"2015-07-09\" }\n"
                                             + "string String\n"
                                             + "list ListOfString { member: String }\n"
                                             + structure)
                           .assemble()
                           .unwrap();
        return new ApiGatewayAddSdkTypesPosition().preprocess(model, CustomizationConfig.create())
                                                  .expectShape(SDK_TYPES, StructureShape.class);
    }
}
