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

package software.amazon.awssdk.codegen.customization.processors;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.validation.ModelInvalidException;
import software.amazon.awssdk.codegen.validation.ValidationEntry;
import software.amazon.awssdk.codegen.validation.ValidationErrorId;
import software.amazon.awssdk.codegen.validation.ValidationErrorSeverity;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.TraitDefinition;

/**
 * The Smithy path's counterpart to the validation-only C2J preprocessors: {@link NewAndLegacyEventStreamProcessor},
 * {@link ExplicitStringPayloadQueryProtocolProcessor}, and {@link LowercaseShapeValidatorProcessor}. Checks the shapes
 * the service uses, after smithy-build transforms, and fails with the same errors as C2J.
 */
@SdkInternalApi
public final class SmithySourceModelValidator {

    private SmithySourceModelValidator() {
    }

    public static void validate(Model model, ServiceShape service, String protocol) {
        Set<Shape> serviceShapes = new Walker(model).walkShapes(service);
        validateEventStreams(service, serviceShapes);
        validateNoExplicitStringPayload(model, service, protocol);
        validateShapeNames(serviceShapes);
    }

    private static void validateEventStreams(ServiceShape service, Set<Shape> serviceShapes) {
        service.getTrait(ServiceTrait.class).ifPresent(serviceTrait -> {
            List<String> eventStreams = serviceShapes.stream()
                                                     .filter(s -> s.isUnionShape() && s.hasTrait(StreamingTrait.class))
                                                     .map(s -> s.getId().getName())
                                                     .sorted()
                                                     .collect(Collectors.toList());
            NewAndLegacyEventStreamProcessor.validateEventStreams(serviceTrait.getSdkId(), eventStreams);
        });
    }

    private static void validateNoExplicitStringPayload(Model model, ServiceShape service, String protocol) {
        if (!"ec2".equals(protocol) && !"query".equals(protocol)) {
            return;
        }
        for (ShapeId operationId : service.getAllOperations()) {
            OperationShape operation = model.expectShape(operationId, OperationShape.class);
            if (hasExplicitStringPayload(model, operation.getInputShape())
                || hasExplicitStringPayload(model, operation.getOutputShape())) {
                throw new RuntimeException("Operations with explicit String payloads are not supported for Query "
                                           + "protocols. Unsupported operation: " + operationId.getName());
            }
        }
    }

    private static boolean hasExplicitStringPayload(Model model, ShapeId structureId) {
        for (MemberShape member : model.expectShape(structureId).members()) {
            if (member.hasTrait(HttpPayloadTrait.class) && model.expectShape(member.getTarget()) instanceof StringShape) {
                return true;
            }
        }
        return false;
    }

    private static void validateShapeNames(Set<Shape> serviceShapes) {
        serviceShapes.stream()
                     .filter(s -> (s.isStructureShape() || s.isUnionShape()) && !s.hasTrait(TraitDefinition.class))
                     .map(s -> s.getId().getName())
                     .filter(name -> Character.isLowerCase(name.charAt(0)))
                     .sorted()
                     .findFirst()
                     .ifPresent(name -> {
                         String errorMsg = String.format("Shape name '%s' starts with a lowercase character. Shape names "
                                                         + "must start with an uppercase character. Please update the "
                                                         + "shape name in your service model", name);
                         throw ModelInvalidException.fromEntry(
                             ValidationEntry.create(ValidationErrorId.INVALID_IDENTIFIER_NAME,
                                                    ValidationErrorSeverity.DANGER, errorMsg));
                     });
    }
}
