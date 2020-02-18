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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.internal.Constant;
import software.amazon.awssdk.codegen.model.config.customization.OperationModifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.customization.ArtificialResultWrapper;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.awssdk.codegen.model.service.ShapeType;

/**
 * This processor internally keeps track of all the result wrapper shapes it
 * created during pre-processing, therefore the caller needs to make sure this
 * processor is only invoked once.
 */
final class OperationModifiersProcessor implements CodegenCustomizationProcessor {

    private final Map<String, OperationModifier> operationModifiers;

    private final Set<String> createdWrapperShapes = new HashSet<>();

    OperationModifiersProcessor(Map<String, OperationModifier> operationModifiers) {
        this.operationModifiers = operationModifiers;
    }

    @Override
    public void preprocess(ServiceModel serviceModel) {

        if (operationModifiers == null) {
            return;
        }

        for (Entry<String, OperationModifier> entry : operationModifiers.entrySet()) {
            String operationName = entry.getKey();
            OperationModifier modifier = entry.getValue();

            if (modifier.isExclude()) {
                preprocessExclude(serviceModel, operationName);
                continue;
            }

            if (modifier.isUseWrappingResult()) {
                String createdWrapperShape = preprocessCreateResultWrapperShape(
                        serviceModel, operationName, modifier);
                // Keep track of all the wrappers we created
                createdWrapperShapes.add(createdWrapperShape);
            }
        }

    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {

        if (operationModifiers == null) {
            return;
        }

        // Find all the wrapper shapes in the intermediate model (by its
        // "original" c2j name), and add the customization metadata

        for (ShapeModel shape : intermediateModel.getShapes().values()) {

            if (!createdWrapperShapes.contains(shape.getC2jName())) {
                continue;
            }

            if (shape.getMembers().size() != 1) {
                throw new IllegalStateException("Result wrapper "
                                                + shape.getShapeName() + " has not just one member!");
            }

            MemberModel wrappedMember = shape.getMembers().get(0);

            /*
             * "RunInstancesResult" : {
             *   "customization" : {
             *     "artificialResultWrapper" : {
             *       "wrappedMemberName" : "Reservation",
             *       "wrappedMemberSimpleType" : "Reservation"
             *     }
             *   }
             * }
             */
            shape.getCustomization().setArtificialResultWrapper(
                    createArtificialResultWrapperInfo(
                            shape, wrappedMember));
        }
    }

    private void preprocessExclude(ServiceModel serviceModel, String operationName) {
        Operation operation = serviceModel.getOperation(operationName);
        // Remove input and output shapes of the operation
        serviceModel.getShapes().remove(operation.getInput().getShape());
        serviceModel.getShapes().remove(operation.getOutput().getShape());

        serviceModel.getOperations().remove(operationName);
    }

    private String preprocessCreateResultWrapperShape(ServiceModel serviceModel,
                                                      String operationName, OperationModifier modifier) {

        String wrappedShapeName = modifier.getWrappedResultShape();
        Shape wrappedShape = serviceModel.getShapes().get(wrappedShapeName);

        String wrapperShapeName = operationName + Constant.RESPONSE_CLASS_SUFFIX;
        String wrappedAsMember = modifier.getWrappedResultMember();

        if (serviceModel.getShapes().containsKey(wrapperShapeName)) {
            throw new IllegalStateException(wrapperShapeName
                                            + " shape already exists in the service model.");
        }

        Shape wrapperShape = createWrapperShape(wrapperShapeName,
                                                wrappedShapeName, wrappedShape, wrappedAsMember);

        // Add the new shape to the model
        serviceModel.getShapes().put(wrapperShapeName, wrapperShape);

        // Update the operation model to point to this new shape
        Operation operation = serviceModel.getOperations().get(operationName);
        operation.getOutput().setShape(wrapperShapeName);

        return wrapperShapeName;
    }

    private Shape createWrapperShape(String wrapperShapeName, String wrappedShapeName, Shape wrapped, String wrappedAsMember) {

        Shape wrapper = new Shape();
        wrapper.setType(ShapeType.Structure.getName());
        wrapper.setDocumentation("A simple result wrapper around the "
                                 + wrappedShapeName + " object that was sent over the wire.");

        Member member = new Member();
        member.setShape(wrappedShapeName);
        member.setDocumentation(wrapped.getDocumentation());
        wrapper.setMembers(Collections.singletonMap(wrappedAsMember, member));

        return wrapper;
    }

    private ArtificialResultWrapper createArtificialResultWrapperInfo(ShapeModel shape, MemberModel wrappedMember) {
        ArtificialResultWrapper wrapper = new ArtificialResultWrapper();
        wrapper.setWrappedMemberName(wrappedMember.getName());
        wrapper.setWrappedMemberSimpleType(wrappedMember.getVariable().getSimpleType());
        return wrapper;
    }
}
