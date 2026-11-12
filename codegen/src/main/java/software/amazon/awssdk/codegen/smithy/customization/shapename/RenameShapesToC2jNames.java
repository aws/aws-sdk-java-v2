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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
public final class RenameShapesToC2jNames implements SmithyServiceCustomization {
    private static final Set<String> SDK_IDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "deadline", "Notifications", "QApps", "MediaLive", "Pinpoint", "Bedrock AgentCore Control")));
    private static final Map<ShapeId, ShapeId> RENAMES = renames();

    @Override
    public Set<String> sdkIds() {
        return SDK_IDS;
    }

    @Override
    public Model preprocess(Model model, CustomizationConfig config) {
        String namespace = model.getServiceShapes().iterator().next().getId().getNamespace();
        Map<ShapeId, ShapeId> renamed = new LinkedHashMap<>();
        RENAMES.forEach((from, to) -> {
            if (from.getNamespace().equals(namespace)) {
                model.expectShape(from);
                renamed.put(from, to);
            }
        });
        return ModelTransformer.create().renameShapes(model, renamed);
    }

    private static Map<ShapeId, ShapeId> renames() {
        Map<ShapeId, ShapeId> renames = new LinkedHashMap<>();
        put(renames, "com.amazonaws.deadline#DeadlinePrincipalType", "com.amazonaws.deadline#PrincipalType");
        put(renames, "com.amazonaws.notifications#NotificationEventSchema",
            "com.amazonaws.notifications#NotificationEvent");
        put(renames, "com.amazonaws.qapps#Action", "com.amazonaws.qapps#PermissionOutputActionEnum");
        put(renames, "com.amazonaws.qapps#UserType", "com.amazonaws.qapps#PrincipalOutputUserTypeEnum");
        put(renames, "com.amazonaws.medialive#UpdateNodeStateShape", "com.amazonaws.medialive.sdk#UpdateNodeState");
        put(renames, "com.amazonaws.pinpoint#CreateRecommenderConfigurationShape",
            "com.amazonaws.pinpoint.sdk#CreateRecommenderConfiguration");
        put(renames, "com.amazonaws.pinpoint#UpdateRecommenderConfigurationShape",
            "com.amazonaws.pinpoint.sdk#UpdateRecommenderConfiguration");
        put(renames, "com.amazonaws.bedrockagentcorecontrol#GatewayApiKeyCredentialProvider",
            "com.amazonaws.bedrockagentcorecontrol.sdk#ApiKeyCredentialProvider");
        return Collections.unmodifiableMap(renames);
    }

    private static void put(Map<ShapeId, ShapeId> renames, String from, String to) {
        renames.put(ShapeId.from(from), ShapeId.from(to));
    }
}
