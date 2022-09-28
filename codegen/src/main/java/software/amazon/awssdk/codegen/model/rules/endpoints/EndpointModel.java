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

public class EndpointModel {
    private TreeNode url;
    private Map<String, List<TreeNode>> headers;
    private Map<String, TreeNode> properties;

    public TreeNode getUrl() {
        return url;
    }

    public void setUrl(TreeNode url) {
        this.url = url;
    }

    public Map<String, List<TreeNode>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<TreeNode>> headers) {
        this.headers = headers;
    }

    public Map<String, TreeNode> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, TreeNode> properties) {
        this.properties = properties;
    }
}
