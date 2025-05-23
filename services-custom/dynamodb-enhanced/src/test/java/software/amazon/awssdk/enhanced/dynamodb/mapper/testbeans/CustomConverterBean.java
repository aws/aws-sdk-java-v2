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

package software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.CustomType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class CustomConverterBean {
    private String id;
    private LocalDate localDate;
    private Map<String, String> stringsMap;
    private Map<CustomType, String> customKeyMap;
    private Map<String, CustomType> customValueMap;
    private List<CustomType> customList;
    private Set<CustomType> customSet;

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }

    public CustomConverterBean setId(String id) {
        this.id = id;
        return this;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public CustomConverterBean setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
        return this;
    }

    public Map<String, String> getStringsMap() {
        return stringsMap;
    }

    public CustomConverterBean setStringsMap(Map<String, String> stringsMap) {
        this.stringsMap = stringsMap;
        return this;
    }

    public Map<CustomType, String> getCustomKeyMap() {
        return customKeyMap;
    }

    public CustomConverterBean setCustomKeyMap(Map<CustomType, String> customKeyMap) {
        this.customKeyMap = customKeyMap;
        return this;
    }

    public Map<String, CustomType> getCustomValueMap() {
        return customValueMap;
    }

    public CustomConverterBean setCustomValueMap(Map<String, CustomType> customValueMap) {
        this.customValueMap = customValueMap;
        return this;
    }

    public List<CustomType> getCustomList() {
        return customList;
    }

    public CustomConverterBean setCustomList(List<CustomType> customList) {
        this.customList = customList;
        return this;
    }

    public Set<CustomType> getCustomSet() {
        return customSet;
    }

    public CustomConverterBean setCustomSet(Set<CustomType> customSet) {
        this.customSet = customSet;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CustomConverterBean that = (CustomConverterBean) o;
        return Objects.equals(id, that.id) && Objects.equals(localDate, that.localDate) && Objects.equals(stringsMap, that.stringsMap) && Objects.equals(customKeyMap, that.customKeyMap) && Objects.equals(customValueMap, that.customValueMap) && Objects.equals(customList, that.customList) && Objects.equals(customSet, that.customSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, localDate, stringsMap, customKeyMap, customValueMap, customList, customSet);
    }
}