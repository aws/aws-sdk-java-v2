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

package software.amazon.awssdk.codegen.smithy.customization.processors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.ModifyModelShapeModifier;
import software.amazon.awssdk.codegen.model.config.customization.ShapeModifier;
import software.amazon.awssdk.codegen.model.config.customization.SmithyMemberDefinition;
import software.amazon.awssdk.codegen.model.config.customization.SmithyModifyShapeModifier;
import software.amazon.awssdk.codegen.model.config.customization.SmithyShapeModifier;
import software.amazon.awssdk.codegen.model.intermediate.EnumModel;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.smithy.customization.ShapeIdResolver;
import software.amazon.awssdk.utils.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Smithy equivalent of {@link software.amazon.awssdk.codegen.customization.processors.ShapeModifiersProcessor}.
 *
 * <p>This is a Category C processor (dual-config pattern). It modifies shapes in the Smithy model:
 * exclude members, inject members, modify member properties, exclude entire shapes, and mark shapes
 * as unions. In postprocess, it handles operations that work on the IntermediateModel (emitEnumName,
 * emitEnumValue, excludeShape skip flags, emitPropertyName with existingNameDeprecated,
 * alternateBeanPropertyName).
 *
 * <p>Follows the dual-config pattern: accepts both old C2J {@link ShapeModifier} map and new
 * Smithy-native {@link SmithyShapeModifier} map. Old C2J {@link Member} objects in inject are
 * converted to {@link SmithyMemberDefinition}, and {@link ModifyModelShapeModifier} objects in
 * modify are converted to {@link SmithyModifyShapeModifier} (dropping C2J-only fields).
 */
public class ShapeModifiersProcessor
        extends AbstractDualConfigProcessor<Map<String, ShapeModifier>, Map<String, SmithyShapeModifier>> {

    private static final Logger log = Logger.loggerFor(ShapeModifiersProcessor.class);
    private static final String ALL = "*";

    /**
     * Resolved config stored during preprocess for use in postprocess.
     */
    private Map<String, SmithyShapeModifier> resolvedConfig;

    public ShapeModifiersProcessor(Map<String, ShapeModifier> oldConfig,
                                         Map<String, SmithyShapeModifier> newConfig) {
        super(oldConfig, newConfig, "shapeModifiers", "smithyShapeModifiers");
    }

    @Override
    protected boolean isSet(Object config) {
        if (config instanceof Map) {
            return !((Map<?, ?>) config).isEmpty();
        }
        return config != null;
    }

    // -----------------------------------------------------------------------
    // convertOldToNew: C2J ShapeModifier → SmithyShapeModifier
    // -----------------------------------------------------------------------

    @Override
    protected Map<String, SmithyShapeModifier> convertOldToNew(Map<String, ShapeModifier> old,
                                                                Model model,
                                                                ServiceShape service) {
        Map<String, SmithyShapeModifier> result = new LinkedHashMap<>();
        for (Map.Entry<String, ShapeModifier> entry : old.entrySet()) {
            result.put(entry.getKey(), convertShapeModifier(entry.getValue()));
        }
        return result;
    }

    private SmithyShapeModifier convertShapeModifier(ShapeModifier old) {
        SmithyShapeModifier smithy = new SmithyShapeModifier();
        smithy.setExcludeShape(old.isExcludeShape());
        smithy.setExclude(old.getExclude());
        smithy.setUnion(old.isUnion());

        // Convert inject: C2J Map<String, Member> list → List<SmithyMemberDefinition>
        if (old.getInject() != null) {
            List<SmithyMemberDefinition> smithyInjects = new ArrayList<>();
            for (Map<String, Member> injectMap : old.getInject()) {
                for (Map.Entry<String, Member> e : injectMap.entrySet()) {
                    SmithyMemberDefinition def = new SmithyMemberDefinition();
                    def.setName(e.getKey());
                    // C2J Member.getShape() is a simple name; will be resolved at apply time
                    def.setTarget(e.getValue().getShape());
                    smithyInjects.add(def);
                }
            }
            smithy.setInject(smithyInjects);
        }

        // Convert modify: ModifyModelShapeModifier → SmithyModifyShapeModifier
        if (old.getModify() != null) {
            List<Map<String, SmithyModifyShapeModifier>> smithyModifies = new ArrayList<>();
            for (Map<String, ModifyModelShapeModifier> modMap : old.getModify()) {
                Map<String, SmithyModifyShapeModifier> converted = new LinkedHashMap<>();
                for (Map.Entry<String, ModifyModelShapeModifier> e : modMap.entrySet()) {
                    converted.put(e.getKey(), convertModifier(e.getValue()));
                }
                smithyModifies.add(converted);
            }
            smithy.setModify(smithyModifies);
        }

        return smithy;
    }

    private SmithyModifyShapeModifier convertModifier(ModifyModelShapeModifier old) {
        SmithyModifyShapeModifier smithy = new SmithyModifyShapeModifier();
        smithy.setDeprecated(old.isDeprecated());
        smithy.setDeprecatedMessage(old.getDeprecatedMessage());
        smithy.setExistingNameDeprecated(old.isExistingNameDeprecated());
        smithy.setEmitPropertyName(old.getEmitPropertyName());
        smithy.setEmitEnumName(old.getEmitEnumName());
        smithy.setEmitEnumValue(old.getEmitEnumValue());
        smithy.setAlternateBeanPropertyName(old.getAlternateBeanPropertyName());
        smithy.setEmitAsType(old.getEmitAsType());
        // marshallLocationName, unmarshallLocationName, ignoreDataTypeConversionFailures
        // are dropped — they are C2J serialization concepts not applicable to Smithy
        return smithy;
    }

    // -----------------------------------------------------------------------
    // applySmithyLogic: apply shape modifications to the Smithy Model
    // -----------------------------------------------------------------------

    @Override
    protected Model applySmithyLogic(Model model, ServiceShape service,
                                      Map<String, SmithyShapeModifier> config) {
        // Store resolved config for postprocess
        this.resolvedConfig = config;

        Model current = model;
        for (Map.Entry<String, SmithyShapeModifier> entry : config.entrySet()) {
            String key = entry.getKey();
            SmithyShapeModifier modifier = entry.getValue();

            if (ALL.equals(key)) {
                // Apply to all structure shapes in the service closure
                Set<Shape> serviceShapes = new Walker(current).walkShapes(
                    current.expectShape(service.getId()));
                for (Shape shape : serviceShapes) {
                    if (shape.isStructureShape()) {
                        current = applyModifier(current, service,
                            shape.asStructureShape().get(), modifier);
                    }
                }
            } else {
                ShapeId shapeId = ShapeIdResolver.resolve(current, service, key);
                Shape shape = current.expectShape(shapeId);
                if (!shape.isStructureShape()) {
                    throw new IllegalStateException(
                        "ShapeModifier target '" + key + "' is not a structure shape.");
                }
                current = applyModifier(current, service,
                    shape.asStructureShape().get(), modifier);
            }
        }
        return current;
    }

    /**
     * Applies a single shape modifier to a structure shape. Operations are applied in order:
     * excludeShape, exclude members, inject members, modify members, union.
     */
    private Model applyModifier(Model model, ServiceShape service,
                                StructureShape shape, SmithyShapeModifier modifier) {
        Model current = model;

        // 1. excludeShape: remove the entire shape
        if (modifier.isExcludeShape()) {
            return ModelTransformer.create().removeShapes(current,
                Collections.singleton(shape));
        }

        // 2. exclude: remove listed members from the structure
        if (modifier.getExclude() != null) {
            current = excludeMembers(current, shape, modifier.getExclude());
            // Re-fetch shape after modification
            shape = current.expectShape(shape.getId(), StructureShape.class);
        }

        // 3. inject: add new members to the structure
        if (modifier.getInject() != null) {
            current = injectMembers(current, service, shape, modifier.getInject());
            // Re-fetch shape after modification
            shape = current.expectShape(shape.getId(), StructureShape.class);
        }

        // 4. modify: rename members, retarget types, apply deprecated trait
        if (modifier.getModify() != null) {
            current = modifyMembers(current, service, shape, modifier.getModify());
            // Re-fetch shape after modification
            shape = current.expectShape(shape.getId(), StructureShape.class);
        }

        // 5. union: mark the shape as a union by converting to UnionShape
        if (modifier.isUnion() != null && modifier.isUnion()) {
            current = applyUnion(current, shape);
        }

        return current;
    }

    private Model excludeMembers(Model model, StructureShape shape, List<String> membersToExclude) {
        StructureShape.Builder builder = shape.toBuilder();
        for (String memberName : membersToExclude) {
            builder.removeMember(memberName);
        }
        return model.toBuilder()
                    .addShape(builder.build())
                    .build();
    }

    private Model injectMembers(Model model, ServiceShape service,
                                StructureShape shape, List<SmithyMemberDefinition> members) {
        StructureShape.Builder builder = shape.toBuilder();
        String namespace = ShapeIdResolver.namespace(service);

        for (SmithyMemberDefinition def : members) {
            ShapeId targetId = resolveTargetShapeId(model, service, namespace, def.getTarget());
            MemberShape member = MemberShape.builder()
                .id(shape.getId().withMember(def.getName()))
                .target(targetId)
                .build();
            builder.addMember(member);
        }

        return model.toBuilder()
                    .addShape(builder.build())
                    .build();
    }

    /**
     * Resolves a target string to a ShapeId. If the target contains '#', it is treated as a
     * fully-qualified ShapeId. Otherwise, it is resolved as a simple name in the service namespace.
     */
    private ShapeId resolveTargetShapeId(Model model, ServiceShape service,
                                         String namespace, String target) {
        if (target.contains("#")) {
            return ShapeId.from(target);
        }
        return ShapeIdResolver.resolve(model, service, target);
    }

    private Model modifyMembers(Model model, ServiceShape service, StructureShape shape,
                                List<Map<String, SmithyModifyShapeModifier>> modifyList) {
        Model current = model;
        StructureShape currentShape = shape;

        for (Map<String, SmithyModifyShapeModifier> modMap : modifyList) {
            for (Map.Entry<String, SmithyModifyShapeModifier> entry : modMap.entrySet()) {
                String memberName = entry.getKey();
                SmithyModifyShapeModifier modifyModel = entry.getValue();
                current = doModifyMember(current, service, currentShape, memberName, modifyModel);
                currentShape = current.expectShape(shape.getId(), StructureShape.class);
            }
        }

        return current;
    }

    private Model doModifyMember(Model model, ServiceShape service, StructureShape shape,
                                 String memberName, SmithyModifyShapeModifier modifyModel) {
        Model current = model;

        // Apply @deprecated trait to the member
        if (modifyModel.isDeprecated()) {
            current = applyDeprecatedTrait(current, shape, memberName, modifyModel.getDeprecatedMessage());
            shape = current.expectShape(shape.getId(), StructureShape.class);
        }

        // Rename the member (emitPropertyName)
        if (modifyModel.getEmitPropertyName() != null) {
            current = renameMember(current, shape, memberName, modifyModel.getEmitPropertyName());
            shape = current.expectShape(shape.getId(), StructureShape.class);
        }

        // Retarget the member to a different type (emitAsType)
        if (modifyModel.getEmitAsType() != null) {
            String actualMemberName = modifyModel.getEmitPropertyName() != null
                                      ? modifyModel.getEmitPropertyName()
                                      : memberName;
            current = retargetMemberType(current, service, shape, actualMemberName,
                modifyModel.getEmitAsType());
        }

        return current;
    }

    private Model applyDeprecatedTrait(Model model, StructureShape shape,
                                       String memberName, String deprecatedMessage) {
        MemberShape member = shape.getMember(memberName)
            .orElseThrow(() -> new IllegalStateException(
                "Cannot apply deprecated trait: member '" + memberName
                + "' not found in shape " + shape.getId()));

        DeprecatedTrait.Builder traitBuilder = DeprecatedTrait.builder();
        if (deprecatedMessage != null) {
            traitBuilder.message(deprecatedMessage);
        }

        MemberShape updatedMember = member.toBuilder()
            .addTrait(traitBuilder.build())
            .build();

        StructureShape updatedShape = shape.toBuilder()
            .removeMember(memberName)
            .addMember(updatedMember)
            .build();

        return model.toBuilder()
                    .addShape(updatedShape)
                    .build();
    }

    private Model renameMember(Model model, StructureShape shape,
                               String oldName, String newName) {
        MemberShape oldMember = shape.getMember(oldName)
            .orElseThrow(() -> new IllegalStateException(
                "Cannot rename member: '" + oldName + "' not found in shape " + shape.getId()));

        // Create new member with the new name but same target and traits
        MemberShape newMember = MemberShape.builder()
            .id(shape.getId().withMember(newName))
            .target(oldMember.getTarget())
            .traits(oldMember.getAllTraits().values())
            .build();

        StructureShape updatedShape = shape.toBuilder()
            .removeMember(oldName)
            .addMember(newMember)
            .build();

        return model.toBuilder()
                    .removeShape(oldMember.getId())
                    .addShape(updatedShape)
                    .build();
    }

    private Model retargetMemberType(Model model, ServiceShape service,
                                     StructureShape shape, String memberName,
                                     String emitAsType) {
        String namespace = ShapeIdResolver.namespace(service);
        String syntheticShapeName = "SDK_" + emitAsType;
        ShapeId syntheticId = ShapeId.from(namespace + "#" + syntheticShapeName);

        // Create a synthetic shape of the specified type if it doesn't already exist
        Model current = model;
        if (!current.getShape(syntheticId).isPresent()) {
            Shape syntheticShape = createSyntheticShape(syntheticId, emitAsType);
            current = current.toBuilder()
                             .addShape(syntheticShape)
                             .build();
        }

        // Retarget the member to the synthetic shape
        MemberShape member = shape.getMember(memberName)
            .orElseThrow(() -> new IllegalStateException(
                "Cannot retarget member: '" + memberName + "' not found in shape " + shape.getId()));

        MemberShape updatedMember = member.toBuilder()
            .target(syntheticId)
            .build();

        StructureShape updatedShape = shape.toBuilder()
            .removeMember(memberName)
            .addMember(updatedMember)
            .build();

        return current.toBuilder()
                      .addShape(updatedShape)
                      .build();
    }

    /**
     * Creates a synthetic Smithy shape of the specified type. Uses Smithy's abstract shape
     * builders to create the appropriate shape type.
     */
    private Shape createSyntheticShape(ShapeId id, String typeName) {
        switch (typeName) {
            case "string":
                return software.amazon.smithy.model.shapes.StringShape.builder().id(id).build();
            case "integer":
                return software.amazon.smithy.model.shapes.IntegerShape.builder().id(id).build();
            case "long":
                return software.amazon.smithy.model.shapes.LongShape.builder().id(id).build();
            case "double":
                return software.amazon.smithy.model.shapes.DoubleShape.builder().id(id).build();
            case "float":
                return software.amazon.smithy.model.shapes.FloatShape.builder().id(id).build();
            case "boolean":
                return software.amazon.smithy.model.shapes.BooleanShape.builder().id(id).build();
            case "blob":
                return software.amazon.smithy.model.shapes.BlobShape.builder().id(id).build();
            case "timestamp":
                return software.amazon.smithy.model.shapes.TimestampShape.builder().id(id).build();
            case "bigDecimal":
                return software.amazon.smithy.model.shapes.BigDecimalShape.builder().id(id).build();
            case "bigInteger":
                return software.amazon.smithy.model.shapes.BigIntegerShape.builder().id(id).build();
            case "byte":
                return software.amazon.smithy.model.shapes.ByteShape.builder().id(id).build();
            case "short":
                return software.amazon.smithy.model.shapes.ShortShape.builder().id(id).build();
            default:
                throw new IllegalStateException(
                    "Unsupported emitAsType: '" + typeName + "'. Cannot create synthetic shape.");
        }
    }

    private Model applyUnion(Model model, StructureShape shape) {
        // Convert the structure shape to a union shape by building a UnionShape
        // with the same members
        software.amazon.smithy.model.shapes.UnionShape.Builder unionBuilder =
            software.amazon.smithy.model.shapes.UnionShape.builder().id(shape.getId());

        for (MemberShape member : shape.getAllMembers().values()) {
            unionBuilder.addMember(member);
        }

        return model.toBuilder()
                    .removeShape(shape.getId())
                    .addShape(unionBuilder.build())
                    .build();
    }

    // -----------------------------------------------------------------------
    // postprocess: handle operations that work on the IntermediateModel
    // -----------------------------------------------------------------------

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        if (resolvedConfig == null || resolvedConfig.isEmpty()) {
            return;
        }

        for (Map.Entry<String, SmithyShapeModifier> entry : resolvedConfig.entrySet()) {
            String key = entry.getKey();
            SmithyShapeModifier modifier = entry.getValue();

            if (ALL.equals(key)) {
                continue;
            }

            List<ShapeModel> shapeModels = Utils.findShapesByC2jName(intermediateModel, key);
            if (shapeModels.isEmpty()) {
                // Shape may have been excluded during preprocess; skip silently
                if (modifier.isExcludeShape()) {
                    continue;
                }
                throw new IllegalStateException(String.format(
                    "Cannot find c2j shape [%s] in the intermediate model when processing "
                    + "customization config shapeModifiers.%s", key, key));
            }

            for (ShapeModel shapeModel : shapeModels) {
                postprocessShape(shapeModel, modifier);
            }
        }
    }

    private void postprocessShape(ShapeModel shapeModel, SmithyShapeModifier modifier) {
        if (modifier.isExcludeShape()) {
            shapeModel.getCustomization().setSkipGeneratingModelClass(true);
            shapeModel.getCustomization().setSkipGeneratingMarshaller(true);
            shapeModel.getCustomization().setSkipGeneratingUnmarshaller(true);
        } else if (modifier.getModify() != null) {
            for (Map<String, SmithyModifyShapeModifier> modMap : modifier.getModify()) {
                for (Map.Entry<String, SmithyModifyShapeModifier> entry : modMap.entrySet()) {
                    postprocessModifyMember(shapeModel, entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private void postprocessModifyMember(ShapeModel shapeModel, String memberName,
                                         SmithyModifyShapeModifier modifyModel) {
        if (modifyModel.getEmitEnumName() != null) {
            EnumModel enumModel = shapeModel.findEnumModelByValue(memberName);
            if (enumModel == null) {
                throw new IllegalStateException(String.format(
                    "Cannot find enum [%s] in the intermediate model when processing "
                    + "customization config shapeModifiers.%s", memberName, memberName));
            }
            enumModel.setName(modifyModel.getEmitEnumName());
        }

        if (modifyModel.getEmitEnumValue() != null) {
            EnumModel enumModel = shapeModel.findEnumModelByValue(memberName);
            if (enumModel == null) {
                throw new IllegalStateException(String.format(
                    "Cannot find enum [%s] in the intermediate model when processing "
                    + "customization config shapeModifiers.%s", memberName, memberName));
            }
            enumModel.setValue(modifyModel.getEmitEnumValue());
        }

        if (modifyModel.getEmitPropertyName() != null && modifyModel.isExistingNameDeprecated()) {
            MemberModel memberModel = shapeModel.tryFindMemberModelByC2jName(memberName, false);
            if (memberModel != null) {
                memberModel.setDeprecatedName(memberName);
            }
        }

        if (modifyModel.getAlternateBeanPropertyName() != null) {
            MemberModel memberModel = shapeModel.tryFindMemberModelByC2jName(memberName, false);
            if (memberModel != null) {
                String alternatePropertyName = modifyModel.getAlternateBeanPropertyName();
                String setter = String.format("set%s", alternatePropertyName);
                memberModel.setAdditionalBeanStyleSetterName(setter);
            }
        }

        // marshallLocationName, unmarshallLocationName, ignoreDataTypeConversionFailures,
        // staxTargetDepthOffset are DROPPED — not applicable to Smithy
    }
}
