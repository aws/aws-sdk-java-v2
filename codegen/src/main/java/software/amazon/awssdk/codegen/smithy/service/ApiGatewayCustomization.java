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

package software.amazon.awssdk.codegen.smithy.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * {@code SdkTypes.position} exists only in C2J; keeping it keeps {@code GetSdkTypesResponse.position()}. It goes first,
 * where C2J has it.
 */
@SdkInternalApi
final class ApiGatewayCustomization implements SmithyServiceCustomization {
    private static final ShapeId SDK_TYPES = ShapeId.from("com.amazonaws.apigateway#SdkTypes");

    @Override
    public Set<String> sdkIds() {
        return Collections.singleton("API Gateway");
    }

    @Override
    public Model preprocess(Model model, CustomizationConfig config) {
        StructureShape sdkTypes = model.expectShape(SDK_TYPES, StructureShape.class);
        List<MemberShape> members = new ArrayList<>();
        members.add(MemberShape.builder().id(SDK_TYPES.withMember("position"))
                               .target("com.amazonaws.apigateway#String").build());
        members.addAll(sdkTypes.members());
        return ModelTransformer.create().replaceShapes(
            model, Collections.singletonList(sdkTypes.toBuilder().members(members).build()));
    }
}
