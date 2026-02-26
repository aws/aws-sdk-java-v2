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

import java.util.Optional;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.smithy.customization.SmithyCustomizationProcessor;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpPayloadTrait;

/**
 * Smithy equivalent of
 * {@link software.amazon.awssdk.codegen.customization.processors.ExplicitStringPayloadQueryProtocolProcessor}.
 *
 * <p>This is a Category B processor (direct Smithy equivalent). Operations with explicit String
 * payloads ({@code @httpPayload} on a string member) are not supported for services using Query
 * protocols ({@code aws.protocols#awsQuery} or {@code aws.protocols#ec2Query}). This processor
 * validates the model at build time and throws a {@link RuntimeException} if an invalid
 * combination is detected.
 */
public final class ExplicitStringPayloadQueryProtocolProcessor implements SmithyCustomizationProcessor {

    private static final ShapeId AWS_QUERY_TRAIT_ID = ShapeId.from("aws.protocols#awsQuery");
    private static final ShapeId EC2_QUERY_TRAIT_ID = ShapeId.from("aws.protocols#ec2Query");

    @Override
    public Model preprocess(Model model, ServiceShape service) {
        if (!isQueryProtocol(service)) {
            return model;
        }

        for (ShapeId operationId : service.getOperations()) {
            OperationShape operation = model.expectShape(operationId, OperationShape.class);

            Optional<ShapeId> inputId = operation.getInput();
            if (inputId.isPresent() && isExplicitStringPayload(model, inputId.get())) {
                throw new RuntimeException(
                    "Operations with explicit String payloads are not supported for Query "
                    + "protocols. Unsupported operation: " + operationId.getName());
            }

            Optional<ShapeId> outputId = operation.getOutput();
            if (outputId.isPresent() && isExplicitStringPayload(model, outputId.get())) {
                throw new RuntimeException(
                    "Operations with explicit String payloads are not supported for Query "
                    + "protocols. Unsupported operation: " + operationId.getName());
            }
        }

        return model;
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // no-op
    }

    private static boolean isQueryProtocol(ServiceShape service) {
        return service.hasTrait(AWS_QUERY_TRAIT_ID) || service.hasTrait(EC2_QUERY_TRAIT_ID);
    }

    /**
     * Checks whether the given structure shape has a member with the {@code @httpPayload} trait
     * whose target is a string shape.
     */
    private static boolean isExplicitStringPayload(Model model, ShapeId structureId) {
        Shape shape = model.getShape(structureId).orElse(null);
        if (shape == null || !shape.isStructureShape()) {
            return false;
        }

        StructureShape structure = shape.asStructureShape().get();
        for (MemberShape member : structure.getAllMembers().values()) {
            if (member.hasTrait(HttpPayloadTrait.class)) {
                Shape targetShape = model.getShape(member.getTarget()).orElse(null);
                return targetShape != null && targetShape.isStringShape();
            }
        }

        return false;
    }
}
