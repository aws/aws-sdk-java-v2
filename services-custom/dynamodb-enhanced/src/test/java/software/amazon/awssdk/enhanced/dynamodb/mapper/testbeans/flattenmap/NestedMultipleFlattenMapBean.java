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
public class NestedMultipleFlattenMapBean {
    private String id;
    private NestedClassWithMultipleMaps nestedClass;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbFlatten
    public NestedClassWithMultipleMaps getNestedClass() {
        return nestedClass;
    }

    public void setNestedClass(NestedClassWithMultipleMaps nestedClass) {
        this.nestedClass = nestedClass;
    }

    @DynamoDbBean
    public static class NestedClassWithMultipleMaps {
        private Map<String, String> firstMap;
        private Map<String, String> secondMap;

        @DynamoDbFlatten
        public Map<String, String> getFirstMap() {
            return firstMap;
        }

        public void setFirstMap(Map<String, String> firstMap) {
            this.firstMap = firstMap;
        }

        @DynamoDbFlatten
        public Map<String, String> getSecondMap() {
            return secondMap;
        }

        public void setSecondMap(Map<String, String> secondMap) {
            this.secondMap = secondMap;
        }
    }
}