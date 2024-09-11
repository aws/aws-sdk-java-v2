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

import java.util.Map;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.Input;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.Output;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;

/**
 * Operations with explicit String payloads are not supported for services with Query protocol. We fail the codegen if the
 * httpPayload or eventPayload trait is set on a String.
 */
public class ExplicitStringPayloadQueryProtocolProcessor implements CodegenCustomizationProcessor {
    @Override
    public void preprocess(ServiceModel serviceModel) {
        String protocol = serviceModel.getMetadata().getProtocol();
        if (!"ec2".equals(protocol) && !"query".equals(protocol)) {
            return;
        }

        Map<String, Shape> c2jShapes = serviceModel.getShapes();

        serviceModel.getOperations().forEach((operationName, op) -> {

            Input input = op.getInput();
            if (input != null && isExplicitStringPayload(c2jShapes, c2jShapes.get(input.getShape()))) {
                throw new RuntimeException("Operations with explicit String payloads are not supported for Query "
                                           + "protocols. Unsupported operation: " + operationName);

            }

            Output output = op.getOutput();
            if (output != null && isExplicitStringPayload(c2jShapes, c2jShapes.get(output.getShape()))) {
                throw new RuntimeException("Operations with explicit String payloads are not supported for Query "
                                           + "protocols. Unsupported operation: " + operationName);

            }
        });
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // no-op
    }

    private boolean isExplicitStringPayload(Map<String, Shape> c2jShapes, Shape shape) {
        if (shape == null || shape.getPayload() == null) {
            return false;
        }

        Member payloadMember = shape.getMembers().get(shape.getPayload());
        Shape payloadShape = c2jShapes.get(payloadMember.getShape());
        return payloadShape != null && "String".equalsIgnoreCase(payloadShape.getType());
    }
}
