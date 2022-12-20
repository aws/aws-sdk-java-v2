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
import java.util.Map;

public class OperationInput {
    private String operationName;
    private Map<String, TreeNode> operationParams;

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public Map<String, TreeNode> getOperationParams() {
        return operationParams;
    }

    public void setOperationParams(Map<String, TreeNode> operationParams) {
        this.operationParams = operationParams;
    }
}
