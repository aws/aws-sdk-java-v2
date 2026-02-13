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

import static software.amazon.awssdk.codegen.internal.TypeUtils.getDataTypeMapping;
import static software.amazon.awssdk.codegen.internal.Utils.capitalize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.model.intermediate.EnumModel;
import software.amazon.awssdk.codegen.model.intermediate.ListModel;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.ParameterHttpMapping;
import software.amazon.awssdk.codegen.model.intermediate.ReturnTypeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeMarshaller;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.intermediate.ShapeUnmarshaller;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;
import software.amazon.awssdk.codegen.model.service.Location;
import software.amazon.awssdk.codegen.naming.DefaultSmithyNamingStrategy;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.RetryableTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.UniqueItemsTrait;

/**
 * Processes Smithy shapes and converts them to intermediate ShapeModels.
 */
final class AddSmithyShapes {
    private final Model model;
    private final ServiceShape service;
    private final DefaultSmithyNamingStrategy namingStrategy;
    private final TopDownIndex topDownIndex;
    private final String protocol;

    AddSmithyShapes(SmithyIntermediateModelBuilder builder) {
        this.model = builder.getSmithyModel();
        this.service = builder.getService();
        this.namingStrategy = (DefaultSmithyNamingStrategy) builder.getNamingStrategy();
        this.topDownIndex = TopDownIndex.of(model);
        this.protocol = software.amazon.awssdk.codegen.utils.ProtocolUtils.resolveProtocol(
            builder.getServiceIndex(), service);
    }

    /**
     * Constructs all shapes (input, output, exception, and model shapes) from the Smithy model.
     */
    public Map<String, ShapeModel> constructShapes() {
        Map<String, ShapeModel> shapes = new HashMap<>();
        shapes.putAll(constructInputShapes());
        shapes.putAll(constructOutputShapes());
        shapes.putAll(constructExceptionShapes());
        shapes.putAll(constructModelShapes(shapes));
        return shapes;
    }

    private Map<String, ShapeModel> constructInputShapes() {
        Map<String, ShapeModel> shapes = new HashMap<>();
        for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
            if (operation.getInput().isPresent()) {
                ShapeId inputShapeId = operation.getInput().get();
                StructureShape inputShape = model.expectShape(inputShapeId, StructureShape.class);
                String javaClassName = namingStrategy.getRequestClassName(operation.toShapeId().getName());
                ShapeModel shapeModel = generateShapeModel(javaClassName, inputShape);
                shapeModel.setType(ShapeType.Request.getValue());

                // Set marshaller from operation's HTTP trait
                ShapeMarshaller marshaller = new ShapeMarshaller()
                    .withAction(operation.toShapeId().getName())
                    .withProtocol(protocol);
                operation.getTrait(HttpTrait.class).ifPresent(httpTrait -> {
                    marshaller.withVerb(httpTrait.getMethod());
                    marshaller.withRequestUri(httpTrait.getUri().toString());
                });
                if (Metadata.usesOperationIdentifier(protocol)) {
                    marshaller.withTarget(operation.toShapeId().getName());
                }
                shapeModel.setMarshaller(marshaller);

                shapes.put(javaClassName, shapeModel);
            }
        }
        return shapes;
    }

    private Map<String, ShapeModel> constructOutputShapes() {
        Map<String, ShapeModel> shapes = new HashMap<>();
        for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
            String javaClassName = namingStrategy.getResponseClassName(operation.toShapeId().getName());
            if (operation.getOutput().isPresent()) {
                ShapeId outputShapeId = operation.getOutput().get();
                if (outputShapeId.toString().equals("smithy.api#Unit")) {
                    ShapeModel emptyShape = createEmptyResponseShape(javaClassName);
                    emptyShape.setUnmarshaller(new ShapeUnmarshaller());
                    shapes.put(javaClassName, emptyShape);
                } else {
                    StructureShape outputShape = model.expectShape(outputShapeId, StructureShape.class);
                    ShapeModel shapeModel = generateShapeModel(javaClassName, outputShape);
                    shapeModel.setType(ShapeType.Response.getValue());
                    shapeModel.setUnmarshaller(new ShapeUnmarshaller());
                    shapes.put(javaClassName, shapeModel);
                }
            } else {
                ShapeModel emptyShape = createEmptyResponseShape(javaClassName);
                emptyShape.setUnmarshaller(new ShapeUnmarshaller());
                shapes.put(javaClassName, emptyShape);
            }
        }
        return shapes;
    }

    private ShapeModel createEmptyResponseShape(String javaClassName) {
        ShapeModel shapeModel = new ShapeModel(javaClassName);
        shapeModel.setShapeName(javaClassName);
        shapeModel.setType(ShapeType.Response.getValue());
        String variableName = namingStrategy.getVariableName(javaClassName);
        shapeModel.setVariable(new VariableModel(variableName, javaClassName));
        return shapeModel;
    }

    private Map<String, ShapeModel> constructExceptionShapes() {
        Map<String, ShapeModel> shapes = new HashMap<>();
        for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
            for (ShapeId errorShapeId : operation.getErrors()) {
                StructureShape errorShape = model.expectShape(errorShapeId, StructureShape.class);
                String javaClassName = namingStrategy.getExceptionName(errorShapeId.getName());
                if (!shapes.containsKey(javaClassName)) {
                    ShapeModel shapeModel = generateShapeModel(javaClassName, errorShape);
                    shapeModel.setType(ShapeType.Exception.getValue());

                    // Set error code (the shape name, matching C2J behavior)
                    shapeModel.setErrorCode(errorShapeId.getName());

                    // Set HTTP status code from @httpError trait or @error trait
                    errorShape.getTrait(software.amazon.smithy.model.traits.HttpErrorTrait.class).ifPresent(httpError -> {
                        shapeModel.setHttpStatusCode(httpError.getCode());
                    });

                    shapes.put(javaClassName, shapeModel);
                }
            }
        }
        return shapes;
    }

    /**
     * Recursively discovers and constructs all model shapes (structures and enums)
     * referenced by the already-constructed shapes.
     */
    private Map<String, ShapeModel> constructModelShapes(Map<String, ShapeModel> existingShapes) {
        Map<String, ShapeModel> modelShapes = new HashMap<>();
        Set<ShapeId> processedShapeIds = new HashSet<>();
        Set<ShapeId> toProcess = new HashSet<>();

        // Collect all shape IDs referenced by inputs, outputs, and exceptions
        for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
            if (operation.getInput().isPresent()) {
                StructureShape inputShape = model.expectShape(operation.getInput().get(), StructureShape.class);
                collectReferencedShapes(inputShape, toProcess);
            }
            if (operation.getOutput().isPresent()) {
                ShapeId outputShapeId = operation.getOutput().get();
                if (!outputShapeId.toString().equals("smithy.api#Unit")) {
                    StructureShape outputShape = model.expectShape(outputShapeId, StructureShape.class);
                    collectReferencedShapes(outputShape, toProcess);
                }
            }
            for (ShapeId errorShapeId : operation.getErrors()) {
                StructureShape errorShape = model.expectShape(errorShapeId, StructureShape.class);
                collectReferencedShapes(errorShape, toProcess);
            }
        }

        while (!toProcess.isEmpty()) {
            ShapeId shapeId = toProcess.iterator().next();
            toProcess.remove(shapeId);

            if (processedShapeIds.contains(shapeId)) {
                continue;
            }
            processedShapeIds.add(shapeId);

            if (shapeId.getNamespace().equals("smithy.api")) {
                continue;
            }

            Shape shape = model.expectShape(shapeId);

            if (shape.isStructureShape()) {
                StructureShape structureShape = shape.asStructureShape().get();
                String javaClassName = namingStrategy.getShapeClassName(shapeId.getName());
                if (!existingShapes.containsKey(javaClassName) && !modelShapes.containsKey(javaClassName)) {
                    ShapeModel shapeModel = generateShapeModel(javaClassName, structureShape);
                    shapeModel.setType(ShapeType.Model.getValue());
                    shapeModel.setUnmarshaller(new ShapeUnmarshaller());
                    modelShapes.put(javaClassName, shapeModel);
                    collectReferencedShapes(structureShape, toProcess);
                }
            } else if (shape.isStringShape()) {
                StringShape stringShape = shape.asStringShape().get();
                if (stringShape.hasTrait(EnumTrait.class)) {
                    String javaClassName = namingStrategy.getShapeClassName(shapeId.getName());
                    if (!existingShapes.containsKey(javaClassName) && !modelShapes.containsKey(javaClassName)) {
                        ShapeModel enumShapeModel = generateEnumShapeModel(javaClassName, stringShape);
                        enumShapeModel.setUnmarshaller(new ShapeUnmarshaller());
                        modelShapes.put(javaClassName, enumShapeModel);
                    }
                }
            }
        }

        return modelShapes;
    }

    private void collectReferencedShapes(StructureShape structure, Set<ShapeId> toProcess) {
        for (MemberShape member : structure.members()) {
            ShapeId targetId = member.getTarget();
            toProcess.add(targetId);
            Shape targetShape = model.expectShape(targetId);
            if (targetShape.isListShape()) {
                toProcess.add(targetShape.asListShape().get().getMember().getTarget());
            } else if (targetShape.isMapShape()) {
                toProcess.add(targetShape.asMapShape().get().getKey().getTarget());
                toProcess.add(targetShape.asMapShape().get().getValue().getTarget());
            }
        }
    }

    private ShapeModel generateEnumShapeModel(String javaClassName, StringShape stringShape) {
        ShapeModel shapeModel = new ShapeModel(stringShape.toShapeId().getName());
        shapeModel.setShapeName(javaClassName);
        shapeModel.setType(ShapeType.Enum.getValue());

        // Always call setDocumentation so null goes through escapeIllegalCharacters (matching C2J)
        shapeModel.setDocumentation(stringShape.getTrait(DocumentationTrait.class)
                                               .map(DocumentationTrait::getValue)
                                               .orElse(null));

        String variableName = namingStrategy.getVariableName(javaClassName);
        shapeModel.setVariable(new VariableModel(variableName, javaClassName));

        EnumTrait enumTrait = stringShape.expectTrait(EnumTrait.class);
        enumTrait.getValues().forEach(enumDef -> {
            String enumValue = enumDef.getValue();
            String enumName = enumDef.getName().orElse(namingStrategy.getEnumValueName(enumValue));
            shapeModel.addEnum(new EnumModel(enumName, enumValue));
        });

        return shapeModel;
    }

    private ShapeModel generateShapeModel(String javaClassName, StructureShape shape) {
        ShapeModel shapeModel = new ShapeModel(shape.toShapeId().getName());
        shapeModel.setShapeName(javaClassName);

        // Always call setDocumentation so null goes through escapeIllegalCharacters (which converts null to "")
        // This matches C2J behavior where Shape.getDocumentation() returns null but setDocumentation normalizes it
        shapeModel.setDocumentation(shape.getTrait(DocumentationTrait.class)
                                         .map(DocumentationTrait::getValue)
                                         .orElse(null));

        String variableName = namingStrategy.getVariableName(javaClassName);
        shapeModel.setVariable(new VariableModel(variableName, javaClassName));

        boolean isException = shape.hasTrait(ErrorTrait.class);
        // In Smithy, unions are a separate shape type (UnionShape), not a trait on structures.
        // Since we only process StructureShape here, isUnion is always false.
        // Union shapes would need separate handling if/when we support them.
        boolean isUnion = false;

        // Set required fields list
        List<String> requiredMembers = new ArrayList<>();
        for (MemberShape member : shape.members()) {
            if (member.hasTrait(RequiredTrait.class)) {
                requiredMembers.add(member.getMemberName());
            }
        }
        if (!requiredMembers.isEmpty()) {
            shapeModel.setRequired(requiredMembers);
        }

        // Set deprecation
        shape.getTrait(DeprecatedTrait.class).ifPresent(dep -> {
            shapeModel.setDeprecated(true);
            dep.getMessage().ifPresent(shapeModel::setDeprecatedMessage);
        });

        // Set fault/retryable/throttling for exception shapes
        if (isException) {
            ErrorTrait errorTrait = shape.expectTrait(ErrorTrait.class);
            // fault is only true for server errors (matching C2J behavior where fault=true means 5xx)
            shapeModel.withIsFault(errorTrait.isServerError());
            shape.getTrait(RetryableTrait.class).ifPresent(retryable -> {
                shapeModel.withIsRetryable(true);
                shapeModel.withIsThrottling(retryable.getThrottling());
            });
        }

        // Set union flag
        shapeModel.withIsUnion(isUnion);

        // Track header/payload/streaming members
        boolean hasHeaderMember = false;
        boolean hasPayloadMember = false;

        // Add members
        for (MemberShape member : shape.members()) {
            // Skip "message" member for exceptions
            if (isException && member.getMemberName().equalsIgnoreCase("message")) {
                continue;
            }

            MemberModel memberModel = generateMemberModel(member, shape, isException, isUnion);
            shapeModel.addMember(memberModel);

            if (memberModel.getHttp().getLocation() == Location.HEADER) {
                hasHeaderMember = true;
            } else if (memberModel.getHttp().getIsPayload()) {
                hasPayloadMember = true;
            }
        }

        shapeModel.withHasHeaderMember(hasHeaderMember)
                  .withHasPayloadMember(hasPayloadMember);

        return shapeModel;
    }

    private MemberModel generateMemberModel(MemberShape memberShape, StructureShape parentShape,
                                            boolean isException, boolean isUnion) {
        String memberName = memberShape.getMemberName();
        ShapeId targetShapeId = memberShape.getTarget();
        Shape targetShape = model.expectShape(targetShapeId);

        // Determine if this member (or its contents) involves enums
        boolean isOrContainsEnum = isOrContainsEnum(targetShape);
        boolean isList = targetShape.isListShape();
        boolean isMap = targetShape.isMapShape();

        // Get the Java type for this member
        String javaType = getJavaType(targetShape);

        // For enums, the variable type is String, but we track the enum class separately
        String variableType = javaType;
        String enumType = null;
        if (targetShape.isStringShape() && targetShape.hasTrait(EnumTrait.class)) {
            variableType = "String";
            enumType = javaType;
        }

        String variableName = namingStrategy.getVariableName(memberName);
        VariableModel variableModel = new VariableModel(variableName, variableType, variableType);

        memberShape.getTrait(DocumentationTrait.class).ifPresent(doc -> {
            variableModel.withDocumentation(doc.getValue());
        });

        MemberModel memberModel = new MemberModel();
        memberModel.withC2jName(memberName)
                   .withC2jShape(targetShapeId.getName())
                   .withName(capitalize(memberName))
                   .withVariable(variableModel)
                   .withSetterModel(new VariableModel(variableName, variableType, variableType))
                   .withGetterModel(new ReturnTypeModel(variableType));

        memberModel.setDocumentation(memberShape.getTrait(DocumentationTrait.class)
                                                .map(DocumentationTrait::getValue)
                                                .orElse(null));

        if (enumType != null) {
            memberModel.withEnumType(enumType);
        }

        // Set fluent method names using Smithy-aware overloads
        memberModel.withFluentGetterMethodName(
            namingStrategy.getFluentGetterMethodName(memberName, isException, isUnion, isOrContainsEnum, isList, isMap));
        memberModel.withFluentEnumGetterMethodName(
            namingStrategy.getFluentEnumGetterMethodName(memberName, isException, isUnion, isOrContainsEnum));
        memberModel.withFluentSetterMethodName(
            namingStrategy.getFluentSetterMethodName(memberName, isException, isUnion, isOrContainsEnum, isList, isMap));
        memberModel.withFluentEnumSetterMethodName(
            namingStrategy.getFluentEnumSetterMethodName(memberName, isException, isUnion, isOrContainsEnum));
        memberModel.withBeanStyleGetterMethodName(
            namingStrategy.getBeanStyleGetterMethodName(memberName, isException, isUnion, isOrContainsEnum));
        memberModel.withBeanStyleSetterMethodName(
            namingStrategy.getBeanStyleSetterMethodName(memberName, isException, isUnion, isOrContainsEnum));
        memberModel.withExistenceCheckMethodName(
            namingStrategy.getExistenceCheckMethodName(memberName, isException, isUnion));
        memberModel.setUnionEnumTypeName(namingStrategy.getUnionEnumTypeName(memberModel));

        // Set required flag
        memberModel.setRequired(memberShape.hasTrait(RequiredTrait.class));

        // Set sensitive flag - check both the member and the target shape
        memberModel.setSensitive(isSensitive(memberShape, targetShape));

        // Set idempotency token
        memberModel.setIdempotencyToken(memberShape.hasTrait(IdempotencyTokenTrait.class));

        // Set HTTP mapping
        memberModel.setHttp(generateHttpMapping(memberShape, memberName));

        // Set timestamp format - check member first, then target shape (matching C2J resolveTimestampFormat)
        memberModel.withTimestampFormat(resolveTimestampFormat(memberShape, targetShape));

        // Set list/map models
        if (targetShape.isListShape()) {
            memberModel.setListModel(generateListModel(targetShape));
        } else if (targetShape.isMapShape()) {
            memberModel.setMapModel(generateMapModel(targetShape));
        }

        return memberModel;
    }

    /**
     * Checks if a shape is sensitive, considering the member trait and the target shape's trait.
     * Also checks container types (list members, map keys/values) recursively.
     */
    private boolean isSensitive(MemberShape memberShape, Shape targetShape) {
        if (memberShape.hasTrait(SensitiveTrait.class) || targetShape.hasTrait(SensitiveTrait.class)) {
            return true;
        }
        // Check container types
        if (targetShape.isListShape()) {
            Shape listMemberTarget = model.expectShape(targetShape.asListShape().get().getMember().getTarget());
            return listMemberTarget.hasTrait(SensitiveTrait.class);
        }
        if (targetShape.isMapShape()) {
            Shape keyTarget = model.expectShape(targetShape.asMapShape().get().getKey().getTarget());
            Shape valueTarget = model.expectShape(targetShape.asMapShape().get().getValue().getTarget());
            return keyTarget.hasTrait(SensitiveTrait.class) || valueTarget.hasTrait(SensitiveTrait.class);
        }
        return false;
    }

    /**
     * Checks if a Smithy shape is an enum or contains enums (for list/map types).
     */
    private boolean isOrContainsEnum(Shape shape) {
        if (shape.isStringShape() && shape.hasTrait(EnumTrait.class)) {
            return true;
        }
        if (shape.isListShape()) {
            Shape memberTarget = model.expectShape(shape.asListShape().get().getMember().getTarget());
            return isOrContainsEnum(memberTarget);
        }
        if (shape.isMapShape()) {
            Shape keyTarget = model.expectShape(shape.asMapShape().get().getKey().getTarget());
            Shape valueTarget = model.expectShape(shape.asMapShape().get().getValue().getTarget());
            return isOrContainsEnum(keyTarget) || isOrContainsEnum(valueTarget);
        }
        return false;
    }

    private ParameterHttpMapping generateHttpMapping(MemberShape memberShape, String memberName) {
        ParameterHttpMapping mapping = new ParameterHttpMapping();

        if (memberShape.hasTrait(HttpHeaderTrait.class)) {
            HttpHeaderTrait headerTrait = memberShape.expectTrait(HttpHeaderTrait.class);
            mapping.withLocation(Location.HEADER)
                   .withUnmarshallLocationName(headerTrait.getValue())
                   .withMarshallLocationName(headerTrait.getValue());
        } else if (memberShape.hasTrait(HttpLabelTrait.class)) {
            mapping.withLocation(Location.URI)
                   .withUnmarshallLocationName(memberName)
                   .withMarshallLocationName(memberName);
        } else if (memberShape.hasTrait(HttpQueryTrait.class)) {
            HttpQueryTrait queryTrait = memberShape.expectTrait(HttpQueryTrait.class);
            mapping.withLocation(Location.QUERY_STRING)
                   .withUnmarshallLocationName(queryTrait.getValue())
                   .withMarshallLocationName(queryTrait.getValue());
        } else if (memberShape.hasTrait(HttpPayloadTrait.class)) {
            mapping.withPayload(true)
                   .withUnmarshallLocationName(memberName)
                   .withMarshallLocationName(memberName);
        } else {
            // Default: body member with member name as location name
            mapping.withUnmarshallLocationName(memberName)
                   .withMarshallLocationName(memberName);
        }

        return mapping;
    }

    private ListModel generateListModel(Shape listShape) {
        MemberShape listMember = listShape.asListShape().get().getMember();
        Shape listMemberTarget = model.expectShape(listMember.getTarget());
        String memberType = getJavaType(listMemberTarget, true);

        // Create a member model for the list element (mirrors C2J's recursive generateMemberModel call)
        MemberModel listMemberModel = createContainerElementMemberModel("member", listMember, listMemberTarget, memberType);

        // If the list member is an enum, set the enum type
        if (listMemberTarget.isStringShape() && listMemberTarget.hasTrait(EnumTrait.class)) {
            listMemberModel.withEnumType(namingStrategy.getShapeClassName(listMember.getTarget().getName()));
        }

        String listImpl = getDataTypeMapping(TypeUtils.TypeKey.LIST_DEFAULT_IMPL);
        String listInterface = getDataTypeMapping(TypeUtils.TypeKey.LIST_INTERFACE);

        return new ListModel(memberType, null, listImpl, listInterface, listMemberModel);
    }

    private MapModel generateMapModel(Shape mapShape) {
        MemberShape keyMember = mapShape.asMapShape().get().getKey();
        MemberShape valueMember = mapShape.asMapShape().get().getValue();
        Shape keyTarget = model.expectShape(keyMember.getTarget());
        Shape valueTarget = model.expectShape(valueMember.getTarget());

        String keyType = getJavaType(keyTarget, true);
        String valueType = getJavaType(valueTarget, true);

        // Create key member model with full naming
        MemberModel keyModel = createContainerElementMemberModel("key", keyMember, keyTarget, keyType);

        // Create value member model with full naming
        MemberModel valueModel = createContainerElementMemberModel("value", valueMember, valueTarget, valueType);

        // If value is an enum, set enum type
        if (valueTarget.isStringShape() && valueTarget.hasTrait(EnumTrait.class)) {
            valueModel.withEnumType(namingStrategy.getShapeClassName(valueMember.getTarget().getName()));
        }

        String mapImpl = getDataTypeMapping(TypeUtils.TypeKey.MAP_DEFAULT_IMPL);
        String mapInterface = getDataTypeMapping(TypeUtils.TypeKey.MAP_INTERFACE);

        return new MapModel(mapImpl, mapInterface, "key", keyModel, "value", valueModel);
    }

    /**
     * Creates a member model for a container element (list member, map key, or map value).
     * Includes all naming fields to match C2J behavior.
     */
    private MemberModel createContainerElementMemberModel(String elementName, MemberShape member,
                                                          Shape targetShape, String javaType) {
        MemberModel memberModel = new MemberModel();
        String variableName = namingStrategy.getVariableName(elementName);

        // Get documentation from the member shape itself (not the target shape).
        // In C2J, list/map member documentation comes from the Member definition,
        // which typically has no documentation. This matches that behavior.
        String documentation = member.getTrait(DocumentationTrait.class)
                                     .map(DocumentationTrait::getValue)
                                     .orElse(null);

        VariableModel variable = new VariableModel(variableName, javaType, javaType);
        variable.withDocumentation(documentation);

        memberModel.withC2jName(elementName)
                   .withC2jShape(member.getTarget().getName())
                   .withName(capitalize(elementName))
                   .withVariable(variable)
                   .withSetterModel(new VariableModel(variableName, javaType, javaType))
                   .withGetterModel(new ReturnTypeModel(javaType));
        memberModel.setDocumentation(documentation);

        boolean isEnum = isOrContainsEnum(targetShape);
        boolean isList = targetShape.isListShape();
        boolean isMap = targetShape.isMapShape();
        memberModel.withFluentGetterMethodName(
            namingStrategy.getFluentGetterMethodName(elementName, false, false, isEnum, isList, isMap));
        memberModel.withFluentEnumGetterMethodName(
            namingStrategy.getFluentEnumGetterMethodName(elementName, false, false, isEnum));
        memberModel.withFluentSetterMethodName(
            namingStrategy.getFluentSetterMethodName(elementName, false, false, isEnum, isList, isMap));
        memberModel.withFluentEnumSetterMethodName(
            namingStrategy.getFluentEnumSetterMethodName(elementName, false, false, isEnum));
        memberModel.withBeanStyleGetterMethodName(
            namingStrategy.getBeanStyleGetterMethodName(elementName, false, false, isEnum));
        memberModel.withBeanStyleSetterMethodName(
            namingStrategy.getBeanStyleSetterMethodName(elementName, false, false, isEnum));
        memberModel.withExistenceCheckMethodName(
            namingStrategy.getExistenceCheckMethodName(elementName, false, false));
        memberModel.setUnionEnumTypeName(namingStrategy.getUnionEnumTypeName(memberModel));

        ParameterHttpMapping mapping = new ParameterHttpMapping();
        mapping.withUnmarshallLocationName(elementName)
               .withMarshallLocationName(elementName);
        memberModel.setHttp(mapping);

        return memberModel;
    }

    /**
     * Resolves the timestamp format for a member, checking the member's trait first,
     * then falling back to the target shape's trait. Converts Smithy format names to
     * C2J format names.
     */
    private String resolveTimestampFormat(MemberShape memberShape, Shape targetShape) {
        // Check member first (like C2J's c2jMemberDefinition.getTimestampFormat())
        if (memberShape.hasTrait(TimestampFormatTrait.class)) {
            return smithyToC2jTimestampFormat(memberShape.expectTrait(TimestampFormatTrait.class).getValue());
        }
        // Fall back to target shape (like C2J's c2jShape.getTimestampFormat())
        if (targetShape.hasTrait(TimestampFormatTrait.class)) {
            return smithyToC2jTimestampFormat(targetShape.expectTrait(TimestampFormatTrait.class).getValue());
        }
        return null;
    }

    /**
     * Converts a Smithy timestamp format string to the equivalent C2J format string.
     * Smithy uses "date-time", "epoch-seconds", "http-date" while C2J uses
     * "iso8601", "unixTimestamp", "rfc822".
     */
    private static String smithyToC2jTimestampFormat(String smithyFormat) {
        switch (smithyFormat) {
            case TimestampFormatTrait.DATE_TIME:
                return "iso8601";
            case TimestampFormatTrait.EPOCH_SECONDS:
                return "unixTimestamp";
            case TimestampFormatTrait.HTTP_DATE:
                return "rfc822";
            default:
                return smithyFormat;
        }
    }

    private String getJavaType(Shape shape) {
        return getJavaType(shape, false);
    }

    /**
     * Gets the Java type name for a Smithy shape.
     *
     * @param shape The shape to get the type for
     * @param forCollectionElement If true and shape is an enum, returns String instead of enum class name
     */
    private String getJavaType(Shape shape, boolean forCollectionElement) {
        if (shape.isStringShape()) {
            if (shape.hasTrait(EnumTrait.class)) {
                if (forCollectionElement) {
                    return "String";
                }
                return namingStrategy.getShapeClassName(shape.getId().getName());
            }
            return "String";
        } else if (shape.isBooleanShape()) {
            return "Boolean";
        } else if (shape.isIntegerShape()) {
            return "Integer";
        } else if (shape.isLongShape()) {
            return "Long";
        } else if (shape.isFloatShape()) {
            return "Float";
        } else if (shape.isDoubleShape()) {
            return "Double";
        } else if (shape.isBlobShape()) {
            return "SdkBytes";
        } else if (shape.isTimestampShape()) {
            return "java.time.Instant";
        } else if (shape.isListShape()) {
            Shape memberShape = model.expectShape(shape.asListShape().get().getMember().getTarget());
            String memberType = getJavaType(memberShape, true);
            return "java.util.List<" + memberType + ">";
        } else if (shape.isMapShape()) {
            Shape keyShape = model.expectShape(shape.asMapShape().get().getKey().getTarget());
            Shape valueShape = model.expectShape(shape.asMapShape().get().getValue().getTarget());
            String keyType = getJavaType(keyShape, true);
            String valueType = getJavaType(valueShape, true);
            return "java.util.Map<" + keyType + ", " + valueType + ">";
        } else if (shape.isStructureShape()) {
            return namingStrategy.getShapeClassName(shape.getId().getName());
        }
        return "Object";
    }
}
