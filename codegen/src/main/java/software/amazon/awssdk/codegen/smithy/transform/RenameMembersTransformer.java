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
import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.expectNamedMembersContainer;
import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.memberId;
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
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.HostLabelTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Renames structure and union members. Smithy's built-in {@code renameShapes} ignores member IDs.
 *
 * <pre>{@code
 * {
 *   "name": "renameMembers",
 *   "args": {
 *     "renamed": {
 *       "ns#Result$RequestId": {
 *         "name": "directoryRequestId",
 *         "traits": { "smithy.api#jsonName": "RequestId" }
 *       }
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>{@code traits} are added to the renamed member, replacing any trait with the same ID. They carry the wire name
 * when the old name was the wire name, so the rename changes only the generated Java. A renamed member moves to the
 * end of its container, as it does on the C2J path. URI and host-prefix labels bound to the member are renamed with
 * it, because Smithy requires a label to match its member's name.
 */
@SdkInternalApi
public final class RenameMembersTransformer implements ProjectionTransformer {

    public static final String NAME = "renameMembers";

    private static final String RENAMED = "renamed";
    private static final String MEMBER_NAME = "name";
    private static final String TRAITS = "traits";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Model transform(TransformContext context) {
        ObjectNode settings = context.getSettings();
        settings.expectNoAdditionalProperties(Collections.singleton(RENAMED));

        Model model = context.getModel();
        for (Map.Entry<StringNode, Node> entry : settings.expectObjectMember(RENAMED).getMembers().entrySet()) {
            ObjectNode rename = entry.getValue().expectObjectNode();
            rename.expectNoAdditionalProperties(Arrays.asList(MEMBER_NAME, TRAITS));
            model = rename(context.getTransformer(),
                           model,
                           memberId(NAME, entry.getKey().getValue()),
                           rename.expectStringMember(MEMBER_NAME).getValue(),
                           rename.getObjectMember(TRAITS).orElse(Node.objectNode()));
        }
        return model;
    }

    private static Model rename(ModelTransformer transformer, Model model, ShapeId oldId, String newName,
                                ObjectNode traitsNode) {
        MemberShape member = expectMember(NAME, model, oldId);
        Shape container = expectNamedMembersContainer(NAME, model, oldId.withoutMember());
        String oldName = member.getMemberName();

        if (!ShapeId.isValidIdentifier(newName)) {
            throw error(NAME, "'" + newName + "' is not a valid member name");
        }
        for (String existing : container.getMemberNames()) {
            if (!existing.equals(oldName) && existing.equalsIgnoreCase(newName)) {
                throw error(NAME, "'" + container.getId() + "' already has a member named '" + existing + "'");
            }
        }

        ShapeId newId = oldId.withMember(newName);
        MemberShape renamed = member.toBuilder()
                                    .id(newId)
                                    .addTraits(traits(NAME, newId, traitsNode))
                                    .build();

        List<MemberShape> members = new ArrayList<>();
        for (MemberShape existing : container.members()) {
            if (!existing.getId().equals(oldId)) {
                members.add(existing);
            }
        }
        members.add(renamed);

        List<Shape> replacements = new ArrayList<>();
        replacements.add(withMembers(container, members));
        replacements.addAll(relabelOperations(model, member, container.getId(), oldName, newName));
        return transformer.replaceShapes(model, replacements);
    }

    private static List<Shape> relabelOperations(Model model, MemberShape member, ShapeId inputId,
                                                 String oldName, String newName) {
        boolean uriLabel = member.hasTrait(HttpLabelTrait.class);
        boolean hostLabel = member.hasTrait(HostLabelTrait.class);
        if (!uriLabel && !hostLabel) {
            return Collections.emptyList();
        }

        List<Shape> operations = new ArrayList<>();
        for (OperationShape operation : model.getOperationShapes()) {
            if (!operation.getInputShape().equals(inputId)) {
                continue;
            }
            OperationShape.Builder builder = operation.toBuilder();
            if (uriLabel && operation.hasTrait(HttpTrait.class)) {
                HttpTrait http = operation.expectTrait(HttpTrait.class);
                String uri = relabel(http.getUri().toString(), oldName, newName);
                builder.addTrait(http.toBuilder().uri(UriPattern.parse(uri)).build());
            }
            if (hostLabel && operation.hasTrait(EndpointTrait.class)) {
                EndpointTrait endpoint = operation.expectTrait(EndpointTrait.class);
                String hostPrefix = relabel(endpoint.getHostPrefix().toString(), oldName, newName);
                builder.addTrait(endpoint.toBuilder().hostPrefix(hostPrefix).build());
            }
            operations.add(builder.build());
        }
        return operations;
    }

    private static String relabel(String pattern, String oldName, String newName) {
        return pattern.replace("{" + oldName + "}", "{" + newName + "}")
                      .replace("{" + oldName + "+}", "{" + newName + "+}");
    }
}
