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

package software.amazon.awssdk.enhanced.dynamodb.model;

import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlattenMap;

public class RecordWithFlattenMap {

    private String id;
    private Integer sort;
    private Integer value;
    private String gsiId;
    private Integer gsiSort;

    private String stringAttribute;

    private Map<String, String> attributesMap;

    public String getId() {
        return id;
    }

    public RecordWithFlattenMap setId(String id) {
        this.id = id;
        return this;
    }

    public Integer getSort() {
        return sort;
    }

    public RecordWithFlattenMap setSort(Integer sort) {
        this.sort = sort;
        return this;
    }

    public Integer getValue() {
        return value;
    }

    public RecordWithFlattenMap setValue(Integer value) {
        this.value = value;
        return this;
    }

    public String getGsiId() {
        return gsiId;
    }

    public RecordWithFlattenMap setGsiId(String gsiId) {
        this.gsiId = gsiId;
        return this;
    }

    public Integer getGsiSort() {
        return gsiSort;
    }

    public RecordWithFlattenMap setGsiSort(Integer gsiSort) {
        this.gsiSort = gsiSort;
        return this;
    }

    public String getStringAttribute() {
        return stringAttribute;
    }

    public RecordWithFlattenMap setStringAttribute(String stringAttribute) {
        this.stringAttribute = stringAttribute;
        return this;
    }

    @DynamoDbFlattenMap
    public Map<String, String> getAttributesMap() {
        return attributesMap;
    }

    public RecordWithFlattenMap setAttributesMap(Map<String, String> attributesMap) {
        this.attributesMap = attributesMap;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordWithFlattenMap record = (RecordWithFlattenMap) o;
        return Objects.equals(id, record.id) &&
               Objects.equals(sort, record.sort) &&
               Objects.equals(value, record.value) &&
               Objects.equals(gsiId, record.gsiId) &&
               Objects.equals(stringAttribute, record.stringAttribute) &&
               Objects.equals(gsiSort, record.gsiSort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sort, value, gsiId, gsiSort, stringAttribute);
    }
}
