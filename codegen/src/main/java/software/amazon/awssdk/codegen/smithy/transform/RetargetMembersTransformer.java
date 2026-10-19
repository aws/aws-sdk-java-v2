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

package software.amazon.awssdk.codegen.smithy.transform;

import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.error;
import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.expectMember;
import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.expectShape;
import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.memberId;
import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.rejectMixins;
import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.shapeId;

import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.MixinTrait;

/**
 * Changes the target of individual members, keeping their traits.
 *
 * <pre>{@code
 * {
 *   "name": "retargetMembers",
 *   "args": {
 *     "targets": { "ns#Spend$Amount": "smithy.api#BigDecimal" }
 *   }
 * }
 * }</pre>
 *
 * <p>Replaces {@code shapeModifiers.<shape>.modify[].<member>.emitAsType} and
 * {@code shapeSubstitutions.<shape>.emitAsShape} or {@code emitAsType} in {@code customization.config}, applied by
 * {@code ShapeModifiersProcessor} and {@code ShapeSubstitutionsProcessor} on the C2J path. For {@code emitAsType}, the
 * scalar target shape is added first with {@code addShapes}. {@code shapeSubstitutions.emitFromMember} and
 * {@code skipMarshallPathForShapes} change only the intermediate model and are not handled here.
 */
@SdkInternalApi
public final class RetargetMembersTransformer implements ProjectionTransformer {

    public static final String NAME = "retargetMembers";

    private static final String TARGETS = "targets";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Model transform(TransformContext context) {
        ObjectNode settings = context.getSettings();
        settings.expectNoAdditionalProperties(Collections.singleton(TARGETS));

        Model model = context.getModel();
        for (Map.Entry<StringNode, Node> entry : settings.expectObjectMember(TARGETS).getMembers().entrySet()) {
            ShapeId id = memberId(NAME, entry.getKey().getValue());
            ShapeId target = shapeId(NAME, entry.getValue().expectStringNode().getValue());

            MemberShape member = expectMember(NAME, model, id);
            rejectMixins(NAME, model, id, member);
            Shape targetShape = expectShape(NAME, model, target);
            if (targetShape.isMemberShape() || targetShape.isOperationShape() || targetShape.isServiceShape()
                || targetShape.isResourceShape()) {
                throw error(NAME, "'" + target + "' is a " + targetShape.getType() + " and cannot be a member target");
            }
            if (targetShape.hasTrait(MixinTrait.class)) {
                throw error(NAME, "'" + target + "' is a mixin and cannot be a member target");
            }
            model = context.getTransformer().replaceShapes(model, Collections.singleton(
                member.toBuilder().target(target).build()));
        }
        return model;
    }
}
