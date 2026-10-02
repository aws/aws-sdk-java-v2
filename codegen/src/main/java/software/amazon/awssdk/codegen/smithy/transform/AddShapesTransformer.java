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
import static software.amazon.awssdk.codegen.smithy.transform.TransformSupport.shapeId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;

/**
 * Adds new shapes, written as Smithy JSON AST shape definitions keyed by absolute shape ID.
 *
 * <pre>{@code
 * {
 *   "name": "addShapes",
 *   "args": {
 *     "shapes": {
 *       "ns#SdkPartType": {
 *         "type": "enum",
 *         "members": {
 *           "DEFAULT": { "target": "smithy.api#Unit", "traits": { "smithy.api#enumValue": "DEFAULT" } },
 *           "LAST": { "target": "smithy.api#Unit", "traits": { "smithy.api#enumValue": "LAST" } }
 *         }
 *       }
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>A shape that already exists is rejected rather than replaced.
 */
@SdkInternalApi
public final class AddShapesTransformer implements ProjectionTransformer {

    public static final String NAME = "addShapes";

    private static final String SHAPES = "shapes";
    private static final String PRELUDE_NAMESPACE = "smithy.api";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Model transform(TransformContext context) {
        ObjectNode settings = context.getSettings();
        settings.expectNoAdditionalProperties(Collections.singleton(SHAPES));
        ObjectNode shapes = settings.expectObjectMember(SHAPES);

        Model model = context.getModel();
        for (StringNode key : shapes.getMembers().keySet()) {
            ShapeId id = shapeId(NAME, key.getValue());
            if (model.getShape(id).isPresent()) {
                throw error(NAME, "shape '" + id + "' already exists in the model");
            }
        }
        return context.getTransformer().replaceShapes(model, parse(shapes));
    }

    /**
     * Loads only the new definitions, without re-validating the rest of the model. Their references to existing
     * shapes are validated when smithy-build validates the projected model.
     */
    private static List<Shape> parse(ObjectNode shapes) {
        ObjectNode document = Node.objectNodeBuilder()
                                  .withMember("smithy", "2.0")
                                  .withMember(SHAPES, shapes)
                                  .build();
        ValidatedResult<Model> result = Model.assembler(AddShapesTransformer.class.getClassLoader())
                                             .addDocumentNode(document)
                                             .disableValidation()
                                             .assemble();
        String errors = result.getValidationEvents().stream()
                              .filter(e -> e.getSeverity() == Severity.ERROR || e.getSeverity() == Severity.DANGER)
                              .map(e -> e.getShapeId().map(id -> id + ": ").orElse("") + e.getMessage())
                              .collect(Collectors.joining("; "));
        if (!errors.isEmpty() || !result.getResult().isPresent()) {
            throw error(NAME, "invalid shape definition: " + errors);
        }

        List<Shape> added = new ArrayList<>();
        for (Map.Entry<StringNode, Node> entry : shapes.getMembers().entrySet()) {
            ShapeId id = ShapeId.from(entry.getKey().getValue());
            if (PRELUDE_NAMESPACE.equals(id.getNamespace())) {
                throw error(NAME, "cannot add '" + id + "' to the prelude namespace");
            }
            Shape shape = result.getResult().get().expectShape(id);
            added.add(shape);
            added.addAll(shape.members());
        }
        return added;
    }
}
