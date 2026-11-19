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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;

import java.util.List;
import java.util.Set;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbUpdateBehavior;

@DynamoDbBean
public final class RecordForUpdateExpressions {
    private String id;
    private String stringAttribute1;
    private List<String> requestAttributeList;
    private Long extensionAttribute1;
    private Set<String> extensionAttribute2;
    private Long incrementedAttribute;
    private NestedRecordForUpdateExpressions objectAttribute;
    private List<NestedRecordForUpdateExpressions> objectListAttribute;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
    public String getStringAttribute() {
        return stringAttribute1;
    }

    public void setStringAttribute(String stringAttribute1) {
        this.stringAttribute1 = stringAttribute1;
    }

    public List<String> getRequestAttributeList() {
        return requestAttributeList;
    }

    public void setRequestAttributeList(List<String> requestAttributeList) {
        this.requestAttributeList = requestAttributeList;
    }

    public Long getExtensionNumberAttribute() {
        return extensionAttribute1;
    }

    public void setExtensionNumberAttribute(Long extensionAttribute1) {
        this.extensionAttribute1 = extensionAttribute1;
    }

    public Set<String> getExtensionSetAttribute() {
        return extensionAttribute2;
    }

    public void setExtensionSetAttribute(Set<String> extensionAttribute2) {
        this.extensionAttribute2 = extensionAttribute2;
    }

    public Long getIncrementedAttribute() {
        return incrementedAttribute;
    }

    public RecordForUpdateExpressions setIncrementedAttribute(Long incrementedAttribute) {
        this.incrementedAttribute = incrementedAttribute;
        return this;
    }

    public NestedRecordForUpdateExpressions getObjectAttribute() {
        return objectAttribute;
    }

    public void setObjectAttribute(NestedRecordForUpdateExpressions objectAttribute) {
        this.objectAttribute = objectAttribute;
    }

    public List<NestedRecordForUpdateExpressions> getObjectListAttribute() {
        return objectListAttribute;
    }

    public void setObjectListAttribute(List<NestedRecordForUpdateExpressions> objectListAttribute) {
        this.objectListAttribute = objectListAttribute;
    }
}
