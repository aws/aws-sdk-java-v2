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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import software.amazon.awssdk.codegen.model.service.XmlNamespace;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.codegen.naming.ShapeInfo;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.smithy.aws.traits.protocols.Ec2QueryNameTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.knowledge.NullableIndex.CheckMode;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.EventHeaderTrait;
import software.amazon.smithy.model.traits.EventPayloadTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;
import software.amazon.smithy.model.traits.RetryableTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.XmlAttributeTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * Shared base for the Smithy shape processors. Reads a Smithy {@link Model}
 * and {@link ServiceShape} and produces {@link ShapeModel} and
 * {@link MemberModel} objects for the intermediate model.
 *
 * <p>Two methods do the work: {@link #generateShapeModel} builds a shape and
 * all of its members, and {@link #generateMemberModel} builds a single
 * member. Subclasses ({@link AddSmithyInputShapes}, {@link AddSmithyOutputShapes},
 * {@link AddSmithyExceptionShapes}, {@link AddSmithyModelShapes}) select which
 * shapes to translate and implement {@code IntermediateModelShapeProcessor}
 * so they compose into a processor chain.
 *
 * <p>Where the Smithy model library already computes a fact, this class asks
 * the library rather than deriving it by hand:
 * <ul>
 *   <li>{@link HttpBindingIndex} resolves HTTP bindings and applies the
 *       Smithy rule that binding traits are honored only on top-level
 *       operation shapes.
 *   <li>{@link NullableIndex} determines member nullability from the combined
 *       effect of {@code @required}, {@code @default}, and
 *       {@code @clientOptional}.
 *   <li>{@link TopDownIndex} enumerates the operations reachable from the
 *       service.
 * </ul>
 *
 * <p>Event stream membership has no dedicated trait, so it is computed once
 * at construction: a union carrying {@code @streaming} is an event stream,
 * and the structures that appear as its members are events.
 */
abstract class AddSmithyShapes {

    private final Model model;
    private final ServiceShape service;
    private final NamingStrategy namingStrategy;
    private final CustomizationConfig customConfig;
    private final String protocol;
    private final TypeUtils typeUtils;
    private final NullableIndex nullableIndex;

    /**
     * Structures that appear as members of any event stream reachable from
     * the service. An event stream is a {@link UnionShape} carrying
     * {@link StreamingTrait}. Computed once at construction.
     */
    private final Set<ShapeId> eventStructures;

    AddSmithyShapes(Model model,
                    ServiceShape service,
                    NamingStrategy namingStrategy,
                    CustomizationConfig customConfig,
                    String protocol,
                    TypeUtils typeUtils) {
        this.model = model;
        this.service = service;
        this.namingStrategy = namingStrategy;
        this.customConfig = customConfig;
        this.protocol = protocol;
        this.typeUtils = typeUtils;
        this.nullableIndex = NullableIndex.of(model);
        this.eventStructures = collectEventStructures(model, service);
    }

    /**
     * Walks every operation reachable from the service and collects the
     * structure shape ids that appear as members of an event stream. The
     * result lets {@link #generateShapeModel} set the event flag with a
     * simple membership check instead of a reverse lookup per shape.
     */
    private static Set<ShapeId> collectEventStructures(Model model, ServiceShape service) {
        Set<ShapeId> events = new HashSet<>();
        TopDownIndex topDown = TopDownIndex.of(model);
        for (OperationShape op : topDown.getContainedOperations(service)) {
            collectEventStructuresFromShape(model, op.getInputShape(), events);
            collectEventStructuresFromShape(model, op.getOutputShape(), events);
        }
        return events;
    }

    private static void collectEventStructuresFromShape(Model model, ShapeId shapeId, Set<ShapeId> events) {
        if (shapeId == null) {
            return;
        }
        Shape shape = model.getShape(shapeId).orElse(null);
        if (shape == null || !shape.isStructureShape()) {
            return;
        }
        for (MemberShape member : shape.members()) {
            Shape target = model.expectShape(member.getTarget());
            if (target.isUnionShape() && target.hasTrait(StreamingTrait.class)) {
                for (MemberShape eventMember : target.asUnionShape().get().members()) {
                    events.add(eventMember.getTarget());
                }
            }
        }
    }

    protected final Model getModel() {
        return model;
    }

    protected final ServiceShape getService() {
        return service;
    }

    protected final NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    protected final CustomizationConfig getCustomConfig() {
        return customConfig;
    }

    protected final String getProtocol() {
        return protocol;
    }

    protected final TypeUtils getTypeUtils() {
        return typeUtils;
    }

    /**
     * Builds a {@link ShapeModel} from a Smithy shape, including all of its
     * members. Shape-level metadata comes from traits where a matching trait
     * exists (documentation, deprecation, XML namespace, error and retry
     * information) and from the shape's kind where the concept is structural
     * (union, event stream, enum values).
     *
     * <p>The caller sets {@code type}, {@code marshaller}, and
     * {@code unmarshaller} after this method returns, since those depend on
     * whether the shape is a request, response, exception, or plain model
     * shape.
     *
     * @param javaClassName the Java class name for the shape, already
     *                      resolved by the caller through the naming strategy.
     * @param smithyShape   the shape being translated (structure, union,
     *                      enum, or int enum).
     * @param bindings      HTTP bindings for the enclosing operation, resolved
     *                      by {@link HttpBindingIndex}, when {@code smithyShape}
     *                      is a direct operation input, output, or error;
     *                      {@code null} for nested model shapes, which carry
     *                      no HTTP bindings.
     */
    protected ShapeModel generateShapeModel(String javaClassName,
                                            Shape smithyShape,
                                            Map<String, HttpBinding> bindings) {
        ShapeModel shapeModel = new ShapeModel(smithyShape.getId().getName());
        shapeModel.setShapeName(javaClassName);

        shapeModel.setDocumentation(
            smithyShape.getTrait(DocumentationTrait.class).map(DocumentationTrait::getValue).orElse(null));

        shapeModel.setVariable(new VariableModel(namingStrategy.getVariableName(javaClassName), javaClassName));

        List<String> required = new ArrayList<>();
        for (MemberShape m : smithyShape.members()) {
            if (m.hasTrait(RequiredTrait.class)) {
                required.add(m.getMemberName());
            }
        }
        shapeModel.setRequired(required.isEmpty() ? null : required);

        smithyShape.getTrait(DeprecatedTrait.class).ifPresent(dep -> {
            shapeModel.setDeprecated(true);
            dep.getMessage().ifPresent(shapeModel::setDeprecatedMessage);
        });

        shapeModel.withIsUnion(smithyShape.isUnionShape());


        boolean isEventStream = smithyShape.isUnionShape() && smithyShape.hasTrait(StreamingTrait.class);
        shapeModel.withIsEventStream(isEventStream);
        shapeModel.withIsEvent(eventStructures.contains(smithyShape.getId()));

        smithyShape.getTrait(XmlNamespaceTrait.class)
                   .ifPresent(ns -> shapeModel.withXmlNamespace(adaptXmlNamespace(ns)));


        if (smithyShape.hasTrait(ErrorTrait.class)) {
            ErrorTrait error = smithyShape.expectTrait(ErrorTrait.class);
            shapeModel.withIsFault(error.isServerError());
            if (smithyShape.hasTrait(RetryableTrait.class)) {
                RetryableTrait retryable = smithyShape.expectTrait(RetryableTrait.class);
                shapeModel.withIsRetryable(true);
                shapeModel.withIsThrottling(retryable.getThrottling());
            }
        }


        boolean hasHeaderMember = false;
        boolean hasStatusCodeMember = false;
        boolean hasPayloadMember = false;
        boolean hasStreamingMember = false;
        boolean hasRequiresLength = false;

        for (MemberShape member : smithyShape.members()) {
            MemberModel memberModel = generateMemberModel(member, smithyShape, bindings);
            shapeModel.addMember(memberModel);

            ParameterHttpMapping http = memberModel.getHttp();
            if (http.getLocation() == Location.HEADER) {
                hasHeaderMember = true;
            } else if (http.getLocation() == Location.STATUS_CODE) {
                hasStatusCodeMember = true;
            } else if (http.getIsPayload()) {
                hasPayloadMember = true;
                if (http.getIsStreaming()) {
                    hasStreamingMember = true;
                }
                if (http.isRequiresLength()) {
                    hasRequiresLength = true;
                }
            }
        }

        shapeModel.withHasHeaderMember(hasHeaderMember)
                  .withHasStatusCodeMember(hasStatusCodeMember)
                  .withHasPayloadMember(hasPayloadMember)
                  .withHasStreamingMember(hasStreamingMember)
                  .withHasRequiresLengthMember(hasRequiresLength);

        // Enum members become one EnumModel each. String enums read their value
        // from @enumValue; int enums stringify the numeric value, since EnumModel
        // stores values as strings.
        if (smithyShape instanceof EnumShape) {
            for (MemberShape enumMember : ((EnumShape) smithyShape).members()) {
                String value = enumMember.expectTrait(EnumValueTrait.class).expectStringValue();
                shapeModel.addEnum(new EnumModel(namingStrategy.getEnumValueName(value), value));
            }
        } else if (smithyShape instanceof IntEnumShape) {
            for (MemberShape enumMember : ((IntEnumShape) smithyShape).members()) {
                int intValue = enumMember.expectTrait(EnumValueTrait.class).expectIntValue();
                String stringValue = String.valueOf(intValue);
                shapeModel.addEnum(new EnumModel(namingStrategy.getEnumValueName(enumMember.getMemberName()),
                                                 stringValue));
            }
        }

        return shapeModel;
    }

    /**
     * Adapts a Smithy {@link XmlNamespaceTrait} into the codegen
     * {@link XmlNamespace} POJO expected by templates.
     */
    private static XmlNamespace adaptXmlNamespace(XmlNamespaceTrait trait) {
        XmlNamespace ns = new XmlNamespace();
        ns.setUri(trait.getUri());
        trait.getPrefix().ifPresent(ns::setPrefix);
        return ns;
    }

    /**
     * Builds a {@link MemberModel} from a Smithy member shape.
     *
     * @param member the member being translated.
     * @param parentShape the enclosing shape that contains {@code member},
     *                    used by the naming strategy for reserved-name checks.
     * @param bindings HTTP bindings for the enclosing shape, resolved by
     *                 {@link HttpBindingIndex}. Non-null when the enclosing
     *                 shape is a direct operation input, output, or error.
     *                 Pass {@code null} for nested container members (list
     *                 element, map key, map value), which carry no HTTP
     *                 binding.
     */
    protected MemberModel generateMemberModel(MemberShape member,
                                              Shape parentShape,
                                              Map<String, HttpBinding> bindings) {
        String memberName = member.getMemberName();
        Shape targetShape = model.expectShape(member.getTarget());

        ShapeInfo parentCtx = ShapeInfo.ofSmithy(parentShape, model);
        ShapeInfo shapeCtx = ShapeInfo.ofSmithy(targetShape, model);

        String variableName = namingStrategy.getVariableName(memberName, parentCtx);
        String variableType = typeUtils.getJavaDataType(model, targetShape);

        String documentation = member.getTrait(DocumentationTrait.class)
                                     .map(DocumentationTrait::getValue)
                                     .orElse(null);

        MemberModel memberModel = new MemberModel();
        memberModel.withC2jName(memberName)
                   .withC2jShape(targetShape.getId().getName())
                   .withName(capitalize(memberName))
                   .withVariable(new VariableModel(variableName, variableType, variableType)
                                     .withDocumentation(documentation))
                   .withSetterModel(new VariableModel(variableName, variableType, variableType))
                   .withGetterModel(new ReturnTypeModel(variableType))
                   .withTimestampFormat(resolveTimestampFormat(member, targetShape));

        memberModel.setDocumentation(documentation);

        memberModel.withFluentGetterMethodName(
                       namingStrategy.getFluentGetterMethodName(memberName, parentCtx, shapeCtx))
                   .withFluentEnumGetterMethodName(
                       namingStrategy.getFluentEnumGetterMethodName(memberName, parentCtx, shapeCtx))
                   .withFluentSetterMethodName(
                       namingStrategy.getFluentSetterMethodName(memberName, parentCtx, shapeCtx))
                   .withFluentEnumSetterMethodName(
                       namingStrategy.getFluentEnumSetterMethodName(memberName, parentCtx, shapeCtx))
                   .withExistenceCheckMethodName(
                       namingStrategy.getExistenceCheckMethodName(memberName, parentCtx))
                   .withBeanStyleGetterMethodName(
                       namingStrategy.getBeanStyleGetterMethodName(memberName, parentCtx, shapeCtx))
                   .withBeanStyleSetterMethodName(
                       namingStrategy.getBeanStyleSetterMethodName(memberName, parentCtx, shapeCtx));
        memberModel.setUnionEnumTypeName(namingStrategy.getUnionEnumTypeName(memberModel));

        member.getTrait(DeprecatedTrait.class).ifPresent(dep -> {
            memberModel.setDeprecated(true);
            dep.getMessage().ifPresent(memberModel::setDeprecatedMessage);
        });

        memberModel.setRequired(!nullableIndex.isMemberNullable(member, CheckMode.CLIENT));

        memberModel.setSensitive(isSensitive(member, targetShape));

        if (member.hasTrait(IdempotencyTokenTrait.class)) {
            if (!variableType.equals(String.class.getSimpleName())) {
                throw new IllegalArgumentException(memberName
                    + " is idempotent. Its shape should be string type but it is of "
                    + variableType + " type.");
            }
            memberModel.setIdempotencyToken(true);
        }

        memberModel.setEventPayload(member.hasTrait(EventPayloadTrait.class));
        memberModel.setEventHeader(member.hasTrait(EventHeaderTrait.class));

        memberModel.setXmlAttribute(member.hasTrait(XmlAttributeTrait.class));

        member.getTrait(XmlNamespaceTrait.class)
              .ifPresent(ns -> memberModel.setXmlNameSpaceUri(ns.getUri()));

        // TODO(smithy-migration): member-level contextParam (smithy.rules#contextParam ->
        // MemberModel.contextParam). Deferred: ContextParamTrait lives in smithy-rules-engine, not
        // yet on the codegen classpath. Kinesis uses it (~70 members) and shows a known diff until
        // the dependency and this translation are added.

        if (targetShape.isEnumShape() || targetShape.isIntEnumShape()) {
            memberModel.withEnumType(namingStrategy.getShapeClassName(targetShape.getId().getName()));
        }

        if (targetShape.isListShape()) {
            memberModel.setListModel(buildListModel(targetShape.asListShape().get()));
        } else if (targetShape.isMapShape()) {
            memberModel.setMapModel(buildMapModel(targetShape.asMapShape().get()));
        }

        memberModel.setHttp(buildHttpMapping(member, targetShape, bindings));

        return memberModel;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Reports whether a member is sensitive. True when the member itself or
     * its target is marked {@link SensitiveTrait}, or, for a list or map
     * target, when the element, key, or value shape is marked sensitive.
     */
    private boolean isSensitive(MemberShape member, Shape targetShape) {
        if (member.hasTrait(SensitiveTrait.class) || targetShape.hasTrait(SensitiveTrait.class)) {
            return true;
        }
        if (targetShape.isListShape()) {
            Shape listMember = model.expectShape(
                targetShape.asListShape().get().getMember().getTarget());
            return listMember.hasTrait(SensitiveTrait.class);
        }
        if (targetShape.isMapShape()) {
            MapShape map = targetShape.asMapShape().get();
            Shape key = model.expectShape(map.getKey().getTarget());
            Shape value = model.expectShape(map.getValue().getTarget());
            return key.hasTrait(SensitiveTrait.class) || value.hasTrait(SensitiveTrait.class);
        }
        return false;
    }

    /**
     * Resolves the timestamp format for a member. The member's own trait takes
     * precedence, followed by the trait on the target shape. The Smithy format
     * name is converted to the name the intermediate model uses downstream.
     */
    private static String resolveTimestampFormat(MemberShape member, Shape targetShape) {
        if (member.hasTrait(TimestampFormatTrait.class)) {
            return smithyToSdkTimestampFormat(
                member.expectTrait(TimestampFormatTrait.class).getValue());
        }
        if (targetShape.hasTrait(TimestampFormatTrait.class)) {
            return smithyToSdkTimestampFormat(
                targetShape.expectTrait(TimestampFormatTrait.class).getValue());
        }
        return null;
    }

    private static String smithyToSdkTimestampFormat(String smithyFormat) {
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

    private ListModel buildListModel(ListShape listShape) {
        MemberShape listMember = listShape.getMember();
        Shape targetShape = model.expectShape(listMember.getTarget());

        // Nested container members carry no HTTP binding, so pass null bindings.
        MemberModel elementMember = generateMemberModel(listMember, listShape, null);

        String elementType = typeUtils.getJavaDataType(model, targetShape);
        String listImpl = TypeUtils.getDataTypeMapping(TypeUtils.TypeKey.LIST_DEFAULT_IMPL);
        String listIface = TypeUtils.getDataTypeMapping(TypeUtils.TypeKey.LIST_INTERFACE);

        String memberLocationName = listMember.getTrait(XmlNameTrait.class)
                                              .map(XmlNameTrait::getValue)
                                              .orElse(null);

        return new ListModel(elementType, memberLocationName, listImpl, listIface, elementMember);
    }

    private MapModel buildMapModel(MapShape mapShape) {
        MemberShape keyMember = mapShape.getKey();
        MemberShape valueMember = mapShape.getValue();

        MemberModel keyModel = generateMemberModel(keyMember, mapShape, null);
        MemberModel valueModel = generateMemberModel(valueMember, mapShape, null);

        String mapImpl = TypeUtils.getDataTypeMapping(TypeUtils.TypeKey.MAP_DEFAULT_IMPL);
        String mapIface = TypeUtils.getDataTypeMapping(TypeUtils.TypeKey.MAP_INTERFACE);

        String keyLocation = keyMember.getTrait(XmlNameTrait.class)
                                      .map(XmlNameTrait::getValue)
                                      .orElse("key");
        String valueLocation = valueMember.getTrait(XmlNameTrait.class)
                                          .map(XmlNameTrait::getValue)
                                          .orElse("value");

        return new MapModel(mapImpl, mapIface, keyLocation, keyModel, valueLocation, valueModel);
    }

    /**
     * Builds the HTTP mapping for a member from the resolved
     * {@link HttpBindingIndex} bindings. When {@code bindings} is null, the
     * member belongs to a nested container and no HTTP location applies,
     * which is consistent with the Smithy rule that binding traits are
     * honored only on top-level operation shapes.
     */
    private ParameterHttpMapping buildHttpMapping(MemberShape member,
                                                  Shape targetShape,
                                                  Map<String, HttpBinding> bindings) {
        ParameterHttpMapping mapping = new ParameterHttpMapping();
        String memberName = member.getMemberName();

        HttpBinding binding = bindings != null ? bindings.get(memberName) : null;

        Location location = null;
        String bindingLocationName = null;
        boolean isPayload = false;

        if (binding != null) {
            switch (binding.getLocation()) {
                case HEADER:
                    location = Location.HEADER;
                    bindingLocationName = binding.getLocationName();
                    break;
                case QUERY:
                    location = Location.QUERY_STRING;
                    bindingLocationName = binding.getLocationName();
                    break;
                case LABEL:
                    location = Location.URI;
                    bindingLocationName = binding.getLocationName();
                    break;
                case PAYLOAD:
                    isPayload = true;
                    break;
                case PREFIX_HEADERS:
                    location = Location.HEADERS;
                    bindingLocationName = binding.getLocationName();
                    break;
                case RESPONSE_CODE:
                    location = Location.STATUS_CODE;
                    break;
                default:
                    // DOCUMENT, QUERY_PARAMS, and similar are body members with no
                    // explicit location.
                    break;
            }
        }

        String marshallLocationName = resolveMarshallLocationName(member, memberName, bindingLocationName);
        String unmarshallLocationName = resolveUnmarshallLocationName(member, memberName, bindingLocationName);

        boolean streaming = targetShape.hasTrait(StreamingTrait.class);
        boolean requiresLength = targetShape.hasTrait(RequiresLengthTrait.class)
                                 || member.hasTrait(RequiresLengthTrait.class);
        boolean flattened = member.hasTrait(XmlFlattenedTrait.class)
                            || targetShape.hasTrait(XmlFlattenedTrait.class);

        // TODO(smithy-migration): isGreedy is left false. It applies only to @httpLabel members
        // bound to a greedy URI segment ({member+}) and requires threading the operation's request
        // URI into this per-member builder. Zero first-batch impact; needed for the S3 family.
        mapping.withLocation(location)
               .withPayload(isPayload)
               .withStreaming(streaming)
               .withRequiresLength(requiresLength)
               .withFlattened(flattened)
               .withUnmarshallLocationName(unmarshallLocationName)
               .withMarshallLocationName(marshallLocationName);

        return mapping;
    }

    /**
     * Resolves the wire name used when writing a member. The EC2 protocol has
     * its own rule (see {@link #deriveEc2MarshallLocationName}). For other
     * protocols the order of preference is the protocol's name-override trait
     * ({@code @xmlName} or {@code @jsonName}), then the HTTP binding location
     * name, then the member name.
     */
    private String resolveMarshallLocationName(MemberShape member,
                                               String memberName,
                                               String bindingLocationName) {
        if (Protocol.EC2.getValue().equalsIgnoreCase(protocol)) {
            return deriveEc2MarshallLocationName(member, memberName);
        }

        String traitName = memberNameOverrideForProtocol(member);
        if (StringUtils.isNotBlank(traitName)) {
            return traitName;
        }
        if (StringUtils.isNotBlank(bindingLocationName)) {
            return bindingLocationName;
        }
        return memberName;
    }

    // TODO(smithy-migration): flattened-list element wire names (C2J deriveLocationNameForListMember).
    // Not yet reproduced; may surface as EC2 parity diffs since EC2 uses flattened lists heavily.

    /**
     * Resolves the wire name used when reading a member. The order of
     * preference is the protocol's name-override trait ({@code @xmlName} or
     * {@code @jsonName}), then the HTTP binding location name, then the
     * member name.
     */
    private String resolveUnmarshallLocationName(MemberShape member,
                                                 String memberName,
                                                 String bindingLocationName) {
        String traitName = memberNameOverrideForProtocol(member);
        if (StringUtils.isNotBlank(traitName)) {
            return traitName;
        }
        if (StringUtils.isNotBlank(bindingLocationName)) {
            return bindingLocationName;
        }
        return memberName;
    }

    /**
     * Returns the protocol-appropriate wire name override for the member, or {@code null} if none
     * applies. XML protocols ({@code rest-xml}, {@code ec2}) use {@code @xmlName} (C2J's
     * {@code locationName}); JSON protocols use {@code @jsonName}.
     *
     * <p>{@code query} is intentionally omitted: its marshall path also needs C2J's
     * {@code queryName} precedence (no Smithy trait wired yet), so handling only {@code @xmlName}
     * would be half-correct.
     */
    private String memberNameOverrideForProtocol(MemberShape member) {
        if (Protocol.REST_XML.getValue().equalsIgnoreCase(protocol)
            || Protocol.EC2.getValue().equalsIgnoreCase(protocol)) {
            return member.getTrait(XmlNameTrait.class).map(XmlNameTrait::getValue).orElse(null);
        }
        if (Protocol.REST_JSON.getValue().equalsIgnoreCase(protocol)
            || Protocol.AWS_JSON.getValue().equalsIgnoreCase(protocol)) {
            return member.getTrait(JsonNameTrait.class).map(JsonNameTrait::getValue).orElse(null);
        }
        return null;
    }

    /**
     * Resolves the EC2 marshall wire name: {@link Ec2QueryNameTrait} verbatim if present, otherwise
     * {@code @xmlName} (falling back to the member name) with its first character upper-cased. This
     * mirrors C2J's {@code deriveLocationNameForEc2}, which upper-cases the {@code locationName},
     * not the raw member name — they differ when {@code @xmlName} is a distinct stem (e.g.
     * {@code TagSpecifications} with {@code @xmlName("TagSpecification")}).
     */
    private static String deriveEc2MarshallLocationName(MemberShape member, String memberName) {
        String ec2QueryName = member.getTrait(Ec2QueryNameTrait.class)
                                    .map(Ec2QueryNameTrait::getValue)
                                    .orElse(null);
        if (StringUtils.isNotBlank(ec2QueryName)) {
            return ec2QueryName;
        }
        String xmlName = member.getTrait(XmlNameTrait.class)
                               .map(XmlNameTrait::getValue)
                               .orElse(null);
        String base = StringUtils.isNotBlank(xmlName) ? xmlName : memberName;
        return Character.toUpperCase(base.charAt(0)) + base.substring(1);
    }
}
