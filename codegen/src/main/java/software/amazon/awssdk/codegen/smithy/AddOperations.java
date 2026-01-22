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

package software.amazon.awssdk.codegen.smithy;

import static software.amazon.awssdk.codegen.internal.Utils.unCapitalize;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.codegen.checksum.HttpChecksum;
import software.amazon.awssdk.codegen.compression.RequestCompression;
import software.amazon.awssdk.codegen.model.intermediate.EndpointDiscovery;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;
import software.amazon.awssdk.codegen.model.service.EndpointTrait;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.codegen.utils.ProtocolUtils;
import software.amazon.smithy.aws.traits.HttpChecksumTrait;
import software.amazon.smithy.aws.traits.auth.UnsignedPayloadTrait;
import software.amazon.smithy.aws.traits.clientendpointdiscovery.ClientEndpointDiscoveryIndex;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.HttpChecksumRequiredTrait;
import software.amazon.smithy.model.traits.RequestCompressionTrait;
import software.amazon.smithy.model.traits.StringTrait;
import software.amazon.smithy.model.traits.Trait;

final class AddOperations {
    private final Model model;
    private final ServiceShape service;
    private final NamingStrategy namingStrategy;
    private final Map<String, PaginatorDefinition> paginators;
    private final List<String> deprecatedShapes;
    private final ServiceIndex serviceIndex;
    private final OperationIndex operationIndex;
    private final TopDownIndex topDownIndex;
    private final ClientEndpointDiscoveryIndex endpointDiscoveryIndex;

    AddOperations(SmithyIntermediateModelBuilder builder, Paginators paginators) {
        this.paginators = paginators.getPagination();
        this.model = builder.getSmithyModel();
        this.service = builder.getService();
        this.namingStrategy = builder.getNamingStrategy();
        this.deprecatedShapes = builder.getCustomizationConfig().getDeprecatedShapes();
        this.serviceIndex = builder.getServiceIndex();
        this.operationIndex = OperationIndex.of(model);
        this.topDownIndex = TopDownIndex.of(model);
        this.endpointDiscoveryIndex = ClientEndpointDiscoveryIndex.of(model);

    }

    public Map<String, OperationModel> constructOperations() {
        Map<String, OperationModel> javaOperationModels = new TreeMap<>();

        for (OperationShape operationShape : topDownIndex.getContainedOperations(service)) {
            OperationModel operationModel = new OperationModel();

            operationModel.setOperationName(operationShape.toShapeId().getName());
            operationModel.setServiceProtocol(ProtocolUtils.resolveProtocol(serviceIndex, service));
            translateDeprecated(operationShape, operationModel);
            operationModel.setDocumentation(getOperationDocumentation(operationShape));

            operationModel.setPaginated(isPaginated(operationShape));


            operationModel.setEndpointOperation(isEndpointDiscoveryOperation(operationShape));
            operationModel.setEndpointDiscovery(translateEndpointDiscoveryInfo(operationShape));
            operationModel.setEndpointTrait(translateEndpointInfo(operationShape));
            operationModel.setHttpChecksumRequired(operationShape.hasTrait(HttpChecksumRequiredTrait.class));
            operationModel.setHttpChecksum(translateHttpChecksum(operationShape));
            operationModel.setRequestcompression(translateRequestCompression(operationShape));

            translateContextParams(operationShape, operationModel);

            translateAuthentication(operationShape, operationModel);
            operationModel.setUnsignedPayload(operationShape.hasTrait(UnsignedPayloadTrait.class));

            translateInput(operationShape);
            translateOutput(operationShape);
            translateErrors(operationShape);

        }

        return javaOperationModels;
    }

    private void translateInput(OperationShape operationShape, OperationModel operationModel) {
        StructureShape inputShape = model.expectShape(operationShape.getOutputShape(), StructureShape.class);
        String inputShapeName = inputShape.toShapeId().getName();
        String inputClassName = namingStrategy.getRequestClassName(operationShape.toShapeId().getName());
        String documentation = inputShape.getTrait(DocumentationTrait.class)
            .map(t -> t.getValue())
            .orElse(null);
        operationModel.setInput(new VariableModel(unCapitalize(inputClassName), inputClassName)
                                    .withDocumentation(documentation));
    }

    private void translateOutput(OperationShape operationShape, OperationModel operationModel) {
    }

    private void translateErrors(OperationShape operationShape, OperationModel operationModel) {
    }

    private void translateContextParams(OperationShape operationShape, OperationModel operationModel) {
        // TODO: translate both static and operation context params
    }

    private RequestCompression translateRequestCompression(OperationShape operationShape) {
        return operationShape.getTrait(RequestCompressionTrait.class)
            .map(t -> {
                RequestCompression value = new RequestCompression();
                value.setEncodings(t.getEncodings());
                return value;
            }).orElse(null);
    }

    private HttpChecksum translateHttpChecksum(OperationShape operationShape) {
        return operationShape.getTrait(HttpChecksumTrait.class)
            .map(t -> {
                // TODO: We may need to translate the "members"
                HttpChecksum value = new HttpChecksum();
                value.setRequestChecksumRequired(t.isRequestChecksumRequired());
                t.getRequestAlgorithmMember().ifPresent(value::setRequestAlgorithmMember);
                t.getRequestValidationModeMember().ifPresent(value::setRequestValidationModeMember);
                value.setResponseAlgorithms(t.getResponseAlgorithms());
                return value;
            }).orElse(null);
    }

    private static void translateDeprecated(OperationShape operationShape, OperationModel operationModel) {
        if (operationShape.hasTrait(DeprecatedTrait.class)) {
            operationModel.setDeprecated(true);
            DeprecatedTrait deprecatedTrait = operationShape.expectTrait(DeprecatedTrait.class);
            deprecatedTrait.getMessage().ifPresent(operationModel::setDeprecatedMessage);
        } else {
            operationModel.setDeprecated(false);
        }
    }

    private EndpointTrait translateEndpointInfo(OperationShape operationShape) {
        return operationShape.getTrait(software.amazon.smithy.model.traits.EndpointTrait.class)
            .map(ep -> {
                EndpointTrait e = new EndpointTrait();
                e.setHostPrefix(ep.getHostPrefix().toString());
                return e;
            }).orElse(null);
    }

    private EndpointDiscovery translateEndpointDiscoveryInfo(OperationShape operationShape) {
        return endpointDiscoveryIndex.getEndpointDiscoveryInfo(service, operationShape)
            .map(info -> {
                EndpointDiscovery d = new EndpointDiscovery();
                d.setRequired(info.isRequired());
                return d;
            }).orElse(null);
    }

    private boolean isEndpointDiscoveryOperation(OperationShape operationShape) {
        return endpointDiscoveryIndex.getEndpointDiscoveryOperations(service).contains(operationShape.toShapeId());
    }

    private boolean isPaginated(OperationShape op) {
        String operationName = op.toShapeId().getName();
        return paginators.containsKey(operationName) && paginators.get(operationName).isValid();
    }

    private String getOperationDocumentation(OperationShape operationShape) {
        // TODO: fall back to output documentation is operation's docs aren't present
        return operationShape.getTrait(DocumentationTrait.class)
                      .map(StringTrait::getValue)
            .orElse(null);
    }

    private void translateAuthentication(OperationShape operationShape, OperationModel operationModel) {
        // normally we would use: serviceIndex.getEffectiveAuthSchemes
        // but for this translation we care ONLY about traits explicitly on the operation.
        Map<ShapeId, Trait> operationAuth = getAuthTraitValues(service, operationShape);
        if (operationAuth == null) {
            operationModel.setIsAuthenticated(true);
            operationModel.setAuthType(null);
            operationModel.setAuth(Collections.emptyList());
        } else {
            // TODO: We need to fully translate Auth types here
        }
    }

    private static Map<ShapeId, Trait> getAuthTraitValues(Shape service, Shape subject) {
        if (!subject.hasTrait(AuthTrait.ID)) {
            return null;
        }

        AuthTrait authTrait = subject.expectTrait(AuthTrait.class);
        Map<ShapeId, Trait> result = new LinkedHashMap<>();
        for (ShapeId value : authTrait.getValueSet()) {
            service.findTrait(value).ifPresent(trait -> result.put(value, trait));
        }

        return result;
    }
}
