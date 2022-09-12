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

package software.amazon.awssdk.codegen.model.rules.endpoints;

import com.fasterxml.jackson.core.TreeNode;

public class ParameterModel {
    private String type;
    private String builtIn;
    private TreeNode defaultValue;
    private Boolean required;
    private ParameterDeprecatedModel deprecated;
    private String documentation;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BuiltInParameter getBuiltInEnum() {
        if (builtIn == null) {
            return null;
        }
        return BuiltInParameter.fromValue(builtIn);
    }

    public String getBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(String builtIn) {
        this.builtIn = builtIn;
    }

    public TreeNode getDefault() {
        return defaultValue;
    }

    public void setDefault(TreeNode defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean isRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public ParameterDeprecatedModel getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(ParameterDeprecatedModel deprecated) {
        this.deprecated = deprecated;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }
}
