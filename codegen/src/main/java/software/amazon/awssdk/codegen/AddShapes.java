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

import static software.amazon.awssdk.codegen.internal.TypeUtils.getDataTypeMapping;
import static software.amazon.awssdk.codegen.internal.Utils.capitalize;
import static software.amazon.awssdk.codegen.internal.Utils.isListShape;
import static software.amazon.awssdk.codegen.internal.Utils.isMapShape;
import static software.amazon.awssdk.codegen.internal.Utils.isScalar;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.EnumModel;
import software.amazon.awssdk.codegen.model.intermediate.ListModel;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ParameterHttpMapping;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ReturnTypeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;
import software.amazon.awssdk.codegen.model.service.Location;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

abstract class AddShapes {

    private final IntermediateModelBuilder builder;
    private final NamingStrategy namingStrategy;

    AddShapes(IntermediateModelBuilder builder) {
        this.builder = builder;
        this.namingStrategy = builder.getNamingStrategy();
    }

    protected final TypeUtils getTypeUtils() {
        return builder.getTypeUtils();
    }

    protected final NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    protected final ServiceModel getServiceModel() {
        return builder.getService();
    }

    protected final CustomizationConfig getCustomizationConfig() {
        return builder.getCustomConfig();
    }

    protected final ShapeModel generateShapeModel(String javaClassName, String shapeName) {
        ShapeModel shapeModel = new ShapeModel(shapeName);
        shapeModel.setShapeName(javaClassName);
        Shape shape = getServiceModel().getShapes().get(shapeName);

        shapeModel.setDocumentation(shape.getDocumentation());
        shapeModel.setVariable(new VariableModel(getNamingStrategy().getVariableName(javaClassName),
                                                 javaClassName));
        // contains the list of c2j member names that are required for this shape.
        shapeModel.setRequired(shape.getRequired());
        shapeModel.setDeprecated(shape.isDeprecated());
        shapeModel.setWrapper(shape.isWrapper());
        shapeModel.withIsEventStream(shape.isEventstream());
        shapeModel.withIsEvent(shape.isEvent());
        shapeModel.withXmlNamespace(shape.getXmlNamespace());

        boolean hasHeaderMember = false;
        boolean hasStatusCodeMember = false;
        boolean hasPayloadMember = false;
        boolean hasStreamingMember = false;
        boolean hasRequiresLength = false;

        Map<String, Member> members = shape.getMembers();

        if (members != null) {
            for (Map.Entry<String, Member> memberEntry : members.entrySet()) {

                String c2jMemberName = memberEntry.getKey();
                Member c2jMemberDefinition = memberEntry.getValue();
                Shape parentShape = shape;

                MemberModel memberModel = generateMemberModel(c2jMemberName, c2jMemberDefinition,
                                                              getProtocol(), parentShape,
                                                              getServiceModel().getShapes());

                if (memberModel.getHttp().getLocation() == Location.HEADER) {
                    hasHeaderMember = true;

                } else if (memberModel.getHttp().getLocation() == Location.STATUS_CODE) {
                    hasStatusCodeMember = true;

                } else if (memberModel.getHttp().getIsPayload()) {
                    hasPayloadMember = true;
                    if (memberModel.getHttp().getIsStreaming()) {
                        hasStreamingMember = true;
                    }
                    if (memberModel.getHttp().isRequiresLength()) {
                        hasRequiresLength = true;
                    }
                }

                shapeModel.addMember(memberModel);
            }

            shapeModel.withHasHeaderMember(hasHeaderMember)
                      .withHasStatusCodeMember(hasStatusCodeMember)
                      .withHasPayloadMember(hasPayloadMember)
                      .withHasStreamingMember(hasStreamingMember)
                      .withHasRequiresLengthMember(hasRequiresLength);
        }

        List<String> enumValues = shape.getEnumValues();
        if (enumValues != null && !enumValues.isEmpty()) {
            for (String enumValue : enumValues) {
                shapeModel.addEnum(
                        new EnumModel(getNamingStrategy().getEnumValueName(enumValue), enumValue));
            }
        }

        return shapeModel;
    }

    private MemberModel generateMemberModel(String c2jMemberName, Member c2jMemberDefinition,
                                            String protocol, Shape parentShape,
                                            Map<String, Shape> allC2jShapes) {
        String c2jShapeName = c2jMemberDefinition.getShape();
        Shape shape = allC2jShapes.get(c2jShapeName);
        String variableName = getNamingStrategy().getVariableName(c2jMemberName);
        String variableType = getTypeUtils().getJavaDataType(allC2jShapes, c2jShapeName);
        String variableDeclarationType = getTypeUtils()
                .getJavaDataType(allC2jShapes, c2jShapeName, getCustomizationConfig());

        //If member is idempotent, then it should be of string type
        //Else throw IllegalArgumentException.
        if (c2jMemberDefinition.isIdempotencyToken() &&
            !variableType.equals(String.class.getSimpleName())) {
            throw new IllegalArgumentException(c2jMemberName +
                                               " is idempotent. It's shape should be string type but it is of " +
                                               variableType + " type.");
        }

        MemberModel memberModel = new MemberModel();

        memberModel.withC2jName(c2jMemberName)
                   .withC2jShape(c2jShapeName)
                   .withName(capitalize(c2jMemberName))
                   .withVariable(new VariableModel(variableName, variableType, variableDeclarationType)
                                         .withDocumentation(c2jMemberDefinition.getDocumentation()))
                   .withSetterModel(new VariableModel(variableName, variableType, variableDeclarationType))
                   .withGetterModel(new ReturnTypeModel(variableType))
                   .withTimestampFormat(resolveTimestampFormat(c2jMemberDefinition, shape))
                   .withJsonValue(c2jMemberDefinition.getJsonvalue());
        memberModel.setDocumentation(c2jMemberDefinition.getDocumentation());
        memberModel.setDeprecated(c2jMemberDefinition.isDeprecated());
        memberModel.setSensitive(isSensitiveShapeOrContainer(c2jMemberDefinition, allC2jShapes));
        memberModel
                .withFluentGetterMethodName(namingStrategy.getFluentGetterMethodName(c2jMemberName, parentShape, shape))
                .withFluentEnumGetterMethodName(namingStrategy.getFluentEnumGetterMethodName(c2jMemberName, parentShape, shape))
                .withFluentSetterMethodName(namingStrategy.getFluentSetterMethodName(c2jMemberName, parentShape, shape))
                .withFluentEnumSetterMethodName(namingStrategy.getFluentEnumSetterMethodName(c2jMemberName, parentShape, shape))
                .withExistenceCheckMethodName(namingStrategy.getExistenceCheckMethodName(c2jMemberName, parentShape))
                .withBeanStyleGetterMethodName(namingStrategy.getBeanStyleGetterMethodName(c2jMemberName, parentShape, shape))
                .withBeanStyleSetterMethodName(namingStrategy.getBeanStyleSetterMethodName(c2jMemberName, parentShape, shape));
        memberModel.setIdempotencyToken(c2jMemberDefinition.isIdempotencyToken());
        memberModel.setEventPayload(c2jMemberDefinition.isEventpayload());
        memberModel.setEventHeader(c2jMemberDefinition.isEventheader());
        memberModel.setEndpointDiscoveryId(c2jMemberDefinition.isEndpointdiscoveryid());
        memberModel.setXmlAttribute(c2jMemberDefinition.isXmlAttribute());

        // Pass the xmlNameSpace from the member reference
        if (c2jMemberDefinition.getXmlNamespace() != null) {
            memberModel.setXmlNameSpaceUri(c2jMemberDefinition.getXmlNamespace().getUri());
        }

        // Additional member model metadata for list/map/enum types
        fillContainerTypeMemberMetadata(allC2jShapes, c2jMemberDefinition.getShape(), memberModel,
                                        protocol);


        String deprecatedName = c2jMemberDefinition.getDeprecatedName();
        if (StringUtils.isNotBlank(deprecatedName)) {
            checkForValidDeprecatedName(c2jMemberName, shape);

            memberModel.setDeprecatedName(deprecatedName);
            memberModel.setDeprecatedFluentGetterMethodName(
                namingStrategy.getFluentGetterMethodName(deprecatedName, parentShape, shape));
            memberModel.setDeprecatedFluentSetterMethodName(
                namingStrategy.getFluentSetterMethodName(deprecatedName, parentShape, shape));
            memberModel.setDeprecatedBeanStyleSetterMethodName(
                namingStrategy.getBeanStyleSetterMethodName(deprecatedName, parentShape, shape));
        }

        ParameterHttpMapping httpMapping = generateParameterHttpMapping(parentShape,
                                                                              c2jMemberName,
                                                                              c2jMemberDefinition,
                                                                              protocol,
                                                                              allC2jShapes);

        String payload = parentShape.getPayload();

        boolean shapeIsStreaming = shape.isStreaming();
        boolean memberIsStreaming = c2jMemberDefinition.isStreaming();
        boolean payloadIsStreaming = shapeIsStreaming || memberIsStreaming;
        boolean requiresLength = shape.isRequiresLength() || c2jMemberDefinition.isRequiresLength();

        httpMapping.withPayload(payload != null && payload.equals(c2jMemberName))
                   .withStreaming(payloadIsStreaming)
                   .withRequiresLength(requiresLength);

        memberModel.setHttp(httpMapping);

        return memberModel;
    }

    private void checkForValidDeprecatedName(String c2jMemberName, Shape memberShape) {
        if (memberShape.getEnumValues() != null) {
            throw new IllegalStateException(String.format(
                "Member %s has enum values and a deprecated name. Codegen does not support this.",
                c2jMemberName));
        }

        if (isListShape(memberShape)) {
            throw new IllegalStateException(String.format(
                "Member %s is a list and has a deprecated name. Codegen does not support this.",
                c2jMemberName));
        }

        if (isMapShape(memberShape)) {
            throw new IllegalStateException(String.format(
                "Member %s is a map and has a deprecated name. Codegen does not support this.",
                c2jMemberName));
        }
    }

    private boolean isSensitiveShapeOrContainer(Member member, Map<String, Shape> allC2jShapes) {
        if (member == null) {
            return false;
        }

        return member.isSensitive() ||
               isSensitiveShapeOrContainer(allC2jShapes.get(member.getShape()), allC2jShapes);
    }

    private boolean isSensitiveShapeOrContainer(Shape c2jShape, Map<String, Shape> allC2jShapes) {
        if (c2jShape == null) {
            return false;
        }

        return c2jShape.isSensitive() ||
               isSensitiveShapeOrContainer(c2jShape.getListMember(), allC2jShapes) ||
               isSensitiveShapeOrContainer(c2jShape.getMapKeyType(), allC2jShapes) ||
               isSensitiveShapeOrContainer(c2jShape.getMapValueType(), allC2jShapes);
    }

    private String resolveTimestampFormat(Member c2jMemberDefinition, Shape c2jShape) {
        return c2jMemberDefinition.getTimestampFormat() != null ?
               c2jMemberDefinition.getTimestampFormat() : c2jShape.getTimestampFormat();
    }

    private ParameterHttpMapping generateParameterHttpMapping(Shape parentShape,
                                                              String memberName,
                                                              Member member,
                                                              String protocol,
                                                              Map<String, Shape> allC2jShapes) {

        ParameterHttpMapping mapping = new ParameterHttpMapping();

        Shape memberShape = allC2jShapes.get(member.getShape());
        mapping.withLocation(Location.forValue(member.getLocation()))
               .withPayload(member.isPayload()).withStreaming(member.isStreaming())
               .withFlattened(isFlattened(member, memberShape))
               .withUnmarshallLocationName(deriveUnmarshallerLocationName(memberShape, memberName, member))
               .withMarshallLocationName(
                        deriveMarshallerLocationName(memberShape, memberName, member, protocol))
               .withIsGreedy(isGreedy(parentShape, allC2jShapes, mapping));

        return mapping;
    }

    private boolean isFlattened(Member member, Shape memberShape) {
        return member.isFlattened()
               || memberShape.isFlattened();
    }

    /**
     * @param parentShape  Shape containing the member in question.
     * @param allC2jShapes All shapes in the service model.
     * @param mapping      Mapping being built.
     * @return True if the member is bound to a greedy label, false otherwise.
     */
    private boolean isGreedy(Shape parentShape, Map<String, Shape> allC2jShapes, ParameterHttpMapping mapping) {
        if (mapping.getLocation() == Location.URI) {
            // If the location is URI we can assume the parent shape is an input shape.
            String requestUri = findRequestUri(parentShape, allC2jShapes);
            if (requestUri.contains(String.format("{%s+}", mapping.getMarshallLocationName()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Given an input shape, finds the Request URI for the operation that input is referenced from.
     *
     * @param parentShape  Input shape to find operation's request URI for.
     * @param allC2jShapes All shapes in the service model.
     * @return Request URI for operation.
     * @throws RuntimeException If operation can't be found.
     */
    private String findRequestUri(Shape parentShape, Map<String, Shape> allC2jShapes) {
        return builder.getService().getOperations().values().stream()
                .filter(o -> o.getInput() != null)
                .filter(o -> allC2jShapes.get(o.getInput().getShape()).equals(parentShape))
                .map(o -> o.getHttp().getRequestUri())
                .findFirst().orElseThrow(() -> new RuntimeException("Could not find request URI for input shape"));
    }

    private String deriveUnmarshallerLocationName(Shape memberShape, String memberName, Member member) {
        String locationName;
        if (memberShape.getListMember() != null && memberShape.isFlattened()) {
            locationName = deriveLocationNameForListMember(memberShape, member);
        } else {
            locationName = member.getLocationName();
        }

        if (StringUtils.isNotBlank(locationName)) {
            return locationName;
        }

        return memberName;
    }

    private String deriveMarshallerLocationName(Shape memberShape, String memberName, Member member, String protocol) {
        String queryName = member.getQueryName();

        if (StringUtils.isNotBlank(queryName)) {
            return queryName;
        }

        String locationName;

        if (Protocol.EC2.getValue().equalsIgnoreCase(protocol)) {
            locationName = deriveLocationNameForEc2(member);
        } else if (memberShape.getListMember() != null && memberShape.isFlattened()) {
            locationName =  deriveLocationNameForListMember(memberShape, member);
        } else {
            locationName = member.getLocationName();
        }

        if (StringUtils.isNotBlank(locationName)) {
            return locationName;
        }

        return memberName;
    }

    private String deriveLocationNameForEc2(Member member) {
        String locationName = member.getLocationName();

        if (StringUtils.isNotBlank(locationName)) {
            return StringUtils.upperCase(locationName.substring(0, 1)) +
                   locationName.substring(1);
        }

        return null;
    }

    private String deriveLocationNameForListMember(Shape memberShape, Member member) {
        String locationName = memberShape.getListMember().getLocationName();

        if (StringUtils.isNotBlank(locationName)) {
            return locationName;
        }

        return member.getLocationName();
    }

    private void fillContainerTypeMemberMetadata(Map<String, Shape> c2jShapes,
                                                 String memberC2jShapeName, MemberModel memberModel,
                                                 String protocol) {

        Shape memberC2jShape = c2jShapes.get(memberC2jShapeName);

        if (isListShape(memberC2jShape)) {
            Member listMemberDefinition = memberC2jShape.getListMember();
            String listMemberC2jShapeName = listMemberDefinition.getShape();

            MemberModel listMemberModel = generateMemberModel("member", listMemberDefinition, protocol,
                                                              memberC2jShape, c2jShapes);
            String listImpl = getDataTypeMapping(TypeUtils.TypeKey.LIST_DEFAULT_IMPL);
            memberModel.setListModel(
                    new ListModel(getTypeUtils().getJavaDataType(c2jShapes, listMemberC2jShapeName),
                                  memberC2jShape.getListMember().getLocationName(), listImpl,
                                  getDataTypeMapping(TypeUtils.TypeKey.LIST_INTERFACE), listMemberModel));
        } else if (isMapShape(memberC2jShape)) {
            Member mapKeyMemberDefinition = memberC2jShape.getMapKeyType();
            String mapKeyShapeName = mapKeyMemberDefinition.getShape();
            Shape mapKeyShape = c2jShapes.get(mapKeyShapeName);

            Member mapValueMemberDefinition = memberC2jShape.getMapValueType();

            // Complex map keys are not supported.
            Validate.isTrue(isScalar(mapKeyShape), "The key type of %s must be a scalar!", mapKeyShapeName);

            MemberModel mapKeyModel = generateMemberModel("key", mapKeyMemberDefinition, protocol,
                                                          memberC2jShape, c2jShapes);
            MemberModel mapValueModel = generateMemberModel("value", mapValueMemberDefinition, protocol,
                                                            memberC2jShape, c2jShapes);
            String mapImpl = getDataTypeMapping(TypeUtils.TypeKey.MAP_DEFAULT_IMPL);

            String keyLocation = memberC2jShape.getMapKeyType().getLocationName() != null ?
                    memberC2jShape.getMapKeyType().getLocationName() : "key";

            String valueLocation = memberC2jShape.getMapValueType().getLocationName() != null ?
                    memberC2jShape.getMapValueType().getLocationName() : "value";

            memberModel.setMapModel(new MapModel(mapImpl,
                                                 getDataTypeMapping(TypeUtils.TypeKey.MAP_INTERFACE),
                                                 keyLocation,
                                                 mapKeyModel,
                                                 valueLocation,
                                                 mapValueModel));

        } else if (memberC2jShape.getEnumValues() != null) { // enum values
            memberModel.withEnumType(getNamingStrategy().getShapeClassName(memberC2jShapeName));
        }
    }

    protected String getProtocol() {
        return getServiceModel().getMetadata().getProtocol();
    }
}
