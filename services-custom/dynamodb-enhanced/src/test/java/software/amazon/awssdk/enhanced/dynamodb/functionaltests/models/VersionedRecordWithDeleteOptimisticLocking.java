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

import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class VersionedRecordWithDeleteOptimisticLocking {

    private String id;
    private Integer sort;
    private Integer value;
    private String gsiId;
    private Integer gsiSort;

    private String stringAttribute;
    private Integer version;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public VersionedRecordWithDeleteOptimisticLocking setId(String id) {
        this.id = id;
        return this;
    }

    public Integer getSort() {
        return sort;
    }

    public VersionedRecordWithDeleteOptimisticLocking setSort(Integer sort) {
        this.sort = sort;
        return this;
    }

    public Integer getValue() {
        return value;
    }

    public VersionedRecordWithDeleteOptimisticLocking setValue(Integer value) {
        this.value = value;
        return this;
    }

    public String getGsiId() {
        return gsiId;
    }

    public VersionedRecordWithDeleteOptimisticLocking setGsiId(String gsiId) {
        this.gsiId = gsiId;
        return this;
    }

    public Integer getGsiSort() {
        return gsiSort;
    }

    public VersionedRecordWithDeleteOptimisticLocking setGsiSort(Integer gsiSort) {
        this.gsiSort = gsiSort;
        return this;
    }

    public String getStringAttribute() {
        return stringAttribute;
    }

    public VersionedRecordWithDeleteOptimisticLocking setStringAttribute(String stringAttribute) {
        this.stringAttribute = stringAttribute;
        return this;
    }

    @DynamoDbVersionAttribute(useVersionOnDelete = true)
    public Integer getVersion() {
        return version;
    }

    public VersionedRecordWithDeleteOptimisticLocking setVersion(Integer version) {
        this.version = version;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VersionedRecordWithDeleteOptimisticLocking versionedRecord = (VersionedRecordWithDeleteOptimisticLocking) o;
        return Objects.equals(id, versionedRecord.id) &&
               Objects.equals(sort, versionedRecord.sort) &&
               Objects.equals(value, versionedRecord.value) &&
               Objects.equals(gsiId, versionedRecord.gsiId) &&
               Objects.equals(stringAttribute, versionedRecord.stringAttribute) &&
               Objects.equals(gsiSort, versionedRecord.gsiSort) &&
               Objects.equals(version, versionedRecord.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sort, value, gsiId, gsiSort, stringAttribute, version);
    }
}