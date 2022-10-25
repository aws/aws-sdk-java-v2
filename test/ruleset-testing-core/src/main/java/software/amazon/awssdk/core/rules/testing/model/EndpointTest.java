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

package software.amazon.awssdk.core.rules.testing.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

public class EndpointTest {
    private String documentation;
    private Map<String, JsonNode> params;
    private List<String> tags;
    private Expect expect;

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public Map<String, JsonNode> getParams() {
        return params;
    }

    public void setParams(Map<String, JsonNode> params) {
        this.params = params;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Expect getExpect() {
        return expect;
    }

    public void setExpect(Expect expect) {
        this.expect = expect;
    }
}
