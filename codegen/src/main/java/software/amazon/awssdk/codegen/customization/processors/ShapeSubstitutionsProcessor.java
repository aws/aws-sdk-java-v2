/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.ShapeSubstitution;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ErrorMap;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;

/**
 * This processor internally keeps track of all the structure members whose
 * shape is substituted during pre-processing, therefore the caller needs to
 * make sure this processor is only invoked once.
 */
final class ShapeSubstitutionsProcessor implements CodegenCustomizationProcessor {

    private static Logger log = LoggerFactory.getLogger(ShapeSubstitutionsProcessor.class);

    private final Map<String, ShapeSubstitution> shapeSubstitutions;

    /**
     * parentShapeName -> {memberName -> originalShape}
     */
    private final Map<String, Map<String, String>> substitutedShapeMemberReferences = new HashMap<String, Map<String, String>>();

    /**
     * parentShapeName -> {listTypeMemberName -> originalShapeOfTheMemberOfTheListTypeMember...}
     */
    private final Map<String, Map<String, String>> substitutedListMemberReferences = new HashMap<String, Map<String, String>>();

    ShapeSubstitutionsProcessor(
            Map<String, ShapeSubstitution> shapeSubstitutions) {
        this.shapeSubstitutions = shapeSubstitutions;
    }

    @Override
    public void preprocess(ServiceModel serviceModel) {

        if (shapeSubstitutions == null) {
            return;
        }

        // Make sure the substituted shapes exist in the service model
        for (String substitutedShape : shapeSubstitutions.keySet()) {
            if (!serviceModel.getShapes().containsKey(substitutedShape)) {
                throw new IllegalStateException(
                        "shapeSubstitution customization found for shape "
                        + substitutedShape + ", which does not exist in the service model.");
            }
        }

        // Make sure the substituted shapes are not referenced by any operation
        // as the input, output or error shape
        for (Operation operation : serviceModel.getOperations().values()) {
            preprocessAssertNoSubstitutedShapeReferenceInOperation(operation);
        }

        // Substitute references from within shape members
        for (Entry<String, Shape> entry : serviceModel.getShapes().entrySet()) {
            String shapeName = entry.getKey();
            Shape shape = entry.getValue();

            preprocessSubstituteShapeReferencesInShape(shapeName, shape, serviceModel);
        }
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {

        if (shapeSubstitutions == null) {
            return;
        }

        for (ShapeModel shapeModel : intermediateModel.getShapes().values()) {
            postprocessHandleEmitAsMember(shapeModel, intermediateModel);
        }
    }

    private void preprocessAssertNoSubstitutedShapeReferenceInOperation(Operation operation) {

        // Check input
        if (operation.getInput() != null && operation.getInput().getShape() != null) {
            String inputShape = operation.getInput().getShape();
            if (shapeSubstitutions.containsKey(inputShape)) {
                throw new IllegalStateException(
                        "shapeSubstitution customization found for shape "
                        + inputShape + ", but this shape is referenced as the input for operation "
                        + operation.getName());
            }
        }

        // Check output
        if (operation.getOutput() != null && operation.getOutput().getShape() != null) {
            String outputShape = operation.getOutput().getShape();
            if (shapeSubstitutions.containsKey(outputShape)) {
                throw new IllegalStateException(
                        "shapeSubstitution customization found for shape "
                        + outputShape + ", but this shape is referenced as the output for operation "
                        + operation.getName());
            }
        }

        // Check errors
        if (operation.getErrors() != null) {
            for (ErrorMap error : operation.getErrors()) {
                String errorShape = error.getShape();
                if (shapeSubstitutions.containsKey(errorShape)) {
                    throw new IllegalStateException(
                            "shapeSubstitution customization found for shape "
                            + errorShape + ", but this shape is referenced as an error for operation "
                            + operation.getName());
                }
            }
        }
    }

    /**
     * We only handle emitAsShape in the pre-process stage; emitAsMember is
     * handled in post-process stage, after the marshaller/unmarshaller location
     * names are calculated in the intermediate model.
     */
    private void preprocessSubstituteShapeReferencesInShape(
            String shapeName, Shape shape, ServiceModel serviceModel) {

        // structure members
        if (shape.getMembers() != null) {
            for (Entry<String, Member> entry : shape.getMembers().entrySet()) {
                String memberName = entry.getKey();
                Member member = entry.getValue();
                String memberShapeName = member.getShape();
                Shape memberShape = serviceModel.getShapes().get(memberShapeName);

                // First check if it's a list-type member and that the shape of
                // its list element should be substituted
                if (Utils.isListShape(memberShape)) {
                    Member nestedListMember = memberShape.getListMember();
                    String nestedListMemberOriginalShape = nestedListMember.getShape();

                    ShapeSubstitution appliedSubstitutionOnListMember = substituteMemberShape(nestedListMember);
                    if (appliedSubstitutionOnListMember != null &&
                        appliedSubstitutionOnListMember.getEmitFromMember() != null) {
                        // we will handle the emitFromMember customizations in post-process stage
                        trackListMemberSubstitution(shapeName, memberName, nestedListMemberOriginalShape);
                    }
                } else {
                    // Then check if the shape of the member itself is to be substituted
                    ShapeSubstitution appliedSubstitution = substituteMemberShape(member);
                    if (appliedSubstitution != null &&
                        appliedSubstitution.getEmitFromMember() != null) {
                        // we will handle the emitFromMember customizations in post-process stage
                        trackShapeMemberSubstitution(shapeName, memberName, memberShapeName);
                    }
                }

            }
        } else if (shape.getMapKeyType() != null) {
            // no need to check if the shape is a list, since a list shape is
            // always referenced by a top-level structure shape and that's already
            // handled by the code above

            // map key is not allowed to be substituted
            String mapKeyShape = shape.getMapKeyType().getShape();

            if (shapeSubstitutions.containsKey(mapKeyShape)) {
                throw new IllegalStateException(
                        "shapeSubstitution customization found for shape "
                        + mapKeyShape + ", but this shape is the key for a map shape.");
            }
        } else if (shape.getMapValueType() != null) {
            // map value is not allowed to be substituted
            String mapValShape = shape.getMapValueType().getShape();

            if (shapeSubstitutions.containsKey(mapValShape)) {
                throw new IllegalStateException(
                        "shapeSubstitution customization found for shape "
                        + mapValShape + ", but this shape is the value for a map shape.");
            }
        }
    }

    /**
     * @return the ShapeSubstitution customization that should be applied to
     *         this member, or null if there is no such customization specified
     *         for this member.
     */
    private ShapeSubstitution substituteMemberShape(Member member) {
        ShapeSubstitution substitute = shapeSubstitutions.get(member.getShape());

        if (substitute != null) {
            member.setShape(substitute.getEmitAsShape());
            return substitute;
        }

        return null;
    }

    private void postprocessHandleEmitAsMember(
            ShapeModel shape, IntermediateModel intermediateModel) {

        /*
         * For structure members whose shape is substituted, we need to add the
         * additional marshalling/unmarshalling path to the corresponding member
         * model
         */
        for (Entry<String, Map<String, String>> ref : substitutedShapeMemberReferences.entrySet()) {
            String parentShapeC2jName = ref.getKey();
            Map<String, String> memberOriginalShapeMap = ref.getValue();

            ShapeModel parentShape = Utils.findShapeModelByC2jName(
                    intermediateModel, parentShapeC2jName);

            for (Entry<String, String> entry : memberOriginalShapeMap.entrySet()) {
                String memberC2jName = entry.getKey();
                String originalShapeC2jName = entry.getValue();

                MemberModel member = parentShape.findMemberModelByC2jName(memberC2jName);

                ShapeModel originalShape = Utils.findShapeModelByC2jName(intermediateModel, originalShapeC2jName);

                MemberModel emitFromMember =
                        originalShape.findMemberModelByC2jName(
                                shapeSubstitutions.get(originalShapeC2jName)
                                                  .getEmitFromMember());
                // Pass in the original member model's marshalling/unmarshalling location name

                /**
                 * This customization is specifically added for
                 * EC2 where we replace all occurrences of AttributeValue with Value in
                 * the model classes. However the wire representation is not changed.
                 *
                 * TODO This customization has been added to preserve backwards
                 * compatibility of EC2 APIs. This should be removed as part of next major
                 * version bump.
                 */
                if (!shouldSkipAddingMarshallingPath(shapeSubstitutions.get(originalShapeC2jName), parentShapeC2jName)) {
                    member.getHttp().setAdditionalMarshallingPath(
                            emitFromMember.getHttp().getMarshallLocationName());
                }
                member.getHttp().setAdditionalUnmarshallingPath(
                        emitFromMember.getHttp().getUnmarshallLocationName());
            }
        }

        /*
         * For list shapes whose member shape is substituted, we need to add the
         * additional path into the "http" metadata of all the shape members
         * that reference to this list-type shape.
         */
        for (Entry<String, Map<String, String>> ref : substitutedListMemberReferences.entrySet()) {
            String parentShapeC2jName = ref.getKey();
            // {listTypeMemberName -> nestedListMemberOriginalShape}
            Map<String, String> nestedListMemberOriginalShapeMap = ref.getValue();

            ShapeModel parentShape = Utils.findShapeModelByC2jName(
                    intermediateModel, parentShapeC2jName);

            for (Entry<String, String> entry : nestedListMemberOriginalShapeMap.entrySet()) {
                String listTypeMemberC2jName = entry.getKey();
                String nestedListMemberOriginalShapeC2jName = entry.getValue();

                MemberModel listTypeMember = parentShape.findMemberModelByC2jName(listTypeMemberC2jName);

                ShapeModel nestedListMemberOriginalShape = Utils.findShapeModelByC2jName(intermediateModel,
                        nestedListMemberOriginalShapeC2jName);

                MemberModel emitFromMember =
                        nestedListMemberOriginalShape.findMemberModelByC2jName(
                                shapeSubstitutions
                                        .get(nestedListMemberOriginalShapeC2jName)
                                        .getEmitFromMember()
                                                                              );

                /**
                 * This customization is specifically added for
                 * EC2 where we replace all occurrences of AttributeValue with Value in
                 * the model classes. However the wire representation is not changed.
                 *
                 * TODO This customization has been added to preserve backwards
                 * compatibility of EC2 APIs. This should be removed as part of next major
                 * version bump.
                 */
                if (!shouldSkipAddingMarshallingPath(shapeSubstitutions.get(nestedListMemberOriginalShapeC2jName),
                        parentShapeC2jName)) {
                    listTypeMember.getListModel().setMemberAdditionalMarshallingPath(
                            emitFromMember.getHttp().getMarshallLocationName());
                }
                listTypeMember.getListModel().setMemberAdditionalUnmarshallingPath(
                        emitFromMember.getHttp().getUnmarshallLocationName());
            }
        }
    }

    private void trackShapeMemberSubstitution(String shapeName, String memberName, String originalShape) {
        log.info("{} -> ({} -> {})", shapeName, memberName, originalShape);
        if (!substitutedShapeMemberReferences.containsKey(shapeName)) {
            substitutedShapeMemberReferences.put(shapeName, new HashMap<>());
        }
        substitutedShapeMemberReferences.get(shapeName).put(memberName, originalShape);
    }

    private void trackListMemberSubstitution(String shapeName, String listTypeMemberName, String nestedListMemberOriginalShape) {
        log.info("{} -> ({} -> {})", shapeName, listTypeMemberName, nestedListMemberOriginalShape);
        if (!substitutedListMemberReferences.containsKey(shapeName)) {
            substitutedListMemberReferences.put(shapeName, new HashMap<>());
        }
        substitutedListMemberReferences.get(shapeName).put(listTypeMemberName, nestedListMemberOriginalShape);
    }

    private boolean shouldSkipAddingMarshallingPath(ShapeSubstitution substitutionConfig,
                                                    String parentShapeName) {
        return substitutionConfig.getSkipMarshallPathForShapes() == null
               ? false
               : substitutionConfig.getSkipMarshallPathForShapes().contains(parentShapeName);
    }

}
