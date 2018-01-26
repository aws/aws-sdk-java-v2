/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.protocol.reflect;

import static software.amazon.awssdk.utils.Validate.paramNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Transforms a JSON representation (using C2J member names) of a modeled POJO into that POJO.
 */
public class ShapeModelReflector {

    private final IntermediateModel model;
    private final String shapeName;
    private final JsonNode input;

    public ShapeModelReflector(IntermediateModel model, String shapeName, JsonNode input) {
        this.model = paramNotNull(model, "model");
        this.shapeName = paramNotNull(shapeName, "shapeName");
        this.input = input;
    }

    public Object createShapeObject() {
        try {
            return createStructure(model.getShapes().get(shapeName), input);
        } catch (Exception e) {
            throw new TestCaseReflectionException(e);
        }
    }

    /**
     * Get the value for the streaming member in the {@link JsonNode}.
     */
    public String getStreamingMemberValue() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(input.fields(), Spliterator.ORDERED), false)
                            .filter(f -> model.getShapes().get(shapeName)
                                              .getMemberByC2jName(f.getKey())
                                              .getHttp().getIsStreaming())
                            .map(f -> f.getValue().asText())
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Streaming member not found in " + shapeName));
    }

    private Object createStructure(ShapeModel structureShape, JsonNode input) throws Exception {
        String fqcn = getFullyQualifiedModelClassName(structureShape.getShapeName());

        Class<?> shapeClass = Class.forName(fqcn);

        Method builderMethod = null;

        try {
            builderMethod = shapeClass.getDeclaredMethod("builder");
        } catch (NoSuchMethodException ignored) {
            // Ignored
        }

        if (builderMethod != null) {
            builderMethod.setAccessible(true);
            Object builderInstance = builderMethod.invoke(null);

            if (input != null) {
                initializeFields(structureShape, input, builderInstance);
            }

            Method buildMethod = builderInstance.getClass().getDeclaredMethod("build");
            buildMethod.setAccessible(true);
            return buildMethod.invoke(builderInstance);
        } else {
            Object shapeObject = Class.forName(fqcn).newInstance();
            if (input != null) {
                initializeFields(structureShape, input, shapeObject);
            }
            return shapeObject;
        }
    }

    private void initializeFields(ShapeModel structureShape, JsonNode input,
                                  Object shapeObject) throws Exception {
        Iterator<String> fieldNames = input.fieldNames();
        while (fieldNames.hasNext()) {
            String memberName = fieldNames.next();
            MemberModel memberModel = structureShape.getMemberByC2jName(memberName);
            if (memberModel == null) {
                throw new IllegalArgumentException("Member " + memberName + " was not found in the " +
                                                   structureShape.getC2jName() + " shape.");
            }
            final Object toSet = getMemberValue(input.get(memberName), memberModel);
            if (toSet != null) {
                Method setter = getMemberSetter(shapeObject.getClass(), memberModel);
                setter.setAccessible(true);
                setter.invoke(shapeObject, toSet);
            }
        }
    }

    private String getFullyQualifiedModelClassName(String modelClassName) {
        return String.format("%s.%s", model.getMetadata().getFullModelPackageName(), modelClassName);
    }

    /**
     * Find the corresponding setter method for the member. Assumes only simple types are
     * supported.
     *
     * @param currentMember Member to get setter for.
     * @return Setter Method object.
     */
    private Method getMemberSetter(Class<?> containingClass, MemberModel currentMember) throws
                                                                                        Exception {
        return containingClass.getMethod(StringUtils.uncapitalize(currentMember.getName()),
                                         Class.forName(getFullyQualifiedType(currentMember)));
    }

    private String getFullyQualifiedType(MemberModel memberModel) {
        if (memberModel.isSimple()) {
            switch (memberModel.getVariable().getSimpleType()) {
                case "Instant":
                case "ByteBuffer":
                case "InputStream":
                    return memberModel.getSetterModel().getVariableSetterType();
                default:
                    return "java.lang." + memberModel.getSetterModel().getVariableSetterType();
            }
        } else if (memberModel.isList()) {
            return "java.util.Collection";
        } else if (memberModel.isMap()) {
            return "java.util.Map";
        } else {
            return getFullyQualifiedModelClassName(
                    memberModel.getSetterModel().getVariableSetterType());
        }
    }

    /**
     * Get the value of the member as specified in the test description. Only supports simple types
     * at the moment.
     *
     * @param currentNode JsonNode containing value of member.
     */
    private Object getMemberValue(JsonNode currentNode, MemberModel memberModel) {
        // Streaming members are not in the POJO
        if (currentNode.isNull()) {
            return null;
        }
        if (memberModel.isSimple()) {
            return getSimpleMemberValue(currentNode, memberModel);
        } else if (memberModel.isList()) {
            return getListMemberValue(currentNode, memberModel);
        } else if (memberModel.isMap()) {
            return getMapMemberValue(currentNode, memberModel);
        } else {
            ShapeModel structureShape = model.getShapes()
                                             .get(memberModel.getVariable().getVariableType());
            try {
                return createStructure(structureShape, currentNode);
            } catch (Exception e) {
                throw new TestCaseReflectionException(e);
            }
        }
    }

    private Object getMapMemberValue(JsonNode currentNode, MemberModel memberModel) {
        Map<String, Object> map = new HashMap<>();
        currentNode.fields()
                   .forEachRemaining(e -> map.put(e.getKey(),
                                                  getMemberValue(e.getValue(), memberModel.getMapModel().getValueModel())));
        return map;
    }

    private Object getListMemberValue(JsonNode currentNode, MemberModel memberModel) {
        ArrayList<Object> list = new ArrayList<>();
        currentNode.elements()
                   .forEachRemaining(e -> list.add(getMemberValue(e, memberModel.getListModel().getListMemberModel())));
        return list;
    }

    private Object getSimpleMemberValue(JsonNode currentNode, MemberModel memberModel) {
        if (memberModel.getHttp().getIsStreaming()) {
            return null;
        }
        switch (memberModel.getVariable().getSimpleType()) {
            case "Long":
                return currentNode.asLong();
            case "Integer":
                return currentNode.asInt();
            case "String":
                return currentNode.asText();
            case "Boolean":
                return currentNode.asBoolean();
            case "Double":
                return currentNode.asDouble();
            case "Instant":
                return Instant.ofEpochMilli(currentNode.asLong());
            case "ByteBuffer":
                return ByteBuffer.wrap(currentNode.asText().getBytes(StandardCharsets.UTF_8));
            case "Float":
                return (float) currentNode.asDouble();
            case "Character":
                return asCharacter(currentNode);
            default:
                throw new IllegalArgumentException(
                        "Unsupported fieldType " + memberModel.getVariable().getSimpleType());
        }
    }

    private Character asCharacter(JsonNode currentNode) {
        String text = currentNode.asText();
        if (text != null && text.length() > 1) {
            throw new IllegalArgumentException("Invalid character " + currentNode.asText());
        } else if (text != null && text.length() == 1) {
            return currentNode.asText().charAt(0);
        } else {
            return null;
        }
    }


}
