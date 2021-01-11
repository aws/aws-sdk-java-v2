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

import software.amazon.awssdk.codegen.internal.TypeUtils;

public class ListModel {

    private String implType;

    private String memberType;

    private String interfaceType;

    private MemberModel listMemberModel;

    private String memberLocationName;

    private String memberAdditionalMarshallingPath;

    private String memberAdditionalUnmarshallingPath;

    public ListModel() {
    }

    public ListModel(String memberType,
                     String memberLocationName,
                     String implType,
                     String interfaceType,
                     MemberModel listMemberModel) {

        this.memberType = memberType;
        this.memberLocationName = memberLocationName;
        this.implType = implType;
        this.interfaceType = interfaceType;
        this.listMemberModel = listMemberModel;
    }

    public String getImplType() {
        return implType;
    }

    public void setImplType(String implType) {
        this.implType = implType;
    }

    public String getMemberType() {
        return memberType;
    }

    public void setMemberType(String memberType) {
        this.memberType = memberType;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public MemberModel getListMemberModel() {
        return listMemberModel;
    }

    public void setListMemberModel(MemberModel listMemberModel) {
        this.listMemberModel = listMemberModel;
    }

    public String getMemberLocationName() {
        return memberLocationName;
    }

    public void setMemberLocationName(String memberLocationName) {
        this.memberLocationName = memberLocationName;
    }

    public String getMemberAdditionalMarshallingPath() {
        return memberAdditionalMarshallingPath;
    }

    public void setMemberAdditionalMarshallingPath(
            String memberAdditionalMarshallingPath) {
        this.memberAdditionalMarshallingPath = memberAdditionalMarshallingPath;
    }

    public String getMemberAdditionalUnmarshallingPath() {
        return memberAdditionalUnmarshallingPath;
    }

    public void setMemberAdditionalUnmarshallingPath(
            String memberAdditionalUnmarshallingPath) {
        this.memberAdditionalUnmarshallingPath = memberAdditionalUnmarshallingPath;
    }

    public boolean isSimple() {
        return TypeUtils.isSimple(memberType);
    }

    public boolean isMap() {
        return memberType.startsWith(TypeUtils
                                             .getDataTypeMapping(TypeUtils.TypeKey.MAP_INTERFACE));
    }

    public String getTemplateType() {
        return interfaceType + "<" + memberType + ">";
    }

    public String getTemplateImplType() {
        return implType + "<" + memberType + ">";
    }

    public String getSimpleType() {
        int startIndex = memberType.lastIndexOf(".");
        return memberType.substring(startIndex + 1);
    }
}
