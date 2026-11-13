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
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.smithy.customization.SmithyServiceCustomization;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Gives {@code PermissionInput.action} its own copy of the enum it shares with {@code PermissionOutput.action}, so it
 * keeps its own enum class. Copies the current target, so it works before or after {@link RenameShapesToC2jNames}.
 */
@SdkInternalApi
final class QAppsSplitActionEnum implements SmithyServiceCustomization {
    private static final ShapeId INPUT_ACTION = ShapeId.from("com.amazonaws.qapps#PermissionInput$action");
    private static final ShapeId INPUT_ACTION_ENUM = ShapeId.from("com.amazonaws.qapps#PermissionInputActionEnum");

    @Override
    public Set<String> sdkIds() {
        return Collections.singleton("QApps");
    }

    @Override
    public Model preprocess(Model model, CustomizationConfig config) {
        MemberShape input = model.getShape(INPUT_ACTION).flatMap(Shape::asMemberShape).orElseThrow(
            () -> new IllegalStateException(INPUT_ACTION + " is not in the model, so its enum cannot be split"));
        if (input.getTarget().equals(INPUT_ACTION_ENUM)) {
            return model;
        }
        Shape copy = Shape.shapeToBuilder(model.expectShape(input.getTarget())).id(INPUT_ACTION_ENUM).build();
        return ModelTransformer.create().replaceShapes(
            model, Arrays.asList(copy, input.toBuilder().target(INPUT_ACTION_ENUM).build()));
    }
}
