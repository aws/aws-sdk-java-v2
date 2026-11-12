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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.smithy.customization.SmithyServiceCustomization;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Adds {@code SdkTypes.position} as the first member, so {@code GetSdkTypesResponse.position()} stays.
 */
@SdkInternalApi
public final class ApiGatewayAddSdkTypesPosition implements SmithyServiceCustomization {
    private static final ShapeId SDK_TYPES = ShapeId.from("com.amazonaws.apigateway#SdkTypes");
    private static final ShapeId STRING = ShapeId.from("com.amazonaws.apigateway#String");

    @Override
    public Set<String> sdkIds() {
        return Collections.singleton("API Gateway");
    }

    @Override
    public Model preprocess(Model model, CustomizationConfig config) {
        StructureShape sdkTypes = model.expectShape(SDK_TYPES, StructureShape.class);
        if (sdkTypes.getMember("position").isPresent()) {
            return model;
        }
        model.expectShape(STRING);
        List<MemberShape> members = new ArrayList<>();
        members.add(MemberShape.builder().id(SDK_TYPES.withMember("position")).target(STRING).build());
        members.addAll(sdkTypes.members());
        return ModelTransformer.create().replaceShapes(
            model, Collections.singletonList(sdkTypes.toBuilder().members(members).build()));
    }
}
