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

import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.codegen.naming.ShapeInfo;
import software.amazon.awssdk.codegen.smithy.traits.AlternateBeanPropertyNameTrait;
import software.amazon.awssdk.codegen.smithy.traits.DeprecatedNameTrait;
import software.amazon.awssdk.codegen.smithy.traits.EnumConstantNameTrait;
import software.amazon.awssdk.codegen.smithy.traits.IgnoreDataTypeConversionFailuresTrait;
import software.amazon.awssdk.codegen.smithy.traits.SyntheticTrait;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Applies the codegen traits that carry Java-only customizations (see {@code software.amazon.awssdk.codegen.smithy
 * .traits}) to the intermediate model, setting the same fields C2J's {@code AddShapes} sets.
 */
final class SmithyCodegenTraits {

    private SmithyCodegenTraits() {
    }

    /**
     * Read from the member's target shape, as C2J marks the added SDK shape rather than the member.
     */
    static void applySynthetic(MemberModel memberModel, Shape targetShape) {
        memberModel.setSynthetic(targetShape.hasTrait(SyntheticTrait.class));
    }

    static void applyAlternateBeanPropertyName(MemberModel memberModel, MemberShape member) {
        member.getTrait(AlternateBeanPropertyNameTrait.class).ifPresent(
            t -> memberModel.setAdditionalBeanStyleSetterName(String.format("set%s", t.getValue())));
    }

    static void applyIgnoreDataTypeConversionFailures(MemberModel memberModel, MemberShape member) {
        if (member.hasTrait(IgnoreDataTypeConversionFailuresTrait.class)) {
            memberModel.ignoreDataTypeConversionFailures(true);
        }
    }

    static void applyDeprecatedName(MemberModel memberModel, MemberShape member, Shape targetShape,
                                    NamingStrategy namingStrategy, ShapeInfo parentCtx, ShapeInfo shapeCtx) {
        String deprecatedName = member.getTrait(DeprecatedNameTrait.class)
                                      .map(DeprecatedNameTrait::getValue)
                                      .orElse(null);
        if (StringUtils.isBlank(deprecatedName)) {
            return;
        }
        checkForValidDeprecatedName(member.getMemberName(), targetShape);
        memberModel.setDeprecatedName(deprecatedName);
        memberModel.setDeprecatedFluentGetterMethodName(
            namingStrategy.getFluentGetterMethodName(deprecatedName, parentCtx, shapeCtx));
        memberModel.setDeprecatedFluentSetterMethodName(
            namingStrategy.getFluentSetterMethodName(deprecatedName, parentCtx, shapeCtx));
        memberModel.setDeprecatedBeanStyleSetterMethodName(
            namingStrategy.getBeanStyleSetterMethodName(deprecatedName, parentCtx, shapeCtx));
    }

    /**
     * Read from an enum value, not a structure member. Falls back to the name derived from the value.
     */
    static String enumConstantName(MemberShape enumMember, String value, NamingStrategy namingStrategy) {
        return enumMember.getTrait(EnumConstantNameTrait.class)
                         .map(EnumConstantNameTrait::getValue)
                         .orElseGet(() -> namingStrategy.getEnumValueName(value));
    }

    private static void checkForValidDeprecatedName(String memberName, Shape targetShape) {
        if (targetShape.isEnumShape()) {
            throw new IllegalStateException(String.format(
                "Member %s has enum values and a deprecated name. Codegen does not support this.", memberName));
        }
        if (targetShape.isListShape()) {
            throw new IllegalStateException(String.format(
                "Member %s is a list and has a deprecated name. Codegen does not support this.", memberName));
        }
        if (targetShape.isMapShape()) {
            throw new IllegalStateException(String.format(
                "Member %s is a map and has a deprecated name. Codegen does not support this.", memberName));
        }
    }
}
