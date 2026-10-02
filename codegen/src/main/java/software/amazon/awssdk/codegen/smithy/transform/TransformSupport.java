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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

/**
 * Argument parsing and shape rebuilding shared by the codegen transforms. Every failure names the transform, so a
 * misconfigured {@code smithy-build.json} points at the entry to fix.
 */
@SdkInternalApi
final class TransformSupport {

    private static final TraitFactory TRAIT_FACTORY =
        TraitFactory.createServiceFactory(TransformSupport.class.getClassLoader());

    private TransformSupport() {
    }

    static IllegalArgumentException error(String transform, String message) {
        return new IllegalArgumentException("Transform '" + transform + "': " + message);
    }

    static ShapeId shapeId(String transform, String value) {
        ShapeId id = parseId(transform, value);
        if (id.hasMember()) {
            throw error(transform, "expected a shape ID without a member, but got '" + value + "'");
        }
        return id;
    }

    static ShapeId memberId(String transform, String value) {
        ShapeId id = parseId(transform, value);
        if (!id.hasMember()) {
            throw error(transform, "expected a member ID such as 'ns#Shape$member', but got '" + value + "'");
        }
        return id;
    }

    static Shape expectShape(String transform, Model model, ShapeId id) {
        return model.getShape(id).orElseThrow(() -> error(transform, "shape '" + id + "' does not exist in the model"));
    }

    static MemberShape expectMember(String transform, Model model, ShapeId id) {
        Shape shape = expectShape(transform, model, id);
        return shape.asMemberShape().orElseThrow(() -> error(transform, "'" + id + "' is not a member"));
    }

    /**
     * Returns the structure or union that can own named members, rejecting shapes that use mixins because their
     * inherited members cannot be renamed or reordered in place.
     */
    static Shape expectNamedMembersContainer(String transform, Model model, ShapeId id) {
        Shape container = expectShape(transform, model, id);
        if (!container.isStructureShape() && !container.isUnionShape()) {
            throw error(transform, "'" + id + "' is a " + container.getType() + "; only structures and unions are supported");
        }
        if (!container.getMixins().isEmpty()) {
            throw error(transform, "'" + id + "' uses mixins, which are not supported");
        }
        return container;
    }

    /**
     * Rebuilds a structure or union with exactly the given members, in the given order.
     */
    static Shape withMembers(Shape container, Collection<MemberShape> members) {
        if (container.isStructureShape()) {
            StructureShape.Builder builder = container.asStructureShape().get().toBuilder();
            builder.clearMembers();
            members.forEach(builder::addMember);
            return builder.build();
        }
        UnionShape.Builder builder = container.asUnionShape().get().toBuilder();
        builder.clearMembers();
        members.forEach(builder::addMember);
        return builder.build();
    }

    /**
     * Creates traits from a JSON object keyed by absolute trait shape ID, the same form a Smithy JSON AST uses.
     */
    static List<Trait> traits(String transform, ShapeId target, ObjectNode traitsNode) {
        List<Trait> traits = new ArrayList<>();
        for (Map.Entry<StringNode, Node> entry : traitsNode.getMembers().entrySet()) {
            ShapeId traitId = shapeId(transform, entry.getKey().getValue());
            Trait trait = TRAIT_FACTORY.createTrait(traitId, target, entry.getValue())
                                       .orElseThrow(() -> error(transform, "unknown trait '" + traitId + "'"));
            traits.add(trait);
        }
        return traits;
    }

    private static ShapeId parseId(String transform, String value) {
        try {
            return ShapeId.from(value);
        } catch (ShapeIdSyntaxException e) {
            throw error(transform, "'" + value + "' is not an absolute shape ID");
        }
    }
}
