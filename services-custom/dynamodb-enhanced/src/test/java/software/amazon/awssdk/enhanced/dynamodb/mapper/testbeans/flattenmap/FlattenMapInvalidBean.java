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
public class FlattenMapInvalidBean {
    private String id;
    private String rootAttribute1;
    private String rootAttribute2;
    private Map<String, String> attributesMap;
    private Map<String, String> secondaryAttributesMap;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRootAttribute1() {
        return rootAttribute1;
    }

    public void setRootAttribute1(String rootAttribute1) {
        this.rootAttribute1 = rootAttribute1;
    }

    public String getRootAttribute2() {
        return rootAttribute2;
    }

    public void setRootAttribute2(String rootAttribute2) {
        this.rootAttribute2 = rootAttribute2;
    }

    @DynamoDbFlatten
    public Map<String, String> getAttributesMap() {
        return attributesMap;
    }

    public void setAttributesMap(Map<String, String> attributesMap) {
        this.attributesMap = attributesMap;
    }

    @DynamoDbFlatten
    public Map<String, String> getSecondaryAttributesMap() {
        return secondaryAttributesMap;
    }

    public void setSecondaryAttributesMap(Map<String, String> secondaryAttributesMap) {
        this.secondaryAttributesMap = secondaryAttributesMap;
    }
}