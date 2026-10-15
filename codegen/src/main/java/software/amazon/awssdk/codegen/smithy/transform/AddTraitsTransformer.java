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

import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.expectShape;
import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.traits;

import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Adds traits to existing shapes or members, replacing a trait with the same ID.
 *
 * <pre>{@code
 * {
 *   "name": "addTraits",
 *   "args": {
 *     "traits": {
 *       "ns#CopyObjectRequest$CopySource": { "smithy.api#deprecated": { "message": "Use SourceBucket instead." } }
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>A Smithy {@code apply} statement cannot do this when the model already carries the trait, because the two
 * values conflict instead of one replacing the other.
 */
@SdkInternalApi
public final class AddTraitsTransformer implements ProjectionTransformer {

    public static final String NAME = "addTraits";

    private static final String TRAITS = "traits";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Model transform(TransformContext context) {
        ObjectNode settings = context.getSettings();
        settings.expectNoAdditionalProperties(Collections.singleton(TRAITS));

        Model model = context.getModel();
        for (Map.Entry<StringNode, Node> entry : settings.expectObjectMember(TRAITS).getMembers().entrySet()) {
            ShapeId id = TransformSupport.parseShapeOrMemberId(NAME, entry.getKey().getValue());
            Shape shape = expectShape(NAME, model, id);
            AbstractShapeBuilder<?, ?> builder = Shape.shapeToBuilder(shape);
            builder.addTraits(traits(NAME, id, entry.getValue().expectObjectNode()));
            model = context.getTransformer().replaceShapes(model, Collections.singleton((Shape) builder.build()));
        }
        return model;
    }
}
