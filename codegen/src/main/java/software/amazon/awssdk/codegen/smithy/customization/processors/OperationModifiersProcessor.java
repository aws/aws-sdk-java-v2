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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.codegen.model.config.customization.OperationModifier;
import software.amazon.awssdk.codegen.model.config.customization.SmithyOperationModifier;
import software.amazon.awssdk.codegen.smithy.customization.ShapeIdResolver;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Smithy equivalent of
 * {@link software.amazon.awssdk.codegen.customization.processors.OperationModifiersProcessor}.
 *
 * <p>
 * This is a Category C processor (dual-config pattern). It excludes operations
 * or wraps
 * operation results in the Smithy model. When {@code exclude} is true for an
 * operation, the
 * operation shape is removed from the model. When {@code useWrappingResult} is
 * true, a new
 * wrapper structure shape is created containing a member that targets the
 * specified wrapped
 * result shape, and the operation's output is updated to point to the wrapper.
 *
 * <p>
 * Follows the dual-config pattern: accepts both old C2J
 * {@link OperationModifier} map and new
 * Smithy-native {@link SmithyOperationModifier} map. Old C2J simple names for
 * {@code wrappedResultShape} are resolved to full ShapeId strings via
 * {@link ShapeIdResolver}.
 */
public class OperationModifiersProcessor
        extends AbstractDualConfigProcessor<Map<String, OperationModifier>, Map<String, SmithyOperationModifier>> {

    public OperationModifiersProcessor(Map<String, OperationModifier> oldConfig,
            Map<String, SmithyOperationModifier> newConfig) {
        super(oldConfig, newConfig, "operationModifiers", "smithyOperationModifiers");
    }

    @Override
    protected boolean isSet(Object config) {
        if (config instanceof Map) {
            return !((Map<?, ?>) config).isEmpty();
        }
        return config != null;
    }

    // -----------------------------------------------------------------------
    // convertOldToNew: C2J OperationModifier → SmithyOperationModifier
    // -----------------------------------------------------------------------

    @Override
    protected Map<String, SmithyOperationModifier> convertOldToNew(Map<String, OperationModifier> old,
            Model model,
            ServiceShape service) {
        Map<String, SmithyOperationModifier> result = new LinkedHashMap<>();
        for (Map.Entry<String, OperationModifier> entry : old.entrySet()) {
            SmithyOperationModifier smithy = new SmithyOperationModifier();
            OperationModifier c2j = entry.getValue();
            smithy.setExclude(c2j.isExclude());
            smithy.setUseWrappingResult(c2j.isUseWrappingResult());
            smithy.setWrappedResultMember(c2j.getWrappedResultMember());
            // Convert simple name to full ShapeId
            if (c2j.getWrappedResultShape() != null) {
                ShapeId resolved = ShapeIdResolver.resolve(model, service, c2j.getWrappedResultShape());
                smithy.setWrappedResultShape(resolved.toString());
            }
            result.put(ShapeIdResolver.resolve(model, service, entry.getKey()).toString(), smithy);
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // applySmithyLogic: exclude operations and wrap results
    // -----------------------------------------------------------------------

    @Override
    protected Model applySmithyLogic(Model model, ServiceShape service,
            Map<String, SmithyOperationModifier> config) {
        Model current = model;
        for (Map.Entry<String, SmithyOperationModifier> entry : config.entrySet()) {
            String operationName = entry.getKey();
            SmithyOperationModifier modifier = entry.getValue();

            if (modifier.isExclude()) {
                current = excludeOperation(current, service, operationName);
            } else if (modifier.isUseWrappingResult()) {
                current = wrapOperationResult(current, service, operationName, modifier);
            }
        }
        return current;
    }

    private Model excludeOperation(Model model, ServiceShape service, String operationName) {
        ShapeId opId = ShapeIdResolver.resolve(model, service, operationName);
        return ModelTransformer.create().removeShapes(model, Collections.singleton(
                model.expectShape(opId)));
    }

    private Model wrapOperationResult(Model model, ServiceShape service,
            String operationName, SmithyOperationModifier modifier) {
        String namespace = ShapeIdResolver.namespace(service);
        ShapeId opId = ShapeIdResolver.resolve(model, service, operationName);
        OperationShape operation = model.expectShape(opId, OperationShape.class);

        // Create wrapper structure shape
        String simpleOpName = ShapeIdResolver.toShapeName(operationName);
        String wrapperName = simpleOpName + "Response";
        ShapeId wrapperId = ShapeId.from(namespace + "#" + wrapperName);
        String wrappedMemberName = modifier.getWrappedResultMember();

        // For SmithyOperationModifier, wrappedResultShape is already a full ShapeId
        ShapeId wrappedShapeId = ShapeId.from(modifier.getWrappedResultShape());

        MemberShape member = MemberShape.builder()
                .id(wrapperId.withMember(wrappedMemberName))
                .target(wrappedShapeId)
                .build();

        StructureShape wrapperShape = StructureShape.builder()
                .id(wrapperId)
                .addMember(member)
                .build();

        OperationShape updatedOp = operation.toBuilder()
                .output(wrapperId)
                .build();

        return model.toBuilder()
                .addShape(wrapperShape)
                .addShape(updatedOp)
                .build();
    }
}
