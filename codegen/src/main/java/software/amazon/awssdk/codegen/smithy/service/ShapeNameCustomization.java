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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Shapes renamed in Smithy keep their C2J class names. Where an operation or resource already has the C2J name, the
 * shape moves to a second namespace; Smithy would reject that, but the model is not validated again, and codegen
 * names classes from the shape name only.
 */
@SdkInternalApi
final class ShapeNameCustomization implements SmithyServiceCustomization {
    private static final Map<String, Map<String, String>> RENAMES = new LinkedHashMap<>();

    static {
        RENAMES.put("deadline", Collections.singletonMap("com.amazonaws.deadline#DeadlinePrincipalType",
                                                         "com.amazonaws.deadline#PrincipalType"));
        RENAMES.put("Notifications", Collections.singletonMap("com.amazonaws.notifications#NotificationEventSchema",
                                                              "com.amazonaws.notifications#NotificationEvent"));
        Map<String, String> qapps = new LinkedHashMap<>();
        qapps.put("com.amazonaws.qapps#Action", "com.amazonaws.qapps#PermissionOutputActionEnum");
        qapps.put("com.amazonaws.qapps#UserType", "com.amazonaws.qapps#PrincipalOutputUserTypeEnum");
        RENAMES.put("QApps", qapps);
        RENAMES.put("MediaLive", Collections.singletonMap("com.amazonaws.medialive#UpdateNodeStateShape",
                                                          "com.amazonaws.medialive.sdk#UpdateNodeState"));
        Map<String, String> pinpoint = new LinkedHashMap<>();
        pinpoint.put("com.amazonaws.pinpoint#CreateRecommenderConfigurationShape",
                     "com.amazonaws.pinpoint.sdk#CreateRecommenderConfiguration");
        pinpoint.put("com.amazonaws.pinpoint#UpdateRecommenderConfigurationShape",
                     "com.amazonaws.pinpoint.sdk#UpdateRecommenderConfiguration");
        RENAMES.put("Pinpoint", pinpoint);
        RENAMES.put("Bedrock AgentCore Control",
                    Collections.singletonMap("com.amazonaws.bedrockagentcorecontrol#GatewayApiKeyCredentialProvider",
                                             "com.amazonaws.bedrockagentcorecontrol.sdk#ApiKeyCredentialProvider"));
    }

    @Override
    public Set<String> sdkIds() {
        return RENAMES.keySet();
    }

    @Override
    public Model preprocess(Model model, CustomizationConfig config) {
        String sdkId = model.getServiceShapes().iterator().next()
                            .expectTrait(software.amazon.smithy.aws.traits.ServiceTrait.class).getSdkId();
        Model result = model;
        if ("QApps".equals(sdkId)) {
            // One Smithy enum is two C2J enums: the input member gets its own copy.
            ShapeId copy = ShapeId.from("com.amazonaws.qapps#PermissionInputActionEnum");
            Shape action = model.expectShape(ShapeId.from("com.amazonaws.qapps#Action"));
            MemberShape input = model.expectShape(ShapeId.from("com.amazonaws.qapps#PermissionInput$action"),
                                                  MemberShape.class);
            result = ModelTransformer.create().replaceShapes(result, Arrays.asList(
                (Shape) Shape.shapeToBuilder(action).id(copy).build(), input.toBuilder().target(copy).build()));
        }
        Map<ShapeId, ShapeId> renamed = new LinkedHashMap<>();
        RENAMES.get(sdkId).forEach((from, to) -> renamed.put(ShapeId.from(from), ShapeId.from(to)));
        return ModelTransformer.create().renameShapes(result, renamed);
    }
}
