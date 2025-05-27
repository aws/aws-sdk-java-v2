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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ListModel;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.utils.Logger;

/**
 * Validator that ensures any shapes shared between two services are completely identical. This validator returns a validation
 * entry for each shape that is present in both service models but has differing definitions in each model.
 */
public final class SharedModelsValidator implements ModelValidator {
    private static final Logger LOG = Logger.loggerFor(SharedModelsValidator.class);

    @Override
    public List<ValidationEntry> validateModels(ModelValidationContext context) {
        if (!context.shareModelsTarget().isPresent()) {
            return Collections.emptyList();
        }

        return validateSharedShapes(context.intermediateModel(), context.shareModelsTarget().get());
    }

    private List<ValidationEntry> validateSharedShapes(IntermediateModel m1, IntermediateModel m2) {
        List<ValidationEntry> errors = new ArrayList<>();

        Map<String, ShapeModel> m1Shapes = m1.getShapes();
        Map<String, ShapeModel> m2Shapes = m2.getShapes();

        m1Shapes.forEach((name, m1Shape) -> {
            if (!m2Shapes.containsKey(name)) {
                return;
            }

            ShapeModel m2Shape = m2Shapes.get(name);

            if (!shapesAreIdentical(m1Shape, m2Shape)) {
                String detailMsg = String.format("Services '%s' and '%s' have differing definitions of the shared model '%s'",
                                                 m1.getMetadata().getServiceName(),
                                                 m2.getMetadata().getServiceName(),
                                                 name);
                LOG.warn(() -> detailMsg);

                errors.add(new ValidationEntry().withErrorId(ValidationErrorId.SHARED_MODELS_DIFFER)
                                                .withSeverity(ValidationErrorSeverity.DANGER)
                                                .withDetailMessage(detailMsg));
            }
        });

        return errors;
    }

    private boolean shapesAreIdentical(ShapeModel m1, ShapeModel m2) {
        // Note: We can't simply do m1.equals(m2) because shared models can still differ slightly in the
        // marshalling/unmarshalling info such as the exact request operation name on the wire.
        // In particular, we leave out comparing the `unmarshaller` and `marshaller` members of ShapeModel.
        // Additionally, the List<MemberModel> are not compared with equals() because we handle MemberModel equality specially
        // as well.
        return m1.isDeprecated() == m2.isDeprecated()
               && m1.isHasPayloadMember() == m2.isHasPayloadMember()
               && m1.isHasHeaderMember() == m2.isHasHeaderMember()
               && m1.isHasStatusCodeMember() == m2.isHasStatusCodeMember()
               && m1.isHasStreamingMember() == m2.isHasStreamingMember()
               && m1.isHasRequiresLengthMember() == m2.isHasRequiresLengthMember()
               && m1.isWrapper() == m2.isWrapper()
               && m1.isSimpleMethod() == m2.isSimpleMethod()
               && m1.isFault() == m2.isFault()
               && m1.isEventStream() == m2.isEventStream()
               && m1.isEvent() == m2.isEvent()
               && m1.isDocument() == m2.isDocument()
               && m1.isUnion() == m2.isUnion()
               && m1.isRetryable() == m2.isRetryable()
               && m1.isThrottling() == m2.isThrottling()
               && Objects.equals(m1.getC2jName(), m2.getC2jName())
               && Objects.equals(m1.getShapeName(), m2.getShapeName())
               && Objects.equals(m1.getDeprecatedMessage(), m2.getDeprecatedMessage())
               && Objects.equals(m1.getType(), m2.getType())
               && Objects.equals(m1.getRequired(), m2.getRequired())
               && Objects.equals(m1.getRequestSignerClassFqcn(), m2.getRequestSignerClassFqcn())
               && Objects.equals(m1.getEndpointDiscovery(), m2.getEndpointDiscovery())
               && memberListsAreIdentical(m1.getMembers(), m2.getMembers())
               && Objects.equals(m1.getEnums(), m2.getEnums())
               && Objects.equals(m1.getVariable(), m2.getVariable())
               && Objects.equals(m1.getErrorCode(), m2.getErrorCode())
               && Objects.equals(m1.getHttpStatusCode(), m2.getHttpStatusCode())
               && Objects.equals(m1.getCustomization(), m2.getCustomization())
               && Objects.equals(m1.getXmlNamespace(), m2.getXmlNamespace())
            ;
    }

    private boolean memberListsAreIdentical(List<MemberModel> memberList1, List<MemberModel> memberList2) {
        if (memberList1.size() != memberList2.size()) {
            return false;
        }

        for (int i = 0; i < memberList1.size(); i++) {
            MemberModel m1 = memberList1.get(i);
            MemberModel m2 = memberList2.get(i);
            if (!memberModelsAreIdentical(m1, m2)) {
                return false;
            }
        }

        return true;
    }

    private boolean memberModelsAreIdentical(MemberModel m1, MemberModel m2) {
        // Similar to ShapeModel, can't call equals() directly. It has a ShapeModel property that is ignored, and ListModel and
        // MapModel are treated similarly
        return m1.isDeprecated() == m2.isDeprecated()
               && m1.isRequired() == m2.isRequired()
               && m1.isSynthetic() == m2.isSynthetic()
               && m1.isIdempotencyToken() == m2.isIdempotencyToken()
               && m1.isJsonValue() == m2.isJsonValue()
               && m1.isEventPayload() == m2.isEventPayload()
               && m1.isEventHeader() == m2.isEventHeader()
               && m1.isEndpointDiscoveryId() == m2.isEndpointDiscoveryId()
               && m1.isSensitive() == m2.isSensitive()
               && m1.isXmlAttribute() == m2.isSensitive()
               && m1.ignoreDataTypeConversionFailures() == m2.ignoreDataTypeConversionFailures()
               && Objects.equals(m1.getName(), m2.getName())
               && Objects.equals(m1.getC2jName(), m2.getC2jName())
               && Objects.equals(m1.getC2jShape(), m2.getC2jShape())
               && Objects.equals(m1.getVariable(), m2.getVariable())
               && Objects.equals(m1.getSetterModel(), m2.getSetterModel())
               && Objects.equals(m1.getGetterModel(), m2.getGetterModel())
               && Objects.equals(m1.getHttp(), m2.getHttp())
               && Objects.equals(m1.getDeprecatedMessage(), m2.getDeprecatedMessage())
               // Note: not equals()
               && listModelsAreIdentical(m1.getListModel(), m2.getListModel())
               // Note: not equals()
               && mapModelsAreIdentical(m1.getMapModel(), m2.getMapModel())
               && Objects.equals(m1.getEnumType(), m2.getEnumType())
               && Objects.equals(m1.getXmlNameSpaceUri(), m2.getXmlNameSpaceUri())
               && Objects.equals(m1.getFluentGetterMethodName(), m2.getFluentGetterMethodName())
               && Objects.equals(m1.getFluentEnumGetterMethodName(), m2.getFluentEnumGetterMethodName())
               && Objects.equals(m1.getFluentSetterMethodName(), m2.getFluentSetterMethodName())
               && Objects.equals(m1.getFluentEnumSetterMethodName(), m2.getFluentEnumSetterMethodName())
               && Objects.equals(m1.getExistenceCheckMethodName(), m2.getExistenceCheckMethodName())
               && Objects.equals(m1.getBeanStyleGetterMethodName(), m2.getBeanStyleGetterMethodName())
               && Objects.equals(m1.getBeanStyleSetterMethodName(), m2.getBeanStyleSetterMethodName())
               && Objects.equals(m1.getUnionEnumTypeName(), m2.getUnionEnumTypeName())
               && Objects.equals(m1.getTimestampFormat(), m2.getTimestampFormat())
               && Objects.equals(m1.getDeprecatedName(), m2.getDeprecatedName())
               && Objects.equals(m1.getDeprecatedFluentGetterMethodName(), m2.getDeprecatedFluentGetterMethodName())
               && Objects.equals(m1.getDeprecatedFluentSetterMethodName(), m2.getDeprecatedFluentSetterMethodName())
               && Objects.equals(m1.getDeprecatedBeanStyleSetterMethodName(), m2.getDeprecatedBeanStyleSetterMethodName())
               && Objects.equals(m1.getContextParam(), m2.getContextParam());
    }

    private boolean listModelsAreIdentical(ListModel m1, ListModel m2) {
        if (m1 == null ^ m2 == null) {
            return false;
        }

        if (m1 == null) {
            return true;
        }

        return Objects.equals(m1.getImplType(), m2.getImplType())
               && Objects.equals(m1.getMemberType(), m2.getMemberType())
               && Objects.equals(m1.getInterfaceType(), m2.getInterfaceType())
               // Note: not equals()
               && memberModelsAreIdentical(m1.getListMemberModel(), m2.getListMemberModel())
               && Objects.equals(m1.getMemberLocationName(), m2.getMemberLocationName())
               && Objects.equals(m1.getMemberAdditionalMarshallingPath(), m2.getMemberAdditionalMarshallingPath())
               && Objects.equals(m1.getMemberAdditionalUnmarshallingPath(), m2.getMemberAdditionalUnmarshallingPath());
    }

    private boolean mapModelsAreIdentical(MapModel m1, MapModel m2) {
        if (m1 == null ^ m2 == null) {
            return false;
        }

        if (m1 == null) {
            return true;
        }

        return Objects.equals(m1.getImplType(), m2.getImplType())
               && Objects.equals(m1.getInterfaceType(), m2.getInterfaceType())
               && Objects.equals(m1.getKeyLocationName(), m2.getKeyLocationName())
               // Note: not equals()
               && memberModelsAreIdentical(m1.getKeyModel(), m2.getKeyModel())
               && Objects.equals(m1.getValueLocationName(), m2.getValueLocationName())
               // Note: not equals()
               && memberModelsAreIdentical(m1.getValueModel(), m2.getValueModel());
    }
}
