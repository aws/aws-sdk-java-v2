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
import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.expectNamedMembersContainer;
import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.expectShape;
import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.memberId;
import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.shapeId;
import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.traits;
import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.withMembers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

/**
 * Adds members to existing structures and unions.
 *
 * <pre>{@code
 * {
 *   "name": "injectMembers",
 *   "args": {
 *     "members": {
 *       "ns#GetObjectOutput$ExpiresString": {
 *         "target": "ns#Expiration",
 *         "traits": {
 *           "smithy.api#documentation": "The date and time at which the object is no longer cacheable.",
 *           "smithy.api#httpHeader": "Expires"
 *         }
 *       }
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>An injected member is appended to its container, as it is on the C2J path. Its target must already exist, so a
 * shape added by {@code addShapes} must be added earlier in the transform list.
 */
@SdkInternalApi
public final class InjectMembersTransformer implements ProjectionTransformer {

    public static final String NAME = "injectMembers";

    private static final String MEMBERS = "members";
    private static final String TARGET = "target";
    private static final String TRAITS = "traits";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Model transform(TransformContext context) {
        ObjectNode settings = context.getSettings();
        settings.expectNoAdditionalProperties(Collections.singleton(MEMBERS));

        Model model = context.getModel();
        for (Map.Entry<StringNode, Node> entry : settings.expectObjectMember(MEMBERS).getMembers().entrySet()) {
            ObjectNode definition = entry.getValue().expectObjectNode();
            definition.expectNoAdditionalProperties(Arrays.asList(TARGET, TRAITS));

            ShapeId id = memberId(NAME, entry.getKey().getValue());
            ShapeId target = shapeId(NAME, definition.expectStringMember(TARGET).getValue());
            ObjectNode traitsNode = definition.getObjectMember(TRAITS).orElse(Node.objectNode());
            model = context.getTransformer().replaceShapes(model, Collections.singleton(
                inject(model, id, target, traitsNode)));
        }
        return model;
    }

    private static Shape inject(Model model, ShapeId id, ShapeId target, ObjectNode traitsNode) {
        Shape container = expectNamedMembersContainer(NAME, model, id.withoutMember());
        String name = id.getMember().get();
        for (String existing : container.getMemberNames()) {
            if (existing.equalsIgnoreCase(name)) {
                throw error(NAME, "'" + container.getId() + "' already has a member named '" + existing + "'");
            }
        }
        Shape targetShape = expectShape(NAME, model, target);
        if (targetShape.isMemberShape() || targetShape.isOperationShape() || targetShape.isServiceShape()
            || targetShape.isResourceShape()) {
            throw error(NAME, "'" + target + "' is a " + targetShape.getType() + " and cannot be a member target");
        }

        MemberShape member = MemberShape.builder()
                                        .id(id)
                                        .target(target)
                                        .addTraits(traits(NAME, id, traitsNode))
                                        .build();
        List<MemberShape> members = new ArrayList<>(container.members());
        members.add(member);
        return withMembers(container, members);
    }
}
