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

public class MapModel {

    private String implType;

    private String interfaceType;

    private String keyLocationName;

    private MemberModel keyModel;

    private String valueLocationName;

    private MemberModel valueModel;

    public MapModel() {
    }

    public MapModel(String implType,
                    String interfaceType,
                    String keyLocationName,
                    MemberModel keyModel,
                    String valueLocationName,
                    MemberModel valueModel) {

        this.implType = implType;
        this.interfaceType = interfaceType;
        this.keyLocationName = keyLocationName;
        this.keyModel = keyModel;
        this.valueLocationName = valueLocationName;
        this.valueModel = valueModel;
    }

    public String getImplType() {
        return implType;
    }

    public void setImplType(String implType) {
        this.implType = implType;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getKeyLocationName() {
        return keyLocationName;
    }

    public void setKeyLocationName(String keyLocationName) {
        this.keyLocationName = keyLocationName;
    }

    public MemberModel getKeyModel() {
        return keyModel;
    }

    public void setKeyModel(MemberModel keyModel) {
        this.keyModel = keyModel;
    }

    public String getValueLocationName() {
        return valueLocationName;
    }

    public void setValueLocationName(String valueLocationName) {
        this.valueLocationName = valueLocationName;
    }

    public MemberModel getValueModel() {
        return valueModel;
    }

    public void setValueModel(MemberModel valueModel) {
        this.valueModel = valueModel;
    }

    public String getTemplateType() {
        return interfaceType +
               "<" + keyModel.getVariable().getVariableType() + "," + valueModel.getVariable().getVariableType() + ">";
    }

    public String getEntryType() {
        return String.format("Map.Entry<%s, %s>",
                             keyModel.getVariable().getVariableType(), valueModel.getVariable().getVariableType());
    }
}
