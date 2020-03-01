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

package software.amazon.awssdk.codegen;

import static software.amazon.awssdk.codegen.internal.Utils.unCapitalize;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.codegen.model.intermediate.ExceptionModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ReturnTypeModel;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.model.service.ErrorMap;
import software.amazon.awssdk.codegen.model.service.ErrorTrait;
import software.amazon.awssdk.codegen.model.service.Input;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.Output;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.awssdk.codegen.naming.NamingStrategy;

/**
 * Constructs the operation model for every operation defined by the service.
 */
final class AddOperations {

    private final ServiceModel serviceModel;
    private final NamingStrategy namingStrategy;
    private final Map<String, PaginatorDefinition> paginators;
    private final List<String> deprecatedShapes;

    AddOperations(IntermediateModelBuilder builder) {
        this.serviceModel = builder.getService();
        this.namingStrategy = builder.getNamingStrategy();
        this.paginators = builder.getPaginators().getPaginators();
        this.deprecatedShapes = builder.getCustomConfig().getDeprecatedShapes();
    }

    private static boolean isAuthenticated(Operation op) {
        return op.getAuthType() == null || !op.getAuthType().equals(AuthType.NONE);
    }

    private static String getOperationDocumentation(final Output output, final Shape outputShape) {
        return output.getDocumentation() != null ? output.getDocumentation() :
               outputShape.getDocumentation();
    }

    /**
     * @return True if shape is a Blob type. False otherwise
     */
    private static boolean isBlobShape(Shape shape) {
        return shape != null && "blob".equals(shape.getType());
    }

    /**
     * If there is a member in the output shape that is explicitly marked as the payload (with the
     * payload trait) this method returns the target shape of that member. Otherwise this method
     * returns null.
     *
     * @param c2jShapes
     *            All C2J shapes
     * @param outputShape
     *            Output shape of operation that may contain a member designated as the payload
     */
    public static Shape getPayloadShape(Map<String, Shape> c2jShapes, Shape outputShape) {
        if (outputShape.getPayload() == null) {
            return null;
        }
        Member payloadMember = outputShape.getMembers().get(outputShape.getPayload());
        return c2jShapes.get(payloadMember.getShape());
    }

    /**
     *  In query protocol, the wrapped result is the real return type for the given operation. In the c2j model,
     *  if the output shape has only one member, and the member shape is wrapped (wrapper is true), then the
     *  return type is the wrapped member shape instead of the output shape. In the following example, the service API is:
     *
     *  public Foo operation(OperationRequest operationRequest);
     *
     *  And the wire log is:
     *  <OperationResponse>
     *    <OperationResult>
     *      <Foo>
     *      ...
     *      </Foo>
     *    </OperationResult>
     *    <OperationMetadata>
     *    </OperationMetadata>
     *  </OperationResponse>
     *
     *  The C2j model is:
     *  "Operation": {
     *      "input": {"shape": "OperationRequest"},
     *      "output": {
     *          "shape": "OperationResult",
     *          "resultWrapper": "OperationResult"
     *      }
     *  },
     *  "OperationResult": {
     *      ...
     *      "members": {
     *          "Foo": {"shape": "Foo"}
     *      }
     *  },
     *  "Foo" : {
     *      ...
     *      "wrapper" : true
     *  }
     *
     *  Return the wrapped shape name from the given operation if it conforms to the condition
     *  described above, otherwise, simply return the direct output shape name.
     */
    private static String getResultShapeName(Operation operation, Map<String, Shape> shapes) {
        Output output = operation.getOutput();
        if (output == null) {
            return null;
        }
        Shape outputShape = shapes.get(output.getShape());
        if (outputShape.getMembers().keySet().size() != 1) {
            return output.getShape();
        }
        Member wrappedMember = outputShape.getMembers().values().toArray(new Member[0])[0];
        Shape wrappedResult = shapes.get(wrappedMember.getShape());
        return wrappedResult.isWrapper() ? wrappedMember.getShape() : output.getShape();
    }

    public Map<String, OperationModel> constructOperations() {

        Map<String, OperationModel> javaOperationModels = new TreeMap<>();
        Map<String, Shape> c2jShapes = serviceModel.getShapes();

        for (Map.Entry<String, Operation> entry : serviceModel.getOperations().entrySet()) {

            String operationName = entry.getKey();
            Operation op = entry.getValue();

            OperationModel operationModel = new OperationModel();

            operationModel.setOperationName(operationName);
            operationModel.setDeprecated(op.isDeprecated());
            operationModel.setDocumentation(op.getDocumentation());
            operationModel.setIsAuthenticated(isAuthenticated(op));
            operationModel.setAuthType(op.getAuthType());
            operationModel.setPaginated(isPaginated(op));
            operationModel.setEndpointOperation(op.isEndpointOperation());
            operationModel.setEndpointDiscovery(op.getEndpointDiscovery());
            operationModel.setEndpointTrait(op.getEndpoint());

            Input input = op.getInput();
            if (input != null) {
                String originalShapeName = input.getShape();
                String inputShape = namingStrategy.getRequestClassName(operationName);
                String documentation = input.getDocumentation() != null ? input.getDocumentation() :
                                       c2jShapes.get(originalShapeName).getDocumentation();

                operationModel.setInput(new VariableModel(unCapitalize(inputShape), inputShape)
                                                .withDocumentation(documentation));

            }

            Output output = op.getOutput();
            if (output != null) {
                String outputShapeName = getResultShapeName(op, c2jShapes);
                Shape outputShape = c2jShapes.get(outputShapeName);
                String responseClassName = namingStrategy.getResponseClassName(operationName);
                String documentation = getOperationDocumentation(output, outputShape);

                operationModel.setReturnType(
                        new ReturnTypeModel(responseClassName).withDocumentation(documentation));
                if (isBlobShape(getPayloadShape(c2jShapes, outputShape))) {
                    operationModel.setHasBlobMemberAsPayload(true);
                }
            }

            if (op.getErrors() != null) {
                for (ErrorMap error : op.getErrors()) {

                    String documentation =
                            error.getDocumentation() != null ? error.getDocumentation() :
                            c2jShapes.get(error.getShape()).getDocumentation();

                    Integer httpStatusCode = getHttpStatusCode(error, c2jShapes.get(error.getShape()));

                    if (!deprecatedShapes.contains(error.getShape())) {
                        operationModel.addException(
                            new ExceptionModel(namingStrategy.getExceptionName(error.getShape()))
                                .withDocumentation(documentation)
                                .withHttpStatusCode(httpStatusCode));
                    }
                }
            }

            javaOperationModels.put(operationName, operationModel);
        }

        return javaOperationModels;
    }

    /**
     * Get HTTP status code either from error trait on the operation reference or the error trait on the shape.
     *
     * @param error ErrorMap on operation reference.
     * @param shape Error shape.
     * @return HTTP status code or null if not present.
     */
    private Integer getHttpStatusCode(ErrorMap error, Shape shape) {
        Integer httpStatusCode = getHttpStatusCode(error.getErrorTrait());
        return httpStatusCode == null ? getHttpStatusCode(shape.getErrorTrait()) : httpStatusCode;
    }

    /**
     * @param errorTrait Error trait.
     * @return HTTP status code from trait or null if not present.
     */
    private Integer getHttpStatusCode(ErrorTrait errorTrait) {
        return errorTrait == null ? null : errorTrait.getHttpStatusCode();
    }

    private boolean isPaginated(Operation op) {
        return paginators.containsKey(op.getName()) && paginators.get(op.getName()).isValid();
    }
}
