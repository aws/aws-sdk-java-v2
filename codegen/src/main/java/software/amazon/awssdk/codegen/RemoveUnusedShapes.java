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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.intermediate.ExceptionModel;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ReturnTypeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;

/**
 * Removes the un-used shapes from the intermediate model. This is done by
 * re-constructing the shapes map. First add the shapes referenced by the
 * operations and then adding the shapes referenced by each shapes.
 */
final class RemoveUnusedShapes {

    private RemoveUnusedShapes() {
    }

    public static Map<String, ShapeModel> removeUnusedShapes(IntermediateModel model) {

        Map<String, ShapeModel> out = new HashMap<>();
        Map<String, ShapeModel> in = model.getShapes();

        for (OperationModel opModel : model.getOperations().values()) {
            addOperationShapes(opModel, in, out);
        }

        return out;
    }

    private static void addOperationShapes(OperationModel op,
                                           Map<String, ShapeModel> in,
                                           Map<String, ShapeModel> out) {

        VariableModel input = op.getInput();
        if (input != null) {
            addShapeAndMembers(input.getSimpleType(), in, out);
        }

        ReturnTypeModel output = op.getReturnType();
        if (output != null) {
            addShapeAndMembers(output.getReturnType(), in, out);
        }

        List<ExceptionModel> exceptions = op.getExceptions();
        if (op.getExceptions() != null) {
            for (ExceptionModel e : exceptions) {
                addShapeAndMembers(e.getExceptionName(), in, out);
            }
        }
    }

    /**
     * Adds the shape. Recursively adds the shapes represented by its members.
     */
    private static void addShapeAndMembers(String shapeName,
                                           Map<String, ShapeModel> in,
                                           Map<String, ShapeModel> out) {

        if (shapeName == null) {
            return;
        }

        ShapeModel shape = in.get(shapeName);
        if (shape == null) {
            return;
        }

        if (!out.containsKey(shapeName)) {
            out.put(shapeName, in.get(shapeName));
            List<MemberModel> members = shape.getMembers();

            if (members != null) {
                for (MemberModel member : members) {
                    List<String> memberShapes = resolveMemberShapes(member);
                    if (memberShapes == null) {
                        continue;
                    }
                    for (String memberShape : memberShapes) {
                        addShapeAndMembers(memberShape, in, out);
                    }
                }
            }
        }
    }

    /**
     * Recursively resolves the shapes represented by the member. When the member is a map,
     * both the key shape and the value shape of the map will be resolved, so that the
     * returning list could have more than one elements.
     */
    private static List<String> resolveMemberShapes(MemberModel member) {

        if (member == null) {
            return new LinkedList<>();
        }
        if (member.getEnumType() != null) {
            return Collections.singletonList(member.getEnumType());
        } else if (member.isList()) {
            return resolveMemberShapes(member.getListModel().getListMemberModel());
        } else if (member.isMap()) {
            List<String> memberShapes = new LinkedList<>();
            memberShapes.addAll(resolveMemberShapes(member.getMapModel().getKeyModel()));
            memberShapes.addAll(resolveMemberShapes(member.getMapModel().getValueModel()));
            return memberShapes;
        } else if (member.isSimple()) {
            // member is scalar, do nothing
            return new LinkedList<>();
        } else {
            // member is a structure.
            return Collections.singletonList(member.getVariable().getSimpleType());
        }
    }
}
