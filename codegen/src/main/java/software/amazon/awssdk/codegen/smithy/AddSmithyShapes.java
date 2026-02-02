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

import static software.amazon.awssdk.codegen.internal.Utils.capitalize;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.codegen.model.intermediate.EnumModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumTrait;

/**
 * Processes Smithy shapes and converts them to intermediate ShapeModels.
 */
final class AddSmithyShapes {
    private final Model model;
    private final ServiceShape service;
    private final NamingStrategy namingStrategy;
    private final TopDownIndex topDownIndex;

    AddSmithyShapes(SmithyIntermediateModelBuilder builder) {
        this.model = builder.getSmithyModel();
        this.service = builder.getService();
        this.namingStrategy = builder.getNamingStrategy();
        this.topDownIndex = TopDownIndex.of(model);
    }

    /**
     * Constructs all shapes (input, output, and model shapes) from the Smithy model.
     */
    public Map<String, ShapeModel> constructShapes() {
        Map<String, ShapeModel> shapes = new HashMap<>();
        
        // Add input shapes
        shapes.putAll(constructInputShapes());
        
        // Add output shapes (including empty responses for Unit outputs)
        shapes.putAll(constructOutputShapes());
        
        // Add exception shapes
        shapes.putAll(constructExceptionShapes());
        
        // Add model shapes (structures and enums referenced by inputs/outputs/exceptions)
        shapes.putAll(constructModelShapes(shapes));
        
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
        
        // Collect all shape IDs that need to be processed
        // Start with shapes referenced by inputs, outputs, and exceptions
        for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
            // Add input shape members
            if (operation.getInput().isPresent()) {
                ShapeId inputShapeId = operation.getInput().get();
                StructureShape inputShape = model.expectShape(inputShapeId, StructureShape.class);
                collectReferencedShapes(inputShape, toProcess);
            }
            
            // Add output shape members
            if (operation.getOutput().isPresent()) {
                ShapeId outputShapeId = operation.getOutput().get();
                if (!outputShapeId.toString().equals("smithy.api#Unit")) {
                    StructureShape outputShape = model.expectShape(outputShapeId, StructureShape.class);
                    collectReferencedShapes(outputShape, toProcess);
                }
            }
            
            // Add error shape members
            for (ShapeId errorShapeId : operation.getErrors()) {
                StructureShape errorShape = model.expectShape(errorShapeId, StructureShape.class);
                collectReferencedShapes(errorShape, toProcess);
            }
        }
        
        // Process all referenced shapes recursively
        while (!toProcess.isEmpty()) {
            ShapeId shapeId = toProcess.iterator().next();
            toProcess.remove(shapeId);
            
            if (processedShapeIds.contains(shapeId)) {
                continue;
            }
            processedShapeIds.add(shapeId);
            
            // Skip built-in Smithy types
            if (shapeId.getNamespace().equals("smithy.api")) {
                continue;
            }
            
            Shape shape = model.expectShape(shapeId);
            
            if (shape.isStructureShape()) {
                StructureShape structureShape = shape.asStructureShape().get();
                String javaClassName = namingStrategy.getShapeClassName(shapeId.getName());
                
                // Only add if not already in existing shapes
                if (!existingShapes.containsKey(javaClassName) && !modelShapes.containsKey(javaClassName)) {
                    ShapeModel shapeModel = generateShapeModel(javaClassName, structureShape);
                    shapeModel.setType(ShapeType.Model.getValue());
                    modelShapes.put(javaClassName, shapeModel);
                    
                    // Recursively collect shapes referenced by this structure
                    collectReferencedShapes(structureShape, toProcess);
                }
            } else if (shape.isStringShape()) {
                StringShape stringShape = shape.asStringShape().get();
                
                // Check if it's an enum
                if (stringShape.hasTrait(EnumTrait.class)) {
                    String javaClassName = namingStrategy.getShapeClassName(shapeId.getName());
                    
                    if (!existingShapes.containsKey(javaClassName) && !modelShapes.containsKey(javaClassName)) {
                        ShapeModel shapeModel = generateEnumShapeModel(javaClassName, stringShape);
                        modelShapes.put(javaClassName, shapeModel);
                    }
                }
            }
        }
        
        return modelShapes;
    }
    
    /**
     * Collects all shape IDs referenced by members of a structure.
     */
    private void collectReferencedShapes(StructureShape structure, Set<ShapeId> toProcess) {
        for (MemberShape member : structure.members()) {
            ShapeId targetId = member.getTarget();
            toProcess.add(targetId);
            
            // Also check if the target is a list or map and collect their members
            Shape targetShape = model.expectShape(targetId);
            if (targetShape.isListShape()) {
                targetShape.asListShape().get().getMember().getTarget();
                toProcess.add(targetShape.asListShape().get().getMember().getTarget());
            } else if (targetShape.isMapShape()) {
                toProcess.add(targetShape.asMapShape().get().getKey().getTarget());
                toProcess.add(targetShape.asMapShape().get().getValue().getTarget());
            }
        }
    }
    
    /**
     * Generates a shape model for an enum (string shape with enum trait).
     */
    private ShapeModel generateEnumShapeModel(String javaClassName, StringShape stringShape) {
        ShapeModel shapeModel = new ShapeModel(stringShape.toShapeId().getName());
        shapeModel.setShapeName(javaClassName);
        shapeModel.setType(ShapeType.Enum.getValue());
        
        // Set documentation
        stringShape.getTrait(DocumentationTrait.class).ifPresent(doc -> {
            shapeModel.setDocumentation(doc.getValue());
        });
        
        // Set variable model
        String variableName = namingStrategy.getVariableName(javaClassName);
        shapeModel.setVariable(new VariableModel(variableName, javaClassName));
        
        // Add enum values
        EnumTrait enumTrait = stringShape.expectTrait(EnumTrait.class);
        enumTrait.getValues().forEach(enumDef -> {
            String enumValue = enumDef.getValue();
            String enumName = enumDef.getName().orElse(
                namingStrategy.getEnumValueName(enumValue)
            );
            shapeModel.addEnum(new EnumModel(enumName, enumValue));
        });
        
        return shapeModel;
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
                
                // For Unit outputs, create an empty response shape
                if (outputShapeId.toString().equals("smithy.api#Unit")) {
                    ShapeModel shapeModel = createEmptyResponseShape(javaClassName);
                    shapes.put(javaClassName, shapeModel);
                } else {
                    StructureShape outputShape = model.expectShape(outputShapeId, StructureShape.class);
                    ShapeModel shapeModel = generateShapeModel(javaClassName, outputShape);
                    shapeModel.setType(ShapeType.Response.getValue());
                    shapes.put(javaClassName, shapeModel);
                }
            } else {
                // No output at all - create an empty response shape
                ShapeModel shapeModel = createEmptyResponseShape(javaClassName);
                shapes.put(javaClassName, shapeModel);
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
        
        // Collect all error shapes from all operations
        for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
            for (ShapeId errorShapeId : operation.getErrors()) {
                StructureShape errorShape = model.expectShape(errorShapeId, StructureShape.class);
                String javaClassName = namingStrategy.getExceptionName(errorShapeId.getName());
                
                // Only add if not already added
                if (!shapes.containsKey(javaClassName)) {
                    ShapeModel shapeModel = generateShapeModel(javaClassName, errorShape);
                    shapeModel.setType(ShapeType.Exception.getValue());
                    shapes.put(javaClassName, shapeModel);
                }
            }
        }
        
        return shapes;
    }

    private ShapeModel generateShapeModel(String javaClassName, StructureShape shape) {
        ShapeModel shapeModel = new ShapeModel(shape.toShapeId().getName());
        shapeModel.setShapeName(javaClassName);
        
        // Set documentation
        shape.getTrait(DocumentationTrait.class).ifPresent(doc -> {
            shapeModel.setDocumentation(doc.getValue());
        });
        
        // Set variable model
        String variableName = namingStrategy.getVariableName(javaClassName);
        shapeModel.setVariable(new VariableModel(variableName, javaClassName));
        
        // Determine if this is an exception shape
        boolean isException = shape.hasTrait(software.amazon.smithy.model.traits.ErrorTrait.class);
        
        // Add members
        for (MemberShape member : shape.members()) {
            // Skip "message" member for exceptions as it's inherited from SdkException
            if (isException && member.getMemberName().equalsIgnoreCase("message")) {
                continue;
            }
            
            MemberModel memberModel = generateMemberModel(member, shape);
            shapeModel.addMember(memberModel);
        }
        
        // TODO: Add required fields
        // TODO: Add deprecation info
        // TODO: Add other shape metadata
        
        return shapeModel;
    }
    
    /**
     * Generates a member model from a Smithy member shape.
     */
    private MemberModel generateMemberModel(MemberShape memberShape, StructureShape parentShape) {
        String memberName = memberShape.getMemberName();
        ShapeId targetShapeId = memberShape.getTarget();
        Shape targetShape = model.expectShape(targetShapeId);
        
        // Get the Java type for this member
        String javaType = getJavaType(targetShape);
        
        // For enums, the variable type is String, but we track the enum class separately
        String variableType = javaType;
        String enumType = null;
        if (targetShape.isStringShape() && targetShape.hasTrait(EnumTrait.class)) {
            variableType = "String";
            enumType = javaType;  // The enum class name
        }
        
        // Create variable model
        String variableName = namingStrategy.getVariableName(memberName);
        VariableModel variableModel = new VariableModel(variableName, variableType, variableType);
        
        // Set documentation if present
        memberShape.getTrait(DocumentationTrait.class).ifPresent(doc -> {
            variableModel.withDocumentation(doc.getValue());
        });
        
        // Create member model
        MemberModel memberModel = new MemberModel();
        memberModel.withC2jName(memberName)
                   .withC2jShape(targetShapeId.getName())
                   .withName(capitalize(memberName))
                   .withVariable(variableModel)
                   .withSetterModel(new VariableModel(variableName, variableType, variableType))
                   .withGetterModel(new software.amazon.awssdk.codegen.model.intermediate.ReturnTypeModel(variableType));
        
        memberModel.setDocumentation(memberShape.getTrait(DocumentationTrait.class)
                                                .map(doc -> doc.getValue())
                                                .orElse(null));
        
        // Set enum type if applicable
        if (enumType != null) {
            memberModel.withEnumType(enumType);
        }
        
        // Set fluent method names
        memberModel.withFluentGetterMethodName(namingStrategy.getFluentGetterMethodName(memberName, null, null))
                   .withFluentSetterMethodName(namingStrategy.getFluentSetterMethodName(memberName, null, null))
                   .withBeanStyleGetterMethodName(namingStrategy.getBeanStyleGetterMethodName(memberName, null, null))
                   .withBeanStyleSetterMethodName(namingStrategy.getBeanStyleSetterMethodName(memberName, null, null));
        
        // TODO: Add HTTP mappings
        // TODO: Add list/map models
        // TODO: Add required flag
        // TODO: Add deprecation
        // TODO: Add other member metadata
        
        return memberModel;
    }
    
    /**
     * Gets the Java type name for a Smithy shape.
     */
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
            // Check if it's an enum
            if (shape.hasTrait(EnumTrait.class)) {
                // For collection elements, enums are represented as String
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
            String memberType = getJavaType(memberShape, true);  // Pass true for collection elements
            return "java.util.List<" + memberType + ">";
        } else if (shape.isMapShape()) {
            Shape keyShape = model.expectShape(shape.asMapShape().get().getKey().getTarget());
            Shape valueShape = model.expectShape(shape.asMapShape().get().getValue().getTarget());
            String keyType = getJavaType(keyShape, true);  // Pass true for collection elements
            String valueType = getJavaType(valueShape, true);  // Pass true for collection elements
            return "java.util.Map<" + keyType + ", " + valueType + ">";
        } else if (shape.isStructureShape()) {
            return namingStrategy.getShapeClassName(shape.getId().getName());
        }
        
        // Default fallback
        return "Object";
    }
}
