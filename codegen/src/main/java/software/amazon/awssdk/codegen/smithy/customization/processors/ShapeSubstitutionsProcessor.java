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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.ShapeSubstitution;
import software.amazon.awssdk.codegen.model.config.customization.SmithyShapeSubstitution;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.smithy.customization.ShapeIdResolver;
import software.amazon.awssdk.utils.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

/**
 * Smithy equivalent of {@link software.amazon.awssdk.codegen.customization.processors.ShapeSubstitutionsProcessor}.
 *
 * <p>This is a Category C processor (dual-config pattern). It substitutes shape references in the
 * Smithy model by retargeting all members pointing to the original shape to point to the substitute
 * shape instead. When {@code emitFromMember} is specified, additional marshalling/unmarshalling
 * paths are set in postprocess to reflect the wire representation of the data source member.
 *
 * <p>Follows the dual-config pattern: accepts both old C2J {@link ShapeSubstitution} map and new
 * Smithy-native {@link SmithyShapeSubstitution} map. Old C2J simple names for {@code emitAsShape}
 * and {@code skipMarshallPathForShapes} are resolved to full ShapeId strings via
 * {@link ShapeIdResolver}.
 */
public class ShapeSubstitutionsProcessor
        extends AbstractDualConfigProcessor<Map<String, ShapeSubstitution>, Map<String, SmithyShapeSubstitution>> {

    private static final Logger log = Logger.loggerFor(ShapeSubstitutionsProcessor.class);

    /**
     * Tracks structure member substitutions for postprocess.
     * parentShapeC2jName -> {memberC2jName -> originalShapeC2jName}
     */
    private final Map<String, Map<String, String>> substitutedShapeMemberReferences = new HashMap<>();

    /**
     * Tracks list member substitutions for postprocess.
     * parentShapeC2jName -> {listTypeMemberC2jName -> nestedListMemberOriginalShapeC2jName}
     */
    private final Map<String, Map<String, String>> substitutedListMemberReferences = new HashMap<>();

    /**
     * Resolved config stored during preprocess for use in postprocess.
     */
    private Map<String, SmithyShapeSubstitution> resolvedConfig;

    public ShapeSubstitutionsProcessor(Map<String, ShapeSubstitution> oldConfig,
                                              Map<String, SmithyShapeSubstitution> newConfig) {
        super(oldConfig, newConfig, "shapeSubstitutions", "smithyShapeSubstitutions");
    }

    @Override
    protected boolean isSet(Object config) {
        if (config instanceof Map) {
            return !((Map<?, ?>) config).isEmpty();
        }
        return config != null;
    }

    // -----------------------------------------------------------------------
    // convertOldToNew: C2J ShapeSubstitution → SmithyShapeSubstitution
    // -----------------------------------------------------------------------

    @Override
    protected Map<String, SmithyShapeSubstitution> convertOldToNew(Map<String, ShapeSubstitution> old,
                                                                    Model model,
                                                                    ServiceShape service) {
        Map<String, SmithyShapeSubstitution> result = new LinkedHashMap<>();
        for (Map.Entry<String, ShapeSubstitution> entry : old.entrySet()) {
            result.put(entry.getKey(), convertSubstitution(entry.getValue(), model, service));
        }
        return result;
    }

    private SmithyShapeSubstitution convertSubstitution(ShapeSubstitution old, Model model, ServiceShape service) {
        SmithyShapeSubstitution smithy = new SmithyShapeSubstitution();

        // Resolve emitAsShape simple name to full ShapeId
        if (old.getEmitAsShape() != null) {
            ShapeId resolved = ShapeIdResolver.resolve(model, service, old.getEmitAsShape());
            smithy.setEmitAsShape(resolved.toString());
        }

        // Pass through emitAsType unchanged
        smithy.setEmitAsType(old.getEmitAsType());

        // Pass through emitFromMember unchanged (member names are strings)
        smithy.setEmitFromMember(old.getEmitFromMember());

        // Resolve skipMarshallPathForShapes simple names to full ShapeIds
        if (old.getSkipMarshallPathForShapes() != null) {
            List<String> resolvedShapes = new ArrayList<>();
            for (String simpleName : old.getSkipMarshallPathForShapes()) {
                ShapeId resolved = ShapeIdResolver.resolve(model, service, simpleName);
                resolvedShapes.add(resolved.toString());
            }
            smithy.setSkipMarshallPathForShapes(resolvedShapes);
        }

        return smithy;
    }

    // -----------------------------------------------------------------------
    // applySmithyLogic: retarget members to substitute shapes
    // -----------------------------------------------------------------------

    @Override
    protected Model applySmithyLogic(Model model, ServiceShape service,
                                      Map<String, SmithyShapeSubstitution> config) {
        this.resolvedConfig = config;

        Model current = model;
        for (Map.Entry<String, SmithyShapeSubstitution> entry : config.entrySet()) {
            String originalShapeKey = entry.getKey();
            SmithyShapeSubstitution substitution = entry.getValue();

            // Resolve the original shape
            ShapeId originalShapeId = ShapeIdResolver.resolve(current, service, originalShapeKey);

            // Determine the substitute target ShapeId
            ShapeId substituteShapeId = resolveSubstituteTarget(
                current, service, substitution, originalShapeId);

            // Retarget all members pointing to the original shape to the substitute shape.
            // emitFromMember is handled in postprocess (marshalling/unmarshalling paths),
            // not in preprocess retargeting.
            current = retargetMembers(current, originalShapeId, substituteShapeId,
                                      originalShapeKey, substitution);
        }

        return current;
    }

    /**
     * Resolves the substitute target ShapeId from the substitution config.
     * Handles both {@code emitAsShape} (existing shape) and {@code emitAsType} (synthetic shape).
     */
    private ShapeId resolveSubstituteTarget(Model model, ServiceShape service,
                                            SmithyShapeSubstitution substitution,
                                            ShapeId originalShapeId) {
        if (substitution.getEmitAsShape() != null) {
            String emitAsShape = substitution.getEmitAsShape();
            // SmithyShapeSubstitution uses full ShapeId strings
            if (emitAsShape.contains("#")) {
                ShapeId shapeId = ShapeId.from(emitAsShape);
                if (!model.getShape(shapeId).isPresent()) {
                    throw new IllegalStateException(
                        String.format("emitAsShape '%s' does not exist in the Smithy model.", emitAsShape));
                }
                return shapeId;
            }
            // Fallback: resolve as simple name (shouldn't happen for Smithy-native config)
            return ShapeIdResolver.resolve(model, service, emitAsShape);
        }

        if (substitution.getEmitAsType() != null) {
            // Return the synthetic shape ID; the actual shape is created in retargetMembers
            String namespace = ShapeIdResolver.namespace(service);
            String syntheticShapeName = "SdkCustomization_" + substitution.getEmitAsType();
            return ShapeId.from(namespace + "#" + syntheticShapeName);
        }

        throw new IllegalStateException(
            String.format("Shape substitution for '%s' must specify either emitAsShape or emitAsType.",
                          originalShapeId));
    }

    /**
     * Retargets all members in the model that point to the original shape to point to the
     * effective target shape instead. Also tracks substitutions for postprocess.
     */
    private Model retargetMembers(Model model, ShapeId originalShapeId, ShapeId effectiveTargetId,
                                  String originalShapeKey, SmithyShapeSubstitution substitution) {
        Model.Builder builder = model.toBuilder();
        boolean modified = false;

        // Ensure synthetic shape exists in the model if needed (for emitAsType)
        if (substitution.getEmitAsType() != null && !model.getShape(effectiveTargetId).isPresent()) {
            Shape syntheticShape = createSyntheticShape(effectiveTargetId, substitution.getEmitAsType());
            builder.addShape(syntheticShape);
            modified = true;
        }

        for (Shape shape : model.toSet()) {
            if (shape.isStructureShape()) {
                StructureShape structureShape = shape.asStructureShape().get();
                StructureShape.Builder shapeBuilder = null;

                for (MemberShape member : structureShape.getAllMembers().values()) {
                    if (member.getTarget().equals(originalShapeId)) {
                        // Direct member targeting the original shape — retarget it
                        if (shapeBuilder == null) {
                            shapeBuilder = structureShape.toBuilder();
                        }
                        MemberShape updatedMember = member.toBuilder()
                            .target(effectiveTargetId)
                            .build();
                        shapeBuilder.removeMember(member.getMemberName());
                        shapeBuilder.addMember(updatedMember);

                        // Track for postprocess if emitFromMember is specified
                        if (substitution.getEmitFromMember() != null) {
                            trackShapeMemberSubstitution(
                                structureShape.getId().getName(),
                                member.getMemberName(),
                                originalShapeKey);
                        }
                    } else {
                        // Check if the member targets a list whose member targets the original shape
                        Shape memberTargetShape = model.getShape(member.getTarget()).orElse(null);
                        if (memberTargetShape != null && memberTargetShape.isListShape()) {
                            ListShape listShape = memberTargetShape.asListShape().get();
                            MemberShape listMember = listShape.getMember();
                            if (listMember.getTarget().equals(originalShapeId)) {
                                // Retarget the list's member
                                MemberShape updatedListMember = listMember.toBuilder()
                                    .target(effectiveTargetId)
                                    .build();
                                ListShape updatedList = listShape.toBuilder()
                                    .member(updatedListMember)
                                    .build();
                                builder.addShape(updatedList);
                                modified = true;

                                // Track for postprocess if emitFromMember is specified
                                if (substitution.getEmitFromMember() != null) {
                                    trackListMemberSubstitution(
                                        structureShape.getId().getName(),
                                        member.getMemberName(),
                                        originalShapeKey);
                                }
                            }
                        }
                    }
                }

                if (shapeBuilder != null) {
                    builder.addShape(shapeBuilder.build());
                    modified = true;
                }
            }
        }

        return modified ? builder.build() : model;
    }

    /**
     * Creates a synthetic Smithy shape of the specified type.
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

    private void trackShapeMemberSubstitution(String shapeName, String memberName, String originalShape) {
        log.debug(() -> String.format("%s -> (%s -> %s)", shapeName, memberName, originalShape));
        substitutedShapeMemberReferences.computeIfAbsent(shapeName, k -> new HashMap<>())
                                        .put(memberName, originalShape);
    }

    private void trackListMemberSubstitution(String shapeName, String listTypeMemberName,
                                             String nestedListMemberOriginalShape) {
        log.debug(() -> String.format("%s -> (%s -> %s)", shapeName, listTypeMemberName,
                                      nestedListMemberOriginalShape));
        substitutedListMemberReferences.computeIfAbsent(shapeName, k -> new HashMap<>())
                                       .put(listTypeMemberName, nestedListMemberOriginalShape);
    }

    // -----------------------------------------------------------------------
    // postprocess: handle emitFromMember marshalling/unmarshalling paths
    // -----------------------------------------------------------------------

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        if (resolvedConfig == null || resolvedConfig.isEmpty()) {
            return;
        }

        postprocessShapeMemberSubstitutions(intermediateModel);
        postprocessListMemberSubstitutions(intermediateModel);
    }

    /**
     * For structure members whose shape was substituted, add the additional
     * marshalling/unmarshalling path to the corresponding member model.
     */
    private void postprocessShapeMemberSubstitutions(IntermediateModel intermediateModel) {
        for (Map.Entry<String, Map<String, String>> ref : substitutedShapeMemberReferences.entrySet()) {
            String parentShapeC2jName = ref.getKey();
            Map<String, String> memberOriginalShapeMap = ref.getValue();

            ShapeModel parentShape = Utils.findShapeModelByC2jNameIfExists(intermediateModel, parentShapeC2jName);
            if (parentShape == null) {
                continue;
            }

            for (Map.Entry<String, String> entry : memberOriginalShapeMap.entrySet()) {
                String memberC2jName = entry.getKey();
                String originalShapeC2jName = entry.getValue();

                SmithyShapeSubstitution substitution = resolvedConfig.get(originalShapeC2jName);
                if (substitution == null || substitution.getEmitFromMember() == null) {
                    continue;
                }

                ShapeModel originalShape = Utils.findShapeModelByC2jNameIfExists(
                    intermediateModel, originalShapeC2jName);
                if (originalShape == null) {
                    continue;
                }

                MemberModel member = parentShape.findMemberModelByC2jName(memberC2jName);
                MemberModel emitFromMember = originalShape.findMemberModelByC2jName(
                    substitution.getEmitFromMember());

                if (!shouldSkipAddingMarshallingPath(substitution, parentShapeC2jName)) {
                    member.getHttp().setAdditionalMarshallingPath(
                        emitFromMember.getHttp().getMarshallLocationName());
                }
                member.getHttp().setAdditionalUnmarshallingPath(
                    emitFromMember.getHttp().getUnmarshallLocationName());
            }
        }
    }

    /**
     * For list shapes whose member shape was substituted, add the additional
     * marshalling/unmarshalling path to the list member model.
     */
    private void postprocessListMemberSubstitutions(IntermediateModel intermediateModel) {
        for (Map.Entry<String, Map<String, String>> ref : substitutedListMemberReferences.entrySet()) {
            String parentShapeC2jName = ref.getKey();
            Map<String, String> nestedListMemberOriginalShapeMap = ref.getValue();

            ShapeModel parentShape = Utils.findShapeModelByC2jNameIfExists(intermediateModel, parentShapeC2jName);
            if (parentShape == null) {
                continue;
            }

            for (Map.Entry<String, String> entry : nestedListMemberOriginalShapeMap.entrySet()) {
                String listTypeMemberC2jName = entry.getKey();
                String nestedListMemberOriginalShapeC2jName = entry.getValue();

                SmithyShapeSubstitution substitution = resolvedConfig.get(nestedListMemberOriginalShapeC2jName);
                if (substitution == null || substitution.getEmitFromMember() == null) {
                    continue;
                }

                ShapeModel nestedListMemberOriginalShape = Utils.findShapeModelByC2jNameIfExists(
                    intermediateModel, nestedListMemberOriginalShapeC2jName);
                if (nestedListMemberOriginalShape == null) {
                    continue;
                }

                MemberModel listTypeMember = parentShape.findMemberModelByC2jName(listTypeMemberC2jName);
                MemberModel emitFromMember = nestedListMemberOriginalShape.findMemberModelByC2jName(
                    substitution.getEmitFromMember());

                if (!shouldSkipAddingMarshallingPath(substitution, parentShapeC2jName)) {
                    listTypeMember.getListModel().setMemberAdditionalMarshallingPath(
                        emitFromMember.getHttp().getMarshallLocationName());
                }
                listTypeMember.getListModel().setMemberAdditionalUnmarshallingPath(
                    emitFromMember.getHttp().getUnmarshallLocationName());
            }
        }
    }

    /**
     * Checks whether the additional marshalling path should be skipped for the given parent shape.
     * In the Smithy-native config, skipMarshallPathForShapes uses full ShapeId strings, but we
     * compare against the simple name (C2J name) since that's what we track in postprocess.
     */
    private boolean shouldSkipAddingMarshallingPath(SmithyShapeSubstitution substitution,
                                                    String parentShapeC2jName) {
        if (substitution.getSkipMarshallPathForShapes() == null) {
            return false;
        }
        for (String shapeIdStr : substitution.getSkipMarshallPathForShapes()) {
            // Compare against both the full ShapeId and the simple name for compatibility
            if (shapeIdStr.equals(parentShapeC2jName)) {
                return true;
            }
            if (shapeIdStr.contains("#")) {
                String simpleName = ShapeId.from(shapeIdStr).getName();
                if (simpleName.equals(parentShapeC2jName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
