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

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.codegen.checksum.HttpChecksum;
import software.amazon.awssdk.codegen.compression.RequestCompression;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.EndpointDiscovery;
import software.amazon.awssdk.codegen.model.intermediate.ExceptionModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ReturnTypeModel;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.model.service.EndpointTrait;
import software.amazon.awssdk.codegen.model.service.OperationContextParam;
import software.amazon.awssdk.codegen.model.service.StaticContextParam;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.smithy.aws.traits.HttpChecksumTrait;
import software.amazon.smithy.aws.traits.auth.UnsignedPayloadTrait;
import software.amazon.smithy.aws.traits.clientendpointdiscovery.ClientEndpointDiscoveryIndex;
import software.amazon.smithy.aws.traits.clientendpointdiscovery.ClientEndpointDiscoveryTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.HttpChecksumRequiredTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.OptionalAuthTrait;
import software.amazon.smithy.model.traits.RequestCompressionTrait;
import software.amazon.smithy.rulesengine.traits.OperationContextParamsTrait;
import software.amazon.smithy.rulesengine.traits.StaticContextParamsTrait;

/**
 * Builds the {@link OperationModel} for every operation reachable from a Smithy service.
 * Smithy counterpart to C2J's {@code AddOperations} (plus the empty input/output wiring from
 * {@code AddEmptyInputShape}/{@code AddEmptyOutputShape}).
 *
 * <p>Uses {@link TopDownIndex} for reachable operations, {@link OperationIndex} for the merged
 * (service + operation) errors, and {@link ClientEndpointDiscoveryIndex} for endpoint discovery.
 * The protocol string is supplied by the caller.
 */
final class AddSmithyOperations {

    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Model model;
    private final ServiceShape service;
    private final NamingStrategy namingStrategy;
    private final String protocol;
    private final List<String> deprecatedShapes;

    private final TopDownIndex topDownIndex;
    private final OperationIndex operationIndex;
    private final ClientEndpointDiscoveryIndex endpointDiscoveryIndex;

    AddSmithyOperations(Model model,
                        ServiceShape service,
                        NamingStrategy namingStrategy,
                        CustomizationConfig customConfig,
                        String protocol) {
        this.model = model;
        this.service = service;
        this.namingStrategy = namingStrategy;
        this.protocol = protocol;
        this.deprecatedShapes = customConfig.getDeprecatedShapes();
        this.topDownIndex = TopDownIndex.of(model);
        this.operationIndex = OperationIndex.of(model);
        this.endpointDiscoveryIndex = ClientEndpointDiscoveryIndex.of(model);
    }

    Map<String, OperationModel> constructOperations() {
        Map<String, OperationModel> operations = new TreeMap<>();

        for (OperationShape op : topDownIndex.getContainedOperations(service)) {
            String operationName = op.getId().getName();

            OperationModel operationModel = new OperationModel();
            operationModel.setOperationName(operationName);
            operationModel.setServiceProtocol(protocol);
            translateDeprecated(op, operationModel);
            operationModel.setDocumentation(documentation(op));

            operationModel.setEndpointOperation(isEndpointOperation(op));
            operationModel.setEndpointDiscovery(translateEndpointDiscovery(op));
            operationModel.setEndpointTrait(translateEndpointTrait(op));
            operationModel.setHttpChecksumRequired(op.hasTrait(HttpChecksumRequiredTrait.class));
            operationModel.setHttpChecksum(translateHttpChecksum(op));
            operationModel.setRequestcompression(translateRequestCompression(op));
            operationModel.setStaticContextParams(translateStaticContextParams(op));
            operationModel.setOperationContextParams(translateOperationContextParams(op));
            operationModel.setUnsignedPayload(op.hasTrait(UnsignedPayloadTrait.class));
            translateAuth(op, operationModel);

            translateInput(op, operationName, operationModel);
            translateOutput(op, operationName, operationModel);
            translateErrors(op, operationModel);

            operations.put(operationName, operationModel);
        }

        return operations;
    }

    private void translateInput(OperationShape op, String operationName, OperationModel operationModel) {
        String inputClass = namingStrategy.getRequestClassName(operationName);
        VariableModel input = new VariableModel(unCapitalize(inputClass), inputClass);

        ShapeId inputId = op.getInputShape();
        if (!UNIT.equals(inputId)) {
            StructureShape inputShape = model.expectShape(inputId, StructureShape.class);
            input.withDocumentation(documentation(inputShape));
        }
        operationModel.setInput(input);
    }

    private void translateOutput(OperationShape op, String operationName, OperationModel operationModel) {
        String outputClass = namingStrategy.getResponseClassName(operationName);
        ReturnTypeModel returnType = new ReturnTypeModel(outputClass);

        ShapeId outputId = op.getOutputShape();
        if (!UNIT.equals(outputId)) {
            StructureShape outputShape = model.expectShape(outputId, StructureShape.class);
            returnType.setDocumentation(documentation(outputShape));
            translatePayloadFlags(outputShape, operationModel);
        }
        operationModel.setReturnType(returnType);
    }

    // The @httpPayload member's target type sets the flag. Enum counts as string (C2J enum
    // shapes report type "string").
    private void translatePayloadFlags(StructureShape outputShape, OperationModel operationModel) {
        for (MemberShape member : outputShape.members()) {
            if (member.hasTrait(HttpPayloadTrait.class)) {
                Shape target = model.expectShape(member.getTarget());
                if (target.isBlobShape()) {
                    operationModel.setHasBlobMemberAsPayload(true);
                } else if (target.isStringShape() || target.isEnumShape()) {
                    operationModel.setHasStringMemberAsPayload(true);
                }
                return;
            }
        }
    }

    private void translateErrors(OperationShape op, OperationModel operationModel) {
        for (StructureShape errorShape : operationIndex.getErrors(service, op)) {
            String errorName = errorShape.getId().getName();
            if (deprecatedShapes != null && deprecatedShapes.contains(errorName)) {
                continue;
            }
            Integer httpStatusCode = errorShape.getTrait(HttpErrorTrait.class)
                                               .map(HttpErrorTrait::getCode)
                                               .orElse(null);
            operationModel.addException(
                new ExceptionModel(namingStrategy.getExceptionName(errorName))
                    .withDocumentation(documentation(errorShape))
                    .withHttpStatusCode(httpStatusCode));
        }
    }

    // Reads only the operation's explicit auth traits, matching C2J (which does not resolve the
    // service default). Resolving effective schemes would populate auth on every operation.
    private void translateAuth(OperationShape op, OperationModel operationModel) {
        // @optionalAuth (unsigned-capable ops) -> C2J authtype=none: AuthType.NONE, unauthenticated.
        // Takes precedence over the empty @auth list these ops also carry.
        if (op.hasTrait(OptionalAuthTrait.class)) {
            operationModel.setIsAuthenticated(false);
            operationModel.setAuthType(AuthType.NONE);
            operationModel.setAuth(Collections.singletonList(AuthType.NONE));
            return;
        }

        if (!op.hasTrait(AuthTrait.class)) {
            operationModel.setIsAuthenticated(true);
            operationModel.setAuthType(null);
            operationModel.setAuth(Collections.emptyList());
            return;
        }

        List<AuthType> authTypes = new ArrayList<>();
        for (ShapeId schemeId : op.expectTrait(AuthTrait.class).getValues()) {
            authTypes.add(AuthType.fromValue(schemeId.toString()));
        }
        operationModel.setAuth(authTypes);

        if (authTypes.isEmpty()) {
            operationModel.setIsAuthenticated(true);
            operationModel.setAuthType(null);
        } else {
            operationModel.setAuthType(authTypes.get(0));
            operationModel.setIsAuthenticated(authTypes.get(0) != AuthType.NONE);
        }
    }

    private HttpChecksum translateHttpChecksum(OperationShape op) {
        return op.getTrait(HttpChecksumTrait.class).map(trait -> {
            HttpChecksum httpChecksum = new HttpChecksum();
            httpChecksum.setRequestChecksumRequired(trait.isRequestChecksumRequired());
            trait.getRequestAlgorithmMember().ifPresent(httpChecksum::setRequestAlgorithmMember);
            trait.getRequestValidationModeMember().ifPresent(httpChecksum::setRequestValidationModeMember);
            httpChecksum.setResponseAlgorithms(trait.getResponseAlgorithms());
            return httpChecksum;
        }).orElse(null);
    }

    private RequestCompression translateRequestCompression(OperationShape op) {
        return op.getTrait(RequestCompressionTrait.class).map(trait -> {
            RequestCompression requestCompression = new RequestCompression();
            requestCompression.setEncodings(trait.getEncodings());
            return requestCompression;
        }).orElse(null);
    }

    private Map<String, StaticContextParam> translateStaticContextParams(OperationShape op) {
        return op.getTrait(StaticContextParamsTrait.class).map(trait -> {
            Map<String, StaticContextParam> params = new LinkedHashMap<>();
            trait.getParameters().forEach((name, definition) -> {
                StaticContextParam param = new StaticContextParam();
                param.setValue(nodeToTreeNode(definition.getValue()));
                params.put(name, param);
            });
            return params;
        }).orElse(null);
    }

    private Map<String, OperationContextParam> translateOperationContextParams(OperationShape op) {
        return op.getTrait(OperationContextParamsTrait.class).map(trait -> {
            Map<String, OperationContextParam> params = new LinkedHashMap<>();
            trait.getParameters().forEach((name, definition) -> {
                OperationContextParam param = new OperationContextParam();
                param.setPath(TextNode.valueOf(definition.getPath()));
                params.put(name, param);
            });
            return params;
        }).orElse(null);
    }

    private EndpointDiscovery translateEndpointDiscovery(OperationShape op) {
        return endpointDiscoveryIndex.getEndpointDiscoveryInfo(service, op).map(info -> {
            EndpointDiscovery endpointDiscovery = new EndpointDiscovery();
            endpointDiscovery.setRequired(info.isRequired());
            return endpointDiscovery;
        }).orElse(null);
    }

    // The operation named by the service's @clientEndpointDiscovery (C2J's endpointoperation
    // flag), not the @clientDiscoveredEndpoint consumers handled in translateEndpointDiscovery.
    private boolean isEndpointOperation(OperationShape op) {
        return service.getTrait(ClientEndpointDiscoveryTrait.class)
                      .map(trait -> trait.getOperation().equals(op.getId()))
                      .orElse(false);
    }

    private EndpointTrait translateEndpointTrait(OperationShape op) {
        return op.getTrait(software.amazon.smithy.model.traits.EndpointTrait.class).map(trait -> {
            EndpointTrait endpointTrait = new EndpointTrait();
            endpointTrait.setHostPrefix(trait.getHostPrefix().toString());
            return endpointTrait;
        }).orElse(null);
    }

    private static void translateDeprecated(OperationShape op, OperationModel operationModel) {
        if (op.hasTrait(DeprecatedTrait.class)) {
            operationModel.setDeprecated(true);
            op.expectTrait(DeprecatedTrait.class).getMessage().ifPresent(operationModel::setDeprecatedMessage);
        }
    }

    private static String documentation(Shape shape) {
        return shape.getTrait(DocumentationTrait.class).map(DocumentationTrait::getValue).orElse(null);
    }

    // Smithy Node -> Jackson TreeNode (the type the C2J context-param POJOs hold), via JSON text.
    private static TreeNode nodeToTreeNode(Node node) {
        try {
            return JSON.readTree(Node.printJson(node));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to convert Smithy node to a JSON tree", e);
        }
    }
}
