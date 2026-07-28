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

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.codegen.IntermediateModelShapeProcessor;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ShapeMarshaller;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * Builds the request {@link ShapeModel} for each operation reachable from the
 * service. When an operation has no input structure it targets Smithy's
 * {@code smithy.api#Unit} sentinel, and this processor synthesizes an empty
 * request shape for it.
 *
 * <p>Each request shape carries a {@link ShapeMarshaller} describing how the
 * request is written to the wire (HTTP verb, request URI, target header, and
 * XML namespace where applicable).
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
                shapeModel = generateShapeModel(javaClassName, inputShape,
                                                bindingIndex.getRequestBindings(op.getId()));
            }
            shapeModel.setType(ShapeType.Request.getValue());
            shapeModel.setMarshaller(buildMarshaller(op, /* synthetic */ UNIT.equals(inputId)));

            shapes.put(javaClassName, shapeModel);
        }

        return shapes;
    }

    private ShapeModel synthesizeEmptyRequest(OperationShape op, String javaClassName) {
        ShapeModel shape = new ShapeModel(javaClassName);
        shape.setShapeName(javaClassName);
        shape.setVariable(new VariableModel(getNamingStrategy().getVariableName(javaClassName),
                                            javaClassName));
        return shape;
    }

    private ShapeMarshaller buildMarshaller(OperationShape op, boolean synthetic) {
        String protocol = getProtocol();
        ShapeMarshaller marshaller = new ShapeMarshaller()
            .withAction(op.toShapeId().getName())
            .withProtocol(protocol);

        // HTTP verb and URI come from @http when present, otherwise the RPC-protocol default.
        if (op.hasTrait(HttpTrait.class)) {
            HttpTrait http = op.expectTrait(HttpTrait.class);
            marshaller.withVerb(http.getMethod());
            marshaller.withRequestUri(http.getUri().toString());
        } else {
            // RPC protocols (awsJson1_0, awsJson1_1, awsQuery, ec2Query, smithy-rpc-v2-cbor)
            // always POST to "/".
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
            input.getTrait(XmlNamespaceTrait.class)
                 .ifPresent(ns -> marshaller.withXmlNameSpaceUri(ns.getUri()));
        }

        marshaller.withIsSynthetic(synthetic);
        return marshaller;
    }

    /**
     * The awsJson family prefixes the target with the service shape name (C2J's
     * {@code ServiceMetadata.getTargetPrefix()}); query/ec2 use a bare operation name.
     */
    private static boolean usesTargetPrefix(String protocol) {
        return Protocol.AWS_JSON.getValue().equals(protocol)
               || Protocol.CBOR.getValue().equals(protocol);
    }
}
