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

package software.amazon.awssdk.codegen.model.intermediate;

import static software.amazon.awssdk.codegen.internal.Constant.LF;
import static software.amazon.awssdk.codegen.internal.DocumentationUtils.defaultExistenceCheck;
import static software.amazon.awssdk.codegen.internal.DocumentationUtils.defaultFluentReturn;
import static software.amazon.awssdk.codegen.internal.DocumentationUtils.defaultGetter;
import static software.amazon.awssdk.codegen.internal.DocumentationUtils.defaultGetterParam;
import static software.amazon.awssdk.codegen.internal.DocumentationUtils.defaultSetter;
import static software.amazon.awssdk.codegen.internal.DocumentationUtils.defaultSetterParam;
import static software.amazon.awssdk.codegen.internal.DocumentationUtils.stripHtmlTags;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.squareup.javapoet.ClassName;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.protocols.core.PathMarshaller;
import software.amazon.awssdk.utils.StringUtils;

public class MemberModel extends DocumentationModel {

    private String name;

    private String c2jName;

    private String c2jShape;

    private VariableModel variable;

    private VariableModel setterModel;

    private ReturnTypeModel getterModel;

    private ParameterHttpMapping http;

    private boolean deprecated;

    private ListModel listModel;

    private MapModel mapModel;

    private String enumType;

    private String xmlNameSpaceUri;

    private boolean idempotencyToken;

    private ShapeModel shape;

    private String fluentGetterMethodName;

    private String fluentEnumGetterMethodName;

    private String fluentSetterMethodName;

    private String fluentEnumSetterMethodName;

    private String existenceCheckMethodName;

    private String beanStyleGetterName;

    private String beanStyleSetterName;

    private boolean isJsonValue;

    private String timestampFormat;

    private boolean eventPayload;

    private boolean eventHeader;

    private boolean endpointDiscoveryId;

    private boolean sensitive;

    private boolean xmlAttribute;

    private String deprecatedName;

    private String fluentDeprecatedGetterMethodName;

    private String fluentDeprecatedSetterMethodName;

    private String deprecatedBeanStyleSetterMethodName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MemberModel withName(String name) {
        setName(name);
        return this;
    }

    public String getC2jName() {
        return c2jName;
    }

    public void setC2jName(String c2jName) {
        this.c2jName = c2jName;
    }

    public MemberModel withC2jName(String c2jName) {
        setC2jName(c2jName);
        return this;
    }

    public String getC2jShape() {
        return c2jShape;
    }

    public void setC2jShape(String c2jShape) {
        this.c2jShape = c2jShape;
    }

    public MemberModel withC2jShape(String c2jShape) {
        setC2jShape(c2jShape);
        return this;
    }

    public VariableModel getVariable() {
        return variable;
    }

    public void setVariable(VariableModel variable) {
        this.variable = variable;
    }

    public MemberModel withVariable(VariableModel variable) {
        setVariable(variable);
        return this;
    }

    public VariableModel getSetterModel() {
        return setterModel;
    }

    public void setSetterModel(VariableModel setterModel) {
        this.setterModel = setterModel;
    }

    public MemberModel withSetterModel(VariableModel setterModel) {
        setSetterModel(setterModel);
        return this;
    }

    public String getFluentGetterMethodName() {
        return fluentGetterMethodName;
    }

    public void setFluentGetterMethodName(String fluentGetterMethodName) {
        this.fluentGetterMethodName = fluentGetterMethodName;
    }

    public MemberModel withFluentGetterMethodName(String getterMethodName) {
        setFluentGetterMethodName(getterMethodName);
        return this;
    }

    public String getFluentEnumGetterMethodName() {
        return fluentEnumGetterMethodName;
    }

    public void setFluentEnumGetterMethodName(String fluentEnumGetterMethodName) {
        this.fluentEnumGetterMethodName = fluentEnumGetterMethodName;
    }

    public MemberModel withFluentEnumGetterMethodName(String fluentEnumGetterMethodName) {
        setFluentEnumGetterMethodName(fluentEnumGetterMethodName);
        return this;
    }

    public String getBeanStyleGetterMethodName() {
        return beanStyleGetterName;
    }

    public void setBeanStyleGetterMethodName(String beanStyleGetterName) {
        this.beanStyleGetterName = beanStyleGetterName;
    }

    public MemberModel withBeanStyleGetterMethodName(String beanStyleGetterName) {
        this.beanStyleGetterName = beanStyleGetterName;
        return this;
    }

    public String getBeanStyleSetterMethodName() {
        return beanStyleSetterName;
    }

    public void setBeanStyleSetterMethodName(String beanStyleSetterName) {
        this.beanStyleSetterName = beanStyleSetterName;
    }

    public MemberModel withBeanStyleSetterMethodName(String beanStyleSetterName) {
        this.beanStyleSetterName = beanStyleSetterName;
        return this;
    }

    public String getFluentSetterMethodName() {
        return fluentSetterMethodName;
    }

    public void setFluentSetterMethodName(String fluentSetterMethodName) {
        this.fluentSetterMethodName = fluentSetterMethodName;
    }

    public MemberModel withFluentSetterMethodName(String fluentMethodName) {
        setFluentSetterMethodName(fluentMethodName);
        return this;
    }

    public String getFluentEnumSetterMethodName() {
        return fluentEnumSetterMethodName;
    }

    public void setFluentEnumSetterMethodName(String fluentEnumSetterMethodName) {
        this.fluentEnumSetterMethodName = fluentEnumSetterMethodName;
    }

    public MemberModel withFluentEnumSetterMethodName(String fluentEnumSetterMethodName) {
        setFluentEnumSetterMethodName(fluentEnumSetterMethodName);
        return this;
    }

    public String getExistenceCheckMethodName() {
        return existenceCheckMethodName;
    }

    public void setExistenceCheckMethodName(String existenceCheckMethodName) {
        this.existenceCheckMethodName = existenceCheckMethodName;
    }

    public MemberModel withExistenceCheckMethodName(String existenceCheckMethodName) {
        setExistenceCheckMethodName(existenceCheckMethodName);
        return this;
    }

    public ReturnTypeModel getGetterModel() {
        return getterModel;
    }

    public void setGetterModel(ReturnTypeModel getterModel) {
        this.getterModel = getterModel;
    }

    public MemberModel withGetterModel(ReturnTypeModel getterModel) {
        setGetterModel(getterModel);
        return this;
    }

    public ParameterHttpMapping getHttp() {
        return http;
    }

    public void setHttp(ParameterHttpMapping parameterHttpMapping) {
        this.http = parameterHttpMapping;
    }

    public boolean isSimple() {
        return TypeUtils.isSimple(variable.getVariableType());
    }

    public boolean isList() {
        return listModel != null;
    }

    public boolean isMap() {
        return mapModel != null;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public boolean isEventPayload() {
        return eventPayload;
    }

    public void setEventPayload(boolean eventPayload) {
        this.eventPayload = eventPayload;
    }

    public boolean isEventHeader() {
        return eventHeader;
    }

    public void setEventHeader(boolean eventHeader) {
        this.eventHeader = eventHeader;
    }


    public boolean isEndpointDiscoveryId() {
        return endpointDiscoveryId;
    }

    public void setEndpointDiscoveryId(boolean endpointDiscoveryId) {
        this.endpointDiscoveryId = endpointDiscoveryId;
    }

    public ListModel getListModel() {
        return listModel;
    }

    public void setListModel(ListModel listModel) {
        this.listModel = listModel;
    }

    public MemberModel withListModel(ListModel list) {
        setListModel(list);
        return this;
    }

    public MapModel getMapModel() {
        return mapModel;
    }

    public void setMapModel(MapModel map) {
        this.mapModel = map;
    }

    public MemberModel withMapModel(MapModel map) {
        setMapModel(map);
        return this;
    }

    public String getEnumType() {
        return enumType;
    }

    public void setEnumType(String enumType) {
        this.enumType = enumType;
    }

    public MemberModel withEnumType(String enumType) {
        setEnumType(enumType);
        return this;
    }

    public String getXmlNameSpaceUri() {
        return xmlNameSpaceUri;
    }

    public void setXmlNameSpaceUri(String xmlNameSpaceUri) {
        this.xmlNameSpaceUri = xmlNameSpaceUri;
    }

    public MemberModel withXmlNameSpaceUri(String xmlNameSpaceUri) {
        setXmlNameSpaceUri(xmlNameSpaceUri);
        return this;
    }

    public String getSetterDocumentation() {
        StringBuilder docBuilder = new StringBuilder();

        docBuilder.append(StringUtils.isNotBlank(documentation) ? documentation : defaultSetter().replace("%s", name) + "\n");

        docBuilder.append(getParamDoc())
                .append(getEnumDoc());

        return docBuilder.toString();
    }

    public String getGetterDocumentation() {
        StringBuilder docBuilder = new StringBuilder();
        docBuilder.append(StringUtils.isNotBlank(documentation) ? documentation : defaultGetter().replace("%s", name))
                .append(LF);

        if (returnTypeIs(List.class) || returnTypeIs(Map.class)) {
            appendParagraph(docBuilder, "Attempts to modify the collection returned by this method will result in an "
                                        + "UnsupportedOperationException.");
        }

        if (enumType != null) {
            if (returnTypeIs(List.class)) {
                appendParagraph(docBuilder,
                                "If the list returned by the service includes enum values that are not available in the "
                                + "current SDK version, {@link #%s} will use {@link %s#UNKNOWN_TO_SDK_VERSION} in place of those "
                                + "values in the list. The raw values returned by the service are available from {@link #%s}.",
                                getFluentEnumGetterMethodName(), getEnumType(), getFluentGetterMethodName());
            } else if (returnTypeIs(Map.class)) {
                appendParagraph(docBuilder,
                                "If the map returned by the service includes enum values that are not available in the "
                                + "current SDK version, {@link #%s} will not include those keys in the map. {@link #%s} "
                                + "will include all data from the service.",
                                getFluentEnumGetterMethodName(), getEnumType(), getFluentGetterMethodName());
            } else {
                appendParagraph(docBuilder,
                                "If the service returns an enum value that is not available in the current SDK version, "
                                + "{@link #%s} will return {@link %s#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the "
                                + "service is available from {@link #%s}.",
                                getFluentEnumGetterMethodName(), getEnumType(), getFluentGetterMethodName());
            }
        }

        if (getAutoConstructClassIfExists().isPresent()) {
            appendParagraph(docBuilder,
                            "You can use {@link #%s()} to see if a value was sent in this field.",
                            getExistenceCheckMethodName());
        }

        String variableDesc = StringUtils.isNotBlank(documentation) ? documentation : defaultGetterParam().replace("%s", name);

        docBuilder.append("@return ")
                  .append(stripHtmlTags(variableDesc))
                  .append(getEnumDoc());

        return docBuilder.toString();
    }

    public String getDeprecatedGetterDocumentation() {
        String getterDocumentation = getGetterDocumentation();
        return getterDocumentation
               + LF
               + "@deprecated Use {@link #" + getFluentGetterMethodName() + "()}"
               + LF;
    }

    private boolean returnTypeIs(Class<?> clazz) {
        String returnType = this.getGetterModel().getReturnType();
        return returnType != null && returnType.startsWith(clazz.getName()); // Use startsWith in case it's parametrized
    }

    public String getFluentSetterDocumentation() {
        return getSetterDocumentation()
               + LF
               + "@return " + stripHtmlTags(defaultFluentReturn())
               + getEnumDoc();
    }

    public String getExistenceCheckDocumentation() {
        return defaultExistenceCheck().replace("%s", name) + LF;
    }

    public String getDeprecatedSetterDocumentation() {
        return getFluentSetterDocumentation()
            + LF
            + "@deprecated Use {@link #" + getFluentSetterMethodName() + "(" + setterModel.getSimpleType() + ")}"
            + LF;
    }

    public String getDefaultConsumerFluentSetterDocumentation() {
        return (StringUtils.isNotBlank(documentation) ? documentation : defaultSetter().replace("%s", name) + "\n")
               + LF
               + "This is a convenience that creates an instance of the {@link "
               + variable.getSimpleType()
               + ".Builder} avoiding the need to create one manually via {@link "
               + variable.getSimpleType()
               + "#builder()}.\n"
               + LF
               + "When the {@link Consumer} completes, {@link "
               + variable.getSimpleType()
               + ".Builder#build()} is called immediately and its result is passed to {@link #"
               + getFluentGetterMethodName()
               + "("
               + variable.getSimpleType()
               + ")}."
               + LF
               + "@param "
               + variable.getVariableName()
               + " a consumer that will call methods on {@link "
               + variable.getSimpleType() + ".Builder}"
               + LF
               + "@return " + stripHtmlTags(defaultFluentReturn())
               + LF
               + "@see #"
               + getFluentSetterMethodName()
               + "("
               + variable.getSimpleType()
               + ")";
    }

    private String getParamDoc() {
        return LF
               + "@param "
               + variable.getVariableName()
               + " "
               + stripHtmlTags(StringUtils.isNotBlank(documentation) ? documentation : defaultSetterParam().replace("%s", name));
    }

    private String getEnumDoc() {
        StringBuilder docBuilder = new StringBuilder();

        if (enumType != null) {
            docBuilder.append(LF).append("@see ").append(enumType);
        }

        return docBuilder.toString();
    }

    public boolean isIdempotencyToken() {
        return idempotencyToken;
    }

    public void setIdempotencyToken(boolean idempotencyToken) {
        this.idempotencyToken = idempotencyToken;
    }

    public boolean getIsBinary() {
        return http.getIsStreaming() ||
               (isSdkBytesType() && (http.getIsPayload() || isEventPayload()));
    }

    /**
     * @return Implementation of {@link PathMarshaller} to use if this member is bound the the URI.
     * @throws IllegalStateException If this member is not bound to the URI. Templates should first check
     * {@link ParameterHttpMapping#isUri()} first.
     */
    // TODO remove when rest XML marshaller refactor is merged
    @JsonIgnore
    public String getPathMarshaller() {
        if (!http.isUri()) {
            throw new IllegalStateException("Only members bound to the URI have a path marshaller");
        }
        String prefix = PathMarshaller.class.getName();
        if (http.isGreedy()) {
            return prefix + ".GREEDY";
        } else if (isIdempotencyToken()) {
            return prefix + ".IDEMPOTENCY";
        } else {
            return prefix + ".NON_GREEDY";
        }
    }

    public boolean isJsonValue() {
        return isJsonValue;
    }

    public void setJsonValue(boolean jsonValue) {
        isJsonValue = jsonValue;
    }

    public MemberModel withJsonValue(boolean jsonValue) {
        setJsonValue(jsonValue);
        return this;
    }

    public String getTimestampFormat() {
        return timestampFormat;
    }

    public void setTimestampFormat(String timestampFormat) {
        this.timestampFormat = timestampFormat;
    }

    public MemberModel withTimestampFormat(String timestampFormat) {
        setTimestampFormat(timestampFormat);
        return this;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public boolean isXmlAttribute() {
        return xmlAttribute;
    }

    public void setXmlAttribute(boolean xmlAttribute) {
        this.xmlAttribute = xmlAttribute;
    }

    public MemberModel withXmlAttribtue(boolean xmlAttribtue) {
        this.xmlAttribute = xmlAttribtue;
        return this;
    }

    public String getDeprecatedName() {
        return deprecatedName;
    }

    public void setDeprecatedName(String deprecatedName) {
        this.deprecatedName = deprecatedName;
    }

    public MemberModel withDeprecatedName(String deprecatedName) {
        this.deprecatedName = deprecatedName;
        return this;
    }

    @JsonIgnore
    public boolean hasBuilder() {
        return !(isSimple() || isList() || isMap());
    }

    @JsonIgnore
    public boolean isCollectionWithBuilderMember() {
        return (isList() && getListModel().getListMemberModel() != null && getListModel().getListMemberModel().hasBuilder()) ||
               (isMap() && getMapModel().getValueModel() != null && getMapModel().getValueModel().hasBuilder());
    }

    @JsonIgnore
    public boolean isSdkBytesType() {
        return SdkBytes.class.getName().equals(variable.getVariableType());
    }

    /**
     * @return Marshalling type to use when creating a {@link SdkField}. Must be a
     * field of {@link MarshallingType}.
     */
    public String getMarshallingType() {
        if (isList()) {
            return "LIST";
        } else if (isMap()) {
            return "MAP";
        } else if (!isSimple()) {
            return "SDK_POJO";
        } else {
            return TypeUtils.getMarshallingType(variable.getSimpleType());
        }
    }

    @JsonIgnore
    public ShapeModel getShape() {
        return shape;
    }

    public void setShape(ShapeModel shape) {
        this.shape = shape;
    }

    @Override
    public String toString() {
        return c2jName;
    }

    private void appendParagraph(StringBuilder builder, String content, Object... contentArgs) {
        builder.append("<p>")
               .append(LF)
               .append(String.format(content, contentArgs))
               .append(LF)
               .append("</p>")
               .append(LF);
    }

    public Optional<ClassName> getAutoConstructClassIfExists() {
        if (isList()) {
            return Optional.of(ClassName.get(SdkAutoConstructList.class));
        } else if (isMap()) {
            return Optional.of(ClassName.get(SdkAutoConstructMap.class));
        }

        return Optional.empty();
    }

    public void setDeprecatedFluentGetterMethodName(String fluentDeprecatedGetterMethodName) {
        this.fluentDeprecatedGetterMethodName = fluentDeprecatedGetterMethodName;
    }

    public String getDeprecatedFluentGetterMethodName() {
        return fluentDeprecatedGetterMethodName;
    }

    public void setDeprecatedFluentSetterMethodName(String fluentDeprecatedSetterMethodName) {
        this.fluentDeprecatedSetterMethodName = fluentDeprecatedSetterMethodName;
    }

    public String getDeprecatedFluentSetterMethodName() {
        return fluentDeprecatedSetterMethodName;
    }

    public String getDeprecatedBeanStyleSetterMethodName() {
        return deprecatedBeanStyleSetterMethodName;
    }

    public void setDeprecatedBeanStyleSetterMethodName(String deprecatedBeanStyleSetterMethodName) {
        this.deprecatedBeanStyleSetterMethodName = deprecatedBeanStyleSetterMethodName;
    }
}
