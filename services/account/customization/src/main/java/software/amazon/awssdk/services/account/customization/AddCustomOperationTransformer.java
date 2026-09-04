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

import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;

/**
 * A {@code smithy-build} transform that adds a synthetic {@code MyCustomOperation} to a service.
 *
 * <p>This exists to demonstrate applying a service-specific customization as real Java code rather than as
 * {@code customization.config} entries. It is referenced by name from {@code smithy-build.json}:
 *
 * <pre>
 * "transforms": [
 *     { "name": "addAccountCustomOperation" }
 * ]
 * </pre>
 *
 * <p>The transform accepts an optional {@code service} argument naming the service to modify. When omitted, the
 * model must contain exactly one service shape.
 */
@SdkInternalApi
public final class AddCustomOperationTransformer implements ProjectionTransformer {
    static final String NAME = "addAccountCustomOperation";

    private static final String OPERATION_NAME = "MyCustomOperation";
    private static final String MEMBER_NAME = "message";
    private static final ShapeId STRING_TARGET = ShapeId.from("smithy.api#String");

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Model transform(TransformContext context) {
        Model model = context.getModel();
        ServiceShape service = resolveService(context, model);
        String namespace = service.getId().getNamespace();

        StructureShape input = structure(namespace, OPERATION_NAME + "Request", true);
        StructureShape output = structure(namespace, OPERATION_NAME + "Response", false);

        OperationShape operation =
            OperationShape.builder()
                          .id(ShapeId.fromParts(namespace, OPERATION_NAME))
                          .input(input.getId())
                          .output(output.getId())
                          .addTrait(HttpTrait.builder()
                                             .method("POST")
                                             .uri(UriPattern.parse("/myCustomOperation"))
                                             .code(200)
                                             .build())
                          .addTrait(new DocumentationTrait(
                              "An operation added at build time by a custom Smithy transform."))
                          .build();

        // Adding the updated service replaces the existing shape, because shapes are keyed by ID.
        ServiceShape updatedService = service.toBuilder()
                                             .addOperation(operation.getId())
                                             .build();

        return model.toBuilder()
                    .addShapes(input, output, operation)
                    .addShape(updatedService)
                    .build();
    }

    private static StructureShape structure(String namespace, String name, boolean isInput) {
        StructureShape.Builder builder = StructureShape.builder()
                                                       .id(ShapeId.fromParts(namespace, name))
                                                       .addMember(MEMBER_NAME, STRING_TARGET);
        return isInput ? builder.addTrait(new InputTrait()).build()
                       : builder.addTrait(new OutputTrait()).build();
    }

    private static ServiceShape resolveService(TransformContext context, Model model) {
        String configured = context.getSettings().getStringMemberOrDefault("service", null);

        if (configured != null) {
            ShapeId serviceId = ShapeId.from(configured);
            return model.getShape(serviceId)
                        .flatMap(shape -> shape.asServiceShape())
                        .orElseThrow(() -> new IllegalArgumentException(String.format(
                            "The '%s' transform was configured with service '%s', which is not a service shape in "
                            + "the model.", NAME, serviceId)));
        }

        Set<ServiceShape> services = model.getServiceShapes();
        if (services.size() != 1) {
            throw new IllegalArgumentException(String.format(
                "The '%s' transform found %d service shapes in the model, so the target service must be given as the "
                + "transform's 'service' argument.", NAME, services.size()));
        }
        return services.iterator().next();
    }
}
