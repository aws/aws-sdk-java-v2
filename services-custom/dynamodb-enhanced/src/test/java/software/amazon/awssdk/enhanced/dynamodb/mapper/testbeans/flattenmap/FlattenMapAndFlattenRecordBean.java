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
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FlattenRecord;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlattenMap;

@DynamoDbBean
public class FlattenMapAndFlattenRecordBean {
    private String rootAttribute1;
    private String rootAttribute2;
    private FlattenRecord flattenRecord;
    private Map<String, String> attributesMap;

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
    public FlattenRecord getFlattenRecord() {
        return flattenRecord;
    }

    public void setFlattenRecord(FlattenRecord flattenRecord) {
        this.flattenRecord = flattenRecord;
    }

    @DynamoDbFlattenMap
    public Map<String, String> getAttributesMap() {
        return attributesMap;
    }
    public void setAttributesMap(Map<String, String> abstractMap) {
        this.attributesMap = abstractMap;
    }
}