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
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.smithy.customization.SmithyServiceCustomization;

/**
 * The customizations that keep class names the Smithy models change.
 */
@SdkInternalApi
public final class ShapeNameCustomizations {

    private ShapeNameCustomizations() {
    }

    public static List<SmithyServiceCustomization> all() {
        return Arrays.asList(new QAppsSplitActionEnum(),
                             RenameShapesToC2jNames.deadline(),
                             RenameShapesToC2jNames.notifications(),
                             RenameShapesToC2jNames.qApps(),
                             RenameShapesToC2jNames.mediaLive(),
                             RenameShapesToC2jNames.pinpoint(),
                             RenameShapesToC2jNames.bedrockAgentCoreControl());
    }
}
