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

package software.amazon.awssdk.codegen.smithy.customization.processors;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.smithy.customization.ShapeIdResolver;
import software.amazon.awssdk.codegen.smithy.customization.SmithyCustomizationProcessor;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Smithy equivalent of {@link software.amazon.awssdk.codegen.customization.processors.RenameShapesProcessor}.
 *
 * <p>This is a Category B processor (direct Smithy equivalent). It renames shapes in the
 * Smithy model using {@link ModelTransformer#renameShapes(Model, Map)}, which automatically
 * updates all references to the renamed shapes (member targets, operation inputs/outputs/errors).
 */
public final class RenameShapesProcessor implements SmithyCustomizationProcessor {

    private final Map<String, String> renameShapes;

    public RenameShapesProcessor(Map<String, String> renameShapes) {
        this.renameShapes = renameShapes;
    }

    @Override
    public Model preprocess(Model model, ServiceShape service) {
        if (renameShapes == null || renameShapes.isEmpty()) {
            return model;
        }

        String namespace = ShapeIdResolver.namespace(service);
        Map<ShapeId, ShapeId> shapeIdMap = new HashMap<>();

        for (Map.Entry<String, String> entry : renameShapes.entrySet()) {
            ShapeId oldId = ShapeIdResolver.resolve(model, service, entry.getKey());
            ShapeId newId = ShapeId.from(namespace + "#" + entry.getValue());

            if (model.getShape(newId).isPresent()) {
                throw new IllegalStateException(
                    String.format("Cannot rename shape '%s' to '%s': a shape with name '%s' already exists "
                                  + "in namespace '%s'.", entry.getKey(), entry.getValue(),
                                  entry.getValue(), namespace));
            }

            shapeIdMap.put(oldId, newId);
        }

        return ModelTransformer.create().renameShapes(model, shapeIdMap);
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // no-op
    }
}
