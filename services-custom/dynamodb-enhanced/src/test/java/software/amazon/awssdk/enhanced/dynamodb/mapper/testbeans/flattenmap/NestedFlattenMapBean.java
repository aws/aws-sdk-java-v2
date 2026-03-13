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

package software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.flattenmap;

import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class NestedFlattenMapBean {
    private String id;
    private String rootAttribute;
    private NestedClassWithMap nestedClass;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRootAttribute() {
        return rootAttribute;
    }

    public void setRootAttribute(String rootAttribute) {
        this.rootAttribute = rootAttribute;
    }

    @DynamoDbFlatten
    public NestedClassWithMap getNestedClass() {
        return nestedClass;
    }

    public void setNestedClass(NestedClassWithMap nestedClass) {
        this.nestedClass = nestedClass;
    }

    @DynamoDbBean
    public static class NestedClassWithMap {
        private String nestedAttribute;
        private Map<String, String> nestedMap;

        public String getNestedAttribute() {
            return nestedAttribute;
        }

        public void setNestedAttribute(String nestedAttribute) {
            this.nestedAttribute = nestedAttribute;
        }

        @DynamoDbFlatten
        public Map<String, String> getNestedMap() {
            return nestedMap;
        }

        public void setNestedMap(Map<String, String> nestedMap) {
            this.nestedMap = nestedMap;
        }
    }
}