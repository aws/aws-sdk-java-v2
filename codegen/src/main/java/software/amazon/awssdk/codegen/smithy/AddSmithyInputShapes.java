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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.codegen.IntermediateModelShapeProcessor;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.EndpointDiscovery;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ShapeMarshaller;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.smithy.aws.traits.clientendpointdiscovery.ClientEndpointDiscoveryIndex;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * Builds the request {@link ShapeModel} and its {@link ShapeMarshaller} for each operation
 * reachable from the service. A {@code smithy.api#Unit} input yields a synthesized empty request.
 */
final class AddSmithyInputShapes extends AddSmithyShapes implements IntermediateModelShapeProcessor {

    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");

    AddSmithyInputShapes(Model model,
                         ServiceShape service,
                         NamingStrategy namingStrategy,
                         CustomizationConfig customConfig,
                         String protocol,
                         TypeUtils typeUtils) {
        super(model, service, namingStrategy, customConfig, protocol, typeUtils);
    }

    @Override
    public Map<String, ShapeModel> process(Map<String, OperationModel> currentOperations,
                                           Map<String, ShapeModel> currentShapes) {
        Map<String, ShapeModel> shapes = new HashMap<>();
        Model model = getModel();
        NamingStrategy naming = getNamingStrategy();
        HttpBindingIndex bindingIndex = HttpBindingIndex.of(model);
        TopDownIndex topDown = TopDownIndex.of(model);

        for (OperationShape op : topDown.getContainedOperations(getService())) {
            String opName = op.toShapeId().getName();
            String javaClassName = naming.getRequestClassName(opName);

            ShapeId inputId = op.getInputShape();
            ShapeModel shapeModel;
            if (UNIT.equals(inputId)) {
                shapeModel = synthesizeEmptyRequest(op, javaClassName);
            } else {
                StructureShape inputShape = model.expectShape(inputId, StructureShape.class);
                Map<String, HttpBinding> requestBindings =
                    httpBindingsHonored() ? bindingIndex.getRequestBindings(op.getId())
                                          : Collections.emptyMap();
                shapeModel = generateShapeModel(javaClassName, inputShape, requestBindings);
                shapeModel.setEndpointDiscovery(endpointDiscovery(op));
            }
            shapeModel.setType(ShapeType.Request.getValue());
            shapeModel.setMarshaller(buildMarshaller(op, bindingIndex, /* synthetic */ UNIT.equals(inputId)));

            shapes.put(javaClassName, shapeModel);
        }

        return shapes;
    }

    private EndpointDiscovery endpointDiscovery(OperationShape op) {
        return ClientEndpointDiscoveryIndex.of(getModel())
                                           .getEndpointDiscoveryInfo(getService(), op)
                                           .map(info -> {
                                               EndpointDiscovery discovery = new EndpointDiscovery();
                                               discovery.setRequired(info.isRequired());
                                               return discovery;
                                           })
                                           .orElse(null);
    }

    private ShapeModel synthesizeEmptyRequest(OperationShape op, String javaClassName) {
        ShapeModel shape = new ShapeModel(javaClassName);
        shape.setShapeName(javaClassName);
        shape.setVariable(new VariableModel(getNamingStrategy().getVariableName(javaClassName),
                                            javaClassName));
        return shape;
    }

    private ShapeMarshaller buildMarshaller(OperationShape op, HttpBindingIndex bindingIndex, boolean synthetic) {
        String protocol = getProtocol();
        ShapeMarshaller marshaller = new ShapeMarshaller()
            .withAction(op.toShapeId().getName())
            .withProtocol(protocol);

        // RPC protocols ignore any @http trait and always POST to "/". For smithy-rpc-v2-cbor the
        // "/service/{id}/operation/{op}" URI is applied later by the deferred rpcv2Cbor processor.
        if (httpBindingsHonored() && op.hasTrait(HttpTrait.class)) {
            HttpTrait http = op.expectTrait(HttpTrait.class);
            marshaller.withVerb(http.getMethod());
            marshaller.withRequestUri(http.getUri().toString());
        } else {
            marshaller.withVerb("POST");
            marshaller.withRequestUri("/");
        }

        // Populated only for protocols that identify the operation by name.
        if (Metadata.usesOperationIdentifier(protocol)) {
            String targetPrefix = usesTargetPrefix(protocol) ? getService().getId().getName() : null;
            marshaller.withTarget(StringUtils.isEmpty(targetPrefix)
                                  ? op.toShapeId().getName()
                                  : targetPrefix + "." + op.toShapeId().getName());
        }

        if (!UNIT.equals(op.getInputShape())) {
            StructureShape input = getModel().expectShape(op.getInputShape(), StructureShape.class);
            // C2J sources both from the operation's input reference. The converter drops the
            // locationName (it equals the request shape name) and hoists the namespace to the
            // service, so both are reconstructed here. rest-xml only: ec2Query also carries a
            // service @xmlNamespace, but C2J does not propagate it to the marshaller.
            if (Protocol.REST_XML.getValue().equals(protocol) && hasDocumentBody(bindingIndex, op)) {
                marshaller.withLocationName(xmlRootElementName(input));
                marshaller.withXmlNameSpaceUri(xmlNamespaceUri(input));
            } else {
                input.getTrait(XmlNamespaceTrait.class)
                     .ifPresent(ns -> marshaller.withXmlNameSpaceUri(ns.getUri()));
            }
        }

        marshaller.withIsSynthetic(synthetic);
        return marshaller;
    }

    /**
     * True when at least one member is serialized in the body. C2J authors
     * {@code input.locationName} / {@code input.xmlNamespace} only on body-bearing operations.
     */
    private static boolean hasDocumentBody(HttpBindingIndex bindingIndex, OperationShape op) {
        return bindingIndex.getRequestBindings(op.getId()).values().stream()
                           .anyMatch(b -> b.getLocation() == HttpBinding.Location.DOCUMENT);
    }

    /**
     * XML root element name: the {@code @xmlName} override when present, otherwise the shape name.
     */
    private static String xmlRootElementName(StructureShape input) {
        return input.getTrait(XmlNameTrait.class)
                    .map(XmlNameTrait::getValue)
                    .orElse(input.getId().getName());
    }

    /**
     * The input shape's own {@code @xmlNamespace}, falling back to the service-level one.
     */
    private String xmlNamespaceUri(StructureShape input) {
        return input.getTrait(XmlNamespaceTrait.class)
                    .map(XmlNamespaceTrait::getUri)
                    .orElseGet(() -> getService().getTrait(XmlNamespaceTrait.class)
                                                 .map(XmlNamespaceTrait::getUri)
                                                 .orElse(null));
    }

    /**
     * The awsJson family prefixes the target with the service shape name; query/ec2 do not.
     */
    private static boolean usesTargetPrefix(String protocol) {
        return Protocol.AWS_JSON.getValue().equals(protocol)
               || Protocol.CBOR.getValue().equals(protocol);
    }
}
