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

package software.amazon.awssdk.services.account.customization;

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.JsonNameTrait;

/**
 * Renames structure members, preserving the member's target and traits.
 *
 * <p>Smithy's built-in {@code renameShapes} transform only renames shapes; member IDs passed to it are silently
 * ignored. This fills that gap, and is the Smithy equivalent of the SDK's {@code shapeModifiers} /
 * {@code emitPropertyName} customization.
 *
 * <p>Renaming a member changes the name on the wire as well as in Java, because for protocols such as
 * {@code restJson1} the member name is the serialized key. To match {@code emitPropertyName}, which only affects
 * generated Java, an {@code @jsonName} trait carrying the original name is added so the wire format is unchanged.
 * A member that already declares {@code @jsonName} keeps it.
 *
 * <p>Only JSON protocols are handled. A service using {@code restXml} would need the equivalent
 * {@code @xmlName} treatment.
 *
 * <pre>
 * {
 *     "name": "renameMembers",
 *     "args": {
 *         "renamed": {
 *             "com.amazonaws.account#GetAccountInformationResponse$AccountName":
 *                 "com.amazonaws.account#GetAccountInformationResponse$Name"
 *         }
 *     }
 * }
 * </pre>
 */
@SdkInternalApi
public final class RenameMembersTransformer implements ProjectionTransformer {
    static final String NAME = "renameMembers";

    private static final String RENAMED = "renamed";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Model transform(TransformContext context) {
        Model model = context.getModel();

        // Renames are grouped by containing shape so that several members of one structure can be renamed together.
        Map<ShapeId, Map<String, String>> renamesByContainer = new LinkedHashMap<>();
        context.getSettings()
               .expectObjectMember(RENAMED)
               .getStringMap()
               .forEach((from, to) -> {
                   ShapeId fromId = ShapeId.from(from);
                   ShapeId toId = ShapeId.from(to.expectStringNode().getValue());
                   validate(fromId, toId);
                   renamesByContainer.computeIfAbsent(fromId.withoutMember(), id -> new LinkedHashMap<>())
                                     .put(fromId.getMember().get(), toId.getMember().get());
               });

        Model.Builder builder = model.toBuilder();
        renamesByContainer.forEach((containerId, renames) -> applyRenames(model, builder, containerId, renames));
        return builder.build();
    }

    private static void applyRenames(Model model, Model.Builder builder, ShapeId containerId,
                                     Map<String, String> renames) {
        Shape shape = model.getShape(containerId)
                           .orElseThrow(() -> new IllegalArgumentException(String.format(
                               "The '%s' transform was asked to rename members of '%s', which is not in the model.",
                               NAME, containerId)));

        if (!shape.isStructureShape()) {
            throw new IllegalArgumentException(String.format(
                "The '%s' transform can only rename members of structures, but '%s' is a %s.",
                NAME, containerId, shape.getType()));
        }

        StructureShape container = shape.asStructureShape().get();
        StructureShape.Builder containerBuilder = container.toBuilder();

        renames.forEach((oldName, newName) -> {
            MemberShape member = container.getMember(oldName)
                                          .orElseThrow(() -> new IllegalArgumentException(String.format(
                                              "The '%s' transform was asked to rename member '%s' of '%s', which "
                                              + "does not exist. Members present: %s",
                                              NAME, oldName, containerId, container.getMemberNames())));

            MemberShape.Builder renamed = member.toBuilder();
            renamed.id(containerId.withMember(newName));

            // Keep the serialized name stable; without this the rename would also change the wire format.
            if (!member.hasTrait(JsonNameTrait.class)) {
                renamed.addTrait(new JsonNameTrait(oldName));
            }

            containerBuilder.removeMember(oldName);
            containerBuilder.addMember(renamed.build());

            // Drop the old member shape, which would otherwise linger in the shape map.
            builder.removeShape(member.getId());
        });

        builder.addShape(containerBuilder.build());
    }

    private static void validate(ShapeId from, ShapeId to) {
        if (!from.hasMember() || !to.hasMember()) {
            throw new IllegalArgumentException(String.format(
                "The '%s' transform requires member shape IDs of the form 'namespace#Shape$member', but got '%s' -> "
                + "'%s'.", NAME, from, to));
        }
        if (!from.withoutMember().equals(to.withoutMember())) {
            throw new IllegalArgumentException(String.format(
                "The '%s' transform cannot move a member between shapes: '%s' -> '%s'.", NAME, from, to));
        }
    }
}
