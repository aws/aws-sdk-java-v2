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

package software.amazon.awssdk.codegen.model.intermediate;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MapModel {

    private final String implType;

    private final String interfaceType;

    private final String keyLocationName;

    private final MemberModel keyModel;

    private final String valueLocationName;

    private final MemberModel valueModel;

    public MapModel(
            @JsonProperty("implType") String implType,
            @JsonProperty("interfaceType") String interfaceType,
            @JsonProperty("keyLocationName") String keyLocationName,
            @JsonProperty("keyModel") MemberModel keyModel,
            @JsonProperty("valueLocationName") String valueLocationName,
            @JsonProperty("valueModel") MemberModel valueModel) {

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

    public String getInterfaceType() {
        return interfaceType;
    }

    public String getKeyLocationName() {
        return keyLocationName;
    }

    public MemberModel getKeyModel() {
        return keyModel;
    }

    public String getValueLocationName() {
        return valueLocationName;
    }

    public MemberModel getValueModel() {
        return valueModel;
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
