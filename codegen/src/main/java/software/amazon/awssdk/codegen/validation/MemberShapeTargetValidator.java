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

package software.amazon.awssdk.codegen.validation;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;

/**
 * Validates that every member which is expected to reference a model shape actually resolves to one after member-to-shape
 * linking. A member can be left referencing a target shape that no longer exists in the intermediate model (missing from the
 * service model, removed by a customization, or misspelled), which otherwise surfaces as a cryptic {@link NullPointerException}
 * deep inside code emission.
 *
 * <p>The classification of which members reference a shape mirrors {@code RemoveUnusedShapes.resolveMemberShapes} so that
 * validation and shape retention never disagree.
 */
public final class MemberShapeTargetValidator {

    private MemberShapeTargetValidator() {
    }

    /**
     * Fails fast on the first member whose target shape cannot be resolved.
     *
     * @throws ModelInvalidException if a member references a shape that does not exist in the model.
     */
    public static void validate(IntermediateModel model) {
        for (ShapeModel shape : model.getShapes().values()) {
            if (shape.getMembers() == null) {
                continue;
            }
            for (MemberModel member : shape.getMembers()) {
                validateMember(model, shape, member, member);
            }
        }
    }

    /**
     * @param topLevelMember the member declared directly on the shape; for list/map element members this stays pointed at the
     *                       container member so the error message identifies a member the service team can locate.
     */
    private static void validateMember(IntermediateModel model, ShapeModel shape, MemberModel topLevelMember,
                                       MemberModel member) {
        if (member == null) {
            return;
        }

        if (member.getEnumType() != null) {
            requireResolvable(model, shape, topLevelMember, member, member.getEnumType());
        } else if (member.isList()) {
            validateMember(model, shape, topLevelMember, member.getListModel().getListMemberModel());
        } else if (member.isMap()) {
            MapModel mapModel = member.getMapModel();
            validateMember(model, shape, topLevelMember, mapModel.getKeyModel());
            validateMember(model, shape, topLevelMember, mapModel.getValueModel());
        } else if (!member.isSimple()) {
            requireResolvable(model, shape, topLevelMember, member, member.getVariable().getSimpleType());
        }
    }

    private static void requireResolvable(IntermediateModel model, ShapeModel shape, MemberModel topLevelMember,
                                          MemberModel member, String targetName) {
        // linkMembersToShapes only links members declared directly on a shape, so a list/map element member always carries a
        // null shape; resolve its target by name (matching shape retention) instead of reading the unset getShape().
        boolean resolved = member == topLevelMember ? member.getShape() != null
                                                    : model.getShapes().containsKey(targetName);
        if (!resolved) {
            throw dangling(shape, topLevelMember, targetName);
        }
    }

    private static ModelInvalidException dangling(ShapeModel shape, MemberModel member, String targetName) {
        String detail = String.format(
            "Member '%s' of shape '%s' targets shape '%s' which does not exist in the intermediate model. The target shape "
            + "may be missing from the service model, removed by a customization, or misspelled.",
            member.getC2jName(), shape.getShapeName(), targetName);

        return ModelInvalidException.fromEntry(
            ValidationEntry.create(ValidationErrorId.UNKNOWN_SHAPE_MEMBER, ValidationErrorSeverity.DANGER, detail));
    }
}
