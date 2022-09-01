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

import java.util.List;

public class RuleModel {
    private String type;
    private List<ConditionModel> conditions;

    // for type == error
    private String error;

    // for type == tree
    private List<RuleModel> rules;

    // for type == endpoint
    private EndpointModel endpoint;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<ConditionModel> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConditionModel> conditions) {
        this.conditions = conditions;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<RuleModel> getRules() {
        return rules;
    }

    public void setRules(List<RuleModel> rules) {
        this.rules = rules;
    }

    public EndpointModel getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(EndpointModel endpoint) {
        this.endpoint = endpoint;
    }
}
