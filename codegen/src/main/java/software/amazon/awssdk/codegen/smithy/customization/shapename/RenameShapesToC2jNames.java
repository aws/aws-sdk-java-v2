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

package software.amazon.awssdk.codegen.smithy.customization.shapename;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.smithy.customization.SmithyServiceCustomization;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Renames shapes back to the names their classes have. A shape whose name an operation or resource already has moves
 * to a {@code .sdk} namespace, since class names ignore the namespace.
 */
@SdkInternalApi
final class RenameShapesToC2jNames implements SmithyServiceCustomization {
    private final String sdkId;
    private final Map<ShapeId, ShapeId> renames;

    private RenameShapesToC2jNames(String sdkId, Map<ShapeId, ShapeId> renames) {
        this.sdkId = sdkId;
        this.renames = Collections.unmodifiableMap(renames);
    }

    static RenameShapesToC2jNames deadline() {
        Map<ShapeId, ShapeId> renames = new LinkedHashMap<>();
        put(renames, "com.amazonaws.deadline#DeadlinePrincipalType", "com.amazonaws.deadline#PrincipalType");
        return new RenameShapesToC2jNames("deadline", renames);
    }

    static RenameShapesToC2jNames notifications() {
        Map<ShapeId, ShapeId> renames = new LinkedHashMap<>();
        put(renames, "com.amazonaws.notifications#NotificationEventSchema",
            "com.amazonaws.notifications#NotificationEvent");
        return new RenameShapesToC2jNames("Notifications", renames);
    }

    static RenameShapesToC2jNames qApps() {
        Map<ShapeId, ShapeId> renames = new LinkedHashMap<>();
        put(renames, "com.amazonaws.qapps#Action", "com.amazonaws.qapps#PermissionOutputActionEnum");
        put(renames, "com.amazonaws.qapps#UserType", "com.amazonaws.qapps#PrincipalOutputUserTypeEnum");
        return new RenameShapesToC2jNames("QApps", renames);
    }

    static RenameShapesToC2jNames mediaLive() {
        Map<ShapeId, ShapeId> renames = new LinkedHashMap<>();
        put(renames, "com.amazonaws.medialive#UpdateNodeStateShape", "com.amazonaws.medialive.sdk#UpdateNodeState");
        return new RenameShapesToC2jNames("MediaLive", renames);
    }

    static RenameShapesToC2jNames pinpoint() {
        Map<ShapeId, ShapeId> renames = new LinkedHashMap<>();
        put(renames, "com.amazonaws.pinpoint#CreateRecommenderConfigurationShape",
            "com.amazonaws.pinpoint.sdk#CreateRecommenderConfiguration");
        put(renames, "com.amazonaws.pinpoint#UpdateRecommenderConfigurationShape",
            "com.amazonaws.pinpoint.sdk#UpdateRecommenderConfiguration");
        return new RenameShapesToC2jNames("Pinpoint", renames);
    }

    static RenameShapesToC2jNames bedrockAgentCoreControl() {
        Map<ShapeId, ShapeId> renames = new LinkedHashMap<>();
        put(renames, "com.amazonaws.bedrockagentcorecontrol#GatewayApiKeyCredentialProvider",
            "com.amazonaws.bedrockagentcorecontrol.sdk#ApiKeyCredentialProvider");
        return new RenameShapesToC2jNames("Bedrock AgentCore Control", renames);
    }

    @Override
    public Set<String> sdkIds() {
        return Collections.singleton(sdkId);
    }

    @Override
    public Model preprocess(Model model, CustomizationConfig config) {
        for (ShapeId from : renames.keySet()) {
            if (!model.getShape(from).isPresent()) {
                throw new IllegalStateException(from + " is not in the " + sdkId + " model. If the model now uses the "
                                                + "C2J name, remove this rename.");
            }
        }
        return ModelTransformer.create().renameShapes(model, renames);
    }

    private static void put(Map<ShapeId, ShapeId> renames, String from, String to) {
        renames.put(ShapeId.from(from), ShapeId.from(to));
    }
}
