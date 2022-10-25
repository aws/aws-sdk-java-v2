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
import java.util.List;
import java.util.Map;

public class EndpointTestModel {
    private String documentation;
    private Map<String, TreeNode> params;
    private List<OperationInput> operationInputs;
    private ExpectModel expect;

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public Map<String, TreeNode> getParams() {
        return params;
    }

    public void setParams(Map<String, TreeNode> params) {
        this.params = params;
    }

    public List<OperationInput> getOperationInputs() {
        return operationInputs;
    }

    public void setOperationInputs(List<OperationInput> operationInputs) {
        this.operationInputs = operationInputs;
    }

    public ExpectModel getExpect() {
        return expect;
    }

    public void setExpect(ExpectModel expect) {
        this.expect = expect;
    }
}
